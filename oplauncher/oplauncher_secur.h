#ifndef OPLAUNCHER_SECUR_H
#define OPLAUNCHER_SECUR_H

#include "utils.h"

#define DES3_KEY_SIZE             24
#define BASE64_ENCODED_LEN(X)     (((X + 2) / 3) * 4 + 1)
#define INITOKEN_ACTIVE(X)        (((X) & 0x000000ff) == 0x000000ff)
#define BASE64_CHARACTERS         "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
#ifdef _DES3_CUSTOM_IMPL
#define DES3_BLOCK_SIZE            8 /*DES block size*/
#endif

void base64_encode(const byte_t *input, char **b64str);
returncode_t base64_decode(const char *input, byte_t **output, DWORD *output_len);
returncode_t generate_des3_key(byte_t **key);

returncode_t des3_encrypt(const byte_t *key, const char *plaintext, byte_t **ciphertext, ULONG length);
returncode_t des3_decrypt(const byte_t *key, const char *b64cypher, byte_t **decrypt_txt);

BOOL check_magicnumber_ratio(const umagicnum_t *givennum);
BOOL is_sandbox_active(const char* stat);

#endif //OPLAUNCHER_SECUR_H
