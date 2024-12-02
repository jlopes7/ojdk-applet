#include <Cocoa/Cocoa.h>
#include <dispatch/dispatch.h>
#include <pthread.h>
#include "mac_java_console.h"

static NSWindow *consoleWindow = nil;
static NSTextView *textView = nil;
static NSPipe *outputPipe = nil;
static NSFileHandle *pipeReadHandle = nil;
static pthread_t monitorThread;
static BOOL stopMonitorThread = NO;

/**
 * Creates the pipe to redirect stdout/stderr.
 */
static void create_pipe_for_output() {
    @autoreleasepool {
        outputPipe = [NSPipe pipe];
        pipeReadHandle = [outputPipe fileHandleForReading];
        if (!pipeReadHandle) {
            NSLog(@"Pipe read handle is nil. The console will not work properly.");
            return;
        }

        // Redirect stdout and stderr
        int fd = [outputPipe fileHandleForWriting].fileDescriptor;

        // Redirect stdout
        if (dup2(fd, STDOUT_FILENO) == -1) {
            perror("dup2 failed for stdout");
            return;
        }

        // Redirect stderr
        if (dup2(fd, STDERR_FILENO) == -1) {
            perror("dup2 failed for stderr");
            return;
        }

        // Set stdout and stderr to line-buffered to ensure they are written immediately
        setvbuf(stdout, NULL, _IOLBF, 0);
        setvbuf(stderr, NULL, _IOLBF, 0);

        // Retain the pipeReadHandle to prevent it from being deallocated
        [pipeReadHandle retain];

        // Set up notification for when data is available to read
        [[NSNotificationCenter defaultCenter] addObserverForName:NSFileHandleReadCompletionNotification
                                                          object:pipeReadHandle
                                                           queue:nil
                                                      usingBlock:^(NSNotification *notification) {
            NSData *data = notification.userInfo[NSFileHandleNotificationDataItem];
            if (data.length > 0) {
                NSString *output = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
                if (output) {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        if (textView) {
                            NSTextStorage *textStorage = [textView textStorage];
                            if (textStorage) {
                                [textStorage appendAttributedString:[[NSAttributedString alloc] initWithString:output]];

                                // Scroll to the end of the text view
                                NSRange range = NSMakeRange([[textView string] length], 0);
                                [textView scrollRangeToVisible:range];
                            }
                        }
                    });
                }
            }
            if (!stopMonitorThread) {
                [pipeReadHandle readInBackgroundAndNotify]; // Continue reading
            }
        }];

        [pipeReadHandle readInBackgroundAndNotify]; // Start reading
    }
}

/**
 * Monitor the pipe for output data.
 */
static void *monitor_pipe_thread(void *param) {
    @autoreleasepool {
        NSData *data;
        while (!stopMonitorThread) {
            @autoreleasepool {
                if (!pipeReadHandle) {
                    usleep(10000); // Sleep for 10ms to avoid busy-waiting
                    continue;
                }

                @try {
                    data = [pipeReadHandle availableData];
                }
                @catch (NSException *exception) {
                    NSLog(@"Exception while reading from pipe: %@", exception);
                    break;
                }

                if (!data || [data length] == 0) {
                    usleep(10000); // Sleep for 10ms to avoid busy-waiting
                    continue;
                }

                NSString *output = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
                if (!output) {
                    continue; // Skip if data cannot be converted to a string
                }

                // Debug: Print output to console to verify
                NSLog(@"Pipe output: %@", output);

                dispatch_async(dispatch_get_main_queue(), ^{
                    if (textView) {
                        NSTextStorage *textStorage = [textView textStorage];
                        if (textStorage) {
                            [textStorage appendAttributedString:[[NSAttributedString alloc] initWithString:output]];

                            // Scroll to the end of the text view
                            NSRange range = NSMakeRange([[textView string] length], 0);
                            [textView scrollRangeToVisible:range];
                        }
                    }
                });
            }
        }
    }
    return NULL;
}

/**
 * Placeholder event handlers for the buttons.
 */
static void on_thread_dump() {
    NSLog(@"TBD: Thread dump not implemented yet");
}

static void on_clear() {
    if (textView) {
        [[textView textStorage] setAttributedString:[[NSAttributedString alloc] initWithString:@""]];
    }
}

static void on_save() {
    NSSavePanel *savePanel = [NSSavePanel savePanel];
    [savePanel setAllowedFileTypes:@[@"txt", @"log"]];
    [savePanel setAllowsOtherFileTypes:NO];
    [savePanel setExtensionHidden:NO];
    [savePanel setCanCreateDirectories:YES];

    if ([savePanel runModal] == NSModalResponseOK) {
        NSURL *fileURL = [savePanel URL];
        NSString *content = [textView string];
        NSError *error = nil;

        BOOL success = [content writeToURL:fileURL
                                atomically:YES
                                  encoding:NSUTF8StringEncoding
                                     error:&error];
        if (!success) {
            NSLog(@"Failed to save file: %@", [error localizedDescription]);
        }
    }
}

