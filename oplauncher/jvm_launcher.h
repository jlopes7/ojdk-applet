#ifndef _JVM_LAUNCHER_H
#define _JVM_LAUNCHER_H

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <io.h>

#include "utils.h"

#define CL_APPLET_CLASSLOADER	"org/oplauncher/AppletClassLoader"

#ifdef _WIN32
    #include <io.h> // For access() on Windows
    #ifndef F_OK
        #define F_OK 0 // Test for file existence
    #endif
#endif


typedef struct {
    JavaVM *jvm;
    JNIEnv *env;
} jvm_launcher_t;

void get_executable_directory(char *buffer, size_t size);
int jvm_launcher_init(const char *class_name);
void jvm_launcher_terminate(void);
void trigger_applet_execution(const char *class_name, data_tuplet_t *params);

#endif
