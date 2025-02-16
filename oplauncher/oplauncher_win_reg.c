#include <windows.h>
#include <stdio.h>
#include <time.h>

#include "logging.h"
#include "oplauncher_win_reg.h"

returncode_t ensure_registry_path() {
    HKEY hKey;
    returncode_t result = RegCreateKeyEx(HKEY_LOCAL_MACHINE, REG_PATH, 0, NULL, REG_OPTION_NON_VOLATILE, KEY_WRITE, NULL, &hKey, NULL);
    if (result != ERROR_SUCCESS) {
        logmsg(LOGGING_ERROR, "Failed to create/open registry key: %ld\n", result);
        return RC_ERR_CREATEUPT_REGKEY;
    }
    RegCloseKey(hKey);

    return EXIT_SUCCESS;
}

/**
 * Write or update a string or DWORD to the Windows registry
 */
returncode_t crtupt_registry_value(const char *key_name, const void *value, DWORD type) {
    HKEY hKey;
    returncode_t result;

    result = ensure_registry_path();
    if ( !_IS_SUCCESS(result) ) {
        return result;
    }

    logmsg(LOGGING_NORMAL, "Saving the registry key: %s := %s", key_name, value);
    result = RegCreateKeyEx(HKEY_LOCAL_MACHINE, REG_PATH, 0, NULL, REG_OPTION_NON_VOLATILE, KEY_WRITE, NULL, &hKey, NULL);
    if ( !_IS_SUCCESS(result) ) {
        logmsg(LOGGING_ERROR, "Failed to create/open registry key: %ld", result);
        return RC_ERR_CREATEUPT_REGKEY;
    }

    result = RegSetValueEx(hKey, key_name, 0, type, (const BYTE*)value, (type == REG_SZ) ? (DWORD)(strlen((const char*)value) + 1) : sizeof(DWORD));
    RegCloseKey(hKey);

    if ( !_IS_SUCCESS(result) ) {
        logmsg(LOGGING_ERROR, "Failed to set/update registry value: %ld", result);
        return RC_ERR_CREATEUPT_REGKEY;
    }

    return EXIT_SUCCESS;
}

/**
 * Read a string value from the registry
 */
returncode_t read_registry_string(const char *key_name, char *buffer, DWORD buffer_size) {
    HKEY hKey;
    returncode_t result;
    DWORD type = REG_SZ;

    result = RegOpenKeyEx(HKEY_LOCAL_MACHINE, REG_PATH, 0, KEY_READ, &hKey);
    if ( !_IS_SUCCESS(result) ) {
        logmsg(LOGGING_ERROR, "Failed to open registry key: %ld", result);
        return RC_ERR_READ_READ_REGVAL;
    }

    result = RegQueryValueEx(hKey, key_name, NULL, &type, (LPBYTE)buffer, &buffer_size);
    RegCloseKey(hKey);

    if ( !_IS_SUCCESS(result) ) {
        logmsg(LOGGING_ERROR, "Failed to read registry value: %ld", result);
        return RC_ERR_READ_READ_REGVAL;
    }

    return EXIT_SUCCESS;
}

/**
 * Read a DWORD (date) value from the registry
 */
returncode_t read_registry_dword(const char *key_name, DWORD *value) {
    HKEY hKey;
    returncode_t result;
    DWORD type = REG_DWORD;
    DWORD size = sizeof(DWORD);

    result = RegOpenKeyEx(HKEY_LOCAL_MACHINE, REG_PATH, 0, KEY_READ, &hKey);
    if (result != ERROR_SUCCESS) {
        logmsg(LOGGING_ERROR, "Failed to open registry key: %ld", result);
        return RC_ERR_READ_READ_REGVAL;
    }

    result = RegQueryValueEx(hKey, key_name, NULL, &type, (LPBYTE)value, &size);
    RegCloseKey(hKey);

    if (result != ERROR_SUCCESS) {
        printf("Failed to read registry value: %ld\n", result);
        return 1;
    }

    return 0;
}

void get_now_dword(DWORD *dt) {
    time_t now = time(NULL);
    struct tm *tm_now = localtime(&now);
    PTR(dt) = (tm_now->tm_year + 1900) * 10000 + (tm_now->tm_mon + 1) * 100 + tm_now->tm_mday;
}

BOOL is_udate_within_numdays(DWORD stored_date, const uint8_t days) {
    DWORD current_date;
    get_now_dword(&current_date);

    DWORD diff = current_date - stored_date;
    return (diff >= 0 && diff <= days) ? TRUE : FALSE;
}
