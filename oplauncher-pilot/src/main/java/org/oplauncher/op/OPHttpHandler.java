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

public class OPHttpHandler implements HttpAsyncRequestHandler<HttpRequest> {
    static private final Logger LOGGER = LogManager.getLogger(OPHttpHandler.class);
    public OPHttpHandler(HttpOPServer opserv) {
        _opserver = opserv;
        _objectMapper = new ObjectMapper();
    }

    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        // simple consumer...
        return new BasicAsyncRequestConsumer();
    }

    private void _VALIDATE_REQUEST_(HttpRequest request) throws OPLauncherException {
        String configuredToken = ConfigurationHelper.getOPChromeToken().trim();
        Header header = request.getFirstHeader(HTTP_HEADER_CHROMEEXT_TKN);
        if (header == null) {
            throw new OPLauncherException("Missing ext token in the request", NO_VALID_CHROME_TOKEN_FOUND);
        }
        String requestToken = header.getValue();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Validating request header: {}", header);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Internal Token ({}) === Request Token ({})", configuredToken, requestToken);
        }

        if ( !requestToken.equals(configuredToken) ) {
            throw new OPLauncherException("Invalid or not supported request token", NO_VALID_CHROME_TOKEN_FOUND);
        }
    }

    private String getClientIp(HttpContext context) {
        NHttpServerConnection connection = (NHttpServerConnection) context.getAttribute("http.connection");
        String clientIp = "Unknown";
        if (connection != null) {
            Socket socket = (Socket) connection.getContext().getAttribute("socket");
            if (socket != null) {
                InetAddress remoteAddress = socket.getInetAddress();
                clientIp = remoteAddress != null ? remoteAddress.getHostAddress() : "unknown";
            }
        }

        return clientIp;
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

    private void processResponse(OPResponse opresp, HttpAsyncExchange httpExchange) throws OPLauncherException {
        HttpResponse response = httpExchange.getResponse();

        try {
            String responseJson = getJSONObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(opresp);

            // Send a response based on the type (error or not)
            if (opresp.isSuccess()) {
                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(new StringEntity(responseJson, APPLICATION_JSON));
            }
            // error processing
            else {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                response.setEntity(new StringEntity(responseJson, APPLICATION_JSON));
            }

            httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
        }
        catch (Exception e) {
            LOGGER.error("It was not possible to write the JSON message back to chrome", e);
        }
    }

    protected HttpOPServer getOpServerRef() {
        return _opserver;
    }

    public ObjectMapper getJSONObjectMapper() {
        return _objectMapper;
    }

    // class properties
    private HttpOPServer _opserver;
    private ObjectMapper _objectMapper;
}
