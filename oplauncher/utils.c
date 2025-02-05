#include "utils.h"
#include "deps/cJSON.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#include <shlobj.h> // For SHGetFolderPath
#include <corecrt_io.h>
#include <direct.h> // For _mkdir
#include <fcntl.h>  // For O_BINARY
#include <io.h>     // For _setmode
#else
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>
#endif

#include <stdint.h>
#include "logging.h"

returncode_t create_nested_dirs(const char *);

/**
 * Retrieve a formated error message to the stdout
 * @param errorMsg  the error message
 * @param errorCode the error code
 */
returncode_t send_jsonerror_message(const char* errorMsg, const returncode_t errorCode) {
    returncode_t rc;
    // Create the root JSON object
    cJSON *root = cJSON_CreateObject();

    // Add "status" key-value pair
    cJSON_AddStringToObject(root, "status", "error");

    // Add "error" key-value pair
    cJSON_AddStringToObject(root, "error", errorMsg);

    // Add "errorCode" key-value pair
    cJSON_AddNumberToObject(root, "errorCode", errorCode);

    // Convert cJSON object to a JSON-formatted string
    char *jsonString = cJSON_PrintUnformatted(root);
    if (jsonString == NULL) {
        logmsg(LOGGING_ERROR, "Failed to print JSON");
        cJSON_Delete(root);
        return EXIT_FAILURE;
    }

    logmsg(LOGGING_ERROR, "About to send the error JSON to Chrome:\n%s", jsonString);

    // Print the JSON string
    rc = chrome_send_message(errorMsg);

    // Free memory
    cJSON_free(jsonString);  // Use cJSON_free to free the string memory
    cJSON_Delete(root);      // Delete the cJSON object

    return rc;
}

/**
 * 
 * @param message
 * @return 
 */
returncode_t send_jsonsuccess_message(const char* message) {
    returncode_t rc;
    // Create the root JSON object
    cJSON *root = cJSON_CreateObject();

    // Add "status" key-value pair
    cJSON_AddStringToObject(root, "status", "success");

    cJSON_AddStringToObject(root, "message", message);

    // Convert cJSON object to a JSON-formatted string
    char *jsonString = cJSON_PrintUnformatted(root);
    if (jsonString == NULL) {
        logmsg(LOGGING_ERROR, "Failed to print JSON");
        cJSON_Delete(root);
        return EXIT_FAILURE;
    }

    logmsg(LOGGING_NORMAL, "About to send the success JSON message to Chrome:%s", jsonString);

    // Print the JSON string
    rc = chrome_send_message(jsonString);

    // Free memory
    cJSON_free(jsonString);  // Use cJSON_free to free the string memory
    cJSON_Delete(root);      // Delete the cJSON object

    return rc;
}

const char* getOpLauncherCommanderJarFileName() {
    // TODO: Currently hardcoded, change in the future
    return "oplauncher-commander-1.0-beta.jar";
}

#if defined(_WIN32) || defined(_WIN64)
int resolveJNIDllDepsOnEnvVar(const char *relativePath) {
    // Retrieve the Java home directory from the environment variable
    char javaHome[MAX_PATH];
    DWORD length = GetEnvironmentVariable("OPLAUNCHER_JAVA_HOME", javaHome, MAX_PATH);

    if (length == 0) {
        logmsg(LOGGING_ERROR, "OPLAUNCHER_JAVA_HOME environment variable is not set or invalid");
        return RC_ERR_MISSING_OPLAUCH_ENVVAR;
    }

    // Construct the path to the bin directory
    char libPath[MAX_PATH];
    snprintf(libPath, MAX_PATH, "%s/%s", javaHome, relativePath);

    // Use AddDllDirectory to add the Java bin directory
    HMODULE kernel32 = GetModuleHandle("kernel32.dll");
    if (!kernel32) {
        logmsg(LOGGING_ERROR, "Unable to load kernel32.dll");
        return RC_ERR_FILE_LOADSYSLIBS;
    }

    typedef BOOL(WINAPI * AddDllDirectoryProc)(PCWSTR);
    AddDllDirectoryProc addDllDirectory = (AddDllDirectoryProc)GetProcAddress(kernel32, "AddDllDirectory");
    if (!addDllDirectory) {
        logmsg(LOGGING_ERROR, "AddDllDirectory is not supported on this version of Windows.");
        return RC_ERR_FILE_LOADSYSLIBS;
    }

    WCHAR wBinPath[MAX_PATH];
    MultiByteToWideChar(CP_ACP, 0, libPath, -1, wBinPath, MAX_PATH);

    if (!AddDllDirectory(wBinPath)) {
        logmsg(LOGGING_ERROR, "Failed to add DLL directory %s", libPath);
        return RC_ERR_FILE_LOADSYSLIBS;
    }

    logmsg(LOGGING_NORMAL, "Successfully added DLL directory %s", libPath);

    return EXIT_SUCCESS;
}
#endif

