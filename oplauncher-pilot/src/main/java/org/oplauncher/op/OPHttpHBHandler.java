package org.oplauncher.op;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.OPLauncherException;

import java.io.IOException;

import static org.oplauncher.ErrorCode.FAILED_TO_LOAD_RESOURCE;
import static org.oplauncher.IConstants.NO_PROP_CODE;
import static org.oplauncher.IConstants.SUCCESS_RESPONSE;

public class OPHttpHBHandler extends OPHandler {
    static private final Logger LOGGER = LogManager.getLogger(OPHttpHBHandler.class);

    public OPHttpHBHandler(HttpOPServer opserv) {
        super(opserv);
    }

    @Override
    public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext httpContext) throws HttpException, IOException {
        try {
            //_VALIDATE_REQUEST_(request, false);

            OPResponse response = new OPResponse(SUCCESS_RESPONSE, true, NO_PROP_CODE);
            // process the response
            processResponse(response, httpExchange);
        }
        catch (Exception e) {
            LOGGER.error("Failed while processing the request from Chrome", e);

            OPMessage message = new OPMessage(new OPPayload());
            message.setError().setErrorDetails(e);
            if ( e instanceof OPLauncherException) {
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
