#include <stdio.h>

#include "jvm_launcher.h"
#include "utils.h"
#include "oplauncher.h"

// Global variable to store the JVM pointer
extern jvm_launcher_t *jvm_launcher;

/**
 * Signal handler function
 */
void signal_handler(int sig) {
    fprintf(stderr, "\nReceived signal %d, terminating JVM...\n", sig);

    if (jvm_launcher && jvm_launcher->jvm) {
        // Call DestroyJavaVM to clean up the JVM
        jvm_launcher_terminate();
    }
}

/**
 * Code Main Execution
 */
int main(void) {
    char buffer[BUFFER_SIZE];
    int rc = EXIT_SUCCESS;

#if defined(_WIN32) || defined(_WIN64)
    char javaHome[MAX_PATH];
    GetEnvironmentVariable("OPLAUNCHER_JAVA_HOME", javaHome, MAX_PATH);

    // Retrieve the OPLAUNCHER_JAVA_HOME environment variable
    if ( resolveJNIDllDepsOnEnvVar("jre/bin/server") != EXIT_SUCCESS ) {
        return (EXIT_FAILURE);
    }
    if ( resolveJNIDllDepsOnEnvVar("jre/bin") != EXIT_SUCCESS ) {
        return (EXIT_FAILURE);
    }
    if ( resolveJNIDllDepsOnEnvVar("lib") != EXIT_SUCCESS ) {
        return (EXIT_FAILURE);
    }

    SetDefaultDllDirectories(LOAD_LIBRARY_SEARCH_DEFAULT_DIRS | LOAD_LIBRARY_SEARCH_USER_DIRS);
    SetDllDirectory(NULL);

    // Use LoadLibraryEx to load jvm.dll with the extended DLL search path
    snprintf(javaHome, MAX_PATH, "%s/jre/bin/server/jvm.dll", javaHome);
    HMODULE hJvm = LoadLibraryEx(javaHome, NULL, LOAD_LIBRARY_SEARCH_USER_DIRS);
    if (!hJvm) {
        fprintf(stderr, "Error: Failed to load jvm.dll. Error code: %lu\n", GetLastError());
        return EXIT_FAILURE;
    }
#endif

    memset(buffer, 0, BUFFER_SIZE);
    /// Initializes the JVM
    rc = jvm_launcher_init(CL_APPLET_CLASSLOADER);
    if (rc != EXIT_SUCCESS) {
        sendErrorMessage("Could not launch the JVM", rc);
    }

#if !defined(_WIN32) || !defined(_WIN64)
    // Register signal handler for SIGINT and SIGTERM
    signal(SIGINT, signal_handler);  // Handle Ctrl+C
    signal(SIGTERM, signal_handler); // Handle termination signal
#endif

    /// Trigger the dispatcher service
    while (chrome_read_message(buffer)) {
        // Parse the incoming JSON (a simple example without full JSON parsing)
        char *class_name = NULL;
        char *jar_path = NULL;

        data_tuplet_t params[MAXARRAYSIZE];
        memset(params, 0, sizeof(params));

        rc = read_msg_from_chrome(buffer, &class_name, &jar_path, params);
        if (rc != EXIT_SUCCESS) {
            sendErrorMessage("Could not read the message sent from chrome", rc);
        }

        if (strlen(class_name) > 0 && strlen(jar_path) > 0) {
            //launch_jvm(class_name, jar_path, params);
        }
        else {
            sendErrorMessage("Invalid message format", RC_ERR_INVALID_MSGFORMAT);
        }
    }

#if defined(_WIN32) || defined(_WIN64)
    // Free the loaded library when done
    FreeLibrary(hJvm);
    jvm_launcher_terminate();
#endif

    return EXIT_SUCCESS;
}
