#include <Cocoa/Cocoa.h>
#include <objc/runtime.h>
#include "mac_splash.h"

static NSWindow *splashWindow = nil;
static NSImageView *imageView = nil;

/**
 * Show the splash screen
 */
void show_splash_screen() {
    @autoreleasepool {
        if (![NSThread isMainThread]) {
            dispatch_sync(dispatch_get_main_queue(), ^{
                show_splash_screen();
            });
            return;
        }

        [NSApplication sharedApplication];
        [NSApp setActivationPolicy:NSApplicationActivationPolicyRegular];

        // Get the screen dimensions
        NSScreen *screen = [NSScreen mainScreen];
        NSRect screenFrame = [screen frame];

        // Load the splash screen image
        NSString *imagePath = @"oplauncher_splash.png";
        NSImage *splashImage = [[NSImage alloc] initWithContentsOfFile:imagePath];
        if (!splashImage) {
            NSLog(@"Failed to load image from path: %@", imagePath);
            return;
        }

        // Define splash screen dimensions
        CGFloat splashWidth  = SPLASH_SCREEN_WIDTH;  // Desired splash screen width
        CGFloat splashHeight = SPLASH_SCREEN_HEIGHT; // Desired splash screen height
        /*NSSize imageSize = [splashImage size];
        CGFloat splashWidth = imageSize.width;
        CGFloat splashHeight = imageSize.height;*/

        // Calculate the centered position
        CGFloat splashX = NSMidX(screenFrame) - (splashWidth / 2);
        CGFloat splashY = NSMidY(screenFrame) - (splashHeight / 2);


        NSRect frame = NSMakeRect(splashX, splashY, splashWidth, splashHeight);
        splashWindow = [[NSWindow alloc] initWithContentRect:frame
                                                    styleMask:NSWindowStyleMaskBorderless
                                                      backing:NSBackingStoreBuffered
                                                        defer:NO];
        [splashWindow setLevel:NSStatusWindowLevel];
        [splashWindow setBackgroundColor:[NSColor whiteColor]];
        [splashWindow setOpaque:NO];
        [splashWindow setHasShadow:YES];

        imageView = [[NSImageView alloc] initWithFrame:NSMakeRect(0, 0, splashWidth, splashHeight)];
        [imageView setImage:splashImage];
        [imageView setImageScaling:NSImageScaleAxesIndependently];
        [[splashWindow contentView] addSubview:imageView];

        [splashWindow makeKeyAndOrderFront:nil];
        [NSApp activateIgnoringOtherApps:YES];
        [NSApp run];
    }
}

/**
 * Hide the splash screen
 */
void hide_splash_screen() {
    @autoreleasepool {
        if (splashWindow) {
            [splashWindow close];
            splashWindow = nil;
        }
    }
}