/**
 * Show the console window.
 */
void show_console() {
    @autoreleasepool {
        if (![NSThread isMainThread]) {
            dispatch_sync(dispatch_get_main_queue(), ^{
                show_console();
            });
            return;
        }

        if (consoleWindow) {
            [consoleWindow makeKeyAndOrderFront:nil];
            return;
        }

        [NSApplication sharedApplication];
        [NSApp setActivationPolicy:NSApplicationActivationPolicyRegular];

        NSRect frame = NSMakeRect(100, 100, CONSOLE_WIDTH, CONSOLE_HEIGHT);
        consoleWindow = [[NSWindow alloc] initWithContentRect:frame
                                                    styleMask:(NSWindowStyleMaskTitled |
                                                               NSWindowStyleMaskClosable)
                                                      backing:NSBackingStoreBuffered
                                                        defer:NO];
        [consoleWindow setTitle:@"OJDK Applet Console"];
        [consoleWindow setReleasedWhenClosed:NO];

        // Create text view for console output
        CGFloat buttonHeight = 40.0;
        CGFloat buttonSpacing = 20.0;
        CGFloat buttonAreaHeight = buttonHeight + buttonSpacing * 2;

        NSRect scrollViewFrame = NSMakeRect(10, buttonAreaHeight, frame.size.width - 20, frame.size.height - buttonAreaHeight - 10);
        NSScrollView *scrollView = [[NSScrollView alloc] initWithFrame:scrollViewFrame];
        [scrollView setHasVerticalScroller:YES];
        [scrollView setHasHorizontalScroller:NO];
        [scrollView setAutoresizingMask:(NSViewWidthSizable | NSViewHeightSizable)];

        NSRect textViewFrame = NSMakeRect(0, 0, scrollViewFrame.size.width, scrollViewFrame.size.height);
        textView = [[NSTextView alloc] initWithFrame:textViewFrame];
        [textView setEditable:NO];
        [textView setFont:[NSFont fontWithName:@"Arial" size:12.0]];

        [scrollView setDocumentView:textView];
        [[consoleWindow contentView] addSubview:scrollView];

        // Calculate button positions and create buttons
        CGFloat buttonWidth = 100.0;
        CGFloat totalButtonWidth = buttonWidth * 3 + buttonSpacing * 2;
        CGFloat buttonY = buttonSpacing;
        CGFloat buttonXStart = (frame.size.width - totalButtonWidth) / 2;

        NSButton *threadDumpBtn = [[NSButton alloc] initWithFrame:NSMakeRect(buttonXStart, buttonY, buttonWidth, buttonHeight)];
        [threadDumpBtn setTitle:@"Thread Dump"];
        [threadDumpBtn setTarget:NSApp];
        [threadDumpBtn setAction:@selector(on_thread_dump)];
        [threadDumpBtn setAutoresizingMask:(NSViewMinYMargin | NSViewMaxXMargin)];
        [[consoleWindow contentView] addSubview:threadDumpBtn];

        NSButton *clearBtn = [[NSButton alloc] initWithFrame:NSMakeRect(buttonXStart + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight)];
        [clearBtn setTitle:@"Clear"];
        [clearBtn setTarget:NSApp];
        [clearBtn setAction:@selector(on_clear)];
        [clearBtn setAutoresizingMask:(NSViewMinYMargin | NSViewMinXMargin | NSViewMaxXMargin)];
        [[consoleWindow contentView] addSubview:clearBtn];

        NSButton *saveBtn = [[NSButton alloc] initWithFrame:NSMakeRect(buttonXStart + 2 * (buttonWidth + buttonSpacing), buttonY, buttonWidth, buttonHeight)];
        [saveBtn setTitle:@"Save"];
        [saveBtn setTarget:NSApp];
        [saveBtn setAction:@selector(on_save)];
        [saveBtn setAutoresizingMask:(NSViewMinYMargin | NSViewMinXMargin)];
        [[consoleWindow contentView] addSubview:saveBtn];

        [consoleWindow makeKeyAndOrderFront:nil];
        [NSApp activateIgnoringOtherApps:YES];

        // Create the pipe and start monitoring
        create_pipe_for_output();
        stopMonitorThread = NO;

        //pthread_create(&monitorThread, NULL, monitor_pipe_thread, NULL);

        [NSApp run];
    }
}

/**
 * Hide the console window and restore stdout/stderr.
 */
void hide_console() {
    @autoreleasepool {
        if (![NSThread isMainThread]) {
            dispatch_sync(dispatch_get_main_queue(), ^{
                hide_console();
            });
            return;
        }

        if (consoleWindow) {
            [consoleWindow close];
            consoleWindow = nil;
        }

        stopMonitorThread = YES;
        pthread_join(monitorThread, NULL);

        // Release the pipe read handle
        if (pipeReadHandle) {
            [pipeReadHandle closeFile];
            [pipeReadHandle release];
            pipeReadHandle = nil;
        }
    }
}
