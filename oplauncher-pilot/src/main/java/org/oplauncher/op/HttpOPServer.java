package org.oplauncher.op;

import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.oplauncher.ErrorCode.ERROR_LISTENING_OPSERVER;
import static org.oplauncher.IConstants.*;

public class HttpOPServer<P extends OPPayload> extends OPServer<P> {
    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(HttpOPServer.class);
    private final AtomicBoolean SERVER_RUNNING_CONTROL = new AtomicBoolean(false);

    protected HttpOPServer(String host, int port) {
        super(host, port);

        _successCallbacks = Collections.synchronizedList(new ArrayList<>());
        _errorCallbacks   = Collections.synchronizedList(new ArrayList<>());
    }

    public OPServer<P> startOPServer() throws OPLauncherException {
        LOCK.lock();
        try {
            String ctxroot = String.format("/%s", ConfigurationHelper.getOPServerContextRoot());
            String hbroot = String.format("/%s", DEFAULT_HB_CTXROOT);
            LOGGER.info("About to start the OP server listening on {} => {}:{}", ctxroot, getHost(), getPort());
            LOGGER.info("About to start the OP server listening on {} => {}:{}", hbroot, getHost(), getPort());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("-> Socket SoTimeout: {} mills", DEFAULT_CONNECTION_SETSOTIMEOUT_SEC);
                LOGGER.debug("-> Socket backlog size: {}", DEFAULT_CONNECTION_BACKLOG);
                LOGGER.debug("-> Number of thread counts: {} threads", Runtime.getRuntime().availableProcessors());
            }
            IOReactorConfig reactorConfig = IOReactorConfig.custom()
                                                            .setSoTimeout(DEFAULT_CONNECTION_SETSOTIMEOUT_SEC)
                                                            .setBacklogSize(DEFAULT_CONNECTION_BACKLOG)
                                                            .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                                                            .setSoKeepAlive(true)
                                                            .setTcpNoDelay(true)
                                                            .build();
            // Configure the server
            _server = ServerBootstrap.bootstrap()
                        .setListenerPort(getPort())
                        .setListenerPort(getPort())
                        .setLocalAddress(InetAddress.getByName(getHost()))
                        .registerHandler(ctxroot, new OPHttpHandler<>(this))
                        .registerHandler(hbroot, new OPHttpHBHandler<>(this))
                        .setIOReactorConfig(reactorConfig)
                    .create();
            // Start the server
            getServer().start();

            LOGGER.info("OP server started successfully on {}:{}", getHost(), getPort());
            SERVER_RUNNING_CONTROL.set(true);

            // Keep the server running for a impossible time, millions of years - DISABLED ! only run when the JVM is active, not the other around, should not block the process
            //getServer().awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

            return this;
        }
        catch (Exception e) {
            throw new OPLauncherException(e, ERROR_LISTENING_OPSERVER);
        }
        finally {
            LOCK.unlock();
        }
    }

    @Override
    public OPServer<P> stopOPServer() throws OPLauncherException {
        LOGGER.warn("Stopping the OP server listening on {}:{}", getHost(), getPort());
        if (getServer() != null) {
            getServer().shutdown(DEFAULT_CONNECTION_SETSOTIMEOUT_SEC, MILLISECONDS); // Graceful shutdown with a 5-second timeout
        }

        SERVER_RUNNING_CONTROL.set(false);
        LOGGER.warn("OP server stopped successfully on {}:{}", getHost(), getPort());
        return this;
    }

    @Override
    public boolean isOPServerRunning() {
        return SERVER_RUNNING_CONTROL.get();
    }

    protected HttpOPServer<P> triggerSuccessCallbacks(OPMessage<P> message) {
        _successCallbacks.forEach(callback -> callback.call(message));
        return this;
    }
    protected HttpOPServer<P> triggerErrorCallbacks(OPMessage<P> message) {
        _errorCallbacks.stream().parallel().forEach(callback -> callback.call(message));
        return this;
    }

    @Override
    public OPServer<P> registerSuccessCallback(OPCallback<P> callback) {
        LOCK.lock();
        try {
            if (callback != null) {
                _successCallbacks.add(callback); // TODO: in the future implement a functionality to not have duplicate "callbacks"
            }
            return this;
        }
        finally {
            LOCK.unlock();
        }
    }

    @Override
    public OPServer<P> registerFailureCallback(OPCallback<P> callback) {
        LOCK.lock();
        try {
            if (callback != null) {
                _errorCallbacks.add(callback); // TODO: in the future implement a functionality to not have duplicate "callbacks"
            }
            return this;
        }
        finally {
            LOCK.unlock();
        }
    }

    protected HttpServer getServer() {
        return _server;
    }

    // class properties
    private HttpServer _server;

    private List<OPCallback<P>> _successCallbacks;
    private List<OPCallback<P>> _errorCallbacks;
}
