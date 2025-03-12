package org.oplauncher.op;

import org.apache.http.*;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;
import org.oplauncher.op.reflection.AppletMethodProxy;

import java.io.IOException;

import static org.oplauncher.ErrorCode.*;
import static org.oplauncher.IConstants.*;

public class OPHttpHandler<P extends OPPayload> extends OPHandler<P> {
    static private final Logger LOGGER = LogManager.getLogger(OPHttpHandler.class);
    public OPHttpHandler(HttpOPServer<P> opserv) {
        super(opserv);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext httpContext) throws IOException {
        OPPayload payload = null;

        try {
            _VALIDATE_REQUEST_(request, ConfigurationHelper.isOPServerAppTokenActive());

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Received a request from the client /{} ", getClientIp(httpContext));
            }
            if (request instanceof HttpEntityEnclosingRequest) {
                OPResponse response = new OPResponse(SUCCESS_RESPONSE, true, NO_PROP_CODE);

                // Parse the JSON payload
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                String json = EntityUtils.toString(entity);

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Payload received by the request: {} ", json);
                    LOGGER.info("Parsing the JSON message");
                }

                if (ConfigurationHelper.isSecurePayloadActive()) {
                    payload = getJSONObjectMapper().readValue(json, OPSecurePayload.class);
                    OPMessage<OPSecurePayload> message = new OPMessage<>((OPSecurePayload) payload);

                    ///  trigger the execution of all registered observables for new requests based on async/sync config
                    if ( !((OPSecurePayload) payload).isSyncedResponse() ) {
                        getOpServerRef().triggerSuccessCallbacks((OPMessage<P>) message);
                    }
                    // Synchronous processing ...
                    else {
                        try {
                            AppletMethodProxy<OPSecurePayload> proxy = new AppletMethodProxy<>(message, (OPHandler<OPSecurePayload>) this);
                            response = proxy.invoke();
                        }
                        catch (Exception e) {
                            LOGGER.error("Failed to invoke the Applet method", e);
                            response.setUnsuccess()
                                    .setMessage(e.getMessage())
                                    .setErrorCode((e instanceof OPLauncherException) ?
                                                    ((OPLauncherException) e).getErrorCode().code() :
                                                    APPLET_EXECUTION_ERROR.code());
                        }
                    }
                }
                else {
                    payload = getJSONObjectMapper().readValue(json, OPPlainPayload.class);
                    OPMessage<OPPlainPayload> message = new OPMessage<>((OPPlainPayload) payload);
                    ///  trigger the execution of all registered observables for new requests
                    getOpServerRef().triggerSuccessCallbacks((OPMessage<P>) message);
                }

                // process the response
                processResponse(response, httpExchange);
            }
            ///  Problems on the Horizon...
            else {
                throw new OPLauncherException("Invalid format. Expected JSON payload", JSON_PROTOCOL_ERROR);
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed while processing the request from Chrome", e);

            OPMessage<OPPayload> message = new OPMessage<>(payload);
            message.setError().setErrorDetails(e);
            if ( e instanceof OPLauncherException ) {
                message.setErrorCode(((OPLauncherException) e).getErrorCode());
            }
            else {
                message.setErrorCode(FAILED_TO_LOAD_RESOURCE);
            }

            ///  trigger the execution of all registered observables for error requests
            getOpServerRef().triggerErrorCallbacks((OPMessage<P>) message);

            OPResponse response = new OPResponse(e.getMessage(), false, message.getErrorType().code());
            // Send the error response back to chrome
            processResponse(response, httpExchange);
        }
    }
}
