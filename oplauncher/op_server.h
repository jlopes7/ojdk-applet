#ifndef _OP_SERVER_H
#define _OP_SERVER_H

#include "utils.h"

#define PORT_BUFF_SIZE  16
#define DEF_PORT        3333

typedef returncode_t (*tcpipop_callbackfct_t)(const char *);

#if defined(_WIN32) || defined(_WIN64)
typedef int socklen_t;

// Define `INET_ADDRSTRLEN` if missing
#   ifndef INET_ADDRSTRLEN
#       define INET_ADDRSTRLEN 16  // Standard IPv4 address size
#   endif
#   pragma comment(lib, "ws2_32.lib")

DWORD WINAPI tcp_server_thread(LPVOID arg);
#else
returncode_t *tcp_server_thread(void *arg);
#endif
returncode_t get_client_ipaddr(int client_socket, struct sockaddr_in *client_addr, char *client_ip);
returncode_t handle_client_op(SOCKET client_socket, char **buffer, tcpipop_callbackfct_t callback);
returncode_t process_tciipop_request(const char *json_message, tcpipop_callbackfct_t callback);
returncode_t start_op_server(tcpipop_callbackfct_t callback);

BOOL check_op_server_enabled(void);

#endif //IO_SERVER_H
