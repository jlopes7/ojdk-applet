package org.oplauncher.op;

@FunctionalInterface
public interface OPCallback {

    public abstract void call(OPMessage message);
}
