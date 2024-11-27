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

int chrome_read_message(char *buffer);
void chrome_send_message(const char *message);
void launch_jvm(const char *class_name, const char *jar_path, const char *params);
void process_json(const char *input);

#endif

