#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "logging.h"
#include "oplauncher_secur.h"

#ifdef _WIN32
#    include <windows.h>
#    include <bcrypt.h>
#    pragma comment(lib, "bcrypt.lib")
#else
#    include <fcntl.h>
#    include <unistd.h>
#endif

const umagicnum_t MAGIC_NUMBER = 142857l;

BOOL check_magicnumber_ratio(const umagicnum_t *givennum) {
    return ( PTR(givennum) % MAGIC_NUMBER ) == 0;
}

/**
 * Generate a random DES3 key
 */
returncode_t generate_des3_key(unsigned char **key) {
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
void base64_encode_key(const unsigned char *input, char **b64str) {
    const char base64_chars[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    int i, j;

    const uint32_t klen = BASE64_ENCODED_LEN(DES3_KEY_SIZE), kdes3len = DES3_KEY_SIZE;
    PTR(b64str) = malloc(klen +1);
    _MEMZERO(PTR(b64str), klen +1);

    for (i = 0, j = 0; i < kdes3len; i += 3) {
        int val = (input[i] << 16) + ((i + 1 < kdes3len) ? (input[i + 1] << 8) : 0) + ((i + 2 < kdes3len) ? input[i + 2] : 0);
        PTR(b64str)[j++] = base64_chars[(val >> 18) & 0x3F];
        PTR(b64str)[j++] = base64_chars[(val >> 12) & 0x3F];
        PTR(b64str)[j++] = (i + 1 < kdes3len) ? base64_chars[(val >> 6) & 0x3F] : '=';
        PTR(b64str)[j++] = (i + 2 < kdes3len) ? base64_chars[val & 0x3F] : '=';
    }
}
