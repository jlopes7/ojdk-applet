#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "logging.h"
#include "oplauncher_secur.h"

#ifdef _WIN32
#   include <windows.h>
#   include <bcrypt.h>
#   pragma comment(lib, "bcrypt.lib")
#else
#   include <fcntl.h>
#   include <unistd.h>
#   include <openssl/des.h>
#   include <openssl/rand.h>
#endif

const umagicnum_t MAGIC_NUMBER = 142857l;

BOOL check_magicnumber_ratio(const umagicnum_t *givennum) {
    return ( PTR(givennum) % MAGIC_NUMBER ) == 0;
}

#ifdef _DES3_CUSTOM_IMPL
void simple_des_encrypt(byte_t block[DES3_BLOCK_SIZE], const byte_t key[8]) {
    for (int i = 0; i < DES3_BLOCK_SIZE; i++) {
        block[i] ^= key[i];  // XOR with key (not actual DES, just a placeholder)
    }
}
void simple_des_decrypt(byte_t block[DES3_BLOCK_SIZE], const byte_t key[8]) {
    for (int i = 0; i < DES3_BLOCK_SIZE; i++) {
        block[i] ^= key[i];  // Reverse XOR
    }
}
#endif

returncode_t des3_encrypt(const byte_t *key, const char *plaintext, byte_t **ciphertext, ULONG length) {
#ifdef _DES3_CUSTOM_IMPL

#else
    PTR(ciphertext) = malloc(length +1);
    _MEMZERO(PTR(ciphertext), length + 1);
#ifdef _WIN32
    BCRYPT_ALG_HANDLE hAlgorithm;
    BCRYPT_KEY_HANDLE hKey;
    returncode_t result;

    if ( !_IS_SUCCESS(BCryptOpenAlgorithmProvider(&hAlgorithm, BCRYPT_3DES_ALGORITHM, NULL, 0)) ) {
        logmsg(LOGGING_ERROR, "Failed to open BCryptOpenAlgorithmProvider for DES3 encrypt process");
        return RC_ERR_SECUR_FAILED_DES3_ENCRYPT;
    }
    if ( !_IS_SUCCESS(BCryptGenerateSymmetricKey(hAlgorithm, &hKey, NULL, 0, (PUCHAR)key, DES3_KEY_SIZE, 0)) ) {
        logmsg(LOGGING_ERROR, "Failed to generate the symmetric key for DES3 encrypt process");
        BCryptCloseAlgorithmProvider(hAlgorithm, 0);
        return RC_ERR_SECUR_FAILED_DES3_ENCRYPT;
    }
    result = BCryptEncrypt(hKey, (PUCHAR)plaintext, length, NULL, NULL, 0, PTR(ciphertext), length, &length, 0);
    BCryptDestroyKey(hKey);
    BCryptCloseAlgorithmProvider(hAlgorithm, 0);
    return _IS_SUCCESS(result) ? EXIT_SUCCESS : RC_ERR_SECUR_FAILED_DES3_ENCRYPT;
#else
    DES_cblock key1, key2, key3;
    memcpy(key1, key, 8);
    memcpy(key2, key + 8, 8);
    memcpy(key3, key + 16, 8);
    DES_key_schedule ks1, ks2, ks3;
    DES_set_key_unchecked(&key1, &ks1);
    DES_set_key_unchecked(&key2, &ks2);
    DES_set_key_unchecked(&key3, &ks3);
    for (int i = 0; i < length; i += 8) {
        DES_ecb3_encrypt((DES_cblock *)(PTR(ciphertext) + i), (DES_cblock *)(PTR(ciphertext) + i), &ks1, &ks2, &ks3, DES_ENCRYPT);
    }
#endif
#endif // _DES3_CUSTOM_IMPL
    return EXIT_SUCCESS;
}

