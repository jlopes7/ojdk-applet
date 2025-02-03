#include "oplauncher.h"

#include "jvm_launcher.h"

extern char *applet_policy_filepath;

// Function to launch the JVM (cross-platform)
void launch_jvm(const char *class_name, const char *jar_path, const char *params) {
    char command[BUFFER_SIZE];

    // TODO: Implement
}


returncode_t load_applet(const char *op, const char *className, const char *appletName, const char *archiveUrl,
						 const char *baseUrl, const char *codebase, const char *height, const char *width,
						 const char *cookies, const char *parameters, double posx, double posy) {
	int param_counter = 0;
	char additional_params[BUFFER_SIZE];
	char *applet_params[OPLAUNCHER_PROTO_MAXPARAMS];
	_MEMZERO(applet_params, OPLAUNCHER_PROTO_MAXPARAMS * sizeof(char));

	applet_params[param_counter++] = op;
	applet_params[param_counter++] = baseUrl;
	applet_params[param_counter++] = codebase;
	applet_params[param_counter++] = archiveUrl;
	applet_params[param_counter++] = appletName;

	_MEMZERO(additional_params, BUFFER_SIZE * sizeof(char));
	/// Add the additional parameters
	snprintf(additional_params, BUFFER_SIZE, "width=%s;height=%s;posx=%.4f;posy=%.4f", width, height, posx, posy);
	if ( parameters != NULL ) {
		snprintf(additional_params, BUFFER_SIZE, "%s;%s", parameters, additional_params);
	}
	applet_params[param_counter++] = additional_params;

	/// APPLET CLASS NAME
	applet_params[param_counter++] = className;

	return trigger_applet_execution(className, applet_params, param_counter );
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
