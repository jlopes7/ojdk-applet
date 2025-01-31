const OP_LOAD = "load_applet";
const OPLAUNCHER_RESPONSE_CODE = "oplauncher_applet_response";
const APPLET_HTML_CONTENT = `
<html>
  	<head>
  	<style>
    	html, body { 
			width: 100%;
            height: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
            font-family: Verdana, sans-serif;
            font-weight: bold;
            color: darkblue;
            font-size: 14px;
            overflow: hidden; /* Remove scrollbars */
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

		console.info ("Found Applet:", { className, baseUrl, archiveUrl, codebase, appletName, width, height });

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

		let cookies = []
		if (chrome.cookies) {
			cookies = chrome.cookies.getAll({url: window.location.origin});
		}

		if (cookies.length === 0) {
			console.warn('No cookies found for', window.location.origin);
		}
		else {
			console.info("Cookies found in the for the domain %s:", window.location.origin, cookies);
		}

		/// We have to wait a bit for the iframe to be rendered
		setTimeout(() => {
			const position = getAppletElementPosition(iframe);
			const requestMsg = {
				op: 		OP_LOAD,
				className: 	className,
				archiveUrl: archiveUrl,
				codebase: 	codebase,
				appletName: appletName,
				baseUrl: 	baseUrl,
				width: 		width,
				height: 	height,
				posx: 		position.x,
				posy: 		position.y,
				parameters: parametersStr,
				cookies: 	cookies.join(";")
			}

			console.info("About to send the request to backend port", requestMsg);
			/**
			 * ----------------------
			 *  OP: LOAD THE APPLET
			 * ----------------------
			 * --> Send cookies along with applet details to the background script
			 */
			chrome.runtime.sendMessage(requestMsg);
		}, 100); // delay for 100ms to ensure the page is loaded

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
 * Returns the correct Applet position, where it supposed to be rendered in the screen
 * @param element	the DOM element to be instrospected, e.g., &lt applet / &gt
 * @returns {{x: number, y: number}}
 */
function getAppletElementPosition(element) {
	const rect = element.getBoundingClientRect();
	const x = rect.left + window.scrollX;
	const y = rect.top + window.scrollY;

	return { x, y };
}

/**
 * Returns the base URL of where the applet should be loaded from
 */
function getBasePath(urlString) {
	const url = new URL(urlString);
	return url.origin + url.pathname.substring(0, url.pathname.lastIndexOf('/') + 1);
}