returncode_t des3_decrypt(const byte_t *key, const char *b64cypher, byte_t **decrypt_txt) {
    byte_t *cipher_txt;
    ULONG decrypt_len = 0, encrypt_len = 0;

    returncode_t rc = base64_decode(b64cypher, &cipher_txt, &encrypt_len);
#ifdef _DES3_CUSTOM_IMPL
    PTR(decrypt_txt) = malloc(encrypt_len + 1);
    _MEMZERO(PTR(decrypt_txt), encrypt_len + 1);

    for (size_t i = 0; i < encrypt_len; i += DES3_BLOCK_SIZE) {
        byte_t block[DES3_BLOCK_SIZE];
        memcpy(block, cipher_txt + i, DES3_BLOCK_SIZE);

        // Decrypt with third key
        simple_des_decrypt(block, key + 16);
        // Encrypt with second key
        simple_des_encrypt(block, key + 8);
        // Decrypt with first key
        simple_des_decrypt(block, key);

        memcpy(PTR(decrypt_txt) + i, block, DES3_BLOCK_SIZE);
    }
    logmsg(LOGGING_NORMAL, "des3_decrypt: %s", PTR(decrypt_txt));
#else
#if defined(_WIN32) || defined(_WIN64)
    BCRYPT_ALG_HANDLE hAlgorithm;
    BCRYPT_KEY_HANDLE hKey;
    returncode_t result;

    if ( !BCRYPT_SUCCESS(BCryptOpenAlgorithmProvider(&hAlgorithm, BCRYPT_3DES_ALGORITHM, NULL, 0)) ) {
        logmsg(LOGGING_ERROR, "Failed to open BCryptOpenAlgorithmProvider for DES3 decrypt process");
        free(cipher_txt);
        return RC_ERR_SECUR_FAILED_DES3_DECRYPT;
    }
    if (!BCRYPT_SUCCESS(BCryptSetProperty(hAlgorithm, BCRYPT_CHAINING_MODE, BCRYPT_CHAIN_MODE_ECB, sizeof(BCRYPT_CHAIN_MODE_ECB), 0))) {
        logmsg(LOGGING_ERROR, "Failed to set chaining mode to ECB.");
        BCryptCloseAlgorithmProvider(hAlgorithm, 0);
        free(cipher_txt);
        return RC_ERR_SECUR_FAILED_DES3_DECRYPT;
    }

    if ( !BCRYPT_SUCCESS(BCryptGenerateSymmetricKey(hAlgorithm, &hKey, NULL, 0, (PUCHAR)key, DES3_KEY_SIZE, 0)) ) {
        logmsg(LOGGING_ERROR, "Failed to generate the symmetric key for DES3 decrypt process");
        BCryptCloseAlgorithmProvider(hAlgorithm, 0);
        free(cipher_txt);
        return RC_ERR_SECUR_FAILED_DES3_DECRYPT;
    }

    // Retrieves the size of the ciphered data
    BCryptDecrypt(hKey, cipher_txt, encrypt_len, NULL, NULL, 0, NULL, 0, &decrypt_len, 0);

#ifdef _DEBUG
    char debughexpayload[BUFFER_SIZE];
    char debughexkey[BUFFER_SIZE];

    _MEMZERO(debughexpayload, BUFFER_SIZE);
    _MEMZERO(debughexkey, BUFFER_SIZE);
    for (int i = 0; i < decrypt_len; i++) {
        char _tmp[4];
        _MEMZERO(_tmp, 4);
        snprintf(_tmp, MAX_PATH, "%02x ", cipher_txt[i]);
        strcat(debughexpayload, _tmp);
    }
    logmsg(LOGGING_NORMAL, "Encrypted bytes for the payload: %s", debughexpayload);

    for (size_t i = 0; i < DES3_KEY_SIZE; i++) {
        char _tmp[4];
        _MEMZERO(_tmp, 4);
        snprintf(_tmp, MAX_PATH, "%02x ", key[i]);
        strcat(debughexkey, _tmp);
    }
    logmsg(LOGGING_NORMAL, "Encrypted bytes for the key: %s", debughexkey);
#endif

    PTR(decrypt_txt) = malloc(decrypt_len +1);
    if (!PTR(decrypt_txt)) {
        free(cipher_txt);
        return RC_ERR_MEMORY_ALLOCATION;
    }
    _MEMZERO(PTR(decrypt_txt), decrypt_len + 1);

#ifdef _DEBUG
    logmsg(LOGGING_NORMAL, "BCryptDecrypt Input Length before Decrypt: %zu bytes", decrypt_len);
#endif
    if (decrypt_len % 8 != 0) {
        logmsg(LOGGING_ERROR, "Invalid input length for ECB mode: %zu (must be a multiple of 8)", decrypt_len);
        free(cipher_txt);
        return RC_ERR_SECUR_FAILED_DES3_DECRYPT;
    }


    result = BCryptDecrypt(hKey, cipher_txt, encrypt_len, NULL, NULL, 0, (PUCHAR)PTR(decrypt_txt), decrypt_len, &decrypt_len, 0);
    logmsg(LOGGING_NORMAL, "Decrypted output: %s", (char*)PTR(decrypt_txt));

#ifdef _DEBUG_
    logmsg(LOGGING_NORMAL, "BCryptDecrypt Input Length after Decrypt: %zu bytes", decrypt_len);
#endif

    free(cipher_txt);
    BCryptDestroyKey(hKey);
    BCryptCloseAlgorithmProvider(hAlgorithm, 0);

    if (!BCRYPT_SUCCESS(result)) {
        logmsg(LOGGING_ERROR, "BCryptDecrypt failed with error code: 0x%08X", result);
        return RC_ERR_SECUR_FAILED_DES3_DECRYPT;
    }

    // **REMOVE PKCS7 PADDING (NEW CODE)**
    if (decrypt_len > 0) {
        unsigned char pad_value = PTR(decrypt_txt)[decrypt_len - 1];  // Last byte is padding size
        if (pad_value > 0 && pad_value <= 8) {  // PKCS7 padding range
            decrypt_len -= pad_value;
        }
    }
    PTR(decrypt_txt)[decrypt_len] = '\0';  // Null-terminate the cleaned string

#else
    DES_cblock key1, key2, key3;
    memcpy(key1, key, 8);
    memcpy(key2, key + 8, 8);
    memcpy(key3, key + 16, 8);
    DES_key_schedule ks1, ks2, ks3;
    DES_set_key_unchecked(&key1, &ks1);
    DES_set_key_unchecked(&key2, &ks2);
    DES_set_key_unchecked(&key3, &ks3);
    for (int i = 0; i < length; i += 8) {
        DES_ecb3_encrypt((DES_cblock *)(cipher_txt + i), (DES_cblock *)(PTR(decrypt_txt) + i), &ks1, &ks2, &ks3, DES_DECRYPT);
    }
#endif
#endif // _DES3_CUSTOM_IMPL
    return EXIT_SUCCESS;
}

