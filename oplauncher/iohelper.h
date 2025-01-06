#ifndef _IOHELPER_H
#define _IOHELPER_H

#include <windows.h>
#include "utils.h"

returncode_t decode_base64(const char *base64, unsigned char **decodedData, DWORD *decodedLength);
returncode_t save_decoded_data_to_file(const char *filePath, const unsigned char *data, DWORD length);
returncode_t process_base64_file_from_json(const char *jsonString, const char *outputFilePath);

#endif //_IOHELPER_H
