#include "win_splash.h"

static HWND hWnd;
static HBITMAP hBitmap;

/**
 * Control for the OPLauncher Splash screen
 */
LRESULT CALLBACK splash_wnd_proc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_PAINT: {
            PAINTSTRUCT ps;
            HDC hdc = BeginPaint(hwnd, &ps);

            // Set background color and text
            HBRUSH brush = CreateSolidBrush(RGB(255, 255, 255)); // White background
            FillRect(hdc, &ps.rcPaint, brush);
            DeleteObject(brush);

            // Draw the BMP image
            if (hBitmap) {
                HDC hdcMem = CreateCompatibleDC(hdc);
                SelectObject(hdcMem, hBitmap);
                BitBlt(hdc, 10, 10, IMAGE_WIDTH, IMAGE_HEIGHT, hdcMem, 0, 0, SRCCOPY); // Adjust position and size as needed
                DeleteDC(hdcMem);
            }

            // Draw text on the splash screen (removed for now)
            // TODO: Update in the future to give additional status
            /*SetTextColor(hdc, RGB(0, 0, 0)); // Black text
            SetBkMode(hdc, TRANSPARENT);
            TextOut(hdc, 20, 20, "Welcome to OJDK Applet Plugin Launcher!", 23);
            TextOut(hdc, 20, 50, "Loading, please wait...", 23);*/

            EndPaint(hwnd, &ps);
        } break;

        case WM_CLOSE:
            DestroyWindow(hwnd);
        break;

        case WM_DESTROY:
            DeleteObject(hBitmap); // Clean up the BMP resource
            PostQuitMessage(0);
        break;

        default:
            return DefWindowProc(hwnd, msg, wParam, lParam);
    }
    return 0;
}

void hide_splash_screen() {
    // Destroy the splash screen window
    DestroyWindow(hWnd);
}

/**
 * Show a graphical splash screen
 */
void show_splash_screen(HINSTANCE hInstance) {
    HRGN hRgn;
    RECT rect;
    int screenWidth, screenHeight, x, y;

    WNDCLASS wc = {0};
    wc.lpfnWndProc = splash_wnd_proc;
    wc.hInstance = hInstance;
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    wc.lpszClassName = "SplashClass";

    RegisterClass(&wc);

    // Create a borderless, non-resizable window
    hWnd = CreateWindowEx(
        WS_EX_TOPMOST, // Ensure the window is always on top
        "SplashClass", "",
        WS_POPUP, // No border, title, or buttons
        CW_USEDEFAULT, CW_USEDEFAULT, SPLASH_WIDTH, SPLASH_HEIGHT, // Width and height
        NULL, NULL, hInstance, NULL
    );

    // Load the BMP image
    hBitmap = (HBITMAP)
        LoadImage(NULL, IMAGE_FILE, IMAGE_BITMAP, IMAGE_WIDTH, IMAGE_HEIGHT, LR_LOADFROMFILE);
    if (!hBitmap) {
        MessageBox(NULL, "Failed to load BMP image", "Error", MB_ICONERROR | MB_OK);
        return;
    }

    // Center the window on the screen
    GetWindowRect(hWnd, &rect);
    screenWidth = GetSystemMetrics(SM_CXSCREEN);
    screenHeight = GetSystemMetrics(SM_CYSCREEN);
    x = (screenWidth - (rect.right - rect.left)) / 2;
    y = (screenHeight - (rect.bottom - rect.top)) / 2;
    SetWindowPos(hWnd, NULL, x, y, 0, 0, SWP_NOZORDER | SWP_NOSIZE);

    // Set a rounded bezel for the window
    hRgn = CreateRoundRectRgn(0, 0, SPLASH_WIDTH, SPLASH_HEIGHT, 10, 10); // Width, Height, Ellipse Width, Ellipse Height
    SetWindowRgn(hWnd, hRgn, TRUE);
    DeleteObject(hRgn);

    // Display the window
    ShowWindow(hWnd, SW_SHOW);
    UpdateWindow(hWnd);
}