returncode_t get_directory_from_path(const char *file_path, char *dir_path, size_t size) {
    if (!file_path || !dir_path) {
        return RC_ERR_INVALID_PARAMETER;
    }

    // Copy input path to a temporary buffer (to avoid modifying original)
    char temp_path[MAXPATHLEN];
    strncpy(temp_path, file_path, sizeof(temp_path) - 1);
    temp_path[sizeof(temp_path) - 1] = '\0';

    // Find the last occurrence of the path separator
    char *last_sep = strrchr(temp_path, PATH_SEPARATOR[0]);
    if (!last_sep) {
        return RC_ERR_INVALID_PATH;
    }

    // Terminate string at the last separator to extract directory
    *last_sep = '\0';

    // Copy the directory path to output
    strncpy(dir_path, temp_path, size - 1);
    dir_path[size - 1] = '\0';

    return EXIT_SUCCESS;
}

void print_hello() {
    // Get the current time
    time_t now = time(NULL);

    // Convert it to local time
    struct tm *local = localtime(&now);

    fprintf(stderr, "+---------------------------------------------------+\n");
    fprintf(stderr, "+      OJDK Applet Plugin Launcher version 1.0b     +\n");
    fprintf(stderr, "+      Standard Output Console                      +\n");
    fprintf(stderr, "+---------------------------------------------------+\n");
    fprintf(stderr, "Launch date and time: %04d-%02d-%02d %02d:%02d:%02d\n",
           PTR(local).tm_year + 1900,   // Year since 1900
           PTR(local).tm_mon + 1,       // Month (0-11, so we add 1)
           PTR(local).tm_mday,          // Day of the month
           PTR(local).tm_hour,          // Hours (24-hour format)
           PTR(local).tm_min,           // Minutes
           PTR(local).tm_sec);          // Seconds
    fprintf(stderr, "\n");
}

char* replace_with_crlf(const char* input) {
    if (!input) return NULL;

    // Calculate the size of the new string
    size_t inputLen = strlen(input);
    size_t newLen = 0;

    for (size_t i = 0; i < inputLen; i++) {
        if (input[i] == '\n') {
            newLen += 2; // Add space for \r\n
        } else {
            newLen += 1;
        }
    }

    // Allocate memory for the new string
    char* output = (char*)malloc(newLen + 1); // +1 for the null terminator
    if (!output) return NULL;
    _MEMZERO(output, newLen + 1);

    // Construct the new string
    size_t j = 0;
    for (size_t i = 0; i < inputLen; i++) {
        if (input[i] == '\n') {
            output[j++] = '\r';
            output[j++] = '\n';
        }
        else {
            output[j++] = input[i];
        }
    }

    output[j] = '\0'; // Null-terminate the string
    return output;
}

/**
 *  Read the initial Applet parameters coming from Chrome
 * @param className the name of the class
 * @param jarPath   the JAR path to read from
 * @param params    the Applet parameters
 */
returncode_t parse_msg_from_chrome(const char *jsonmsg, char **clName, char **jpath, data_tuplet_t *tuplet) {
    //return parse_msg_from_chrome_init(jsonmsg, clName, jpath, NULL, tuplet);
    // TODO: Implement
    return 0;
}

/**
 * Read the initial Applet parameters coming from Chrome
 * @param jsonmsg
 * @param op
 * @param className
 * @param appletName
 * @param archiveUrl
 * @param baseUrl
 * @param codebase
 * @param height
 * @param width
 * @param posx
 * @param posy
 * @param tupletCookies
 * @param parameters
 * @return
 */
