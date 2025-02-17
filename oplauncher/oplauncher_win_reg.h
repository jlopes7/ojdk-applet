#ifndef OPLAUNCHER_WIN_REG_H
#define OPLAUNCHER_WIN_REG_H

#include "utils.h"

#define REG_PATH      "SOFTWARE\\OPLauncher"
#define REG_TOKEN     "Token"
#define REG_INITOKEN  "IniToken"
#define REG_USEINITKN "UseIniToken"
#define REG_UDATE     "UDate"

#define RC_REGKEYVAL_DOESNT_EXIST(X)    ((X) == RC_ERR_READ_READ_REGVAL)

returncode_t crtupt_registry_value(const char *key_name, const void *value, DWORD type);
returncode_t read_registry_string(const char *key_name, char *buffer, DWORD buffer_size);
returncode_t read_registry_dword(const char *key_name, DWORD *value);
BOOL is_udate_within_numdays(DWORD stored_date, const uint8_t days);
void get_now_dword(DWORD *dt);

#endif //OPLAUNCHER_WIN_REG_H
