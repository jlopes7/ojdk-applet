#include "op_server.h"
#include "logging.h"
#include "ini_config.h"

#include <stdlib.h>
#include <string.h>

#if defined (_WIN32) || defined(_WIN64)
#   include <windows.h>
#   include <winsock.h>
#else
#include <unistd.h>
#include <arpa/inet.h>
#include <pthread.h>
#endif

extern BOOL End_OpLauncher_Process;

#if defined(_WIN32) || defined(_WIN64)
void cleanup_winsock() {
    WSACleanup();
}
#endif

/**
 * Check if the OP server is active for the OPLauncher
 * @return  True if active, False if it's not active
 */
BOOL check_op_server_enabled() {
    char respbuf[SMALL_BUFFER];
    _MEMZERO(respbuf, SMALL_BUFFER);
    read_ini_value(INI_SECTION_OPLAUNCHER, INI_SECTION_OPLAUNCHER_PROP_ACTIVE, respbuf, PORT_BUFF_SIZE);

    return strncmp(respbuf, ST_YES, SMALL_BUFFER) == 0 ||
            strncmp(respbuf, ST_TRUE, SMALL_BUFFER) == 0 ;
}

/**
 * Start the TCP OP server
 */
returncode_t start_op_server(tcpipop_callbackfct_t callback) {
#if defined(_WIN32) || defined(_WIN64)
    HANDLE hThread = CreateThread(NULL, 0, tcp_server_thread, callback, 0, NULL);
    if (hThread == NULL) {
        logmsg(LOGGING_NORMAL, "Failed to create TCP server thread");
        return RC_ERR_FAILED_CREATE_THREAD;
    }

    CloseHandle(hThread);
#else
    pthread_t server_thread;
    if (pthread_create(&server_thread, NULL, tcp_server_thread, NULL) != 0) {
        logmsg(LOGGING_NORMAL, "Failed to create TCP server thread");
        return RC_ERR_FAILED_CREATE_THREAD;
    }
    pthread_detach(server_thread);
#endif

    return EXIT_SUCCESS;
}

/**
 * Starts the server to listen for OP requests. Running in a separate thread
 */
#if defined(_WIN32) || defined(_WIN64)
DWORD WINAPI tcp_server_thread(LPVOID arg) {
#else
returncode_t *tcp_server_thread(void *arg) {
#endif
    char *buffer;
    char portbuf[PORT_BUFF_SIZE];
    int port;

    SOCKET server_socket, client_socket;
    struct sockaddr_in server_addr, client_addr;
    socklen_t addr_size = sizeof(client_addr);

    tcpipop_callbackfct_t callbackfct = (tcpipop_callbackfct_t) arg;

    _MEMZERO(portbuf, PORT_BUFF_SIZE);
    read_ini_value(INI_SECTION_OPLAUNCHER, INI_SECTION_OPLAUNCHER_PROP_PORT, portbuf, PORT_BUFF_SIZE);
    port = atoi(portbuf);

#if defined(_WIN32) || defined(_WIN64)
    WSADATA wsa;
    if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0) {
        logmsg(LOGGING_ERROR, "Failed to initialize WSA library");
        return RC_ERR_FAILED_WSL_LOAD;
    }
#endif

    // Create the TCP/IP Socket
    server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket == -1) {
        logmsg(LOGGING_ERROR, "Error creating TCP/IP OP socket");
#if defined(_WIN32) || defined(_WIN64)
        cleanup_winsock();
#endif
        return RC_ERR_TCPIP_FAILED;
    }

    // Configure server address
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(port);

    // Bind the TCP/IP socket
    if (bind(server_socket, (struct sockaddr *)&server_addr, sizeof(server_addr)) == -1) {
        logmsg(LOGGING_ERROR, "Error binding TCP/IP OP socket");
#if defined(_WIN32) || defined(_WIN64)
        closesocket(server_socket);
        cleanup_winsock();
#else
        close(server_socket);
#endif
        return RC_ERR_TCPIP_FAILED;
    }

    // Start listening for OP requests
    if (listen(server_socket, 5) == -1) {
        logmsg(LOGGING_ERROR, "Error listening TCP/IP OP socket");
#ifdef _WIN32
        closesocket(server_socket);
        cleanup_winsock();
#else
        close(server_socket);
#endif
        return RC_ERR_TCPIP_FAILED;
    }

    logmsg(LOGGING_NORMAL, "OP Server started successfully and it's listening on port: %d", port);

    // Accept new connections - blocking method
    while (!End_OpLauncher_Process) {
        client_socket = accept(server_socket, (struct sockaddr *)&client_addr, &addr_size);
        if (client_socket == -1) {
            logmsg(LOGGING_ERROR, "Error accepting connection");
            continue;
        }

        // Handle client request in a separate function
        handle_client_op(client_socket, &buffer, callbackfct);
#ifdef _WIN32
        closesocket(client_socket);
#else
        close(client_socket);
#endif
    }

#ifdef _WIN32
    closesocket(server_socket);
    cleanup_winsock();
#else
    close(server_socket);
#endif

    return EXIT_SUCCESS;
}

returncode_t get_client_ipaddr(int client_socket, struct sockaddr_in *client_addr, char *client_ip) {
#ifdef _WIN32
    // Use `inet_ntoa` instead of `inet_ntop`
    strncpy(client_ip, inet_ntoa(client_addr->sin_addr), INET_ADDRSTRLEN);
#else
    inet_ntop(AF_INET, &client_addr->sin_addr, client_ip, sizeof(client_ip));
#endif

    return EXIT_SUCCESS;
}

/**
 * Handle OP requests
 */
returncode_t handle_client_op(SOCKET client_socket, char **buffer, tcpipop_callbackfct_t callback) {
    struct sockaddr_in client_addr;
    char client_ip[INET_ADDRSTRLEN];
    _MEMZERO(client_ip, INET_ADDRSTRLEN);

    get_client_ipaddr(client_socket, &client_addr, client_ip);

    logmsg(LOGGING_NORMAL, "Client connected! IP address: %s", client_ip);

    PTR(buffer) = malloc(BUFFER_SIZE);
    _MEMZERO(buffer, BUFFER_SIZE);

    // Read JSON message
    size_t bytes_received = recv(client_socket, PTR(buffer), BUFFER_SIZE -1, 0);
    if (bytes_received <= 0) {
        logmsg(LOGGING_ERROR, "Error receiving data from client");
        return RC_ERR_TCPIP_CLIENT_CONNECTED;
    }
    PTR(buffer)[bytes_received] = '\0';

    logmsg(LOGGING_NORMAL, "Received data from the client(%s): %s", client_ip, PTR(buffer));
    process_tciipop_request(PTR(buffer), callback);

    return EXIT_SUCCESS;
}

// Process JSON message and check for "unload_applet"
returncode_t process_tciipop_request(const char *json_message, tcpipop_callbackfct_t callback) {
    // TODO: Add more processing as necessary, maybe some cleanup for specific OPs, etc.
    if (callback) {
        callback(json_message);
    }
    else {
        logmsg(LOGGING_ERROR, "Missing callback function to process new requests");
    }

    return EXIT_SUCCESS;
}
