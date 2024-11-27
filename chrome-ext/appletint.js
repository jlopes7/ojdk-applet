/**
 * First it scans for <applet/> tags
 */
document.addEventListener("DOMContentLoaded", () => {
	const applets = document.querySelectorAll("applet");
	applets.forEach((applet) => {
		const appletDetails = {
			tagName: "applet",
			attributes: [...applet.attributes].reduce((acc, attr) => {
				acc[attr.name] = attr.value;
				return acc;
			}, {}),
			params: {}
		};

		// Extract <param> tags within the applet
		applet.querySelectorAll("param").forEach((param) => {
			appletDetails.params[param.name] = param.value;
		});

		console.log ("Found the following Applet parameters:", appletDetails);

		// Send applet details to the background script
		chrome.runtime.sendMessage({ action: "processApplet", data: appletDetails });
	});
});

// Handle responses from the background script
chrome.runtime.onMessage.addListener((message) => {
	if (message.action === "appletResponse") {
		console.log("Applet Response:", message.response);
	}
});

