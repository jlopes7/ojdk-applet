/**
 * Define all the overall constants to be used by the extension
 */
let OPResources = {
    OP_LOAD: 'load_applet',
    OP_UNLOAD: 'unload_applet',
    OP_COOKIES: 'get_cookies',
    OP_BLUR: "blur_applet",
    OP_FOCUS: "focus_applet",
    OP_MOVE: "move_applet",
    OP_CLEAR_EVT: "clear_events",
    OP_INVOKE_METHOD: "method_invoke_applet",

    NATIVE_SERVICE: "org.oplauncher.applet_service",
    OPLAUNCHER_RESPONSE_CODE: "oplauncher_applet_response",
    OPLAUNCHER_IFRAME_ID: "oplauncher_applet_iframe",
    FETCH_REMOTEAPPLET: false,
    DEBUG: false,

    CHROME_PROP_HTTPPORT: "httpPort",
    CHROME_PROP_HOSTURL: "hostURL",
    CHROME_PROP_CTXROOT: "contextRoot",
    CHROME_PROP_APPTKN: "personalToken",
    CHROME_PROP_CIPHACT: "msgCipherActive",
    CHROME_PROP_CIPHERKEY: "cipherKey",

    JSON_BACKEND: "json",
    WS2_BACKEND: "websocket",
    SELECTED_BACKEND_TP: "json",

    JAVA_MIME_TYPE: "application/x-java-applet",

    EVT_MESSAGE: "message",
    EVT_REGISTER_APPLET_RES: "evt_register-applet_res",
    EVT_REGISTER_APPLET_REQ: "evt_register-applet_req",
    EVT_REGISTER_APPLET_BACK_REQ: "evt_register-applet_back_req",
    EVT_REGISTER_APPLET_BACK_RES: "evt_register-applet_back_res",
    EVT_DOCAPPLET_IS_READY: "evt_document-applet_ready",
    EVT_INVOKE_APPLET_REQ: "evt_invoking-applet_req",
    EVT_INVOKE_APPLET_RES: "evt_invoking-applet_res",

    DEFAULT_APP_TOKEN: "9C7vzyfe7gU+U$MaM*WQ2:nJQycR%?bT",

    PIPE_STDOUT: "pip_stdout",
    PIPE_REST: "pip_rest",
    PREFERED_PIPE: "pip_rest",

    DEFAULT_MAGICNUM: 0x22E09,
    MAX_WAIT_TIMEOUT_MILLIS: 5000,

    ALARM_SERVER_HB: "hbCheck",
    HB_CTXROOT: "oplauncher-hb",

    /* Currently controlling if the payload will be encrypted or not */
    ENCRYPTED_PAYLOAD: true,

    /* Default 3DES key */
    DES3_DEF_KEY: "oFcwe0uR6plrVa1eQJljTiqb10clfGaH"
};

let _afterHeadCBList = new Array();
function addHeadCBCallback(cb) {
    if (cb) _afterHeadCBList.push(cb);
}

/**
 * Asynchronous function to be called after the head element is finally inserted into the HTML DOM
 */
let _uniqueCall = true;
async function afterHeadDispatcher() {
    const head = document.head || document.getElementsByTagName("head")[0];
    if (!head) {
        // If <head> is not ready, retry in 10ms
        return setTimeout(afterHeadDispatcher, 10);
    }

    if (_uniqueCall) {
        _uniqueCall = !_uniqueCall;
        // Trigger all the functions
        for (const cb of _afterHeadCBList) {
            if (cb) cb();
        }
    }
}

// Gate switch based on where the script is loaded from
if (typeof window !== "undefined") {
    // Running in content script
    window.OPResources = OPResources;
    console.info("OPResources is now globally available in content script.");
}
else if (typeof self !== "undefined" && typeof importScripts === "function") {
    // Running in service worker mode
    self.OPResources = OPResources;
    console.info("OPResources is now available in service worker mode.");
}
else {
    // Other environments
    console.error("OPResources could not be assigned to a global scope.");
}

globalThis.OPResources = OPResources;
