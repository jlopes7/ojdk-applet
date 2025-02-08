package org.oplauncher.op;

@FunctionalInterface
public interface OPCallback<T> {
    void call(OPMessage message);
}
