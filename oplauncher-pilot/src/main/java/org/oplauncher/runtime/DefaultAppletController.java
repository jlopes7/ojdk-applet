package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.AppletClassLoader;
import org.oplauncher.ErrorCode;
import org.oplauncher.OPLauncherException;
import org.oplauncher.op.OPServerFactory;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.oplauncher.IConstants.OP_PARAM_LOW_VISIBILITY;
import static org.oplauncher.IConstants.SUCCESS_RESPONSE;

public class DefaultAppletController extends AppletController {
    static private final Lock LOCK = new ReentrantLock();
    static private Logger LOGGER = LogManager.getLogger(DefaultAppletController.class);

    protected DefaultAppletController(AppletClassLoader clzloader, AppletContext context) throws OPLauncherException {
        super(clzloader, context);
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

            AppletRegistry.add(getAppletClassLoader().getAppletName(), applet);

            configureApplet(applet).defineAppletFrame(String.format("OPLauncher Applet Window - %s", applet.getName()))
                                   .defineStatusBar("Ready!", applet);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Applet class loaded successfully: {}", klassName);
            }
            applet.setStub(new CustomAppletStub(this));

            Dimension appletDim = new Dimension(getAppletClassLoader().getAppletParameters().getWidth(),
                                                getAppletClassLoader().getAppletParameters().getHeight());
            applet.setSize(appletDim);
            applet.setPreferredSize(appletDim);

            LOGGER.info("Calling applet INIT");
            applet.init();

            LOGGER.info("Now it's time to RENDER the applet Frame");
            String response = renderApplet(applet);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Response is provided from the Applet rendering: {}", response);
            }

            return response;
        }
        catch (Exception e) {
            LOGGER.error("Failed to load Applet class", e);
            throw new OPLauncherException(e, ErrorCode.APPLET_EXECUTION_ERROR);
        }
        finally {
            LOCK.unlock();
        }
    }

    @Override
    protected String changeAppletPosition() throws OPLauncherException {
        LOCK.lock();
        try {
            move(getAppletClassLoader().getAppletParameters().getPositionX(),
                 getAppletClassLoader().getAppletParameters().getPositionY());

            return SUCCESS_RESPONSE;
        }
        catch (Exception e) {
            LOGGER.error("Failed to change Applet position", e);
            throw new OPLauncherException(e, ErrorCode.APPLET_EXECUTION_ERROR);
        }
        finally {
            LOCK.unlock();
        }
    }

    @Override
    protected String focusApplet(String ...params) throws OPLauncherException {
        final int LOWVIS = 0;
        getAppletFrame().setVisible(true);
        if (params != null && params.length > 0) {
            if (params[LOWVIS].trim().equalsIgnoreCase(OP_PARAM_LOW_VISIBILITY)) {
                getAppletFrame().setAlwaysOnTop(false);
                //getAppletFrame().setFocusable(true);
            }
            else {
                getAppletFrame().setAlwaysOnTop(true);
            }
        }
        else {
            getAppletFrame().setAlwaysOnTop(true);
        }
        getAppletFrame().repaint();
        return SUCCESS_RESPONSE;
    }

    @Override
    protected String blurApplet(String ...params) throws OPLauncherException {
        final int LOWVIS = 0;
        getAppletFrame().setAlwaysOnTop(false);

        if (params != null && params.length > 0) {
            if (!params[LOWVIS].trim().equalsIgnoreCase(OP_PARAM_LOW_VISIBILITY)) {
                getAppletFrame().setVisible(false);
            }
        }
        else {
            getAppletFrame().setVisible(false);
        }

        return SUCCESS_RESPONSE;
    }
}
