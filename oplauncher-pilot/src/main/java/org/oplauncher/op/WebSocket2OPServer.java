package org.oplauncher.op;

import org.oplauncher.OPLauncherException;

public class WebSocket2OPServer extends OPServer {
    protected WebSocket2OPServer(String host, int port) {
        super(host, port);
    }

    // TODO: IMPLEMENT!
    @Override
    public OPServer startOPServer() {
        return null;
    }

    // TODO: IMPLEMENT!
    @Override
    public OPServer stopOPServer() throws OPLauncherException {
        return null;
    }

    // TODO: IMPLEMENT!
    @Override
    public boolean isOPServerRunning() {
        return false;
    }

    // TODO: IMPLEMENT!
    @Override
    public OPServer registerSuccessCallback(OPCallback callback) {
        return null;
    }

    // TODO: IMPLEMENT!
    @Override
    public OPServer registerFailureCallback(OPCallback callback) {
        return null;
    }
}
