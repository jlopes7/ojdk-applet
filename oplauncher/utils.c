#include "utils.h"
#include "deps/cJSON.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#include <shlobj.h> // For SHGetFolderPath
#endif

#include <direct.h> // For _mkdir

returncode_t create_nested_dirs(const char *);

/**
 * Retrieve a formated error message to the stdout
 * @param errorMsg  the error message
 * @param errorCode the error code
 */
int sendErrorMessage(const char* errorMsg, const int errorCode) {
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
        fprintf(stderr, "Failed to print JSON\n");
        cJSON_Delete(root);
        return EXIT_FAILURE;
    }

    // Print the JSON string
    chrome_send_message(jsonString);

    // Free memory
    cJSON_free(jsonString);  // Use cJSON_free to free the string memory
    cJSON_Delete(root);      // Delete the cJSON object

    return EXIT_SUCCESS;
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
        fprintf(stderr, "Error: OPLAUNCHER_JAVA_HOME environment variable is not set or invalid.\n");
        return RC_ERR_MISSING_OPLAUCH_ENVVAR;
    }

    // Construct the path to the bin directory
    char libPath[MAX_PATH];
    snprintf(libPath, MAX_PATH, "%s/%s", javaHome, relativePath);

    // Use AddDllDirectory to add the Java bin directory
    HMODULE kernel32 = GetModuleHandle("kernel32.dll");
    if (!kernel32) {
        fprintf(stderr, "Error: Unable to load kernel32.dll\n");
        return RC_ERR_FILE_LOADSYSLIBS;
    }

    typedef BOOL(WINAPI * AddDllDirectoryProc)(PCWSTR);
    AddDllDirectoryProc addDllDirectory = (AddDllDirectoryProc)GetProcAddress(kernel32, "AddDllDirectory");
    if (!addDllDirectory) {
        fprintf(stderr, "Error: AddDllDirectory is not supported on this version of Windows.\n");
        return RC_ERR_FILE_LOADSYSLIBS;
    }

    WCHAR wBinPath[MAX_PATH];
    MultiByteToWideChar(CP_ACP, 0, libPath, -1, wBinPath, MAX_PATH);

    if (!AddDllDirectory(wBinPath)) {
        fprintf(stderr, "Error: Failed to add DLL directory %s\n", libPath);
        return RC_ERR_FILE_LOADSYSLIBS;
    }

    fprintf(stderr, "DLL directory added: %s\n", libPath);

    return EXIT_SUCCESS;
}
#endif

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
    memset(output, 0, newLen + 1);

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
    return parse_msg_from_chrome_init(jsonmsg, clName, jpath, NULL, tuplet);
}

/**
 *  Read the initial Applet parameters coming from Chrome
 * @param className the name of the class
 * @param jarPath   the JAR path to read from
 * @param type      the type to save to the FS, e.g., either Applet class or Jar file
 * @param params    the Applet parameters
 */
returncode_t parse_msg_from_chrome_init(const char *jsonmsg, char **clName, char **jpath, char **type, data_tuplet_t *tuplet) {
    cJSON   *className,
            *jarPath,
            *params,
            *fileType;
    // Parse the JSON string
    cJSON *json = cJSON_Parse(jsonmsg);
    if (json == NULL) {
        const char *error_ptr = cJSON_GetErrorPtr();
        if (error_ptr != NULL) {
            fprintf(stderr, "Error before: %s\n", error_ptr);
        }
        return RC_ERR_COULDNOT_PARSEJSON;
    }

    // Extract the values
    className = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_CLZZFILE);
    jarPath = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_JARFILE);
    fileType = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_FILETYPE);
    params = cJSON_GetObjectItemCaseSensitive(json, CHROME_EXT_MSG_PARAMETERS);

    // Print the extracted values
    if (cJSON_IsString(className) && (className->valuestring != NULL)) {
        PTR(clName) = strdup(className->valuestring);
    }
    if (cJSON_IsString(jarPath) && (jarPath->valuestring != NULL)) {
        PTR(jpath) = strdup(jarPath->valuestring);
    }
    if ( type != NULL && cJSON_IsString(fileType) && (fileType->valuestring != NULL) ) {
        PTR(type) = strdup(fileType->valuestring);
    }
    // Process the params array
    if (params != NULL && cJSON_IsArray(params)) {
        cJSON *param = NULL;

        const int array_size = cJSON_GetArraySize(params);
        for (int i = 0; i < array_size; i++) {
            cJSON *param = cJSON_GetArrayItem(params, i); // Get each object in the array
            if (cJSON_IsObject(param)) {
                cJSON *key_value = NULL;
                cJSON_ArrayForEach(key_value, param) { // Iterate over key-value pairs in the object
                    if (cJSON_IsString(key_value)) {
                        tuplet[i].name  = strdup(key_value->string);
                        tuplet[i].value = strdup(key_value->valuestring);
                    }
                }
            }
        }
    }

    // Clean up
    cJSON_Delete(json);

    return EXIT_SUCCESS;
}

/**
 * Reads the message from Chrome
 */
int chrome_read_message(char *buffer) {
    unsigned int message_length;
    if ( !fread(&message_length, 4, 1, stdin) ) {
        return 0; // End of input
    }

    if ( message_length > BUFFER_SIZE - 1 ) {
        char *errMsg = "Message too large. Not supported";
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg,RC_ERR_CHROME_MESSAGE_TO_LARGE );

        return RC_ERR_CHROME_MESSAGE_TO_LARGE;
    }
    if ( !fread(buffer, message_length, 1, stdin) ) {
        char *errMsg = "Failed to read message";
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg,RC_ERR_CHROME_FAILED_MESSAGE );
        return RC_ERR_CHROME_FAILED_MESSAGE;
    }

    buffer[message_length] = '\0';

    return EXIT_SUCCESS;
}

/**
 * Sends a message to Chrome
 */
void chrome_send_message(const char *message) {
    unsigned int message_length = strnlen(message, BUFFER_SIZE);
    fwrite(&message_length, 4, 1, stdout);
    fwrite(message, message_length, 1, stdout);
    fflush(stdout);
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
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_IO_CREATEDIR_FAILED);
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
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_IO_CREATEDIR_FAILED);
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
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_MSIO_RETRIEVAL_FAILED);
        return RC_ERR_MSIO_RETRIEVAL_FAILED;
    }

    // Construct the path to the cache directory: $HOME/.oplauncher/cache
    snprintf(cache_path, max_size, "%s\\%s", home_dir, OPLAUNCHER_CACHE_BASELINE_PATH);
#else
    // TODO: Implement
#endif

    return EXIT_SUCCESS;
}
