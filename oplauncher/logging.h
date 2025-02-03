#ifndef _LOGGING_H
#define _LOGGING_H

#include "utils.h"
#include <stdio.h>

typedef struct {
    char *log_file;
    char *log_dir;
    char *level;
    FILE *log_file_fp;
} logging_t;

typedef enum {
    LOGGING_NORMAL,
    LOGGING_ERROR
} loglevel_t;

returncode_t logging_init(void);
returncode_t logmsg(loglevel_t lvl, const char *format, ...);
returncode_t logging_end(void);

#endif //LOGGING_H
