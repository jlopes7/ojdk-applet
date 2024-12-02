#include "win_java_console.h"
#include "resource.h"

#include <stdio.h>
#include <io.h>
#include <fcntl.h>

#include "utils.h"

static HWND hConsoleWnd, hTextBox, hThreadDumpBtn, hClearBtn, hSaveBtn;
static HANDLE hConsoleThread = NULL;
static HANDLE hMonitorThread = NULL;

// Signal for monitor thread to stop
volatile BOOL StopMonitorThread = FALSE;

// Pipe handlers
static HANDLE hStdoutRead = NULL;
static HANDLE hStdoutWrite = NULL;

/**
 * Used to create the pipe to redirect the stdout/stderr to the stand textbox stream
 */
static void create_pipe_for_output() {
    SECURITY_ATTRIBUTES sa = {0};
    sa.nLength = sizeof(SECURITY_ATTRIBUTES);
    sa.bInheritHandle = TRUE;  // Allow inheritance
    sa.lpSecurityDescriptor = NULL;

    // Create a pipe for stdout and stderr
    if (!CreatePipe(&hStdoutRead, &hStdoutWrite, &sa, 0)) {
        MessageBox(NULL, "Failed to create pipe for stdout.", "Error", MB_OK | MB_ICONERROR);
        return;
    }

    // Ensure the read handle is not inherited by child processes
    if (!SetHandleInformation(hStdoutRead, HANDLE_FLAG_INHERIT, 0)) {
        MessageBox(NULL, "Failed to set handle information for stdout read.", "Error", MB_OK | MB_ICONERROR);
        return;
    }

    // Redirect stdout and stderr to the write end of the pipe
    if (!SetStdHandle(STD_OUTPUT_HANDLE, hStdoutWrite) ||
        !SetStdHandle(STD_ERROR_HANDLE, hStdoutWrite)) {
        MessageBox(NULL, "Failed to redirect stdout/stderr.", "Error", MB_OK | MB_ICONERROR);
    }

    // Redirect `stdout` and `stderr` for the C runtime
    _dup2(_open_osfhandle((intptr_t)hStdoutWrite, _O_WRONLY), _fileno(stdout));
    _dup2(_open_osfhandle((intptr_t)hStdoutWrite, _O_WRONLY), _fileno(stderr));
}

/**
 * Create a Thread to Monitor the Pipe
 */
DWORD WINAPI monitor_pipe_thread(LPVOID param) {
    char buffer[MAX_PATH];

    while (!StopMonitorThread) {
        DWORD bytesRead;
        // Check if there's data to read
        if (PeekNamedPipe(hStdoutRead, NULL, 0, NULL, NULL, NULL)) {
            if (ReadFile(hStdoutRead, buffer, sizeof(buffer) - 1, &bytesRead, NULL) && bytesRead > 0) {
                buffer[bytesRead] = '\0';

                /// Replace all LFs with CRLFs (Windows workaround...)
                char *formattedBuffer = replace_with_crlf(buffer);

                // Append to the text box
                SendMessage(hTextBox, EM_SETSEL, -1, -1);
                SendMessage(hTextBox, EM_REPLACESEL, FALSE, (LPARAM)formattedBuffer);

                if (formattedBuffer) free(formattedBuffer);
            }
        }
        else {
            Sleep(10); // Avoid busy-waiting when no data is available
        }
    }

    if (hStdoutRead) CloseHandle(hStdoutRead);
    if (hStdoutWrite) CloseHandle(hStdoutWrite);

    return EXIT_SUCCESS;
}

/**
 * Placeholder event handlers for the buttons.
 */
static void on_thread_dump() {
    MessageBox(hConsoleWnd, "TBD: Thread dump not implemented yet", "Info", MB_OK | MB_ICONINFORMATION);
}

static void on_clear() {
    // Clear the text box content by setting it to an empty string
    SetWindowText(hTextBox, "");
    print_hello();
}

