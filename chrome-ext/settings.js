document.addEventListener("DOMContentLoaded", function () {
    const httpPortInput = document.getElementById("httpPort");
    const backendURLInput = document.getElementById("hostURL");
    const contextRootInput = document.getElementById("contextRoot");
    const personalTokenInput = document.getElementById("personalToken");
    const saveButton = document.getElementById("saveSettings");
    const statusMessage = document.getElementById("statusMessage");

    console.info("Settings script loaded successfully.");

    // Load saved settings when the settings page opens
    chrome.storage.local.get(["httpPort", "hostURL", "contextRoot", "personalToken"], function (data) {
        if (chrome.runtime.lastError) {
            console.error("Error accessing storage:", chrome.runtime.lastError);
        }
        else {
            if (data.httpPort) httpPortInput.value = data.httpPort;
            else httpPortInput.value = "7777";

            if (data.hostURL) backendURLInput.value = data.hostURL;
            else backendURLInput.value = "127.0.0.1";

            if (data.contextRoot) contextRootInput.value = data.contextRoot;
            else contextRootInput.value = "oplauncher-op";

            if (data.personalToken) personalTokenInput.value = data.personalToken;
        }
    });

    // Save settings when user clicks "Save"
    saveButton.addEventListener("click", function () {
        const httpPort = httpPortInput.value;
        const hostURL = backendURLInput.value;
        const contextRoot = contextRootInput.value;
        const personalToken = personalTokenInput.value;

        chrome.storage.local.set({ httpPort, hostURL, personalToken, contextRoot }, function () {
            statusMessage.textContent = "Settings saved successfully!";
            setTimeout(() => (statusMessage.textContent = ""), 2000);
        });
    });
});
