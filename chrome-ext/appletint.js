const OP_LOAD = "load_applet";
const OPLAUNCHER_RESPONSE_CODE = "oplauncher_applet_reponse";
const APPLET_HTML_CONTENT = `
<html>
  	<head>
  	<style>
    	body { 
			font-family: Verdana, sans-serif; 
			font-size: 12px; 
			display: flex; 
			justify-content: center; 
			align-items: center; 
			width: 100%; 
			height: 100%;
			text-align: center;
        }
    </style>
    </head>
    <body>OJDK Applet Launcher loaded</body>
</html>
`;

/**
 * First it scans for <applet/> tags
 */
document.addEventListener("DOMContentLoaded", () => {
	console.info("Looking for Applet entries in the page...");
	const applets = document.querySelectorAll("applet");
	applets.forEach((applet) => {
		const className = applet.getAttribute("code");
		const archiveUrl = applet.getAttribute("archive") || "";
		const codebase = applet.getAttribute("codebase") || "";
		const appletName = applet.getAttribute("name") || "";
		const width = applet.getAttribute("width") || 0;
		const height = applet.getAttribute("height") || 0;
		// Construct the full base URL
		const baseUrl = getBasePath(window.location.href);

		console.info ("Found Applet:", { className, baseUrl, archiveUrl, codebase });

		// Create an iframe replacement for the applet
		const iframe = document.createElement("iframe");
		iframe.width = width;
		iframe.height = height;
		iframe.style.border = "none";
		iframe.style.backgroundColor = "white";

		// Set the iframe content dynamically
		iframe.srcdoc = APPLET_HTML_CONTENT;
		// Replace the applet with the iframe
		applet.replaceWith(iframe);

		// Extract applet parameters and format them as "key1=value1;key2=value2"
		let paramArray = [];
		applet.querySelectorAll("param").forEach((param) => {
			const paramName = param.getAttribute("name");
			const paramValue = param.getAttribute("value");
			if (paramName && paramValue) {
				paramArray.push(`${paramName}=${paramValue}`);
			}
		});
		const parametersStr = paramArray.join(";");

		// Fetch cookies for the current domain (this is needed to maintain the session with AppServers if necessary)
		chrome.cookies.getAll({ url: window.location.origin }, function (cookies) {
			/**
			 * ----------------------
			 *  OP: LOAD THE APPLET
			 * ----------------------
			 * --> Send cookies along with applet details to the background script
 			 */
			chrome.runtime.sendMessage({
				op: 		OP_LOAD,
				className: 	className,
				archiveUrl: archiveUrl,
				codebase: 	codebase,
				appletName: appletName,
				baseUrl: 	baseUrl,
				width: 		width,
				height: 	height,
				parameters: parametersStr,
				cookies: 	cookies
			});
		});

		// TODO: Additional operations go here!
	});
});

// Handle responses from the background script
chrome.runtime.onMessage.addListener((message) => {
	if (message.action === OPLAUNCHER_RESPONSE_CODE) {
		console.log("Applet Response:", message.response);
		// TODO: Implement
	}
});

/**
 * Returns the base URL of where the applet should be loaded from
 */
function getBasePath(urlString) {
	const url = new URL(urlString);
	return url.origin + url.pathname.substring(0, url.pathname.lastIndexOf('/') + 1);
}

