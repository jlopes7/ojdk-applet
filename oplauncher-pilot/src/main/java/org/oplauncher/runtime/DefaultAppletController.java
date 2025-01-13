package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.AppletClassLoader;
import org.oplauncher.ErrorCode;
import org.oplauncher.OPLauncherException;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultAppletController extends AppletController {
    static private final Lock LOCK = new ReentrantLock();
    static private Logger LOGGER = LogManager.getLogger(DefaultAppletController.class);

    protected DefaultAppletController(AppletClassLoader clzloader, AppletContext context) throws OPLauncherException {
        super(clzloader, context);
    }

    private String renderApplet(Applet applet) throws Exception {
        LOCK.lock();
        try {
            setAppletFrame("OPLauncher Applet Window").setStatusBarLabel("Ready!");
            getAppletFrame().setSize(getAppletClassLoader().getAppletParameters().getWidth(), getAppletClassLoader().getAppletParameters().getHeight());
            getAppletFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            getAppletFrame().add(applet);

            getAppletStatusBar().setBorder(BorderFactory.createEtchedBorder());

            // Add the status bar to the bottom
            getAppletFrame().add(getAppletStatusBar(), BorderLayout.SOUTH);
            getAppletFrame().setVisible(true);

            return "";
        }
        finally {
            LOCK.unlock();
        }
    }

    private String parseClassName(final String klass) {
        return klass.replace("/", ".")
                    .replace("\\", ".")
                    .replace(".class", "");
    }

    @Override
    protected String loadAppletClass() throws OPLauncherException {
        LOCK.lock();
        try {
            String klassName = parseClassName(getAppletClassLoader().getAppletClassName());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Loading the Applet class... {}", klassName);
            }
            Applet applet = (Applet) getAppletClassLoader().loadClass(klassName)
                                                           .getDeclaredConstructor().newInstance();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Applet class loaded successfully: {}", klassName);
            }
            applet.setStub(new CustomAppletStub(this));

            LOGGER.info("Calling applet INIT");
            applet.init();

            getAppletClassLoader().getAppletController().activateApplet(); /// Mark the applet as active
            LOGGER.info("Calling applet STARTED");
            applet.start();

            LOGGER.info("Now it's time to RENDER the applet Frame");
            return renderApplet(applet);
        }
        catch (Exception e) {
            throw new OPLauncherException(e, ErrorCode.APPLET_EXECUTION_ERROR);
        }
        finally {
            LOCK.unlock();
        }
    }
}
