#ifndef OPLAUNCHER_SECUR_H
#define OPLAUNCHER_SECUR_H

#include "utils.h"

#define DES3_KEY_SIZE             24
#define BASE64_ENCODED_LEN(X)     (((X + 2) / 3) * 4 + 1)

void base64_encode_key(const unsigned char *input, char **b64str);
returncode_t generate_des3_key(unsigned char **key);
BOOL check_magicnumber_ratio(const umagicnum_t *givennum);

#endif //OPLAUNCHER_SECUR_H
