const OP_LOAD = "load_applet";
const NATIVE_SERVICE = "org.oplauncher.applet_service";
const OPLAUNCHER_RESPONSE_CODE = "oplauncher_applet_response";
const FETCH_REMOTEAPPLET = false;
const DEBUG = false;

if (DEBUG) {
    /**
     * Testing...
     */
    chrome.runtime.onInstalled.addListener(() => {
        console.debug("Testing Native Messaging...");
        const port = chrome.runtime.connectNative("org.oplauncher.applet_service");

        port.onDisconnect.addListener((e) => {
            console.error("Failed to connect to native app", e);
        });
    });
}

/**
 * Applet background service, used to communicate with the Applet external service
 */
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.op === OP_LOAD) {
        /*
         * Option 1: Loads the Applet base code and sends its bits as B64 across the wire
         * -> May be a valid option for the future, but for now is deactivated
         */
        if ( FETCH_REMOTEAPPLET) {
            console.info("Option 1 (fetch remote on Chrome Extension) was selected");

            // Extract the applet details from the message
            const { archiveUrl, codeUrl } = message;

            // Determine which file to download: JAR file or class file
            let downloadUrl = null;
            if (archiveUrl) {
                // We have a JAR file to download
                downloadUrl = archiveUrl;
            } else if (codeUrl) {
                // We have a .class file to download
                downloadUrl = codeUrl;
            } else {
                console.error("No archive or code URL provided for applet.");
                // TODO: Show the message in the browser
                return;
            }

            console.info("About to download the Applet codebase: ", downloadUrl)
            // Extract the filename from the URL (either JAR or class)
            //const urlParts = downloadUrl.split('/');
            //const filename = urlParts[urlParts.length - 1]

            // Fetch the file (either .jar or .class) with cookies to maintain session
            fetch(message.archiveUrl || message.codebase)
                .then(response => response.blob())
                .then(blob => {
                    return blob.arrayBuffer(); // Convert the Blob to ArrayBuffer
                })
                .then(arrayBuffer => {
                    // Convert ArrayBuffer to Base64 (optional step to make it more message-friendly)
                    const base64Data = arrayBufferToBase64(arrayBuffer);

                    // Create the message to be sent to native host, including applet details and file content
                    const messageToNative = {
                        ...message.data,
                        fileType: message.archiveUrl ? "jar" : "class",
                        fileContent: base64Data // Pass the file content in Base64 format
                    };

                    send2OPLauncher(messageToNative, (resp, port) => {
                        if (sender && sender.tab && sender.tab.id) {
                            // Send response back to the content script
                            chrome.tabs.sendMessage(sender.tab.id, {action: OPLAUNCHER_RESPONSE_CODE, response});
                        }
                        else {
                            console.warn("Sender tab ID is missing. Cannot send message back.");
                        }
                    });
                })
                .catch(error => {
                    console.error("Failed to fetch or process the file:", error);
                });
        }
        /*
         * Option 2: The applet sourcebase will be dealt by the OPLauncher Pilot
         * -> This is the option that is currently Active!
         */
        else {
            console.info("Option 2 (Applet bits is resolved by OPLauncher) was selected");

            send2OPLauncher(message, (resp, port) => {
                if (sender && sender.tab && sender.tab.id) {
                    // Send response back to the content script
                    chrome.tabs.sendMessage(sender.tab.id, {action: OPLAUNCHER_RESPONSE_CODE, response});
                }
                else {
                    console.warn("Sender tab ID is missing. Cannot send message back.");
                }
            });
        }
    }
});

/**
 * Sends a message to OPLauncher
 * @param messageToNative   the applet computed message
 * @param callback          the callback function. Called when the applet responds back to the Browser
 * @param callbackErr       the error callback function. Called when something bad happened
 */
function send2OPLauncher(messageToNative, callback, callbackErr) {
    const port = chrome.runtime.connectNative(NATIVE_SERVICE);

    console.info("About to send a message to OPLauncher:", messageToNative);
    port.postMessage(messageToNative); // Send applet details and file content to native host

    port.onMessage.addListener((response) => {
        console.info("Response from native host:", response);

        // Calls the return function (callback)
        callback(response, port);
    });

    // Ensure onDisconnect is only added once
    if (!port.onDisconnect.hasListener(handlePortDisconnect)) {
        port.onDisconnect.addListener(() => {
            console.error("Native host (OPLauncher) was disconnected.");
            callbackErr(new Error("Connection failed"));
        });
    }
}

/**
 * Utility function to convert ArrayBuffer to Base64
 */
function arrayBufferToBase64(buffer) {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;

    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
}