BOOL is_sandbox_active(const char* stat) {
    return strncmp(stat, "yes", MAX_PATH) == 0 || strncmp(stat, "true", MAX_PATH) == 0
            || strncmp(stat, "y", MAX_PATH) == 0 || strncmp(stat, "yep", MAX_PATH) == 0;
}

/**
 * Generate a random DES3 key
 */
returncode_t generate_des3_key(byte_t **key) {
    const uint32_t klen = DES3_KEY_SIZE +1;
    PTR(key) = malloc(klen);
    _MEMZERO(PTR(key), klen);

#if defined(_WIN32) || defined(_WIN64)
    if (BCryptGenRandom(NULL, PTR(key), DES3_KEY_SIZE, BCRYPT_USE_SYSTEM_PREFERRED_RNG) != 0) {
        logmsg(LOGGING_ERROR, "Failed to generate random DES3 key with size %d bytes", DES3_KEY_SIZE);
        return RC_ERR_SECUR_GENRAND_DES3_KEY;
    }
#else
    int fd = open("/dev/urandom", O_RDONLY);
    if ( fd < 0 || read(fd, PTR(key), DES3_KEY_SIZE) != DES3_KEY_SIZE) {
        logmsg(LOGGING_ERROR, "Failed to generate random DES3 key.");
        if (fd >= 0) close(fd);
        return RC_ERR_SECUR_GENRAND_DES3_KEY;
    }

    close(fd);
#endif
    return EXIT_SUCCESS;
}

/**
 * Encode a binary key to Base64.
 * Simple alg. copied over and over and over and... over again from the internet ;) ... simple byte masking
 */
void base64_encode(const byte_t *input, char **b64str) {
    int i, j;

    const uint32_t klen = BASE64_ENCODED_LEN(DES3_KEY_SIZE), kdes3len = DES3_KEY_SIZE;
    PTR(b64str) = malloc(klen +1);
    _MEMZERO(PTR(b64str), klen +1);

    for (i = 0, j = 0; i < kdes3len; i += 3) {
        int val = (input[i] << 16) + ((i + 1 < kdes3len) ? (input[i + 1] << 8) : 0) + ((i + 2 < kdes3len) ? input[i + 2] : 0);
        PTR(b64str)[j++] = BASE64_CHARACTERS[(val >> 18) & 0x3F];
        PTR(b64str)[j++] = BASE64_CHARACTERS[(val >> 12) & 0x3F];
        PTR(b64str)[j++] = (i + 1 < kdes3len) ? BASE64_CHARACTERS[(val >> 6) & 0x3F] : '=';
        PTR(b64str)[j++] = (i + 2 < kdes3len) ? BASE64_CHARACTERS[val & 0x3F] : '=';
    }
}

returncode_t base64_decode(const char *input, byte_t **output, DWORD *output_len) {
#if defined(_WIN32) || defined(_WIN64)
    if (!CryptStringToBinaryA(input, 0, CRYPT_STRING_BASE64, NULL, output_len, NULL, NULL)) {
        logmsg(LOGGING_ERROR, "Failed to get decoded message length.");
        return RC_ERR_DECODE_MSG_ERROR;
    }

    PTR(output) = malloc(PTR(output_len) + 1);
    if (!PTR(output)) {
        logmsg(LOGGING_ERROR, "Memory allocation failed for the base64 decode.");
        return RC_ERR_MEMORY_ALLOCATION;
    }
    _MEMZERO(PTR(output), PTR(output_len) + 1);

    if (!CryptStringToBinaryA(input, 0, CRYPT_STRING_BASE64, PTR(output), output_len, NULL, NULL)) {
        logmsg(LOGGING_ERROR, "Failed to decode base64 string.");
        return RC_ERR_DECODE_MSG_ERROR;
    }
#else
    int len = strlen(input);
    int i, j, val = 0, valb = -8;

    PTR(output) = malloc(PTR(output_len) + 1);
    _MEMZERO(PTR(output), PTR(output_len) + 1);

    for (i = 0, j = 0; i < len; i++) {
        char *p = strchr(BASE64_CHARACTERS, input[i]);
        if (p) {
            val = (val << 6) + (p - BASE64_CHARACTERS);
            valb += 6;
            if (valb >= 0) {
                output[j++] = (val >> valb) & 0xFF;
                valb -= 8;
            }
        }
    }
    PTR(output_len) = j;
#endif

    return EXIT_SUCCESS;
}
