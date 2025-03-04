#ifndef _JVM_LAUNCHER_H
#define _JVM_LAUNCHER_H

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#if defined(_WIN32) || defined(_WIN64)
#include <io.h>
#endif

#include "utils.h"

#define JVM_CP_FOLDERNAME                   "libs"

#define CL_APPLET_CLASSLOADER	            "org/oplauncher/OPLauncherController"
#define CL_APPLET_CLASSLOADER_METHOD        "processLoadAppletOp"
#define CL_APPLET_C2A_METHOD                "processAppletC2A"
#define CL_APPLET_CLASSLOADER_PARAMTYPES    "(Ljava/util/List;)Ljava/lang/String;"

#ifdef _WIN32
#include <io.h> // For access() on Windows
#ifndef F_OK
#define F_OK 0 // Test for file existence
#endif
#endif

typedef struct {
    JavaVM *jvm;
    JNIEnv *env;
    jclass *klazz;
    jobject applet_classloader;
} jvm_launcher_t;

void get_executable_directory(char *buffer, size_t size);
returncode_t jvm_launcher_init(const char *class_name);
void jvm_launcher_terminate(void);
returncode_t trigger_applet_execution(const char *class_name, char **params, int param_count);
returncode_t trigger_applet_operation(opcode_t opcode, char **params, int param_count);

#endif
