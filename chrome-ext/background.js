importScripts("crypto-js.min.js", "resources.js");
console.info("Resources loaded successfully:", OPResources);

// Control flag to activate or de-activate the message sent to the background
let BackendControlReady = false;
let BackendConnActive = false;
let TriggeredHBControl = false;

// Saves the token
let commToken = null;

if (OPResources.DEBUG) {
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

async function activateConnSwitch() {
    BackendConnActive = true;
    console.info("Connection switch is activated!");
    return true;
}
async function deactivateConnSwitch() {
    BackendConnActive = false;
    console.info("Connection switch is deactivated!");
    return true;
}
function isConnSwitchActive() {
    return BackendConnActive;
}

/**
 * Applet background service, used to communicate with the Applet external service
 */
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    console.info(`OP selected: ${message.op}. Message:`, message);
    if (message.op === OPResources.OP_COOKIES) {
        chrome.cookies.getAll({ url: message.url }, (cookies) => {
            if (chrome.runtime.lastError) {
                console.error("Error retrieving cookies:", chrome.runtime.lastError);
                sendResponse({ cookies: "" });
                return;
            }

            // Format cookies as "key=value; key2=value2"
            const cookieStr = cookies.map(cookie => `${cookie.name}=${cookie.value}`).join("; ");
            console.info("Sending cookies to content script:", cookieStr);
            sendResponse({ cookies: cookieStr });
        });

        return true;
    }
    else if (message.op === OPResources.OP_LOAD) {
        console.info("About to load the OJDK Applet Launcher for the applet:", message.appletName);

        // Activate the connection switch
        if (!isConnSwitchActive()) activateConnSwitch();

        /*
         * Option 0: Selects the correct Pipe!
         */
        if ( message.firstload || message.pipecfn === OPResources.PIPE_STDOUT) {
            // Activates the connection switch

            /*
             * Option 1: Loads the Applet base code and sends its bits as B64 across the wire
             * -> May be a valid option for the future, but for now is deactivated
             */
            if (OPResources.FETCH_REMOTEAPPLET) {
                console.info("Option 1 (fetch remote on Chrome Extension) was selected");

                // Extract the applet details from the message
                const {archiveUrl, codeUrl} = message;

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
                                commToken = resp.message;
                                console.info("Got the token back from OPLauncher: %s", commToken);
                                // Send response back to the content script
                                chrome.tabs.sendMessage(sender.tab.id, {action: OPResources.OPLAUNCHER_RESPONSE_CODE, response});
                            } else {
                                console.warn("Sender tab ID is missing. Cannot send message back.");
                            }
                        }, /*Errors, including oplauncher disconnects*/ (err) => {
                            console.warn("The native handler OPLauncher was disconnected. Clearing all events. Error: ", err);
                            chrome.tabs.sendMessage(sender.tab.id, {action: OPResources.OP_CLEAR_EVT, err});
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
                    let obj = {action: OPResources.OP_LOAD, response: resp};
                    console.info("Got a response back from the native host", resp)
                    console.info("Sending the response back to the UI", obj);

                    commToken = resp.message;
                    console.info("Request token saved for future requests:", commToken);

                    sendResponse(obj);
                }, /*Errors, including oplauncher disconnects*/ (err) => {
                    console.warn("The native handler OPLauncher was disconnected. Clearing all events. Message: ", err);
                    chrome.tabs.sendMessage(sender.tab.id, {action: OPResources.OP_CLEAR_EVT, err});
                });
            }
        }
        else if ( message.pipecfn === OPResources.PIPE_REST ) {
            let payload = {};
            console.info("Transforming the applet message for a REST OP:", message.appletName);

            Object.assign(payload, {
                op: message.op,
                applet_name: message.appletName,
                params: [
                    message.op,
                    message.baseUrl,
                    message.codebase,
                    message.archiveUrl,
                    message.appletName,
                    `width=${message.width};height=${message.height};posx=${message.posx};posy=${message.posy};`.concat(message.parameters),
                    message.className
                ]
            });
            console.info("Parsed message payload for(%s) OPLauncher: ", message.appletName, payload);

            send2OPLauncherJSONPort(payload, (resp, port) => {
                console.info("Got a response back from the 'unload' OP from the OPLauncher", resp);
                sendResponse ( resp );
            });
        }
        else {
            console.error("Incorrect PIPE configuration [%s]. No changes were applied to the system", message.pipecfn);
        }
    }
    else if (message.op === OPResources.OP_UNLOAD) {
        console.info("About to unload the OJDK Applet Launcher");
        send2OPLauncherJSONPort(message, (resp, port) => {
            console.info("Got a response back from the 'unload' OP from the OPLauncher", resp);
            sendResponse ( resp );
        });
    }
    else if (message.op === OPResources.OP_BLUR) {
        console.info("About to blur the OJDK Applet Launcher");
        send2OPLauncherJSONPort(message, (resp, port) => {
            console.info("Got a response back from the 'blur' OP from the OPLauncher", resp);
            sendResponse ( resp );
        });
    }
    else if (message.op === OPResources.OP_FOCUS) {
        console.info("About to focus the OJDK Applet Launcher");
        send2OPLauncherJSONPort(message, (resp, port) => {
            console.info("Got a response back from the 'focus' OP from the OPLauncher", resp);
            sendResponse ( resp );
        });
    }
    else if (message.op === OPResources.OP_MOVE) {
        console.info("About to move the OJDK Applet Launcher");
        send2OPLauncherJSONPort(message, (resp, port) => {
            console.info("Got a response back from the 'move' OP from the OPLauncher", resp);
            sendResponse ( resp );
        })
    }
    else {
        console.warn(`OP selected no supported: ${message.op} . Expected OPs: ${OPResources.OP_LOAD}, ${OPResources.OP_UNLOAD}, ${OPResources.OP_MOVE}, ${OPResources.OP_BLUR}, ${OPResources.OP_FOCUS}`);
    }

    // keeps the channel open
    return true;
});

