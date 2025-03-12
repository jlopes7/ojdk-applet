package org.oplauncher.op;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.oplauncher.ErrorCode.NO_VALID_CHROME_TOKEN_FOUND;
import static org.oplauncher.IConstants.HTTP_HEADER_CHROMEEXT_TKN;

public abstract class OPHandler<P extends OPPayload> implements HttpAsyncRequestHandler<HttpRequest> {
    static private final Logger LOGGER = LogManager.getLogger(OPHandler.class);

    public OPHandler(HttpOPServer<P> opserv) {
        _opserver = opserv;
        _objectMapper = new ObjectMapper();
    }

    protected void _VALIDATE_REQUEST_(HttpRequest request) throws OPLauncherException {
        _VALIDATE_REQUEST_(request, true);
    }

    protected void _VALIDATE_REQUEST_(HttpRequest request, boolean validateAppToken) throws OPLauncherException {
        if ( validateAppToken ) {
            String configuredToken = ConfigurationHelper.getOPChromeToken().trim();
            if (LOGGER.isDebugEnabled()) {
                Arrays.stream(request.getAllHeaders()).forEach(header -> {
                    LOGGER.debug("(_VALIDATE_REQUEST_) Header entry from request -> {} := {}", header.getName(), header.getValue());
                });
            }
            Header header = request.getFirstHeader(HTTP_HEADER_CHROMEEXT_TKN);
            if (header == null) {
                throw new OPLauncherException("Missing ext token in the request: " + HTTP_HEADER_CHROMEEXT_TKN, NO_VALID_CHROME_TOKEN_FOUND);
            }
            String requestToken = header.getValue();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Validating request header: {}", header);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Internal Token ({}) === Request Token ({})", configuredToken, requestToken);
            }

            if (!requestToken.equals(configuredToken)) {
                throw new OPLauncherException("Invalid or not supported request token", NO_VALID_CHROME_TOKEN_FOUND);
            }
        }
    }

    protected void processResponse(OPResponse opresp, HttpAsyncExchange httpExchange) throws OPLauncherException {
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

    protected String getClientIp(HttpContext context) {
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
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        // simple consumer...
        return new BasicAsyncRequestConsumer();
    }

    public HttpOPServer<P> getOpServerRef() {
        return _opserver;
    }

    public ObjectMapper getJSONObjectMapper() {
        return _objectMapper;
    }

    // class properties
    private HttpOPServer<P> _opserver;
    private ObjectMapper _objectMapper;
}