returncode_t parse_msg_from_chrome_init(const char *jsonmsg, char **op, char **className, char **appletName,
                                        char **archiveUrl, char **baseUrl, char **codebase,
                                        char **height, char **width, double *posx, double *posy,
                                        data_tuplet_t *tupletCookies, data_tuplet_t *parameters) {
    cJSON   *json_op,
            *json_className,
            *json_appletName,
            *json_archiveUrl,
            *json_baseUrl,
            *json_codebase,
            *json_height,
            *json_width,
            *json_posx,
            *json_posy;

    logmsg(LOGGING_NORMAL, "Parsing the JSON Message: %s", jsonmsg);
    // Parse the JSON string
    cJSON *json = cJSON_Parse(jsonmsg);
    if (json == NULL) {
        const char *error_ptr = cJSON_GetErrorPtr();
        if (error_ptr != NULL) {
            logmsg(LOGGING_ERROR, "JSON Parse Error: %s", error_ptr);
        }
        return RC_ERR_COULDNOT_PARSEJSON;
    }

    // Extract the values
    json_op = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_OP);
    json_className = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_CLZZFILE);
    json_appletName = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_APPLETNAME);
    json_archiveUrl = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_JARFILE);
    json_baseUrl = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_BASEURL);
    json_codebase = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_CODEBASE);
    json_height = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_HEIGHT);
    json_width = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_WIDTH);
    json_posx = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_POSX);
    json_posy = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_POSY);

    // Gets the extracted values
    if (cJSON_IsString(json_op) && (json_op->valuestring != NULL)) {
        PTR(op) = strdup(json_op->valuestring);
    }
    if (cJSON_IsString(json_className) && (json_className->valuestring != NULL)) {
        PTR(className) = strdup(json_className->valuestring);
    }
    if (cJSON_IsString(json_appletName) && (json_appletName->valuestring != NULL)) {
        PTR(appletName) = strdup(json_appletName->valuestring);
    }
    if (cJSON_IsString(json_archiveUrl) && (json_archiveUrl->valuestring != NULL)) {
        PTR(archiveUrl) = strdup(json_archiveUrl->valuestring);
    }
    if (cJSON_IsString(json_baseUrl) && (json_baseUrl->valuestring != NULL)) {
        PTR(baseUrl) = strdup(json_baseUrl->valuestring);
    }
    if (cJSON_IsString(json_codebase) && (json_codebase->valuestring != NULL)) {
        PTR(codebase) = strdup(json_codebase->valuestring);
    }
    if (cJSON_IsString(json_height) && (json_height->valuestring != NULL)) {
        PTR(height) = strdup(json_height->valuestring);
    }
    if (cJSON_IsString(json_width) && (json_width->valuestring != NULL)) {
        PTR(width) = strdup(json_width->valuestring);
    }
    if (cJSON_IsNumber(json_posx)) {
        PTR(posx) = json_posx->valuedouble;
    }
    if (cJSON_IsNumber(json_posy)) {
        PTR(posy) = json_posy->valuedouble;
    }

    // Clean up
    cJSON_Delete(json);

    return EXIT_SUCCESS;
}

/**
 * List and formats all JAR files in a specific folder to a CLASSPATH
 * representation string to be used by to load all necessary classes in the JVM
 *
 * @param directory the directory to search for JAR libraries
 * @param output The output formatted classpath
 * @param size The size of the path
 * @return the error code if something bad happens, "0" otherwise
 */
returncode_t format_get_classpath(const char *directory, char **output, size_t size) {
    PTR(output) = malloc(size * sizeof(char));
    _MEMZERO(PTR(output), size * sizeof(char));
    snprintf(PTR(output), size, ".%s", PATH_DELIMITER); // starts with .;

#if defined(_WIN32) || defined(_WIN64)
    WIN32_FIND_DATA findFileData;
    char searchPath[MAX_PATH];
    _MEMZERO(searchPath, MAX_PATH);
    snprintf(searchPath, MAX_PATH, "%s\\*.jar", directory);

    HANDLE hFind = FindFirstFile(searchPath, &findFileData);
    if (hFind == INVALID_HANDLE_VALUE) {
        logmsg(LOGGING_ERROR, "No JAR files found: in %s\n", directory);
        return RC_ERR_IO_FILENOTFOUND;
    }

    do {
        if (!(findFileData.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
            strncat(PTR(output), directory, size - strlen(PTR(output)) - 1);
            strncat(PTR(output), PATH_SEPARATOR, size - strlen(PTR(output)) - 1);
            strncat(PTR(output), findFileData.cFileName, size - strlen(PTR(output)) - 1);
            strncat(PTR(output), PATH_DELIMITER, size - strlen(PTR(output)) - 1);
        }
    }
    while (FindNextFile(hFind, &findFileData) != 0);

    FindClose(hFind);

#else  // Unix-based systems
    DIR *dir = opendir(directory);
    if (!dir) {
        perror("opendir");
        return RC_ERR_IO_FILENOTFOUND;
    }

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (strstr(entry->d_name, ".jar") != NULL) {
            strncat(PTR(output), directory, size - strlen(PTR(output)) - 1);
            strncat(PTR(output), PATH_SEPARATOR, size - strlen(PTR(output)) - 1);
            strncat(PTR(output), entry->d_name, size - strlen(PTR(output)) - 1);
            strncat(PTR(output), PATH_DELIMITER, size - strlen(PTR(output)) - 1);
        }
    }
    closedir(dir);
#endif

    // Remove trailing semicolon if present
    size_t len = strnlen(PTR(output), size);
    if (len > 1 && (PTR(output)[len - 1] == ';' || PTR(output)[len - 1] == ':')/*Unix of Windows*/) {
        PTR(output)[len - 1] = '\0';
    }

    return EXIT_SUCCESS;
}

