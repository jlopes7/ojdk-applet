#include <stdio.h>

#include "jvm_launcher.h"
#include "utils.h"
#include "oplauncher.h"
#include "logging.h"
#include "op_server.h"

#if defined(_WIN32) || defined(_WIN64)
#include "ui/win_tray_ctrl.h"
#else
#include "ui/mac_splash.h"
#include "ui/mac_java_console.h"
#endif

// Global variable to store the JVM pointer
extern jvm_launcher_t *jvm_launcher;

// Controls the ending of the OpLauncher process
volatile BOOL End_OpLauncher_Process = FALSE;

/**
 * Signal handler function
 */
void signal_handler(int sig) {
    logmsg(LOGGING_NORMAL, "Received signal %d, terminating JVM...", sig);

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
                logmsg(LOGGING_ERROR, "[FINISH OPLAUNCHER] Exit code: %ld", ctrlType);
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
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
//int main(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) { // OLD testing cmd version
    if (!hInstance) {
        hInstance = GetModuleHandle(NULL);
    }

    omit_stderr(); // to avoid messing with the Chrome native message protocol
#else
int main(void) {
#endif
    char buffer[BUFFER_SIZE];
    char cache_path[MAX_PATH];
    returncode_t rc = EXIT_SUCCESS;

    // Logging...
    rc = logging_init();
    if ( !_IS_SUCCESS(rc) ) {
        send_jsonerror_message("Could not initialize the log mechanism", rc);
        return rc;
    }

    logmsg(LOGGING_NORMAL, "Waiting from Chrome to parse the first native message (load_applet)");
    _MEMZERO(buffer, BUFFER_SIZE);
    // First chrome message is to prepare and load the applet
    rc = chrome_read_message(buffer);
    if ( _IS_SUCCESS(rc) ) {
        send_jsonsuccess_message(DEF_SUCCESS_MESSAGE);
    }
    else {
        send_jsonerror_message("Could not parse the native message from chrome", rc);
    }

#if defined(_WIN32) || defined(_WIN64)
    char javaHome[MAX_PATH];
    GetEnvironmentVariable("OPLAUNCHER_JAVA_HOME", javaHome, MAX_PATH);

    logmsg(LOGGING_NORMAL, "Creating the OPLauncher tray icon");
    // Tray initialization
    WNDCLASS wc = { 0 };
    wc.lpfnWndProc = WindowProc;
    wc.hInstance = hInstance;
    wc.lpszClassName = "TrayIconClass";

    RegisterClass(&wc);
    HWND hWnd = CreateWindow("TrayIconClass", "Tray App", WS_OVERLAPPEDWINDOW, 0, 0, 0, 0, NULL, NULL, hInstance, NULL);

    // Add the tray icon
    AddTrayIcon(hWnd);

    /*
     * Step 1: Service initialization...
     */
    logmsg(LOGGING_NORMAL, "Initializing all Java libraries");
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

    // Use LoadLibraryEx to load jvm.dll with the extended DLL search path
    snprintf(javaHome, MAX_PATH, "%s/jre/bin/server/jvm.dll", javaHome);
    HMODULE hJvm = LoadLibraryEx(javaHome, NULL, LOAD_LIBRARY_SEARCH_USER_DIRS);
    if (!hJvm) {
        logmsg(LOGGING_ERROR, "Error: Failed to load jvm.dll. Error code: %lu", GetLastError());
        return EXIT_FAILURE;
    }

    // Set the custom control handler
    if (!SetConsoleCtrlHandler(consoleCtrlHandler, TRUE)) {
        logmsg(LOGGING_ERROR, "Error: Could not set control handler");
        return EXIT_FAILURE;
    }
#endif

    logmsg(LOGGING_NORMAL, "Creating the JVM instance");
    /*
     * Step 2: Initializes the JVM
     */
    rc = jvm_launcher_init(CL_APPLET_CLASSLOADER);
    if (rc != EXIT_SUCCESS) {
        send_jsonerror_message("Could not launch the JVM", rc);
    }

    logmsg(LOGGING_NORMAL, "Configuring the application port cache");
    /*
     * Step 3: Prepare the communication with the OPlauncher Pilot
     */
    load_cache_path(cache_path, sizeof(cache_path));

    // Create cache directory if it doesn't exist
    create_cache_directory(cache_path);

    // Parse the incoming JSON (a simple example without full JSON parsing)
    char *op, *applet_name, *class_name, *archive_url, *base_url, *codebase, *height, *width;
    double posx, posy;
    rc = parse_msg_from_chrome_init(buffer, &op, &class_name, &applet_name,
                                    &archive_url, &base_url, &codebase, &height,
                                    &width, &posx, &posy, NULL, NULL);

    if ( !_IS_SUCCESS(rc) ) {
        logmsg(LOGGING_ERROR, "Could not parse the native message from chrome init: %d", rc);
        send_jsonerror_message("Error: Could not parse the native message from chrome init", rc);
        free(op);
        free(class_name);
        free(applet_name);
        free(archive_url);
        free(base_url);
        free(codebase);
        free(height);
        free(width);
        logging_end();
        return EXIT_FAILURE;
    }

    logmsg(LOGGING_NORMAL, "Fields sent by Chrome: OP[%s], CLASSNAME[%s], APPLETNAME[%s], ARCHIVE[%s], CODEBASE[%s], HEIGHT[%s], WIDTH[%s], POSX[%.2f], POSY[%.2f]",
            op, class_name, applet_name, archive_url, codebase, height, width, posx, posy);

    /*
     * Step 3.1: Signal processing (optional step)
     */
#if !defined(_WIN32) || !defined(_WIN64)
    // Register signal handler for SIGINT and SIGTERM
    signal(SIGINT, signal_handler);  // Handle Ctrl+C
    signal(SIGTERM, signal_handler); // Handle termination signal
#endif

    logmsg(LOGGING_NORMAL, "Loading the Applet: %s", class_name);
    /*
     * Step 3.2: Trigger the applet execution
     */
    rc = load_applet (op, class_name, applet_name, archive_url, base_url, codebase, height, width, NULL, NULL, posx, posy);
    if ( !_IS_SUCCESS(rc) ) {
        logmsg(LOGGING_ERROR, "Could not load the Applet from chrome init (%s). Return code %d", class_name, rc);
        return RC_ERR_FAILED_LOAD_APPLET;
    }

    /*
     * Step 3.3: After the Applet is loaded is time to start the OP server
     */
    if ( check_op_server_enabled() ) {
        rc = start_op_server(process_op_tcpip_request);
        if ( !_IS_SUCCESS(rc) ) {
            logmsg(LOGGING_ERROR, "Could not start the OP server. Return code %d", rc);
        }
    }
    else {
        logmsg(LOGGING_NORMAL, "OP Server is not active");
    }

    logmsg(LOGGING_NORMAL, "Applet loaded: %s. Waiting from Chrome messages", class_name);
    /*
     * Step 4: Trigger the dispatcher service
     */
    _MEMZERO(buffer, BUFFER_SIZE);
    while (!End_OpLauncher_Process //Controls either if the process should end naturally or not
                && _IS_SUCCESS(chrome_read_message(buffer))) {
        char *new_op;
        opcode_t opcode;

        logmsg(LOGGING_NORMAL, "New message sent from Chrome: %s", buffer);

        // Send the response JSON message right away confirming the reception of the operation
        send_jsonsuccess_message("Operation received successfully");

        parse_get_jsonprop(buffer, CHROME_EXT_MSG_OP, &new_op);

        get_opcode(&opcode, new_op);
        if ( opcode == OP_UNKNOWN ) {
            logmsg(LOGGING_ERROR, "Unsupported opcode.");
            return RC_WARN_OPCODE_NOT_SUPPORTED;
        }

        // Specific conditions for the OPs
#if defined(_DEBUG_)
        if ( opcode == OP_LOAD ) {
            char *params[1] = { "unload_applet" };
#else
        if ( opcode == OP_UNLOAD ) {
            char *params[1] = { new_op };
#endif
            logmsg(LOGGING_NORMAL, "Unloading Applet: %s", class_name);
            End_OpLauncher_Process = TRUE; // finishes the loop process

            returncode_t rc = trigger_applet_operation(opcode, params, 1);
            if ( !_IS_SUCCESS(rc) ) {
                logmsg(LOGGING_ERROR, "Could not unload Applet: %s", class_name);
            }
            else {
                logmsg(LOGGING_NORMAL, "Applet unloaded successfully: %s", class_name);
            }
        }

        // TODO: Implement
        _MEMZERO(buffer, BUFFER_SIZE);
        free(new_op);

        // just make the CPU happy!
        SLEEP_MS(100);
    }

    logmsg(LOGGING_NORMAL, "Terminating the launcher...");
    // Frees all the pointers...
    free(op);
    free(class_name);
    free(applet_name);
    free(archive_url);
    free(base_url);
    free(codebase);
    free(height);
    free(width);

    // End logging...
    logging_end();
#if defined(_WIN32) || defined(_WIN64)
    // Free the loaded library when done
    FreeLibrary(hJvm);
    jvm_launcher_terminate();
#endif

    return EXIT_SUCCESS;
}
