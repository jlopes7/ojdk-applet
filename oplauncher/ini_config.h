#ifndef INI_CONFIG_H
#define INI_CONFIG_H

#include "utils.h"

#define INI_FILENAME    "oplauncher.exe.ini"
#define CONFIG_FOLDER   "config"

#define INI_SECTION_JVM        "jvm"
#define INI_SECTION_SECURITY   "security"
#define INI_SECTION_LOGGING    "logging"

#define INI_SECTION_JVM_PROP_HOMEPATH      "home_path"
#define INI_SECTION_JVM_PROP_LIBPATH       "lib_path"
#define INI_SECTION_JVM_PROP_JARPATH       "jar_path"
#define INI_SECTION_JVM_PROP_POLICYFILE    "policy_file"
#define INI_SECTION_JVM_PROP_DEBUG         "debug"

#define INI_SECTION_LOGGING_FILE            "log_file"

void get_config_path(char *config_path, size_t size);
void copy_template_ini(const char *dest_path);
void ensure_ini_exists();
void get_local_directory(char *exec_dir, size_t size);
void read_ini_value(const char *section, const char *key, char *output, size_t output_size);

#endif //INI_CONFIG_H
