package org.oplauncher.runtime;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;
import org.oplauncher.op.OPMessage;
import org.oplauncher.op.OPPayload;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.oplauncher.ErrorCode.APPTKN_AUTH_ERROR;
import static org.oplauncher.ErrorCode.EMPTY_JSON_PAYLOAD;

public class AppletOPDispatcher {
    static private final Logger LOGGER = LogManager.getLogger(AppletOPDispatcher.class);

    protected AppletOPDispatcher(AppletController controller) {
        _appletController = controller;
    }

    private boolean authRequest(OPMessage message) throws DecoderException {
        if ( !ConfigurationHelper.isOPServerAppTokenActive() ) return true;

        OPPayload payload = message.getPayload();
        if (payload!=null) {
            String msgtkn = payload.getToken();
            String apptkn = ConfigurationHelper.getOPChromeAppToken();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Given AppToken[{}], Configured AppToken[{}]", msgtkn, apptkn);
            }

            return msgtkn.equals(apptkn);
        }
        else {
            throw new OPLauncherException("Invalid payload. Empty payload.", EMPTY_JSON_PAYLOAD);
        }
    }

    private AppletOPDispatcher prepareExecutionIfNecessary(OPPayload payload) {
        // Parse the parameters of the request based on specific operations
        switch (payload.getOpCode()) {
            case CHANGE_POSTION: {
                Integer posx = payload.getPosX(),
                        posy = payload.getPosY();

                if (posx != null && posy != null) {
                    LOGGER.info("Changing Applet position to X:{}, Y:{}", posx, posy);

                    getAppletController()
                            .getAppletClassLoader()
                            .getAppletParameters()
                            .setPosition(posx, posy);
                }
                else {
                    LOGGER.warn("Cannot change Applet position because one of its Axis was not provided");
                }
                break;
            }
        }

        return this;
    }

    public void processSuccessRequest(OPMessage message) {
        try {
            if (authRequest(message)) {
                OPPayload payload = message.getPayload();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("-> Received payload to be processed from Chrome: {}", payload.toString());
                }
                LOGGER.info("Processing Applet OP [{}]", payload.getOpCode().opcode());

                prepareExecutionIfNecessary(payload)
                        .getAppletController()
                        .executeOP(payload.getOpCode());
            }
            else {
                throw new OPLauncherException("The OP message could be verified: "+message.getPayload().getToken(), APPTKN_AUTH_ERROR);
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed processing the Chrome request payload. No changes were made, and no OP was executed", e);
        }
    }

    public void processFailureRequest(OPMessage message) {
        OPPayload payload = message.getPayload();
        if (payload!=null) {
            LOGGER.warn("Failed to process the Applet OP [{}]", message.getPayload().getToken());
            LOGGER.warn("Error Code: {}/{}", message.getErrorType().name(), message.getErrorType().code());
            LOGGER.warn("No changes were made, and the OP was not executed", message.getError());
        }
        else {
            throw new OPLauncherException("Invalid payload. Empty payload.", EMPTY_JSON_PAYLOAD);
        }
    }

    public AppletController getAppletController() {
        return _appletController;
    }

    // class properties
    private AppletController _appletController;
}
