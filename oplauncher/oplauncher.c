#include "oplauncher.h"

extern char *_applet_policy_filepath_;

/**
 * Reads the message from Chrome
 */
int chrome_read_message(char *buffer) {
	unsigned int message_length;
	if ( !fread(&message_length, 4, 1, stdin) ) {
		return 0; // End of input
	}

	if ( message_length > BUFFER_SIZE - 1 ) {
		fprintf(stderr, "Message too large. Not supported\n");
		exit( RC_ERR_CHROME_MESSAGE_TO_LARGE );
	}
	if ( !fread(buffer, message_length, 1, stdin) ) {
		fprintf(stderr, "Failed to read message\n");
		exit( RC_ERR_CHROME_FAILED_MESSAGE );
	}

	buffer[message_length] = '\0';
	return 1;
}

/**
 * Sends a message to Chrome
 */
void chrome_send_message(const char *message) {
	unsigned int message_length = strnlen(message, BUFFER_SIZE);
	fwrite(&message_length, 4, 1, stdout);
	fwrite(message, message_length, 1, stdout);
	fflush(stdout);
}

// Function to launch the JVM (cross-platform)
void launch_jvm(const char *class_name, const char *jar_path, const char *params) {
    char command[BUFFER_SIZE];

    #ifdef _WIN32
        snprintf(command, BUFFER_SIZE, "java -cp \"%s\" %s %s", jar_path, class_name, params);
        FILE *fp = _popen(command, "r");
    #else
        snprintf(command, BUFFER_SIZE, "java -cp '%s' %s %s", jar_path, class_name, params);
        FILE *fp = popen(command, "r");
    #endif

    if (fp == NULL) {
        chrome_read_message("{\"status\":\"error\",\"error\":\"Failed to start JVM\"}");
        return;
    }

    char output[BUFFER_SIZE];
    while (fgets(output, sizeof(output), fp) != NULL) {
        chrome_read_message(output);
    }

    #ifdef _WIN32
        int return_code = _pclose(fp);
    #else
        int return_code = pclose(fp);
    #endif

    if (return_code != 0) {
        chrome_read_message("{\"status\":\"error\",\"error\":\"JVM exited with an error\"}");
    } else {
        chrome_read_message("{\"status\":\"success\"}");
    }
}

/**
 * Process the JSON input from Chrome
 */
void process_json(const char *input) {
	cJSON *json = cJSON_Parse(input);

	if (json == NULL) {
		char errJSON[BUFFER_SIZE];
		snprintf (errJSON, BUFFER_SIZE, "{\"status\":\"error\",\"error\":\"Failed to launch JVM\", \"errorCode\": %d}", RC_ERR_JSON_FAIL_READ_MSG);
		chrome_read_message(errJSON);
		return;
	}

// Extract fields
	cJSON *className = cJSON_GetObjectItem(json, "className");
	cJSON *jarPath = cJSON_GetObjectItem(json, "jarPath");
	cJSON *params = cJSON_GetObjectItem(json, "params");

	if ( !cJSON_IsString(className) || !cJSON_IsString(jarPath) ) {
		char errJSON[BUFFER_SIZE];
		snprintf (errJSON, BUFFER_SIZE, "{\"status\":\"error\",\"error\":\"Failed to launch JVM\", \"errorCode\": %d}", RC_ERR_JSON_MISS_REQFIELD);
		chrome_read_message(errJSON);
		cJSON_Delete(json);
		return;
	}

	// Launch JVM (example, use the extracted data)
	char command[BUFFER_SIZE];
	snprintf(command, BUFFER_SIZE, "java -cp \"%s\" %s %s",
	jarPath->valuestring,
	className->valuestring,
	cJSON_IsString(params) ? params->valuestring : "");

    FILE *fp = popen(command, "r");
    if (fp == NULL) {
        chrome_read_message("{\"status\":\"error\",\"error\":\"Failed to launch JVM\"}");
        cJSON_Delete(json);
        return;
    }

    char output[BUFFER_SIZE];
    while (fgets(output, sizeof(output), fp) != NULL) {
        chrome_read_message(output);
    }

    int return_code = pclose(fp);
    if (return_code != 0) {
        chrome_read_message("{\"status\":\"error\",\"error\":\"JVM exited with an error\"}");
    } else {
        chrome_read_message("{\"status\":\"success\"}");
    }

    cJSON_Delete(json);
}

/**
 * MAIN EXECUTION
 */
/*int main() {
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
        } else {
            chrome_read_message("{\"status\":\"error\",\"error\":\"Invalid message format\"}");
        }
    }
    return 0;
}*/

