/**
 * Applet background service, used to communicate with the Applet external service
 */
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
	if (message.action === "processApplet") {
		const port = chrome.runtime.connectNative("org.oplauncher.applet_service");

		port.postMessage(message.data); // Send applet details to native host

		port.onMessage.addListener((response) => {
			console.log("Response from native host:", response);

			// Send response back to the content script
			chrome.tabs.sendMessage(sender.tab.id, { action: "appletResponse", response });
		});

		port.onDisconnect.addListener(() => {
			console.error("Native host disconnected.");
		});
	}
});

