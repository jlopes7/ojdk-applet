package org.oplauncher.op.reflection;

import org.oplauncher.op.OPHandler;
import org.oplauncher.op.OPMessage;
import org.oplauncher.op.OPPayload;
import org.oplauncher.op.OPResponse;

public class AppletMethodProxy<P extends OPPayload> extends AppletReflection {
    public AppletMethodProxy(OPMessage<P> message, OPHandler<P> handler) {
        _messagePayload = message;
        _handler = handler;
    }

    protected OPMessage<P> getMessage() {
        return _messagePayload;
    }
    protected OPHandler<P> getHandler() {
        return _handler;
    }

    public OPResponse invoke() {

    }

    // class properties
    private OPMessage<P> _messagePayload;
    private OPHandler<P> _handler;
}
