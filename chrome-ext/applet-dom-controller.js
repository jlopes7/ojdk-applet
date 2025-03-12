let _appletInstances = new Array();

function isPartOfExclusionMethods(methodName) {
    const notAllowedMethods = ['then', 'catch', 'isPlugin2'];

    for (let currMethodName of notAllowedMethods) {
        if ( methodName === currMethodName ) return true;
    }

    return false;
}

/**
 * Listen for registration requests from `applet.js`
 */
window.addEventListener(OPResources.EVT_MESSAGE, (event) => {
    console.info("===> OPlauncher JS Engine received a DOM event: ", event.data.type);

    /// Process all the events...
    if ( event.data.type == OPResources.EVT_REGISTER_APPLET_REQ ) {
        const {requestId, appletName, options} = event.data;
        console.info("About to process the applet from the request(%s): %s", requestId, appletName);

        // Store the applet in document.applets
        let applet = new AppletInstance(appletName, options);
        _appletInstances.push(applet);

        // Send confirmation back to `applet.js`
        window.postMessage({
            type: OPResources.EVT_REGISTER_APPLET_RES,
            requestId: requestId,
            appletName: appletName,
            options: options,
            success: true
        }, "*");
    }
    else if ( event.data.type == OPResources.EVT_INVOKE_APPLET_REQ ) {
        let methodResponse = null;
        let opCallTime = Date.now();
        let continueProcessing = true;
        let foundApplet = false;
        const {requestId, appletName, method, arguments} = event.data;

        console.info("About to process the invoke function(%s) from the request(%s): %s", method, requestId, appletName);

        function sendMessageBack2View(data) {
            // Send confirmation back to `applet.js`
            window.postMessage({
                type: OPResources.EVT_INVOKE_APPLET_RES,
                requestId: data.requestId,
                appletName: data.appletName,
                methodResp: data.methodResponse,
                success: true
            }, "*");
        }

        /// Some methods are not remote methods
        if ( isPartOfExclusionMethods(method) ) {
            let data = {
                requestId: requestId,
                appletName: appletName,
            }
            if ( method === "isPlugin2") {
                Object.assign(data, {
                    methodResponse: false
                });
            }

            sendMessageBack2View(data);
        }
        else {
            /**
             * Execute the method proxY!
             */
            function executeMethodProxy() {
                console.info("About to send the event data to the backend port", event.data);
                sendToRemoteBackgroundPort(event.data, OPResources.OP_INVOKE_METHOD).then((response) => {
                    if (continueProcessing) {
                        methodResponse = response.methodResponse;
                        console.info("Got a response from the backend port for the method invocation", methodResponse);
                    } else {
                        console.warn("A response was received by the backend port, but it too long to process it so it will be ignored. This could happen due to a latency in the network. The maximun waiting time for method processing is 5s.", response);
                        console.warn("Nothing was changed");
                    }
                }).catch(console.error);
            }


            // Searching for the Applet class definition
            for (applet of _appletInstances) {
                if (applet.name === appletName) {
                    console.info("Applet class instance found", applet);
                    foundApplet = true;

                    if (method === "isActive") methodResponse = applet.isActive();
                    else if (method === "getVersion") methodResponse = applet.getVersion();
                    else if (method === "getVendor") methodResponse = applet.getVendor();
                    else if (method === "getAppVersion") methodResponse = applet.getAppVersion();
                    else if (method === "getProp") methodResponse = applet.getProp(arguments[0]);
                    else if (method === "statusbar") methodResponse = applet.statusbar(arguments[0]);
                    else {
                        executeMethodProxy();
                    }
                    break;
                }
            }
            if (!foundApplet) {
                console.warn(`No applet (${appletName}) found in cache. Running manually (${method})`);
                _appletInstances.push(new AppletInstance(appletName, {}));

                executeMethodProxy();
            }

            /**
             * Await until a response is produced from OPLauncher or a timeout is reached (5s)
             */
            async function awaitForMethodResponse() {
                function controlResponse() {
                    let currTime = Date.now();

                    if (methodResponse === null) {
                        if (Date.now() - opCallTime > OPResources.MAX_WAIT_TIMEOUT_MILLIS) {
                            continueProcessing = false;
                            console.error(`The maximum timeout was reached ${OPResources.MAX_WAIT_TIMEOUT_MILLIS}ms and no response was provided back from the backend port`)
                        } else setTimeout(awaitForMethodResponse, 10);
                    }
                    /// Success !!
                    else {
                        console.info("Consolidated response from the Applet DOM Controller", methodResponse);
                        sendMessageBack2View({
                            requestId: requestId,
                            appletName: appletName,
                            methodResp: methodResponse,
                        });

                        continueProcessing = false;
                    }
                }

                await controlResponse();
            }

            awaitForMethodResponse();
        }
    }

    return true;
});

