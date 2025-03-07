/*
 * Inject the applet JS so it can override the document.applets API
 */
(() => {
    addHeadCBCallback(() => {
        const file = "applet.js";

        const script = document.createElement("script");
        const scriptUrl = chrome.runtime.getURL(file);

        console.info("About to inject the JS file into the processing HTML:", scriptUrl);

        script.src = scriptUrl;
        script.type = "text/javascript";

        const head = document.head || document.getElementsByTagName("head")[0];
        console.info("HEAD DOM script", script);
        // Insert the script before the first script tag in <head>
        if (head.firstChild) {
            console.info("FIRST CHILD", head.firstChild);
            head.insertBefore(script, head.firstChild);
        }
        else {
            console.info("WILL BE SET TO FIRST");
            head.appendChild(script);
        }

        // After the script load, we simply remove all references from the DOM
        script.onload = function () {
            this.remove(); // Cleanup
        }
    });
})();
