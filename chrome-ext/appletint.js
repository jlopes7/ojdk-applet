const ACTION_APPLET_DETAILS_ATTRNAME = "APPLET_DETAILS_WITH_COOKIES"

/**
 * First it scans for <applet/> tags
 */
document.addEventListener("DOMContentLoaded", () => {
	const applets = document.querySelectorAll("applet");
	applets.forEach((applet) => {
		const className = applet.getAttribute("code");
		const archiveUrl = applet.getAttribute("archive");

		console.log ("Found the following Applet", applet);

		// Fetch cookies for the current domain (this is needed to maintain the session with AppServers if necessary)
		chrome.cookies.getAll({ url: window.location.origin }, function (cookies) {
			// Send cookies along with applet details to the background script
			chrome.runtime.sendMessage({
				type: ACTION_APPLET_DETAILS_ATTRNAME,
				className: className,
				archiveUrl: archiveUrl,
				cookies: cookies
			});
		});
	});
});

// Handle responses from the background script
chrome.runtime.onMessage.addListener((message) => {
	if (message.action === "appletResponse") {
		console.log("Applet Response:", message.response);
		// TODO: Implement
	}
});

