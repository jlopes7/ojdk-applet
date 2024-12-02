#include <stdio.h>

#include "jvm_launcher.h"
#include "utils.h"
#include "oplauncher.h"

#if defined(_WIN32) || defined(_WIN64)
#include "ui/win_splash.h"
#include "ui/win_java_console.h"
#else
#include "ui/mac_splash.h"
#endif

// Global variable to store the JVM pointer
extern jvm_launcher_t *jvm_launcher;

volatile BOOL End_OpLauncher_Process = FALSE;

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

#if defined(_WIN32) || defined(_WIN64)
/**
 * Process control events from the client
 * @param ctrlType the control type
 * @return
 */
BOOL WINAPI consoleCtrlHandler(DWORD ctrlType) {
    switch (ctrlType) {
        case CTRL_C_EVENT: // Handle Ctrl+C
        case CTRL_BREAK_EVENT: // Handle Ctrl+Break
        case CTRL_CLOSE_EVENT: // Handle console close
        case CTRL_LOGOFF_EVENT: // Handle user logoff (only in services)
        case CTRL_SHUTDOWN_EVENT: // Handle system shutdown (only in services)
            if ( jvm_launcher && PTR(jvm_launcher).jvm && PTR(jvm_launcher).env ) {
                fprintf(stderr, "[FINISH OPLAUNCHER] Exit code: %ld", ctrlType);
                // Call DestroyJavaVM to clean up the JVM
                jvm_launcher_terminate();
            }
            return TRUE;
        default:
            return FALSE; // Pass other events to the next handler
    }
}
#endif

/**
 * Code Main Execution
 */
#if defined(_WIN32) || defined(_WIN64)
int main(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
    if (!hInstance) {
        hInstance = GetModuleHandle(NULL);
    }
#else
int main(void) {
#endif
    char buffer[BUFFER_SIZE];
    int rc = EXIT_SUCCESS;

#if defined(_WIN32) || defined(_WIN64)
    char javaHome[MAX_PATH];
    GetEnvironmentVariable("OPLAUNCHER_JAVA_HOME", javaHome, MAX_PATH);

    // Show the splash screen
    show_splash_screen(hInstance);
    // Show the Java console
    show_console(hInstance);
    Sleep(3000); // TODO: Just idle for a bit... (we want to show our beautiful image don't we...)
    print_hello();

    // Main application logic
    //MessageBox(NULL, "Main application is now running!", "Oplauncher", MB_OK);

    // Retrieve the OPLAUNCHER_JAVA_HOME environment variable
    if ( resolveJNIDllDepsOnEnvVar("jre/bin/server") != EXIT_SUCCESS ) {
        return EXIT_FAILURE;
    }
    if ( resolveJNIDllDepsOnEnvVar("jre/bin") != EXIT_SUCCESS ) {
        return EXIT_FAILURE;
    }
    if ( resolveJNIDllDepsOnEnvVar("lib") != EXIT_SUCCESS ) {
        return EXIT_FAILURE;
    }

    SetDefaultDllDirectories(LOAD_LIBRARY_SEARCH_DEFAULT_DIRS | LOAD_LIBRARY_SEARCH_USER_DIRS);
    //SetDllDirectory(NULL);

    // Use LoadLibraryEx to load jvm.dll with the extended DLL search path
    snprintf(javaHome, MAX_PATH, "%s/jre/bin/server/jvm.dll", javaHome);
    HMODULE hJvm = LoadLibraryEx(javaHome, NULL, LOAD_LIBRARY_SEARCH_USER_DIRS);
    if (!hJvm) {
        fprintf(stderr, "Error: Failed to load jvm.dll. Error code: %lu\n", GetLastError());
        return EXIT_FAILURE;
    }

    // Set the custom control handler
    if (!SetConsoleCtrlHandler(consoleCtrlHandler, TRUE)) {
        fprintf(stderr, "Error: Could not set control handler\n");
        return EXIT_FAILURE;
    }
#else
    show_splash_screen();
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
#else
    // Hide the splash screen
    hide_splash_screen();
#endif

    fprintf(stdout, "TEST!");
    /// Trigger the dispatcher service
    while (!End_OpLauncher_Process /*Controls either if the process should end naturally or not*/
                && chrome_read_message(buffer)) {
        // Parse the incoming JSON (a simple example without full JSON parsing)
        char *class_name = NULL;
        char *jar_path = NULL;

        data_tuplet_t params[MAXARRAYSIZE];
        memset(params, 0, sizeof(params));

        rc = read_msg_from_chrome(buffer, &class_name, &jar_path, params);
        if (rc != EXIT_SUCCESS) {
            sendErrorMessage("Could not read the message sent from chrome", rc);
        }

        if (class_name!=NULL && jar_path!= NULL && strlen(class_name) > 0 && strlen(jar_path) > 0) {
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
