let _appletInstances = new Array();
/**
 * Listen for registration requests from `applet.js`
 */
window.addEventListener(OPResources.EVT_MESSAGE, (event) => {
    if ( event.data.type !== OPResources.EVT_REGISTER_APPLET_REQ ) return;

    const { requestId, appletName, options } = event.data;

    console.info("About to process the applet from the request(%s): %s", requestId, appletName);

    // Store the applet in document.applets
    let applet = new AppletInstance(appletName, options);
    _appletInstances.push(applet);

    // Send confirmation back to `applet.js`
    window.postMessage({
        type: OPResources.EVT_REGISTER_APPLET_RES,
        appletObject: applet,
        appletName: appletName,
        requestId: requestId,
        success: true
    }, "*");
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
                    console.error("Applet error:", response.error);
                    reject(response.error);
                    return;
                }

                resolve(response.result);
            });
        });
    }
}