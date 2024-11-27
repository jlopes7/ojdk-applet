#include <stdio.h>

#include "utils.h"
#include "oplauncher.h"

/**
 * Code Main Execution
 */
int main(void) {
    char buffer[BUFFER_SIZE];

    while (chrome_read_message(buffer)) {
        // Parse the incoming JSON (a simple example without full JSON parsing)
        char class_name[256] = "";
        char jar_path[256] = "";
        char params[256] = "";

        sscanf(buffer, "{\"className\":\"%255[^\"]\",\"jarPath\":\"%255[^\"]\",\"params\":\"%255[^\"]\"}",
               class_name, jar_path, params);

        if (strlen(class_name) > 0 && strlen(jar_path) > 0) {
            launch_jvm(class_name, jar_path, params);
        }
        else {
            chrome_read_message("{\"status\":\"error\",\"error\":\"Invalid message format\"}");
        }
    }

    return EXIT_SUCCESS;
}
