/**
 * Loads all the script dependencies
 * @param callback	the function function after all resources are loaded into DOM
 */
function loadDeps(callback) {
	if (typeof window.OPResources !== "undefined" ) {//&& document.applet) {
		if (callback) callback();
	}
	else {
		console.error("OPResources is not available after loading resources.js!");
	}
}

const LOW_VISIBILITY_ST = "low_visibility";

const OPLAUNCHER_RESPONSE_CODE = "oplauncher_applet_response";

const registeredAppletList = new Array();

let frame_count = 0;
let firstAppletLoaded = true;

/**
 * Wait until the head section of the HTML is loaded to register all
 * the applets together
 * @param timeout
 * @param interval
 * @returns {Promise<DOMElement>}
 */
function waitForHead(timeout = OPResources.MAX_WAIT_TIMEOUT_MILLIS, interval = 50) {
	return new Promise((resolve, reject) => {
		const startTime = Date.now();

		function checkHead() {
			if (document.head) {
				resolve(document.head); // `<head>` is available
				return;
			}
			if (Date.now() - startTime > timeout) {
				reject(new Error("Timeout: head section not found!"));
				return;
			}

			setTimeout(checkHead, interval); // Retry after `interval` ms
		}

		checkHead(); // Start checking
	});
}

/**
 * Await until the `document.applet` object is available from the DOM
 * @param timeout
 * @returns {Promise<{registerApplet}|*|{}|null>}
 */
function waitForDocumentApplet(appletName, options, timeout = OPResources.MAX_WAIT_TIMEOUT_MILLIS) {
	return new Promise((resolve, reject) => {
		const startTime = Date.now();

		function onDocumentAppletReady(event) {
			if (event.source !== window || event.data.type !== OPResources.EVT_DOCAPPLET_IS_READY) return;

			window.removeEventListener(OPResources.EVT_MESSAGE, onDocumentAppletReady);

			registerAppletFromContentScript(appletName, options).then(resolve).catch(reject);
		}

		// Listen for `document.applet` ready message
		window.addEventListener(OPResources.EVT_MESSAGE, onDocumentAppletReady);

		// Fallback timeout in case the event never fires
		setTimeout(() => {
			window.removeEventListener(OPResources.EVT_MESSAGE, onDocumentAppletReady);
			reject(new Error("Timeout waiting for document.applet"));
		}, timeout);
	});
}
/**
 * Sends a message to `applet.js` to register the applet
 */
function registerAppletFromContentScript(appletName, options = {}) {
	return new Promise((resolve, reject) => {
		console.info(`Requesting to register the applet into (applet.js): ${appletName} => ${options}`);

		window.postMessage({
			type: OPResources.EVT_REGISTER_APPLET_BACK_REQ,
			appletName,
			options
		}, "*");

		window.addEventListener(OPResources.EVT_MESSAGE, (event) => {
			if (event.data.type !== OPResources.EVT_REGISTER_APPLET_BACK_RES) return;

			if (event.data.appletName) {
				resolve(event.data.appletName);
			}
			else {
				reject(new Error("(applet.js) Applet could not be registered!"));
			}
		});
	});
}


/* ===========================================================
 	HTML CONTENT FOR THE APPLET IFRAME
   =========================================================== */
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
    <body id="status">OJDK Applet Launcher loading...</body>
