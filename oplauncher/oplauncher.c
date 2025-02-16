#include "oplauncher.h"

#include "jvm_launcher.h"
#if defined(_WIN32) || defined(_WIN64)
#	include "oplauncher_win_reg.h"
#endif
#include "ini_config.h"
#include "oplauncher_secur.h"

extern char *applet_policy_filepath;

// Function to launch the JVM (cross-platform)
void launch_jvm(const char *class_name, const char *jar_path, const char *params) {
    char command[BUFFER_SIZE];

    // TODO: Implement
}

returncode_t process_op_tcpip_request(const char *jsonmsg) {
	// TODO: Implement
	return EXIT_SUCCESS;
}

returncode_t read_encryptkey_token(char **token) {
	returncode_t rc;

	int klen = BASE64_ENCODED_LEN(DES3_KEY_SIZE);

	PTR(token) = malloc(klen +1);
	_MEMZERO(token, klen +1);

	rc = read_registry_string(REG_TOKEN, PTR(token), BUFFER_SIZE);
	if ( !_IS_SUCCESS(rc) && RC_REGKEYVAL_DOESNT_EXIST(rc) ) {
		unsigned char *token_tmp;
		rc = generate_des3_key(&token_tmp);
		if ( !_IS_SUCCESS(rc) ) {
			return rc;
		}
		base64_encode_key(token_tmp, token);
		free(token_tmp);

		// Save the token to the registry
		rc = crtupt_registry_value(REG_TOKEN, PTR(token), REG_SZ);
		if ( !_IS_SUCCESS(rc) ) {
			return rc;
		}
	}
	else if (!_IS_SUCCESS(rc)) {
		return rc;
	}
	/// SUCCESS !!!
	else {
		DWORD regdt;
		rc = read_registry_dword(REG_UDATE, &regdt);
		if ( !_IS_SUCCESS(rc) && RC_REGKEYVAL_DOESNT_EXIST(rc) ) {
			DWORD current_date;
			get_now_dword(&current_date);

			rc = crtupt_registry_value(REG_UDATE, &current_date, REG_DWORD);
			if ( !_IS_SUCCESS(rc) ) {
				return rc;
			}
		}
		else {
			int numdays;
			char numdaysstr[INT_MAX_LEN +1];
			_MEMZERO(numdaysstr, INT_MAX_LEN +1);
			_MEMZERO(token, klen +1);

			read_ini_value(INI_SECTION_SECURITY, INI_SECTION_SECUR_PROP_KEYMAXDAYS, numdaysstr, INT_MAX_LEN);
			numdays = atoi( numdaysstr );

			if ( !is_udate_within_numdays(regdt, numdays) ) {
				char *tmptkn;
				DWORD current_date;
				unsigned char *token_tmp;

				get_now_dword(&current_date);

				rc = generate_des3_key(&token_tmp);
				if ( !_IS_SUCCESS(rc) ) {
					return rc;
				}
				base64_encode_key(token_tmp, &tmptkn);
#if defined(_WIN32) || defined(_WIN64)
				strncpy_s(PTR(token), klen+1, tmptkn, strlen(tmptkn));
#else
				strncpy(PTR(token), tmptkn, klen);
#endif
				free(tmptkn);
				free(token_tmp);

				// Save the new token to the registry
				rc = crtupt_registry_value(REG_TOKEN, PTR(token), REG_SZ);
				if ( !_IS_SUCCESS(rc) ) {
					return rc;
				}
				// Save the current date to the registry
				rc = crtupt_registry_value(REG_UDATE, &current_date, REG_DWORD);
				if ( !_IS_SUCCESS(rc) ) {
					return rc;
				}
			}
		}
	}

	return EXIT_SUCCESS;
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
