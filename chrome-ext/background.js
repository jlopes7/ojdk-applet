const ACTION_APPLET_DETAILS_ATTRNAME = "APPLET_DETAILS_WITH_COOKIES";

/**
 * Applet background service, used to communicate with the Applet external service
 */
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === ACTION_APPLET_DETAILS_ATTRNAME) {
        // Extract the applet details from the message
        const { archiveUrl, codeUrl, cookies } = message.data;

        // Prepare the cookie header for fetch
        const cookieHeader = cookies.map(cookie => `${cookie.name}=${cookie.value}`).join('; ');

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

		console.log("About to download the Applet codebase: ", downloadUrl)
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

                const port = chrome.runtime.connectNative("org.oplauncher.applet_service");

                // Create the message to be sent to native host, including applet details and file content
                const messageToNative = {
                    ...message.data,
                    fileType: message.archiveUrl ? "jar" : "class",
                    fileContent: base64Data // Pass the file content in Base64 format
                };
                
                port.postMessage(messageToNative); // Send applet details and file content to native host

                port.onMessage.addListener((response) => {
                    console.log("Response from native host:", response);

                    // Send response back to the content script
                    chrome.tabs.sendMessage(sender.tab.id, { action: "appletResponse", response });
                });

                port.onDisconnect.addListener(() => {
                    console.error("Native host disconnected.");
                });
            })
            .catch(error => {
                console.error("Failed to fetch or process the file:", error);
            });
    }
});

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