/**
 * Send the mssage to the OP Server using Chrome's HTTP/2 JSON port
 * @param messageToNative   the applet computed message
 * @param callback          the callback function. Called when the applet responds back to the Browser
 * @param callbackErr       the error callback function. Called when something bad happened
 * @returns {boolean}       for async processing
 */
function send2OPLauncherJSONPort(messageToNative, callback, callbackErr) {
    const MAX_TRIES = 10; // 10 seconds
    let try_attempts = 1;
    let ctrl_interval;

    // Safe way only sending a request when the port is open for connection in the background
    function safeSendRequest() {
        if (messageToNative.op) { // don't run on undefined Intervals ... JS bug
            if (BackendControlReady) {
                console.info("OPLauncher backend port is ready! Sending request...");
                chrome.storage.local.get([OPResources.CHROME_PROP_HTTPPORT,
                                          OPResources.CHROME_PROP_HOSTURL,
                                          OPResources.CHROME_PROP_CTXROOT,
                                          OPResources.CHROME_PROP_APPTKN,
                                          OPResources.CHROME_PROP_CIPHERKEY,
                                          OPResources.CHROME_PROP_CIPHACT], function (config) {
                    const host = config.hostURL || "127.0.0.1";
                    const contextRoot = config.contextRoot || "oplauncher-op";
                    const port = config.httpPort || 7777;
                    const token = config.personalToken || OPResources.DEFAULT_APP_TOKEN;
                    const backendURL = `http://${host}:${port}/${contextRoot}`;
                    const cipherKey = config.cipherKey;

                    if (messageToNative) {
                        Object.assign(messageToNative, {
                            _tkn_: token
                        });
                    }

                    if ( config.msgCipherActive ) {
                        encryptPayloadWithDES3(messageToNative, commToken || config.cipherKey || OPResources.DES3_DEF_KEY, OPResources.DEFAULT_MAGICNUM,
                            (encpayload) => {

                            console.info("Payload was encrypted and it's ready to be sent to OPLauncher", encpayload);
                            const requestMsg = JSON.stringify(encpayload);
                            // Send to OPLauncher
                            send2port(OPResources.JSON_BACKEND, requestMsg, backendURL, callback, callbackErr);
                        }, true);
                    }
                    else {
                        const requestMsg = JSON.stringify(messageToNative);
                        console.info("Received unload message from content script. Sending to backend...", messageToNative);
                        // Send to OPLauncher
                        send2port(OPResources.JSON_BACKEND, requestMsg, backendURL, callback, callbackErr);
                    }
                });

                if (ctrl_interval) clearInterval(ctrl_interval);
            }
            else {
                console.warn("OPLauncher backend port is not available yet for (%s). Attempt: %s", messageToNative.appletName, try_attempts);
                try_attempts++;
                if (try_attempts > MAX_TRIES) {
                    console.error("After %s tries, the remote connection door could not be reached, giving up! Applet: %s := %s", try_attempts, messageToNative.op, messageToNative.appletName);
                    if (callbackErr) callbackErr(new Error("OPLauncher backend port is not available"));
                    clearInterval(ctrl_interval);
                }
            }
        }
    }

    if (isConnSwitchActive()) {
        BackendControlReady = false;
        // Lets check start the HB if necessary
        startOPHBCheck();
        // Starts the thread loop to check if the connection is available or not
        ctrl_interval = setInterval(safeSendRequest, 1000); // ping on every second...
    }
    else {
        console.warn("The connection switch is de-activated, no request was made. Reload the page to re-activate the OPLauncher connection.")
    }

    return true;
}

