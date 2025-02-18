package org.oplauncher.op;

import org.oplauncher.OPLauncherException;

public abstract class OPServer<P extends OPPayload> {

    protected OPServer(String host, int port) {
        _host = host;
        _port = port;
    }

    public abstract OPServer<P> startOPServer() throws OPLauncherException;
    public abstract OPServer<P> stopOPServer() throws OPLauncherException;
    public abstract boolean isOPServerRunning();

    public abstract OPServer<P> registerSuccessCallback(final OPCallback<P> callback);
    public abstract OPServer<P> registerFailureCallback(final OPCallback<P> callback);

    public int getPort() {
        return _port;
    }
    public String getHost() {
        return _host;
    }

    // class properties
    private String _host;
    private int _port;
}
