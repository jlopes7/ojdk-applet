#ifndef _UI_TRAY_CTRL_H
#define _UI_TRAY_CTRL_H

#include <windows.h>
#include <shellapi.h>

// Function to create the tray icon
void AddTrayIcon(HWND hWnd);
// Function to show the context menu when right-clicked
void ShowContextMenu(HWND hWnd, POINT pt);

// Function to handle tray icon events
LRESULT CALLBACK WindowProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam);

#endif //UI_TRAY_CTRL_H
