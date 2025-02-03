#ifndef _UTILS_H
#define _UTILS_H

#include "errcodes.h"
#include "chrome_ext_comm.h"
#include <stdio.h>

#define BUFFER_SIZE	    0x00001000 /*4K*/
#define MID_BUFFER_SIZE	0x00008000 /*32K*/
#define MAX_BUFFER_SIZE	0x00100000 /*1M*/
#define MAX_LOG_FILE    0x00000400 /*1K*/
#define	PTR(X)		(*X)

#if !defined (MAXPATHLEN)
#   define MAXPATHLEN	MAX_LOG_FILE
#endif
#if !defined (MAXARRAYSIZE)
#   define MAXARRAYSIZE	BUFFER_SIZE /*4KB arrays only*/
#endif

#if !defined (WIN32)
#   ifndef BOOL
#       define BOOL unsigned short
#   endif
#endif

#ifndef TRUE
#define TRUE	1
#endif
#ifndef FALSE
#define FALSE	0
#endif

#define OPLAUNCHER_HOME_DIRECTORY_NAME  ".oplauncher"
#define OPLAUNCHER_CACHE_BASELINE_PATH	".oplauncher\\cache"

#define _IS_SUCCESS(x)      ((x) == EXIT_SUCCESS)
#if defined(_WIN32) || defined(_WIN64)
#   include <windows.h>  // Windows Sleep function
#   include <direct.h>
#   define SLEEP_S(ms) Sleep((ms) * 1000)
#   define SLEEP_MS(ms) Sleep((ms))
#   define CREATE_DIR(path) _mkdir(path)
#else
#   include <unistd.h>   // Unix sleep function
#   define SLEEP_S(ms) usleep((ms) * 1000 * 1000)
#   define SLEEP_MS(ms) usleep((ms) * 1000)
#   define CREATE_DIR(path) mkdir(path, 0775)
#endif

#ifdef _WIN32
    #define popen _popen
    #define pclose _pclose
#endif

#if defined(_WIN32) || defined(_WIN64)
#   define LINE_BREAK      "\r\n"
#   define PATH_SEPARATOR   "\\"
#   define PATH_DELIMITER   ";"
#else
#   define LINE_BREAK      "\n"
#   define PATH_SEPARATOR "/"
#   define PATH_DELIMITER   ";"
#endif

#define _MEMZERO(var, size) memset((var), 0, (size))

typedef struct {
    char	*name;
    char	*value;
} data_tuplet_t;

typedef unsigned short returncode_t;

int sendErrorMessage(const char* errorMsg, const int errorCode);
returncode_t parse_msg_from_chrome_init(const char *jsonmsg, char **op, char **className, char **appletName,
                                        char **archiveUrl, char **baseUrl, char **codebase,
                                        char **height, char **width, double *posx, double *posy,
                                        data_tuplet_t *tupletCookies, data_tuplet_t *parameters);
returncode_t parse_msg_from_chrome(const char *jsonmsg, char **clName, char **jpath, data_tuplet_t *tuplet);
returncode_t get_oplauncher_home_directory(char *oplauncher_dir, size_t size);
returncode_t get_directory_from_path(const char *file_path, char *dir_path, size_t size);

int chrome_read_message(char *buffer);
void chrome_send_message(const char *message);
char* replace_with_crlf(const char* input);
const char* getOpLauncherCommanderJarFileName(void);

void print_hello(void);

returncode_t format_get_classpath(const char *directory, char **output, size_t size);
returncode_t create_cache_directory(const char *cache_path);
returncode_t load_cache_path(char *cache_path, size_t max_size);

#if defined(_WIN32) || defined(_WIN64)
int resolveJNIDllDepsOnEnvVar(const char *relativePath);
#endif

#endif
