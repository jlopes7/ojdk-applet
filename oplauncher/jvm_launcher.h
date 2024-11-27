#ifndef _JVM_LAUNCHER_H
#define _JVM_LAUNCHER_H

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "utils.h"

#define CL_APPLET_CLASSLOADER	"org/oplauncher/AppletClassLoader"

// Path to the security policy file
char *_applet_policy_filepath_;

void get_executable_directory(char *buffer, size_t size);
void configure_jvm_and_load_applet(const char *class_name, const char *params);

#endif