static void on_save() {
    // Buffer to store the file path
    char filePath[MAX_PATH] = {0};

    // Initialize the OPENFILENAME structure
    OPENFILENAME ofn = {0};
    ofn.lStructSize = sizeof(OPENFILENAME);
    ofn.hwndOwner = hConsoleWnd;
    ofn.lpstrFilter = "Text Files (*.txt)\0*.txt\0Log Files (*.log)\0*.log\0";
    ofn.lpstrFile = filePath;
    ofn.nMaxFile = MAX_PATH;
    ofn.Flags = OFN_OVERWRITEPROMPT;
    ofn.lpstrDefExt = "txt"; // Default file extension

    // Show the "Save As" dialog
    if (GetSaveFileName(&ofn)) {
        // Get the content of the text box
        int textLength = GetWindowTextLength(hTextBox);
        if (textLength > 0) {
            char *buffer = calloc(textLength + 1, sizeof(char));
            if (buffer) {
                GetWindowText(hTextBox, buffer, textLength + 1);

                // Write the content to the selected file
                FILE *file = fopen(filePath, "w");
                if (file) {
                    fwrite(buffer, sizeof(char), textLength, file);
                    fclose(file);
                    MessageBox(hConsoleWnd, "JVM log content saved successfully.", "Success", MB_OK | MB_ICONINFORMATION);
                } else {
                    MessageBox(hConsoleWnd, "Failed to save the file.", "Error", MB_OK | MB_ICONERROR);
                }

                free(buffer);
            }
        }
        else {
            MessageBox(hConsoleWnd, "Text box is empty. Nothing to save.", "Info", MB_OK | MB_ICONINFORMATION);
        }
    }
}

/**
 * Adjusts the positions and sizes of the controls in the window.
 */
static void resize_controls() {
    RECT rect;
    GetClientRect(hConsoleWnd, &rect);

    int clientWidth = rect.right - rect.left;
    int clientHeight = rect.bottom - rect.top;

    // Resize and reposition the text box
    SetWindowPos(hTextBox, NULL, 10, 10, clientWidth - 20, clientHeight - BUTTON_HEIGHT - 40, SWP_NOZORDER);

    // Calculate the total width of the buttons including spacing
    int totalButtonWidth = (3 * BUTTON_WIDTH) + (2 * BUTTON_SPACING);

    // Calculate the starting x position to center the buttons
    int buttonX = (clientWidth - totalButtonWidth) / 2;
    int buttonY = clientHeight - BUTTON_HEIGHT - 10;

    // Reposition the buttons
    SetWindowPos(hThreadDumpBtn, NULL, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, SWP_NOZORDER);
    SetWindowPos(hClearBtn, NULL, buttonX + BUTTON_WIDTH + BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, SWP_NOZORDER);
    SetWindowPos(hSaveBtn, NULL, buttonX + 2 * (BUTTON_WIDTH + BUTTON_SPACING), buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, SWP_NOZORDER);
}

/**
 * Window procedure for the console.
 */
LRESULT CALLBACK console_wnd_proc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_COMMAND:
            if ((HWND)lParam == hThreadDumpBtn) on_thread_dump();
            else if ((HWND)lParam == hClearBtn) on_clear();
            else if ((HWND)lParam == hSaveBtn) on_save();
            break;

        case WM_SIZE:
            // Handle resizing of the window
                resize_controls();
            break;

        case WM_CLOSE:
            hide_console();
            break;

        case WM_DESTROY:
            PostQuitMessage(0);
            break;

        default:
            return DefWindowProc(hwnd, msg, wParam, lParam);
    }
    return 0;
}

/**
 * The thread function for showing the console.
 */