returncode_t chrome_read_message_length(uint32_t *message_length) {
#ifdef _WIN32
    _setmode(_fileno(stdin), _O_BINARY);  // Ensure stdin is in binary mode
#endif

    // Read 4 bytes from stdin
    if (fread(message_length, sizeof(uint32_t), 1, stdin) != 1) {
        PTR(message_length) = 0;
        return RC_ERR_CHROME_FAILED_MESSAGE;
    }

    // Convert from little-endian if necessary
/*#if __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    PTR(message_length) = SWAP_ENDIAN(PTR(message_length));
#endif*/

    return EXIT_SUCCESS;
}

/**
 * After the first message is read (with @{chrome_read_message}), continue to read the next comm message from Chrome
 * @param buffer    the placeholder for the chrome message
 * @return the execution code
 */
returncode_t chrome_read_next_message(char *buffer) {
    // TODO: Implement
    return EXIT_SUCCESS;
}

/**
 * Reads the message from Chrome
 */
returncode_t chrome_read_message(char *buffer) {
    uint32_t message_length;
#if defined(_DEBUG_)
    FILE *file = fopen("test_input.bin", "rb");
    if (!file) {
        perror("Failed to open test_input.bin");
        return 1;
    }
    printf("Reading input from test_input.bin...\n");

    if ( !fread(&message_length, 4, 1, file) ) {
        return 0; // End of input
    }
#else
    _MEMZERO(buffer, BUFFER_SIZE);

    returncode_t rc = chrome_read_message_length(&message_length);
    if ( !_IS_SUCCESS(rc) ) {
        logmsg(LOGGING_ERROR, "Failed to read message length: %d. RC: %ld", message_length, rc);
        return rc;
    }
    logmsg(LOGGING_NORMAL, "Chrome native message size received is: %ld bytes", message_length);
    //fread(buffer, BUFFER_SIZE, 1, stdin);
#endif

    if ( (message_length +1) > BUFFER_SIZE - 1 ) {
        logmsg(LOGGING_ERROR, "Message too large: %d > 4KB. Not supported", message_length);
        return RC_ERR_CHROME_MESSAGE_TO_LARGE;
    }

#if defined(_DEBUG_)
    if ( !fread(buffer, message_length, 1, file) ) {
#else
    if (fgets(buffer, message_length +1, stdin)) {
        logmsg(LOGGING_NORMAL, "Received message from Chrome: %s", buffer);
    }
    else {
        char *errMsg = "Failed to read message";
        logmsg(LOGGING_ERROR, errMsg);
        send_jsonerror_message(errMsg,RC_ERR_CHROME_FAILED_MESSAGE );
        return RC_ERR_CHROME_FAILED_MESSAGE;
#endif
    }

    return EXIT_SUCCESS;
}

returncode_t get_oplauncher_home_directory(char *oplauncher_dir, size_t size) {
    _MEMZERO(oplauncher_dir, size);
#if defined(_WIN32) || defined(_WIN64)
    char home_dir[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathA(NULL, CSIDL_PROFILE, NULL, 0, home_dir))) {
        snprintf(oplauncher_dir,size,"%s\\%s",home_dir, OPLAUNCHER_HOME_DIRECTORY_NAME);
    }
    else {
        logmsg(LOGGING_ERROR, "Failed to retrieve home directory.");
        return RC_ERR_MISSING_ENV_VARIABLE;
    }

    if (_access(oplauncher_dir, 0) != 0) {
        if (_mkdir(oplauncher_dir) != 0) {
            logmsg(LOGGING_ERROR, "Failed to create directory: %s", oplauncher_dir);
            return RC_ERR_IO_CREATEDIR_FAILED;
        }
    }
#else
    struct stat st;
    const char *home_dir = getenv("HOME");
    if (!home_dir) {
        struct passwd *pw = getpwuid(getuid());
        home = pw ? pw->pw_dir : NULL;
    }
    if (!home_dir) {
        fprintf(stderr, "Error: HOME environment variable not set.\n");
        return RC_ERR_MISSING_ENV_VARIABLE;
    }
    snprintf(oplauncher_dir,size,"%s\\%s",home_dir, OPLAUNCHER_HOME_DIRECTORY_NAME);
    if (stat(oplauncher_dir, &st) != 0) { // Directory does not exist
        if (mkdir(oplauncher_dir, 0755) != 0) {
            perror("mkdir");
            return RC_ERR_IO_CREATEDIR_FAILED;
        }
    }
#endif

    return EXIT_SUCCESS;
}

