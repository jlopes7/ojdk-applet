package org.oplauncher.runtime;

import org.oplauncher.AppletClassLoader;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;

import java.applet.AppletContext;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AppletControllerFactory {
    static private final Lock LOCK = new ReentrantLock();

    static public AppletController createAppletController(AppletClassLoader klassLoader) throws OPLauncherException {
        LOCK.lock();
        try {
            AppletContext context = AppletContextFactory.newAppletContext(ConfigurationHelper.getAppletContextType(),
                                                                          klassLoader.getAppletController());
            return new DefaultAppletController(klassLoader, context);
        }
        finally {
            LOCK.unlock();
        }
    }
}
