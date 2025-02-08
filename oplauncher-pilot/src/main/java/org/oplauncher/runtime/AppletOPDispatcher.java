package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.op.OPMessage;

public class AppletOPDispatcher {
    static private final Logger LOGGER = LogManager.getLogger(AppletOPDispatcher.class);

    protected AppletOPDispatcher(AppletController controller) {
        _appletController = controller;
    }

    public void processSuccessRequest(OPMessage message) {
        // TODO: Implement
    }

    public void processFailureRequest(OPMessage message) {
        // TODO: Implement
    }

    public AppletController getAppletController() {
        return _appletController;
    }

    // class properties
    private AppletController _appletController;
}
