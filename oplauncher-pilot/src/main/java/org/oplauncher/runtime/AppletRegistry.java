package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.applet.Applet;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AppletRegistry {
    static private final Lock LOCK = new ReentrantLock();
    static private Logger LOGGER = LogManager.getLogger(AppletRegistry.class);
    static private final Map<String, Applet> APPLET_MAP = new LinkedHashMap<>();

    static public final void add(String appletName, Applet applet) {
        LOCK.lock();
        try {
            APPLET_MAP.put(appletName, applet);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Added the applet {} to the registry. Class: [{}]", appletName, applet.getClass().getName());
            }
        }
        finally {
            LOCK.unlock();
        }
    }

    static public final Applet get(String appletName) {
        LOCK.lock();
        try {
            return APPLET_MAP.get(appletName);
        }
        finally {
            LOCK.unlock();
        }
    }

    static public final Enumeration<Applet> getApplets() {
        LOCK.lock();
        try {
            return Collections.enumeration(APPLET_MAP.values());
        }
        finally {
            LOCK.unlock();
        }
    }
}
