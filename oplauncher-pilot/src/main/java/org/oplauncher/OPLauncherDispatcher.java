package org.oplauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.oplauncher.ErrorCode.DISPATCHER_GENERAL_ERROR;

public class OPLauncherDispatcher {
    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(OPLauncherDispatcher.class.getName());

    protected OPLauncherDispatcher(OPLauncherDispatcherPool poolRef) {
        _appletLoaders = new LinkedHashMap<>();
    }

    public boolean containsAppletLoader(String name) {
        return _appletLoaders.containsKey(name);
    }

    public AppletClassLoader getAppletLoader(String name) {
        LOCK.lock();
        try {
            return _appletLoaders.get(name);
        }
        finally {
            LOCK.unlock();
        }
    }

    public List<AppletClassLoader> getAllAppletLoaders() {
        return _appletLoaders.values().stream().collect(Collectors.toList());
    }

    public OPLauncherDispatcher loadApplet(String name, List<String> parameters) {
        LOGGER.info("About to load and dispatch the Applet execution: {}", name);

        try {
            AppletClassLoader appletClassLoader = new AppletClassLoader(this);
            String response = appletClassLoader.processLoadAppletOp(parameters);
            LOGGER.info("Got a response from the Applet loader: {}", response);

            LOCK.lock();
            try {
                // save the loaded class into the control list
                _appletLoaders.put(name, appletClassLoader);

                return this;
            } finally {
                LOCK.unlock();
            }
        }
        catch (Exception e) {
            if (e instanceof OPLauncherException) throw e;
            else throw new OPLauncherException(e, DISPATCHER_GENERAL_ERROR);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { // Check for instance equality
            return true;
        }
        if (obj == null || getClass()!= obj.getClass()) {
            return false;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this);
    }

    @Override
    public String toString() {
        return new StringBuilder("Dispatcher # of registered loaders: ").append(_appletLoaders.size()).toString();
    }

    public OPLauncherController getOPLauncherController() {
        return _controller;
    }

    // class properties
    private OPLauncherController _controller;

    private Map<String, AppletClassLoader> _appletLoaders;
}
