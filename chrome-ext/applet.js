/*
 * Ensure document.applets is an array to store applets
 */
Object.defineProperty(document, "applets", {
    value: new Array(),
    writable: false,
    configurable: true
});

/**
 * Subset of all the resources - loaded into the DOM
 * @type {{EVT_MESSAGE: string, EVT_REGISTER_APPLET_RES: string, EVT_REGISTER_APPLET_BACK_REQ: string, EVT_REGISTER_APPLET_BACK_RES: string, EVT_REGISTER_APPLET_REQ: string, EVT_DOCAPPLET_IS_READY: string}}
 */
const OPResources = {
    EVT_MESSAGE: "message",
    EVT_REGISTER_APPLET_RES: "evt_register-applet_res",
    EVT_REGISTER_APPLET_BACK_REQ: "evt_register-applet_back_req",
    EVT_REGISTER_APPLET_BACK_RES: "evt_register-applet_back_res",
    EVT_REGISTER_APPLET_REQ: "evt_register-applet_req",
    EVT_DOCAPPLET_IS_READY: "evt_document-applet_ready"
};

(function() {
    // Ensure the object is not overwritten
    if (!window.document.applet) {
        window.document.applet = {};
    }

    console.info("=== APPLET JS API Initialized ===");

    /**
     * Listen for messages from `appletint.js`
     * This allows the content script to request `registerApplet`
     */
    window.addEventListener(OPResources.EVT_MESSAGE, (event) => {
        if (event.source !== window || event.data.type !== OPResources.EVT_REGISTER_APPLET_BACK_REQ) return;

        console.info("(applet.js) Received an Applet registration request:", event.data);

        document.applet.registerApplet(event.data.appletName, event.data.options);

        // Send confirmation back to `appletint.js`
        window.postMessage({
            type: OPResources.EVT_REGISTER_APPLET_BACK_RES,
            appletName: event.data.appletName
        }, "*");
    });

    /**
     * Registers an applet with a given name and its properties.
     * @param {string} name - The applet identifier.
     * @param {object} options - Additional parameters (e.g., width, height).
     */
    window.document.applet.registerApplet = function(name, options = {}) {
        if (!name) {
            console.error("Applet name is required.");
            return;
        }

        const requestId = `register-${name}-${Date.now()}`;

        return new Promise((resolve, reject) => {
            function responseHandler(event) {
                if (event.source !== window || event.data.type !== OPResources.EVT_REGISTER_APPLET_RES || event.data.requestId !== requestId) {
                    return;
                }

                window.removeEventListener(OPResources.EVT_MESSAGE, responseHandler);
                if (event.data.error) {
                    console.warn(event.data.error);
                    reject(event.data.error);
                }
                else {
                    console.info(`Applet ${name} registered successfully. Request ${event.data.requestId}`);
                    console.log("Registering the Applet instance from", requestId, event.data.appletObject);

                    document.applets.push(event.data.appletObject);
                    document.applet[event.data.appletName] = event.data.appletObject;
                    resolve(event.data.appletObject);
                }
            }

            // Listen for confirmation
            window.addEventListener(OPResources.EVT_MESSAGE, responseHandler);

            // Send request to content script
            window.postMessage({
                type: OPResources.EVT_REGISTER_APPLET_REQ,
                requestId,
                appletName: name,
                options
            }, "*");
        });
    };

    // Notify content scripts that `document.applet` is ready
    window.postMessage({ type: OPResources.EVT_DOCAPPLET_IS_READY }, "*");
})();