/**
 * Send the payload to the backend port based on the PROTO send as parameter
 */
function send2port(proto, requestMsg, backendURL, callback, callbackErr) {
    if ( proto === OPResources.JSON_BACKEND ) {
        console.info("Sending payload to backend URL:", backendURL);
        fetch(backendURL, {
            method: "POST",
            body: requestMsg,
            headers: {
                "Content-Type": "application/json",
                "X-Chrome-Extension-Tkn": chrome.runtime.id
            },
        })
        .then(resp => resp.json())
        .then(data => {
            console.info("Successfully receive a response from the OP Server")
            if (callback) callback(data);
        })
        .catch(error => {
            console.warn("Failed to send unload message via fetch:", error);
            if (callbackErr) callbackErr(error);
        });
    }
    // TODO: Implement other port types
    else {
        throw new Error(`Unsupported proto: ${proto}`);
    }
}

/**
 * Sends a message to OPLauncher
 * @param messageToNative   the applet computed message
 * @param callback          the callback function. Called when the applet responds back to the Browser
 * @param callbackErr       the error callback function. Called when something bad happened
 */
function send2OPLauncher(messageToNative, callback, callbackErr) {
    if (!messageToNative || typeof messageToNative !== "object" || Object.keys(messageToNative).length === 0) {
        console.error("Empty message detected! Aborting...", messageToNative);
        return;
    }

    chrome.storage.local.get([OPResources.CHROME_PROP_CIPHERKEY,
                              OPResources.CHROME_PROP_CIPHACT], function (config) {
        // We need to generate the payload if set
        if (config.msgCipherActive) {
            encryptPayloadWithDES3(messageToNative, commToken || config.cipherKey || OPResources.DES3_DEF_KEY, OPResources.DEFAULT_MAGICNUM,
                (encpayload) => {
                    console.info("About to send the encoded payload to OPLauncher", encpayload);
                    send2native(encpayload, callback, callbackErr);
            }, false);
        }
        // Payload with no ecryption, simple payload
        else {
            send2native(messageToNative, callback, callbackErr);
        }
    });

}
function send2native(messageToNative, callback, callbackErr) {
    let jsonMessage = JSON.stringify(messageToNative);
    console.info("Validating Message Before Sending to OPLauncher:", JSON.stringify(messageToNative, null, 2));

    const port = chrome.runtime.connectNative(OPResources.NATIVE_SERVICE);

    console.info("About to send a message to OPLauncher:", messageToNative);
    port.postMessage(messageToNative); // Send applet details and file content to native host
    console.info("Message to OPLauncher sent");

    port.onMessage.addListener((response) => {
        console.info("Response from native host:", response);

        // Calls the return function (callback)
        callback(response, port);
    });

    // Ensure onDisconnect is only added once
    port.onDisconnect.addListener(() => {
        console.warn("Native host (OPLauncher) was disconnected.");
        if (callbackErr) callbackErr("Connection was closed");

        // If the connection is not active, the switch should be de-activated and it will
        // only be active again after the page reload
        if (isConnSwitchActive()) deactivateConnSwitch();
    });
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

/**
 * Checks to see if the backend server is active or not
 */
function checkBackendStatus(hbInterval) {
    if (BackendControlReady) {
        clearInterval(hbInterval);
        return;
    } // Skip check if already ready

    chrome.storage.local.get([OPResources.CHROME_PROP_HTTPPORT,
                              OPResources.CHROME_PROP_HOSTURL,
                              OPResources.CHROME_PROP_APPTKN], function (config) {
        const host = config.hostURL || "127.0.0.1";
        const port = config.httpPort || 7777;
        const token = config.personalToken || OPResources.DEFAULT_APP_TOKEN;
        const backendURL = `http://${host}:${port}/${OPResources.HB_CTXROOT}`;

        console.info("HB status check on:", backendURL);

        fetch(backendURL, { method: "GET" })
        .then(response => {
            if (response.ok) {
                console.info("OPLauncher backend server is now available.");
                BackendControlReady = true;
                TriggeredHBControl = false;
                //chrome.alarms.clear(OPResources.ALARM_SERVER_HB); // Stop checking once backend is ready
                clearInterval(hbInterval);
            }
            else {
                console.warn("Wrong response from the backend, not ready:", response.status);
            }
        })
        .catch(error => {
            console.warn("Backend server no ready yet:", error);
        });
    });
}

/**
 * Starts the ALARM for checking the OPLauncher backend port
 */
function startOPHBCheck() {
    /*chrome.alarms.onAlarm.addListener((alarm) => {
        console.debug("Received an alarm", alarm);
        if (alarm.name === OPResources.ALARM_SERVER_HB) {
            checkBackendStatus();
        }
    });
    chrome.alarms.create(OPResources.ALARM_SERVER_HB, { periodInMinutes: (1 / 60) }); // 1min / 60 seconds ~ 1 sec*/
    /*
     * Creates an Alarm check to see if OPLauncher REST server is active yet or not
     */
    console.info("Starting OPHB check. Triggering status: ", TriggeredHBControl);
    if ( !TriggeredHBControl ) {
        TriggeredHBControl = true;
        const hbInterval = setInterval(() => {
            checkBackendStatus(hbInterval);
        }, 500 /*every 0.5 sec*/);
    }
}

/**
 * Generates a random number from 1000 to 10,000,000
 * @returns {number}
 */
function getRandomNumber() {
    return Math.floor(Math.random() * (10000000 - 1000 + 1)) + 1000;
}

/**
 * Encrypts the payload using the given key passed as parameter
 * @param jsonData      the JSON data to be encrypted
 * @param base64Key     the DES3 key encoded in a Base64 string format
 * @param callback      the callback function
 * @returns {{payload: string, msgsize: number}}
 */
async function encryptPayloadWithDES3(jsonData, base64Key, magicmsk, callback, compact) {
    const rmaskednum = getRandomNumber() | magicmsk;
    console.info("Magic number: ", rmaskednum);

    if (compact) {
        Object.assign(jsonData, {
            mt: rmaskednum
        });
    }
    else {
        Object.assign(jsonData, {
            magicToken: rmaskednum
        });
    }

    // Convert Base64 key to a WordArray (CryptoJS format)
    const keyBytes = CryptoJS.enc.Base64.parse(base64Key);

    // Converts the JSON payload (remove breaklines for correct size)
    const jsonString = JSON.stringify(jsonData).replace(/\r?\n|\r/g, "");

    console.info("About to cipher the payload: %s", jsonString);
    console.info("PAYLOAD size: ", jsonString.length);
    console.debug("-> PAYLOAD key: ", base64Key);

    // Encrypt using DES3 with ECB mode and PKCS7 padding
    const encrypted = CryptoJS.TripleDES.encrypt(jsonString, keyBytes, {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
    });

    // Convert encrypted bytes to Base64
    const encryptedBase64 = encrypted.toString();
    console.info("Encrypted payload: %s", encryptedBase64);

    // Return the formatted message
    if (callback) {
        if (compact) {
            callback({
                p: encryptedBase64,
                msz: jsonString.length
            });
        }
        else {
            callback({
                payload: encryptedBase64,
                msgsize: jsonString.length
            });
        }
    }
}

