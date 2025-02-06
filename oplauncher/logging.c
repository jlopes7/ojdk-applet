#include "ini_config.h"
#include "logging.h"

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <time.h>
#include <string.h>

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#else
#include <sys/stat.h>
#include <sys/types.h>
#endif

logging_t *_logging_config;
BOOL _is_initialized = FALSE;

returncode_t logging_init() {
    char log_dir[MAX_LOG_FILE];
    char log_file[MAX_LOG_FILE];
    returncode_t rc;

    _logging_config = (logging_t *)malloc(sizeof(logging_t));
    _MEMZERO(_logging_config, sizeof(logging_t));
    _MEMZERO(log_dir, MAX_LOG_FILE);
    _MEMZERO(log_file, MAX_LOG_FILE);

    read_ini_value(INI_SECTION_LOGGING, INI_SECTION_LOGGING_FILE, log_file, MAX_LOG_FILE);
    rc = get_directory_from_path(log_file, log_dir, MAX_LOG_FILE);
    if ( !_IS_SUCCESS(rc) ) {
        return rc;
    }

#if defined(_WIN32) || defined(_WIN64)
    if (GetFileAttributesA(log_dir) == INVALID_FILE_ATTRIBUTES) {
        if (CREATE_DIR(log_dir) != 0) {
            fprintf(stderr, "Error: Failed to create log directory %s\n", log_dir);
            return RC_ERR_IO_CREATEDIR_FAILED;
        }
    }
#else
    struct stat st;
    if (stat(log_dir, &st) != 0) {  // Check if directory exists
        if (CREATE_DIR(log_dir) != 0) {
            perror("mkdir");
            return RC_ERR_IO_CREATEDIR_FAILED;
        }
    }
#endif

    size_t dirlen = strnlen(log_dir, MAX_LOG_FILE);
    size_t filelen = strnlen(log_file, MAX_LOG_FILE);
    PTR(_logging_config).log_dir = (char*) malloc(dirlen + 1);
    PTR(_logging_config).log_file = (char*) malloc(filelen + 1);
    if (!PTR(_logging_config).log_dir || !PTR(_logging_config).log_file) {
        fprintf(stderr, "Memory allocation failed for logging config.\n");
        return RC_ERR_MEMORY_ALLOCATION_FAILED;
    }

    _MEMZERO(PTR(_logging_config).log_dir, dirlen+1);
    _MEMZERO(PTR(_logging_config).log_file, filelen+1);
#if defined(_WIN32) || defined(_WIN64)
    strncpy_s(PTR(_logging_config).log_dir, dirlen +1, log_dir, dirlen);
    strncpy_s(PTR(_logging_config).log_file, filelen +1, log_file, filelen);
    if (strlen(PTR(_logging_config).log_file) == 0) {
        fprintf(stderr, "Error: Log file path is empty. Check INI file.\n");
        return RC_ERR_INVALID_PATH;
    }
    //fprintf(stdout, "About to create/open the log file: %s\n", PTR(_logging_config).log_file);
    fopen_s(&PTR(_logging_config).log_file_fp, PTR(_logging_config).log_file, "a+");
#else
    strncpy(PTR(_logging_config).log_dir, log_dir, dirlen);
    strncpy(PTR(_logging_config).log_file, log_file, filelen);
    PTR(_logging_config).log_file_fp = fopen(PTR(_logging_config).log_file, "a+");
#endif
    if (!PTR(_logging_config).log_file_fp) {
        fprintf(stderr, "Error: Could not open log file: %s\n", log_file);
        return RC_ERR_IO_OPEN_FAILED;
    }

    _is_initialized = TRUE;

    /** INITIALIZATION MESSAGE **/
    fprintf(PTR(_logging_config).log_file_fp, "-----------------------------------------------------\n");
    fprintf(PTR(_logging_config).log_file_fp, "       OPLAUNCHER PORT SESSION INITIALIZED !\n");
    fprintf(PTR(_logging_config).log_file_fp, "-----------------------------------------------------\n");
    fflush(PTR(_logging_config).log_file_fp);

    return EXIT_SUCCESS;
}

returncode_t logmsg(loglevel_t lvl, const char *format, ...) {
    char timestamp[32];

    if ( !_is_initialized || !PTR(_logging_config).log_file_fp ) {
        //fprintf(stderr, "Logging not initialized. Call logging_init() first.\n");
        return RC_ERR_NOT_INITIALIZED;
    }

    time_t now = time(NULL);
    struct tm *tm_info = localtime(&now);
    strftime(timestamp, sizeof(timestamp), "%Y-%m-%d %H:%M:%S", tm_info);

    // Write log entry
    va_list args;
    va_start(args, format);
    fprintf(PTR(_logging_config).log_file_fp, "[%s] ", timestamp);
    switch (lvl) {
        case LOGGING_NORMAL: {
            fprintf(PTR(_logging_config).log_file_fp, "(INFO) ");
            break;
        }
        case LOGGING_ERROR: {
            fprintf(PTR(_logging_config).log_file_fp, "(ERROR) ");
            break;
        }
    }
    vfprintf(PTR(_logging_config).log_file_fp, format, args);
    fprintf(PTR(_logging_config).log_file_fp, "\n");
    fflush(PTR(_logging_config).log_file_fp);
    va_end(args);

    return EXIT_SUCCESS;
}

returncode_t logging_end() {
    if ( _is_initialized ) {
        free (PTR(_logging_config).log_dir);
        free (PTR(_logging_config).log_file);
        fclose(PTR(_logging_config).log_file_fp);
        free(_logging_config);
    }

    return EXIT_SUCCESS;
}
