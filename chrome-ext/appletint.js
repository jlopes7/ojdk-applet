const OP_LOAD    = "load_applet";
const OP_COOKIES = "get_cookies";

const OPLAUNCHER_RESPONSE_CODE = "oplauncher_applet_response";

const OPLAUNCHER_IFRAME_ID = "oplauncher_applet_iframe";
const APPLET_HTML_CONTENT_OPEN = `
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
            background: bisque;
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
		iframe.id = OPLAUNCHER_IFRAME_ID;

		// Set the iframe content dynamically
		iframe.srcdoc = APPLET_HTML_CONTENT_OPEN;
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
		getCookies(function (message) {
			let cookieStr = "";
			if ( message.cookies ) {
				cookieStr = message.cookies.map(cookie => `${cookie.name}=${cookie.value}`).join("; ");
				}

			console.info(`Cookies found: [${cookieStr}]`);

			// Ensure message is sent after fetching cookies
			sendAppletMessage(cookieStr);
		});

		/**
		 * Send the applet message
		 */
		function sendAppletMessage(cookieStr) {
			const position = getAbsoluteScreenPosition(iframe);
			const requestMsg = {
				op: OP_LOAD,
				className: className,
				archiveUrl: archiveUrl,
				codebase: codebase,
				appletName: appletName,
				baseUrl: baseUrl,
				width: width,
				height: height,
				posx: position.x,
				posy: position.y,
				parameters: parametersStr,
				cookies: cookieStr
			};

			/// We have to wait a bit for the iframe to be rendered
			//setTimeout(() => {
				if (Object.keys(requestMsg).length > 0) {
					console.info("About to send the request to backend port", requestMsg);
					/**
					 * ----------------------
					 *  OP: LOAD THE APPLET
					 * ----------------------
					 * --> Send cookies along with applet details to the background script
					 */
					chrome.runtime.sendMessage(requestMsg);
				}
				else {
					console.error("Request payload is empty, not sending!");
				}
			//}, 100); // delay for 100ms to ensure the page is loaded
		}

		// TODO: Additional operations go here!
	});
});

function getCookies(callback) {
	console.info("Requesting cookies from background script...");

	chrome.runtime.sendMessage({ op: OP_COOKIES, url: window.location.origin }, (response) => {
		if (chrome.runtime.lastError) {
			console.error("rror requesting cookies:", chrome.runtime.lastError.message);
			callback("{}");
			return;
		}

		console.info("Cookies received:", response.cookies);
		callback(response.cookies || "");
	});
}


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
 * Tries to retrieve the absolute position of a DOM element
 * @param element	the DOM element to verify the position
 * @returns {{x: number, y: number}}
 */
function getAbsoluteScreenPosition(element) {
	const rect = element.getBoundingClientRect();

	// Get browser window offset
	const windowX = window.screenX || window.screenLeft; // Leftmost screen position
	const windowY = window.screenY || window.screenTop;  // Topmost screen position

	// Get browser's top UI height (tabs + address bar)
	const browserTopOffset = window.outerHeight - window.innerHeight;

	// Adjust for page scrolling + browser UI
	const x = parseInt (rect.left + windowX);
	const y = parseInt (rect.top + windowY + browserTopOffset);

	console.log(`Applet absolute Screen Position - X: ${x}, Y: ${y}`);
	return { x, y };
}

/**
 * Returns the base URL of where the applet should be loaded from
 */
function getBasePath(urlString) {
	const url = new URL(urlString);
	return url.origin + url.pathname.substring(0, url.pathname.lastIndexOf('/') + 1);
}

