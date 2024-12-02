#ifndef _WIN_JAVA_CONSOLE_H
#define _WIN_JAVA_CONSOLE_H

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#include <process.h>
#endif

#define CONSOLE_WIDTH   1024
#define CONSOLE_HEIGHT  600
#define BUTTON_HEIGHT   30
#define BUTTON_WIDTH    100
#define BUTTON_SPACING  10

#define CONSOLE_ICON     "oplauncher_64x64.ico"
#define CONSOLE_FONTFACE "Courier New"

#if defined(_WIN32) || defined(_WIN64)
void show_console(HINSTANCE hInstance);
void hide_console(void);
#endif

#endif //_WIN_JAVA_CONSOLE_H
