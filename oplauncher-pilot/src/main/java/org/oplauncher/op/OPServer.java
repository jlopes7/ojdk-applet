package org.oplauncher.op;

import org.oplauncher.OPLauncherException;

public abstract class OPServer {

    protected OPServer(String host, int port) {
        _host = host;
        _port = port;
    }

    public abstract OPServer startOPServer() throws OPLauncherException;
    public abstract OPServer stopOPServer() throws OPLauncherException;
    public abstract boolean isOPServerRunning();

    public abstract OPServer registerSuccessCallback(final OPCallback callback);
    public abstract OPServer registerFailureCallback(final OPCallback callback);

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