unsigned __stdcall console_thread_proc(void* param) {
    HINSTANCE hInstance = (HINSTANCE)param;
    WNDCLASS wc = {0};

    wc.lpfnWndProc = console_wnd_proc;
    wc.hInstance = hInstance;
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    wc.lpszClassName = "ConsoleClass";

    RegisterClass(&wc);

    hConsoleWnd = CreateWindowEx(
        WS_EX_CLIENTEDGE,
        "ConsoleClass", "OJDK Applet Console",
        WS_OVERLAPPEDWINDOW,
        CW_USEDEFAULT, CW_USEDEFAULT, CONSOLE_WIDTH, CONSOLE_HEIGHT,
        NULL, NULL, hInstance, NULL
    );

    // Set the window icon
    HICON hIcon = LoadIcon(hInstance, MAKEINTRESOURCE(IDI_APPICON));
    SendMessage(hConsoleWnd, WM_SETICON, ICON_BIG, (LPARAM)hIcon);
    SendMessage(hConsoleWnd, WM_SETICON, ICON_SMALL, (LPARAM)hIcon);

    // Create the text box
    hTextBox = CreateWindowEx(
        0/*WS_EX_CLIENTEDGE*/, "EDIT", "",
        WS_CHILD | WS_VISIBLE | WS_VSCROLL | WS_HSCROLL | ES_MULTILINE | ES_AUTOVSCROLL | ES_READONLY,
        10, 10, CONSOLE_WIDTH - 20, CONSOLE_HEIGHT - 70,
        hConsoleWnd, NULL, hInstance, NULL
    );

    // Define the console font
    HFONT hFont = CreateFont(
        -15,                        // Height of the font (negative for character height)
        0,                          // Width of the font (0 means default width)
        0,                          // Escapement angle (0 means default)
        0,                          // Orientation angle (0 means default)
        FW_NORMAL,                  // Font weight (FW_NORMAL for non-bold)
        FALSE,                      // Italic attribute (FALSE means no italic)
        FALSE,                      // Underline attribute (FALSE means no underline)
        FALSE,                      // Strikeout attribute (FALSE means no strikeout)
        DEFAULT_CHARSET,            // Character set (DEFAULT_CHARSET for default)
        OUT_DEFAULT_PRECIS,         // Output precision
        CLIP_DEFAULT_PRECIS,        // Clipping precision
        DEFAULT_QUALITY,            // Output quality
        DEFAULT_PITCH | FF_SWISS,   // Pitch and family (DEFAULT_PITCH | FF_SWISS for sans-serif fonts)
        CONSOLE_FONTFACE            // Font face name
    );
    // Set the font for the text box
    SendMessage(hTextBox, WM_SETFONT, (WPARAM)hFont, TRUE); // TRUE to redraw the control


    // Create buttons
    hThreadDumpBtn = CreateWindow("BUTTON", "Thread Dump", WS_TABSTOP | WS_VISIBLE | WS_CHILD | BS_DEFPUSHBUTTON,
                                  10, CONSOLE_HEIGHT - 50, 100, 30, hConsoleWnd, NULL, hInstance, NULL);
    hClearBtn = CreateWindow("BUTTON", "Clear", WS_TABSTOP | WS_VISIBLE | WS_CHILD | BS_DEFPUSHBUTTON,
                             120, CONSOLE_HEIGHT - 50, 100, 30, hConsoleWnd, NULL, hInstance, NULL);
    hSaveBtn = CreateWindow("BUTTON", "Save", WS_TABSTOP | WS_VISIBLE | WS_CHILD | BS_DEFPUSHBUTTON,
                            230, CONSOLE_HEIGHT - 50, 100, 30, hConsoleWnd, NULL, hInstance, NULL);

    ShowWindow(hConsoleWnd, SW_SHOW);
    UpdateWindow(hConsoleWnd);

    resize_controls();

    // Message loop for the window
    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }

    return EXIT_SUCCESS;
}

/**
 * Show the console window and redirect stdout/stderr.
 */
void show_console(HINSTANCE hInstance) {
    /// Create the pipe
    create_pipe_for_output();

    if (hConsoleThread == NULL) {
        hConsoleThread = (HANDLE) _beginthreadex(NULL, 0, console_thread_proc, hInstance, 0, NULL);
    }

    // Start a thread to monitor the pipe
    hMonitorThread = CreateThread(NULL, 0, monitor_pipe_thread, NULL, 0, NULL);
}

/**
 * Hide the console window and restore stdout/stderr.
 */
void hide_console() {
    StopMonitorThread = TRUE;
    // Wait for the thread to exit
    //WaitForSingleObject(hMonitorThread, INFINITE);
    // Close the thread handle
    CloseHandle(hMonitorThread);

    DestroyWindow(hConsoleWnd);
}
