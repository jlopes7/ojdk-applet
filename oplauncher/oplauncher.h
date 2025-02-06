#ifndef _OPLAUNCHER_H
#define _OPLAUNCHER_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#endif

#include "deps/cJSON.h"
#include "utils.h"

#define OPLAUNCHER_PROTO_MAXPARAMS      7

//int chrome_read_message(char *buffer);
returncode_t load_applet(const char *op, const char *className, const char *appletName, const char *archiveUrl,
                         const char *baseUrl, const char *codebase, const char *height, const char *width,
                         const char *cookies, const char *parameters, double posx, double posy);
void launch_jvm(const char *class_name, const char *jar_path, const char *params);
void process_json(const char *input);

returncode_t process_op_tcpip_request(const char *jsonmsg);

#endif

