/**
 * Sends an unload message to the port, so the JVM could be disconnected
 * successfully
 */
function sendUnloadMessageToBackgroundPort() {
    // Dispatch the event for all applets
    registeredAppletList.forEach(entry => {
        console.warn("Unloading the Applet from the backend", entry);
        dispatchToBackground({
            op: OPResources.OP_UNLOAD,
            applet_name: entry.appletName,
        });
    });

    for (let i=0 ; i < frame_count ; i++) {
        let iframe = document.getElementById(getAppletIFrameID(i));
        if (iframe) {
            iframe.contentDocument.getElementById("status").innerHTML = 'OJDK Applet Launcher unloaded.';
        }
    }

    return true;
}

/**
 * Send an async request to the remote backend port
 * @param messageToRemote   the message to be send to backend
 * @param operation         the operation associated with the backend
 * @returns {Promise<unknown>}
 */
function sendToRemoteBackgroundPort(messageToRemote, operation) {
    Object.assign(messageToRemote, {
        op: operation
    });

    console.info(`About to send a request to the OPLauncher backend port: OP[${operation}]`, messageToRemote);

    return new Promise((resolve, reject) => {
        chrome.runtime.sendMessage(messageToRemote, (resp) => {
            if (!resp || !resp.response) {
                console.warn("Received an empty response from the backend", resp);
                return;
            }

            console.info("Received a message from the remote backend port:", resp);
            if (resp.success) {
                resolve(resp);
            }
            else {
                reject(new Error(resp.message));
            }
        });
    });
}

/**
 * Dispatch custom requests to Chrome's beckend framework
 * @param commMsg	the JSON message to be sent to the backend
 */
function dispatchToBackground(commMsg) {
    console.info("About to send the following request to Chrome's backend framework", commMsg);
    chrome.runtime.sendMessage(commMsg, (resp) => {
        if (chrome.runtime.lastError) {
            console.warn(`OP failed ${resp}`);
        }
        else {
            console.info("OP sent successfully:", resp);
        }
    });
}
