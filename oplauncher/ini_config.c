#include "ini_config.h"

#include <logging.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
#include <shlobj.h>
#else
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <pwd.h>
#endif

void get_config_path(char *config_path, size_t size) {
#if defined(_WIN32) || defined(_WIN64)
    char home_path[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPath(NULL, CSIDL_PROFILE, NULL, 0, home_path))) {
        snprintf(config_path, size, "%s\\.oplauncher\\%s", home_path, INI_FILENAME);
    }
    else {
        logmsg(LOGGING_ERROR, "Failed to get home directory.");
        exit(RC_ERR_MISSING_ENV_VARIABLE);
    }
#else
    const char *home = getenv("HOME");
    if (!home) {
        struct passwd *pw = getpwuid(getuid());
        home = pw ? pw->pw_dir : NULL;
    }
    if (!home) {
        logmsg(LOGGING_ERROR, "Failed to get home directory.");
        exit(1);
    }
    snprintf(config_path, size, "%s/.oplauncher/%s", home, INI_FILENAME);
#endif
}

void ensure_ini_exists() {
    char config_path[BUFFER_SIZE];
    get_config_path(config_path, BUFFER_SIZE);

    // Just open to verify if the file exists
    FILE *file = fopen(config_path, "r");
    if (!file) {
        logmsg(LOGGING_NORMAL, "INI file not found, copying default template...");
        copy_template_ini(config_path);
    }
    else {
        fclose(file);
    }
}

void get_local_directory(char *exec_dir, size_t size) {
#ifdef _WIN32
    GetModuleFileNameA(NULL, exec_dir, size);
    char *last_backslash = strrchr(exec_dir, '\\');
    if (last_backslash) {
        *last_backslash = '\0';  // Remove executable name, leaving directory
    }
#else
    ssize_t len = readlink("/proc/self/exe", exec_dir, size - 1);
    if (len != -1) {
        exec_dir[len] = '\0';
        char *dir = dirname(exec_dir);
        strncpy(exec_dir, dir, size);
    }
    else {
        perror("readlink");
        exit(EXIT_FAILURE);
    }
#endif
}

void get_template_path(char *template_path, size_t size) {
    char exec_dir[BUFFER_SIZE];
    get_local_directory(exec_dir, BUFFER_SIZE);

#if defined(_WIN32) || defined(_WIN64)
    snprintf(template_path, size, "%s\\%s\\%s", exec_dir, CONFIG_FOLDER, INI_FILENAME);
#else
    snprintf(template_path, size, "%s/%s/%s", exec_dir, CONFIG_FOLDER, INI_FILENAME);
#endif
}

void copy_template_ini(const char *dest_path) {
    char template_path[BUFFER_SIZE];
    get_template_path(template_path, BUFFER_SIZE);

    FILE *src = fopen(template_path, "r");
    if (!src) {
        logmsg(LOGGING_ERROR, "Template INI file not found at %s", template_path);
        return;
    }

    FILE *dest = fopen(dest_path, "w");
    if (!dest) {
        logmsg(LOGGING_ERROR, "Error: Failed to create INI file at %s", dest_path);
        fclose(src);
        return;
    }

    char buffer[BUFFER_SIZE];
    size_t bytes;
    while ((bytes = fread(buffer, 1, BUFFER_SIZE, src)) > 0) {
        fwrite(buffer, 1, bytes, dest);
        fflush(dest);
    }

    fclose(src);
    fclose(dest);
    logmsg(LOGGING_NORMAL, "Successfully copied the template INI file to: %s", dest_path);
}

void read_ini_value(const char *section, const char *key, char *output, size_t output_size) {
    FILE *file;
    char config_path[BUFFER_SIZE];
    char line[BUFFER_SIZE];
    int in_section = 0;

    ensure_ini_exists();

    get_config_path(config_path, BUFFER_SIZE);

    logmsg(LOGGING_NORMAL, "Reading INI configuration [SECT(%s)->KEY(%s)] from the file: %s", section, key, config_path);
#if defined(_WIN32) || defined(_WIN64)
    errno_t err = fopen_s(&file, config_path, "r");
    if( err != 0 ) {
        logmsg(LOGGING_ERROR, "The file %s could not be opened. Error code: %d", config_path, err);
    }
#else
    file = fopen(config_path, "r");
    if (!file) {
        logmsg(LOGGING_ERROR, "Could not open INI file: %s", config_path);
        return;
    }
#endif

    while (fgets(line, sizeof(line), file)) {
        // Trim newline and spaces
        line[strcspn(line, LINE_BREAK)] = 0;

        // Check if line is a section
        if (line[0] == '[') {
            in_section = (strncmp(line + 1, section, strlen(section)) == 0);
        }
        // Process key-value pairs inside the section
        else if (in_section && strstr(line, key) == line) {
            char *value = strchr(line, '=');
            if (value) {
                size_t len = output_size - 1;
                value++;
                while (*value == ' ') value++; // Trim leading spaces
#if defined(_WIN32) || defined(_WIN64)
                strncpy_s(output, len, value, len);
#else
                strncpy(output, value, len);
#endif
                output[len] = '\0';
                break;
            }
        }
    }

    fclose(file);
}
