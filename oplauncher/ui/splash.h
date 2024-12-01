#ifndef _SPLASH_H
#define _SPLASH_H

#include <windows.h>

// Splash screen dimensions
#define SPLASH_WIDTH        670
#define SPLASH_HEIGHT       400
#define TEXT_AREA_X         60 // Starting x-position for the text area
#define TEXT_AREA_Y         10
#define IMAGE_WIDTH         661
#define IMAGE_HEIGHT        371

#define IMAGE_FILE          "oplauncher_splash.bmp"

void show_splash_screen(HINSTANCE hInstance);
void hide_splash_screen(void);

#endif //SPLASH_H
