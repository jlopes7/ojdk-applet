#include <stdio.h>

#include "resource.h"
#include "win_tray_ctrl.h"
#include "logging.h"
#include "jvm_launcher.h"

NOTIFYICONDATA nid;
HMENU hMenu;

void AddTrayIcon(HWND hWnd) {
    nid.cbSize = sizeof(NOTIFYICONDATA);
    nid.hWnd = hWnd;
    nid.uID = ID_TRAY_ICON;
    nid.uFlags = NIF_ICON | NIF_MESSAGE | NIF_TIP;
    nid.uCallbackMessage = WM_TRAYICON;
    nid.hIcon = LoadImage(GetModuleHandle(NULL), MAKEINTRESOURCE(IDI_APPICON), IMAGE_ICON, 0, 0, LR_DEFAULTSIZE | LR_SHARED);
    strcpy(nid.szTip, "OPLauncher - Right Click for Options");

    Shell_NotifyIcon(NIM_ADD, &nid);
}

void ShowContextMenu(HWND hWnd, POINT pt) {
    if (!hMenu) {
        hMenu = CreatePopupMenu();
        AppendMenu(hMenu, MF_STRING, ID_TRAY_RELOAD_JVM, "Reload the JVM");
        AppendMenu(hMenu, MF_STRING, ID_TRAY_EXIT, "Close");
    }

    SetForegroundWindow(hWnd);
    TrackPopupMenu(hMenu, TPM_RIGHTALIGN | TPM_BOTTOMALIGN | TPM_RIGHTBUTTON, pt.x, pt.y, 0, hWnd, NULL);

    // Prevent potential menu flickering issue
    PostMessage(hWnd, WM_NULL, 0, 0);

    DestroyMenu(hMenu);
}

LRESULT CALLBACK WindowProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_TRAYICON:
            if (lParam == WM_RBUTTONUP) {
                POINT pt;
                GetCursorPos(&pt);
                ShowContextMenu(hWnd, pt);
                return EXIT_SUCCESS;
            }
            break;
        case WM_COMMAND:
            switch (LOWORD(wParam)) {
                case ID_TRAY_RELOAD_JVM: {
                    logmsg(LOGGING_NORMAL, "Reloading JVM...");
                    jvm_launcher_terminate(); // Properly shutdown the current JVM
                    jvm_launcher_init(CL_APPLET_CLASSLOADER); // Reload JVM
                    break;
                }
                case ID_TRAY_EXIT: {
                    logmsg(LOGGING_NORMAL, "Exiting OPLauncher...");
                    Shell_NotifyIcon(NIM_DELETE, &nid);
                    DestroyWindow(hWnd);
                    PostQuitMessage(0);
                    break;
                }
            }
            break;
        case WM_DESTROY:
            Shell_NotifyIcon(NIM_DELETE, &nid);
            PostQuitMessage(0);
            break;
        default:
            return DefWindowProc(hWnd, msg, wParam, lParam);
    }
    return 0;
}
