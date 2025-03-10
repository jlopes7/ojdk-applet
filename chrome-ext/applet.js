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
    EVT_DOCAPPLET_IS_READY: "evt_document-applet_ready",
    EVT_INVOKE_APPLET_REQ: "evt_invoking-applet_req",
    EVT_INVOKE_APPLET_RES: "evt_invoking-applet_res"
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

                    let appletStub = new AppletInstanceStub(event.data.appletName, event.data.options);

                    document.applets.push(appletStub);
                    document.applet[event.data.appletName] = appletStub;
                    resolve(appletStub);
                }
            }

            // Listen for confirmation
            window.addEventListener(OPResources.EVT_MESSAGE, responseHandler);

            // Send request to content script
            window.postMessage({
                type: OPResources.EVT_REGISTER_APPLET_REQ,
                requestId: requestId,
                appletName: name,
                options: options
            }, "*");
        });
    };

    // Notify content scripts that `document.applet` is ready
    window.postMessage({ type: OPResources.EVT_DOCAPPLET_IS_READY }, "*");
})();

class AppletInstanceStub {
    constructor(name, options) {
        this.name = name;
        this.options = options;

        return new Proxy(this, {
            get: (target, prop) => {
                if (prop in target) {
                    return target[prop]; // Return existing properties
                }

                // Create dynamic method for undefined properties
                return (...args) => this._invoke$(prop, ...args);
            }
        });
    }

    /**
     * Simulates a synchronous call by blocking execution
     */
    async _invoke$(method, ...args) {
        const safeArgs = new Array();

        // Convert only primitive types as arguments, no objects!
        args.forEach((arg) => {
            if ( typeof arg !== "function" ) {
                safeArgs.push(arg);
            }
        });

        const resp = await this.invoke(method, safeArgs);

        console.info("Got a response from OPLauncher for the method (%s) execution: %s", method, resp);

        return resp;
    }
    invoke(method, safeArgs) {
        const requestId = `${this.name}-invoke_${method}-${Date.now()}`;
        const startTime = Date.now();
        let response = null;
        let endWait = false;

        // Async response...
        return new Promise((resolve, reject) => {
            function responseHandler(event) {
                if (event.data.type !== OPResources.EVT_INVOKE_APPLET_RES || event.data.requestId !== requestId) {
                    return;
                }

                window.removeEventListener(OPResources.EVT_MESSAGE, responseHandler);

                console.info("Got a response from JS Applet controller", event.data);
                response = event.data.response;
                endWait = true;

                if (event.data.error) {
                    reject(new Error(`Applet remote execution (${method}) error: ${event.data.error}`));
                }
                else {
                    resolve(event.data.methodResp);
                }
            }

            // isten for response from `appletint.js`
            window.addEventListener(OPResources.EVT_MESSAGE, responseHandler);

            console.info("Remote Method Call --> arguments:", safeArgs);
            // Send request to `appletint.js`
            window.postMessage({
                type: OPResources.EVT_INVOKE_APPLET_REQ,
                requestId: requestId,
                appletName: this.name,
                options: this.options,
                method: method,
                arguments: safeArgs
            }, "*");

            /// Control the timeout to wait for a response (5 seconds is more than enough time)
            function factCheck() {
                if (Date.now() - startTime > 5000 && !endWait) {
                    endWait = true;
                    reject(new Error(`Applet method (${method}) timeout. No response from OPLauncher proxy.`));
                }
                else if (!endWait) setTimeout(factCheck, 10);
            }
            factCheck();
        });
    }
}
