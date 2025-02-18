package org.oplauncher.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.*;
import org.oplauncher.op.OPMessage;
import org.oplauncher.op.OPPayload;
import org.oplauncher.op.OPPlainPayload;
import org.oplauncher.op.OPSecurePayload;
import org.oplauncher.runtime.secur.SecurityManager;

import static org.oplauncher.ErrorCode.*;
import static org.oplauncher.OpCode.*;

public class AppletOPDispatcher {
    static private final Logger LOGGER = LogManager.getLogger(AppletOPDispatcher.class);

    protected AppletOPDispatcher(AppletController controller) {
        _appletController = controller;
    }

    private boolean authRequest(OPMessage<OPPlainPayload> message) throws DecoderException {
        if ( !ConfigurationHelper.isOPServerAppTokenActive() ) return true;

        OPPlainPayload payload = message.getPayload();
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

    private AppletOPDispatcher prepareExecutionIfNecessary(OPPlainPayload payload) {
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

    private void processPlainMessage(OPMessage<OPPlainPayload> message) throws DecoderException {
        if (authRequest(message)) {
            OPPlainPayload payload = message.getPayload();

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
    private boolean verifyMagicNumber(OPPlainPayload payload) {
        long mgtkn  = payload.getMagicToken();
        long cfntkn = ConfigurationHelper.getMagicNumberMask();

        return (mgtkn & cfntkn) == cfntkn;
    }
    public <P extends OPPayload>void processSuccessRequest(OPMessage<P> message) {
        try {
            OPPlainPayload payload;
            if (ConfigurationHelper.isSecurePayloadActive()) {
                OPSecurePayload securePayload = (OPSecurePayload) message.getPayload();
                String encPayload = securePayload.getPayload();
                String encKey = ConfigurationHelper.getCipherKey();
                String decPayload = SecurityManager.getCipherProcessor().decryptPayload(encPayload, encKey);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Processing secure payload...");
                    LOGGER.info("Secure payload received from the Chrome extension: {}", encPayload);
                    LOGGER.info("Secure payload deciphered from the Chrome extension: {}", decPayload);
                }

                ObjectMapper objectMapper = new ObjectMapper();
                payload = objectMapper.readValue(decPayload, OPPlainPayload.class);
            }
            else {
                LOGGER.info("Using plain payload, no security implemented");
                payload = (OPPlainPayload) message.getPayload();
            }

            // Just have to verify the magic number
            if ( verifyMagicNumber(payload) ) {
                processPlainMessage(new OPMessage<>(payload));
            }
            else {
                throw new OPLauncherException("Failed to verify the given magic number: " + payload.getMagicToken(), FAILED_VALID_MAGIC_NUMBER);
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed processing the Chrome request payload. No changes were made, and no OP was executed", e);
        }
    }

    public <P extends OPPayload>void processFailureRequest(OPMessage<P> message) {
        P payload = message.getPayload();
        if (payload!=null) {
            LOGGER.warn("Failed to process the Applet OP [{}]", message.getPayload());
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

    public AppletController getAppletController(OPPlainPayload payload) {
        return OPLauncherDispatcherPool
                .getActiveDispatcherInstance(payload.getAppletName())
                .getAppletLoader(payload.getAppletName()).getAppletController();
    }

    // class properties
    private AppletController _appletController;
}
