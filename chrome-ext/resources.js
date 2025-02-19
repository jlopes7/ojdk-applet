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

    DEFAULT_APP_TOKEN: "9C7vzyfe7gU+U$MaM*WQ2:nJQycR%?bT",

    PIPE_STDOUT: "pip_stdout",
    PIPE_REST: "pip_rest",
    PREFERED_PIPE: "pip_rest",

    DEFAULT_MAGICNUM: 0x22E09,

    ALARM_SERVER_HB: "hbCheck",
    HB_CTXROOT: "oplauncher-hb",

    /* Currently controlling if the payload will be encrypted or not */
    ENCRYPTED_PAYLOAD: true,

    /* Default 3DES key */
    DES3_DEF_KEY: "oFcwe0uR6plrVa1eQJljTiqb10clfGaH"
};

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
