#include "oplauncher.h"

extern char *applet_policy_filepath;

// Function to launch the JVM (cross-platform)
void launch_jvm(const char *class_name, const char *jar_path, const char *params) {
    char command[BUFFER_SIZE];

    // TODO: Implement
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