</html>
`;
/* =========================================================== */

/**
 * First it scans for <applet/> tags
 */
document.addEventListener("DOMContentLoaded", () => {
	/*
	 * Dynamically load all the dependent resources before executing
	 */
	loadDeps(() => {
		console.info("The JS API for 'document.applet' is loaded successfully", document.applet);
		console.info("Resources loaded successfully:", OPResources);

		console.info("Looking for Applet entries in the page...");
		const applets = document.querySelectorAll("applet");
		const appletObjs = document.querySelectorAll("object");
		const embededObjs = document.querySelectorAll("embed");
		const appletInstanceList = new Array();

		console.info("Processing the Applet tags...");
		applets.forEach((appletEl) => {
			console.log("applet", appletEl);
			const className = appletEl.getAttribute("code");
			const archiveUrl = appletEl.getAttribute("archive") || "";
			const codebase = appletEl.getAttribute("codebase") || "";
			const appletName = appletEl.getAttribute("name") || genRandomAppletName(16);
			const width = appletEl.getAttribute("width") || 0;
			const height = appletEl.getAttribute("height") || 0;

			// register the Applet instance
			appletInstanceList.push(appletName);

			// Extract applet parameters and format them as "key1=value1;key2=value2"
			let paramArray = [];
			appletEl.querySelectorAll("param").forEach((param) => {
				const paramName = param.getAttribute("name");
				const paramValue = param.getAttribute("value");
				if (paramName && paramValue) {
					paramArray.push(`${paramName}=${paramValue}`);
				}
			});

			// Process the Applet tag
			processAppletDefinition(appletEl, className, archiveUrl, codebase, appletName, width, height, paramArray,
				(resp, iframe) => {
					iframe.contentDocument.getElementById("status").innerHTML = 'OJDK Applet Launcher loaded!';
				});
		});

		console.info("Processing the Object tags...");
		appletObjs.forEach((objectEl) => {
			const elType = objectEl.getAttribute("type");

			if ( elType === OPResources.JAVA_MIME_TYPE ) {
				let className="", archiveUrl="", codebase="", appletName=genRandomAppletName(16),
					width=objectEl.getAttribute("width") || 0,
					height=objectEl.getAttribute("height") || 0;
				console.info("Found an applet to be processed in the Object element!");

				// register the Applet instance
				appletInstanceList.push(appletName);

				let additionalParams = new Array();
				const obParams = objectEl.querySelectorAll("param");
				obParams.forEach((param) => {
					const paramName = param.getAttribute("name");
					const paramValue = param.getAttribute("value") || "";

					switch (paramName) {
						case "codebase": codebase = paramValue; break;
						case "archive": archiveUrl = paramValue; break;
						case "code": className = paramValue; break;
						case "width": width = paramValue; break;
						case "height": height = paramValue; break;
						case "name": appletName = paramValue; break;
						default: {
							additionalParams.push(`${paramName}=${paramValue}`);
						}
					}
				});

				// Process the Object tag
				processAppletDefinition(objectEl, className, archiveUrl, codebase, appletName, width, height, additionalParams,
				(resp, iframe) => {
					iframe.contentDocument.getElementById("status").innerHTML = 'OJDK Applet Launcher loaded from Object element!';
				});
			}
			else {
				console.warn("Object element is not an Applet definition:", objectEl);
			}
		});

		console.info("Processing the Embed tags...");
		embededObjs.forEach((embedEl) => {
			const elType = embedEl.getAttribute("type");

			if ( elType === OPResources.JAVA_MIME_TYPE ) {
				const className = embedEl.getAttribute("code") || "",
					archiveUrl = embedEl.getAttribute("archive") || "",
					codebase = embedEl.getAttribute("codebase") || "",
					appletName = embedEl.getAttribute("name") || genRandomAppletName(16),
					width = embedEl.getAttribute("width") || 0,
					height = embedEl.getAttribute("height") || 0;
				console.info("Found an applet to be processed in the Embed element!");

				// register the Applet instance
				appletInstanceList.push(appletName);

				let additionalParams = new Array();
				// TODO: For now, additional parameters are not processed parameters are not processed

				// Process the Object tag
				processAppletDefinition(embedEl, className, archiveUrl, codebase, appletName, width, height, additionalParams,
				(resp, iframe) => {
					iframe.contentDocument.getElementById("status").innerHTML = 'OJDK Applet Launcher loaded from Embed element!';
				});
			}
			else {
				console.warn("Embed element is not an Applet definition:", embedEl);
			}
		});

		// Wait for document.applet to load the applets into the page basecode
		waitForHead(10000).then((headEl) => {
			appletInstanceList.forEach((appletName) => {
				// Triggers after the head element is loaded
				afterHeadDispatcher();

				console.info("Header element is defined succesfully! Registering all the applets...", headEl);

				// Wait for `document.applet` before calling `registerApplet()`
				waitForDocumentApplet(appletName, {}).then((appletName) => {
					console.info(`(applet.js) Applet ${appletName} REGISTERED!`);
				}).catch(console.error);
			});
		}).catch(console.error);

		// TODO: Additional operations go here!
	});
});

/**
 * Process the Applet definition
 * @param className		the Applet classname
 * @param archiveUrl	the archive URL(s)
 * @param codebase		the Applet codebase (if any)
 * @param appletName	the Applet name
 * @param width			the Applet width
 * @param height		the Applet height
 * @param cb			the callback function after processing
 */
function processAppletDefinition(appletEl, className, archiveUrl, codebase, appletName, width, height, paramArray, cb) {
	// Construct the full base URL
	const baseUrl = getBasePath(window.location.href);

	console.info ("Found Applet:", { className, baseUrl, archiveUrl, codebase, appletName, width, height });

	// Register the Applet name to the list
	registeredAppletList.push({
		appletName: appletName,
		className: className,
		archiveUrl: archiveUrl,
		codebase: codebase,
		width: width,
		height: height
	});
	console.info ("Found Applet: %s . Number of applets in the registered list: %d", appletName, registeredAppletList.length);

	// Create an iframe replacement for the applet
	const iframe = document.createElement("iframe");
	iframe.width = width;
	iframe.height = height;
	iframe.style.border = "none";
	iframe.style.backgroundColor = "white";
	iframe.id = getAppletIFrameID(frame_count++);

	// Set the iframe content dynamically
	iframe.srcdoc = APPLET_HTML_CONTENT_OPEN;
	// Replace the applet with the iframe
	appletEl.replaceWith(iframe);

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

		if (firstAppletLoaded) console.info("First Applet to be loaded:", appletName);
		else console.info("First applet already loaded, preparing to load the next:", appletName);

		// Ensure message is sent after fetching cookies
		sendAppletMessage(cookieStr, firstAppletLoaded, parametersStr);
	});

	/**
	 * Send the applet message
	 */
	function sendAppletMessage(cookieStr, executionTriage, params) {
		const position = getAbsoluteScreenPosition(iframe);

		console.info("Parsed Applet parameters to be transmitted: %s", params);

		const requestMsg = {
			op: OPResources.OP_LOAD,
			className: className,
			archiveUrl: archiveUrl,
			codebase: codebase,
			appletName: appletName,
			baseUrl: baseUrl,
			width: width,
			height: height,
			posx: position.x,
			posy: position.y,
			parameters: params,
			cookies: cookieStr,
			pipecfn: OPResources.PREFERED_PIPE,
			firstload: executionTriage
		};

		firstAppletLoaded = false;
		/// We have to wait a bit for the iframe to be rendered
		setTimeout(() => {
			if (Object.keys(requestMsg).length > 0) {
				console.info("About to send the request to backend port", requestMsg);

				/**
				 * ----------------------
				 *  OP: LOAD THE APPLET
				 * ----------------------
				 * --> Send cookies along with applet details to the background script
				 */
				chrome.runtime.sendMessage(requestMsg, (resp) => {
					if (!resp || !resp.response) {
						console.warn("Received an empty response from the backend", resp);
						return;
					}

					console.log("Applet Response computed:", resp);
					if (resp.action === OPResources.OP_LOAD) {
						if (cb) cb(resp, iframe);

						// TODO: Implement
					}
				});
			}
			else {
				console.error("Request payload is empty, not sending!");
			}
		}, 100); // delay for 100ms to ensure the page is loaded
	}
}

/**
 * return the custom iFrame ID
 * @param idx	the index to be used to create the Applet container element
 * @returns {string}	the new ID
 */
function getAppletIFrameID(idx) {
	return (OPResources.OPLAUNCHER_IFRAME_ID.concat("_" + idx));
}

/**
 * Return all cookies from the backend
 * @param callback	the callback function to be called when a response is provided
 */
function getCookies(callback) {
	console.info("Requesting cookies from background script...");

	chrome.runtime.sendMessage({ op: OPResources.OP_COOKIES, url: window.location.origin }, (response) => {
		if (chrome.runtime.lastError) {
			console.error("rror requesting cookies:", chrome.runtime.lastError.message);
			callback("{}");
			return;
		}

		console.info("Cookies received:", response.cookies);
		callback(response.cookies || "");
	});
}

function genRandomAppletName(size) {
	const characters = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
	let result = '';
	let appletPrefix = 'applet_';
	for (let i = 0; i < size; i++) {
		const randomIndex = Math.floor(Math.random() * characters.length);
		result += characters.charAt(randomIndex);
	}

	result = appletPrefix + result;
	console.info("Applet generated name:", result);

	return result;
}

/**
 * Sends an blue message to the port, so the JVM could be placed behind, another window
 */
function sendBlurFocusMessageToBackgroundPort(visible, lowVisibility) {
	registeredAppletList.forEach(entry => {
		if (visible) {
			console.info("Processing the focus_OP to OPLauncher for the applet:", entry.appletName);
			const commMsg = {
				op: OPResources.OP_FOCUS,
				applet_name: entry.appletName
			};
			if (lowVisibility) Object.assign(commMsg, {
				parameters: [LOW_VISIBILITY_ST]
			});

			dispatchToBackground(commMsg);
		}
		else {
			console.info("Processing the blur_OP to OPLauncher for the applet:", entry.appletName);
			const commMsg = {
				op: OPResources.OP_BLUR,
				applet_name: entry.appletName
			};
			if (lowVisibility) Object.assign(commMsg, {
				parameters: [LOW_VISIBILITY_ST]
			});

			dispatchToBackground(commMsg);
		}
	});

	return true;
}

/**
 * Sends an unload op to the Applet Controller using HTTP/2 JSON
 */
function sendUnloadMessageToJSONPort() {
	console.warn("About to close all active Applet instances...");

	const commMsg = {
		op: OPResources.OP_UNLOAD
	};

	console.info("Sending unload payload to backend:", commMsg);

	const backendURL = "http://localhost:7777/oplauncher-op"; // Update this to your backend if needed
	const blob = new Blob([commMsg], { type: "application/json" });

	if (!navigator.sendBeacon(backendURL, blob)) {
		console.warn("Failed to send unload message via Beacon API.");
	}
}

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
 * Update applet position and send to backend
 */
function updateAppletPosition() {
	const iframe = document.getElementById(OPResources.OPLAUNCHER_IFRAME_ID);
	if (!iframe) {
		console.warn("Could not find any Applet render container in the HTML page")
		return;
	}

	const position = getAbsoluteScreenPosition(iframe);

	const message = {
		op: OPResources.OP_MOVE,
		px: position.x,
		py: position.y
	};

	console.info("Sending updated applet position:", message);
	chrome.runtime.sendMessage(message);
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

/**
 * Remove all registered event OPs from the client
 */
function removeEventListeners() {
	console.warn("Removing all event listeners due to OPLauncher disconnection.");

	window.removeEventListener("unload", sendUnloadMessageToBackgroundPort);
	//document.removeEventListener("visibilitychange", handleVisibilityChange);
	window.removeEventListener("scroll", updateAppletPosition);
	window.removeEventListener("resize", updateAppletPosition);

	// Status message for all the Applet unloading...
	for (let i=0; i<frame_count ;i++) {
		let iframeId = getAppletIFrameID(i);
		const iframe = document.getElementById(iframeId);

		if (iframe) iframe.contentDocument.getElementById("status").innerHTML = 'OJDK Applet Launcher unloaded!';
	}
}

/* ==========================================================
            PAGE LEVEL EVENTS FOR THE EXTENSION
   ========================================================== */
/// Monitor for specific messages comming from the remote port
chrome.runtime.onMessage.addListener((message) => {
	console.info("Received a message from the backend port", message);
	if (message.action === OPResources.OP_CLEAR_EVT) {
		removeEventListeners();
	}
});

//Trigger cleanup when the page is about to unload - TODO: Currently disabled since it needs to be reviewed
//window.addEventListener("pagehide", sendUnloadMessageToBackgroundPort);
window.addEventListener("unload", sendUnloadMessageToBackgroundPort);
document.addEventListener("visibilitychange", function () {
	if (document.hidden) {
		sendBlurFocusMessageToBackgroundPort(false, false);
	}
	else {
		sendBlurFocusMessageToBackgroundPort(true, false);
	}
});
// TODO: Needs to fix this focus problem when the new focus is to Applet itself
/*window.addEventListener("blur", function () {
	console.info("Chrome window lost focus.");
	sendBlurFocusMessageToBackgroundPort(false, false); // Send blur event
});
window.addEventListener("focus", function () {
	console.info("Chrome window gained focus.");
	sendBlurFocusMessageToBackgroundPort(true, false); // Send focus event
});*/

// Listen for events that can change the applet position
window.addEventListener("scroll", updateAppletPosition);
window.addEventListener("resize", updateAppletPosition);
/*window.addEventListener("mousemove", updateAppletPosition);
window.addEventListener("mouseup", updateAppletPosition);*/


