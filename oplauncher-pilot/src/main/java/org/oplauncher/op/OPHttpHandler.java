package org.oplauncher.op;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.oplauncher.ErrorCode.*;
import static org.oplauncher.IConstants.*;

public class OPHttpHandler extends OPHandler {
    static private final Logger LOGGER = LogManager.getLogger(OPHttpHandler.class);
    public OPHttpHandler(HttpOPServer opserv) {
        super(opserv);
    }

    @Override
    public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext httpContext) throws IOException {
        OPPayload payload = null;

        try {
            _VALIDATE_REQUEST_(request);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Received a request from the client /{} ", getClientIp(httpContext));
            }
            if (request instanceof HttpEntityEnclosingRequest) {
                // Parse the JSON payload
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                String json = EntityUtils.toString(entity);

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Payload received by the request: {} ", json);
                    LOGGER.info("Parsing the JSON message");
                }

                payload = getJSONObjectMapper().readValue(json, OPPayload.class);
                OPMessage message = new OPMessage(payload);

                ///  trigger the execution of all registered observables for new requests
                getOpServerRef().triggerSuccessCallbacks(message);

                OPResponse response = new OPResponse(SUCCESS_RESPONSE, true, NO_PROP_CODE);
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

            OPMessage message = new OPMessage(payload);
            message.setError().setErrorDetails(e);
            if ( e instanceof OPLauncherException ) {
                message.setErrorCode(((OPLauncherException) e).getErrorCode());
            }
            else {
                message.setErrorCode(FAILED_TO_LOAD_RESOURCE);
            }

            ///  trigger the execution of all registered observables for error requests
            getOpServerRef().triggerErrorCallbacks(message);

            OPResponse response = new OPResponse(e.getMessage(), false, message.getErrorType().code());
            // Send the error response back to chrome
            processResponse(response, httpExchange);
        }
    }
}
