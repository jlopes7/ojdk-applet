package org.oplauncher.op;

@FunctionalInterface
public interface OPCallback<P extends OPPayload> {
    void call(OPMessage<P> message);
}