/**
 * Sends a message to Chrome
 */
returncode_t chrome_send_message(const char *response) {
    uint32_t message_length = (uint32_t) strnlen(response, BUFFER_SIZE);

    // Ensure little-endian
/*#if __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    message_length = SWAP_ENDIAN(message_length);
#endif*/
    logmsg(LOGGING_NORMAL, "Response size to be sent to Chrome: %ld bytes", message_length);
    logmsg(LOGGING_NORMAL, "Response message to be sent to Chrome: %s", response);

    // Ensure stdout is in binary mode ...Windows workaround...
#ifdef _WIN32
    _setmode(_fileno(stdout), _O_BINARY);
#endif

    if (!fwrite(&message_length, sizeof(uint32_t), 1, stdout)) {
        logmsg(LOGGING_ERROR, "Failed to write message length back to Chrome: %ld", message_length);
        return RC_ERR_CHROME_FAILED_MESSAGE;
    }
    fflush(stdout);
    if (!fwrite(response, message_length, 1, stdout)) {
        logmsg(LOGGING_ERROR, "Failed to write message back to Chrome: %s", response);
        return RC_ERR_CHROME_FAILED_MESSAGE;
    }
    fflush(stdout);

    return EXIT_SUCCESS;
}

/**
 * Serialize the directory path to create nested directories that may not exist
 * @param path the directory structure to be created
 */
returncode_t create_nested_dirs(const char *path) {
#if defined(_WIN32) || defined(__WIN32) && !defined(__CYGWIN__)
    HRESULT result;
    // Validate input
    if (!path || *path == '\0') {
        char errMsg[BUFFER_SIZE];
        snprintf(errMsg, BUFFER_SIZE, "Invalid path: %s", path);
        logmsg(LOGGING_ERROR, errMsg);
        send_jsonerror_message(errMsg, RC_ERR_IO_CREATEDIR_FAILED);
        return RC_ERR_IO_CREATEDIR_FAILED;
    }

    // Create the nested directories
    result = SHCreateDirectoryEx(NULL, path, NULL);
    if (result == ERROR_SUCCESS || result == ERROR_ALREADY_EXISTS) {
        // Directory created or already exists
        return EXIT_SUCCESS;
    } else {
        char errMsg[BUFFER_SIZE];
        snprintf(errMsg, BUFFER_SIZE, "Failed to create directory '%s' (error code: %ld).\n", path, result);
        logmsg(LOGGING_ERROR, errMsg);
        send_jsonerror_message(errMsg, RC_ERR_IO_CREATEDIR_FAILED);
        return RC_ERR_IO_CREATEDIR_FAILED;
    }
#else
    // TODO: Not implemented yet for other platforms
    // TODO: Implement in the future
    return RC_ERR_IO_CREATEDIR_FAILED;
#endif
}

/**
 * Function to create the cache directory if it doesn't exist
 * Wrapper function to @create_nested_dirs
 */
returncode_t create_cache_directory(const char *cache_path) {
    return create_nested_dirs(cache_path);
}

/*
 * Function to construct the path to the cache directory
 */
returncode_t load_cache_path(char *cache_path, size_t max_size) {
#if defined(_WIN32) || defined(_WIN64)
    // Get the user's home directory
    char home_dir[MAX_PATH];
    if (SHGetFolderPath(NULL, CSIDL_PROFILE, NULL, 0, home_dir) != S_OK) {
        const char *errMsg = "Error retrieving home directory";
        logmsg(LOGGING_ERROR, errMsg);
        send_jsonerror_message(errMsg, RC_ERR_MSIO_RETRIEVAL_FAILED);
        return RC_ERR_MSIO_RETRIEVAL_FAILED;
    }

    // Construct the path to the cache directory: $HOME/.oplauncher/cache
    snprintf(cache_path, max_size, "%s\\%s", home_dir, OPLAUNCHER_CACHE_BASELINE_PATH);
#else
    // TODO: Implement
#endif

    return EXIT_SUCCESS;
}