/**
 * AppletInstance - Represents an individual applet loaded into the system.
 */
class AppletInstance {
    constructor(name, options) {
        this.name = name;
        this.options = options;
    }

    /**
     * Check if the applet is active
     * @returns true if it's active, false otherwise
     */
    async isActive() {
        const result = await this.invoke("isActive");

        if (!response || !response.success) {
            console.error("isActive() failed the call for the Applet (%s): %s", this.name, response);
            return false; // Default to false if there's an error
        }

        // Convert "true"/"false" string into a real boolean
        return response.result.toLowerCase() === "true";
    }

    /**
     * Get the Java version running the applet
     * @returns the Java version
     */
    async getVersion() {
        const result = await this.invoke("getVersion");

        if (!response || !response.success) {
            console.error("getVersion() failed the call for the Applet (%s): %s", this.name, response);
            return "unknown";
        }

        // Just retrieve the version computed by the Applet
        return response.result;
    }

    /**
     * Get the Java vendor
     * @returns the Java vendor for the JVM
     */
    async getVendor() {
        const result = await this.invoke("getVendor");

        if (!response || !response.success) {
            console.error("getVendor() failed the call for the Applet (%s): %s", this.name, response);
            return "unknown";
        }

        // Just retrieve the vendor computed by the Applet
        return response.result;
    }

    /**
     * Get the Applet's internal version
     * @returns the Applet internal version
     */
    async getAppVersion() {
        const result = await this.invoke("getAppVersion");

        if (!response || !response.success) {
            console.error("getAppVersion() failed the call for the Applet (%s): %s", this.name, response);
            return "unknown";
        }

        // Just retrieve the applet internal version computed by the Applet
        return response.result;
    }

    /**
     * Check if the applet is using Java Plugin2 (modern applet runtime)
     * @returns This method will always be false since we are using OPLauncher, and not JP2Launcher
     */
    isPlugin2() {
        return false;
    }

    /**
     * Get a Java system property from the applet
     * @param {string} property - The system property to retrieve (e.g., "java.version")
     * @returns {Promise<string>}
     */
    async getProp(property) {
        const result = await this.invoke("getProp", property);

        if (!response || !response.success) {
            console.error("getProp() failed the call for the Applet (%s): %s", this.name, response);
            return null;
        }

        // Just retrieve the system property computed by the Applet
        return response.result;
    }

    /**
     * Set a message in the browser's status bar (simulate behavior)
     * @param {string} message - The message to display
     */
    statusbar(message) {
        console.log("[Applet StatusBar]:", message);
        this.invoke("statusbar", message).then(result => {
            console.info("Processed the status bar Applet request", result);
        }).catch(err => {
            console.error("An error ocurred while processing the status bar Applet request", err);
        });
    }

    /**
     * Sends a method invocation to the OPLauncher backend.
     * @param {string} method - The applet method to invoke.
     * @param {any[]} args - Arguments to pass to the method.
     * @returns {Promise<any>}
     */
    invoke(method, ...args) {
        return new Promise((resolve, reject) => {
            chrome.runtime.sendMessage({
                op: OPResources.OP_INVOKE_METHOD,
                appletName: this.name,
                method,
                args
            }, (response) => {
                if (chrome.runtime.lastError) {
                    console.error("Error invoking applet method:", chrome.runtime.lastError);
                    reject(chrome.runtime.lastError);
                    return;
                }
                if (response?.error) {
                    console.error("Applet error:", response.message);
                    reject(response);
                    return;
                }

                resolve(response.result);
            });
        });
    }
}