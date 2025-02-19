document.addEventListener("DOMContentLoaded", function () {
    const httpPortInput = document.getElementById("httpPort");
    const backendURLInput = document.getElementById("hostURL");
    const contextRootInput = document.getElementById("contextRoot");
    const personalTokenInput = document.getElementById("personalToken");
    const cipherActive = document.getElementById("msgCipherActive");
    const cipherKeyInput = document.getElementById("cipherKey");
    const cipherKeyContainer = document.getElementById("cipherKeyContainer");
    const saveButton = document.getElementById("saveSettings");
    const statusMessage = document.getElementById("statusMessage");

    const defCipherKey = "oFcwe0uR6plrVa1eQJljTiqb10clfGaH"

    console.info("Settings script loaded successfully.");

    // Load saved settings when the settings page opens
    chrome.storage.local.get(["httpPort", "hostURL", "contextRoot", "personalToken", "msgCipherActive", "cipherKey"], function (data) {
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

            cipherActive.checked = data.msgCipherActive !== undefined ? data.msgCipherActive : true;
            cipherKeyContainer.style.display = cipherActive.checked ? "block" : "none";
            cipherKeyInput.value = data.cipherKey || defCipherKey;
        }
    });

    // Toggle Cipher Key Field Visibility on Checkbox Change
    cipherActive.addEventListener("change", function () {
        cipherKeyContainer.style.display = cipherActive.checked ? "block" : "none";
    });

    // Save settings when user clicks "Save"
    saveButton.addEventListener("click", function () {
        const httpPort = httpPortInput.value;
        const hostURL = backendURLInput.value;
        const contextRoot = contextRootInput.value;
        const personalToken = personalTokenInput.value;
        const msgCipherActive = cipherActive.checked;
        const cipherKey = cipherActive.checked ? cipherKeyInput.value : defCipherKey;

        chrome.storage.local.set({ httpPort, hostURL, personalToken, contextRoot, msgCipherActive, cipherKey }, function () {
            statusMessage.textContent = "Settings saved successfully!";
            setTimeout(() => (statusMessage.textContent = ""), 2000);
        });
    });
});
