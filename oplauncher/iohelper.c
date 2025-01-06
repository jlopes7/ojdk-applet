#include "iohelper.h"

#include "deps/cJSON.h"

#if defined(_WIN32) || defined(_WIN64)
#include <stdio.h>
#include <wincrypt.h>

returncode_t decode_base64(const char *base64, unsigned char **decodedData, DWORD *decodedLength) {
    // Determine the required buffer size for the decoded data
    if (!CryptStringToBinaryA(base64, 0, CRYPT_STRING_BASE64, NULL, decodedLength, NULL, NULL)) {
        char errMsg[BUFFER_SIZE];
        snprintf(errMsg, BUFFER_SIZE, "Error in determining buffer size: %lu", GetLastError());
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_FAILED_B64_BUFFERSZ);
        return RC_ERR_FAILED_B64_BUFFERSZ;
    }

    // Allocate buffer for encoded data
    PTR(decodedData) = (char *)malloc(PTR(decodedLength));
    if (PTR(decodedData) == NULL) {
        char *errMsg = "Memory allocation failed";
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_MEMORY_ALLOCATION_FAILED);
        return RC_ERR_MEMORY_ALLOCATION_FAILED;
    }

    // Decode Base64
    if (!CryptStringToBinaryA(base64, 0, CRYPT_STRING_BASE64, PTR(decodedData), decodedLength, NULL, NULL)) {
        char errMsg[BUFFER_SIZE];
        snprintf(errMsg, BUFFER_SIZE, "Error in encoding base64: %lu", GetLastError());
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_ENCDEC_B64);
        return RC_ERR_ENCDEC_B64;
    }

    return EXIT_SUCCESS; // Success
}
#else
returncode_t decode_base64(const char *base64, unsigned char **decodedData) {
    // TODO: Implement

    return EXIT_SUCCESS;
}
#endif

/**
 * Function to save decoded data to a file
 */
returncode_t save_decoded_data_to_file(const char *filePath, const unsigned char *data, DWORD length) {
    FILE *file = fopen(filePath, "wb");
    if (!file) {
        char errMsg[BUFFER_SIZE];
        snprintf(errMsg, BUFFER_SIZE, "Failed to open file for writing: %", filePath);
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_IO_FILEOPEN_FAILED);
        return RC_ERR_IO_FILEOPEN_FAILED;
    }

    size_t written = fwrite(data, 1, length, file);
    if (written != length) {
        char *errMsg = "Failed to write all data to file";
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_IO_READWRITE_FAILED);
        fclose(file);
        return RC_ERR_IO_READWRITE_FAILED;
    }

    fclose(file);
    return EXIT_SUCCESS;
}

/**
 * Main function to parse JSON, decode Base64, and save the file
 */
returncode_t process_base64_file_from_json(const char *jsonString, const char *outputFilePath) {
    unsigned char *decodedData = NULL;
    DWORD decodedLength = 0;
    cJSON *json = cJSON_Parse(jsonString);

    fprintf(stderr, "About to save the Applet bits to: %s\n", outputFilePath);

    if (!json) {
        char *errMsg = "Error parsing JSON";
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_COULDNOT_PARSEJSON);
        return RC_ERR_COULDNOT_PARSEJSON;
    }

    cJSON *base64DataItem = cJSON_GetObjectItem(json, "fileData");
    if (!cJSON_IsString(base64DataItem)) {
        char *errMsg = "Error: fileData is not a string";
        fprintf(stderr, "%s\n", errMsg);
        sendErrorMessage(errMsg, RC_ERR_COULDNOT_PARSEJSON);
        cJSON_Delete(json);
        return RC_ERR_COULDNOT_PARSEJSON;
    }

    const char *base64Data = PTR(base64DataItem).valuestring;
    returncode_t rc = decode_base64(base64Data, &decodedData, &decodedLength);
    if ( rc != EXIT_SUCCESS ) {
        cJSON_Delete(json);
        return rc;
    }

    rc = save_decoded_data_to_file(outputFilePath, decodedData, decodedLength);
    if ( rc != EXIT_SUCCESS ) {
        free(decodedData);
        cJSON_Delete(json);
        return -1;
    }

    // Clean up
    free(decodedData);
    cJSON_Delete(json);

    return EXIT_SUCCESS;
}
