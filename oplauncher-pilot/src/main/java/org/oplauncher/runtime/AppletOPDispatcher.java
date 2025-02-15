package org.oplauncher.runtime;

import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.*;
import org.oplauncher.op.OPMessage;
import org.oplauncher.op.OPPayload;

import static org.oplauncher.ErrorCode.APPTKN_AUTH_ERROR;
import static org.oplauncher.ErrorCode.EMPTY_JSON_PAYLOAD;
import static org.oplauncher.OpCode.*;

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

                    getAppletController(payload)
                            .getAppletClassLoader()
                            .getAppletParameters()
                            .setPosition(payload.getAppletName(), posx, posy);
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

                OpCode opcode = payload.getOpCode();
                /*
                 * We need to switch between the OP_LOAD op, and all other OPs, OP_LOAD requires special attention
                 * because it needs to reinitialize the applet loader
                 */
                if ( opcode != LOAD_APPLET ) {
                    // When the window is detached, these events are ignored
                    if ( (opcode == CHANGE_POSTION || opcode == FOCUS_APPLET || opcode == BLUR_APPLET) &&
                            !ConfigurationHelper.trackBrowserWindowPosition()) {
                        return;
                    }

                    prepareExecutionIfNecessary(payload)
                            .getAppletController(payload)
                            .executeOP(payload.getOpCode(), payload.getParameters().toArray(new String[0]));
                }
                else {
                    OPLauncherController controller = OPLauncherDispatcherPool.getActiveControllerInstance();
                    LOGGER.info("Processing new Load Applet OP [{}]", payload.getOpCode().opcode());
                    // Triggers the execution of the new Applet
                    controller.processLoadAppletOp(payload.getParameters());
                }
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

    public String getAppletName() {
        return getAppletController().getAppletClassLoader().getInstanceName();
    }

    public AppletController getAppletController() {
        return _appletController;
    }

    public AppletController getAppletController(OPPayload payload) {
        return OPLauncherDispatcherPool
                .getActiveDispatcherInstance(payload.getAppletName())
                .getAppletLoader(payload.getAppletName()).getAppletController();
    }

    // class properties
    private AppletController _appletController;
}
