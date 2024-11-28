#ifndef _UTILS_H
#define _UTILS_H

#include "errcodes.h"

#define BUFFER_SIZE	4096
#define	PTR(X)		(*X)

#if !defined (MAXPATHLEN)
#   define MAXPATHLEN	1024
#endif
#if !defined (MAXARRAYSIZE)
#   define MAXARRAYSIZE	4096 /*4KB arrays only*/
#endif

typedef struct {
    char	*name;
    char	*value;
} data_tuplet_t;

int sendErrorMessage(const char* errorMsg, const int errorCode);
int read_msg_from_chrome(const char *jsonmsg, char **clName, char **jpath, data_tuplet_t *tuplet);
int chrome_read_message(char *buffer);
void chrome_send_message(const char *message);

#endif
