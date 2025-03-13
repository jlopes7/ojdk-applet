package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.oplauncher.ConstantsConfig._nullptr_;

/**
 * <p></p>This class can be used by the applet class to change the behaviour of the OJDK Applet Launcher,
 * including:</p>
 * <li>Changing the Applet frame controls (i.e., ICON)</li>
 * <li>Chancing the Applet frame title</li>
 * <li>Change how the operations are controlled</li>
 * <li>more...</li>
 *
 * <p>Applets can use this to the behaviour of running applications, and how the launcher controls them</p>
 *
 * <p>Created at 2025-01-22</p>
 *
 * @author <a href="mailto: jgonzalez@azul.com">Jo^o Gonzalez</a>
 * @version 1.0b
 */
public final class OPLauncherConfig {
    static public final OPLauncherConfig instance = new OPLauncherConfig();
    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(OPLauncherConfig.class);

    private OPLauncherConfig() {
        _controllerMap = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    // TODO: In the future, multiple controllers could be implemented
    protected void registerController(final AppletController controller) {
        LOCK.lock();
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("+++ Registering controller for the Applet: {}", controller);
            }
            _controllerMap.put(controller.getAppletClassLoader().getAppletName(), controller);
        }
        finally {
            LOCK.unlock();
        }
    }

    /**
     * Changes the ICON from the Applet frame
     * @param icon  the new ICON to be used by the OPLauncher
     */
    public void setFrameIcon(final ImageIcon icon, String appletName) {
        LOCK.lock();
        try {
            AppletController controller = getControllerMap().get(appletName);
            if (controller != null) {
                controller.getAppletFrame().setIconImage(icon.getImage());
            }
            else {
                LOGGER.warn("(setFrameIcon) No controller found for applet: {}", appletName);
            }
        }
        finally {
            LOCK.unlock();
        }
    }

    /**
     * Resize the Applet container (frame)
     * @param width     The new frame width
     * @param height    The new frame height
     */
    public void resizeFrame(final int width, final int height, String appletName) {
        LOCK.lock();
        try {
            AppletController controller = getControllerMap().get(appletName);
            if (controller != null) {
                controller.getAppletFrame().setSize(width, height);
            }
            else {
                LOGGER.warn("(resizeFrame) No controller found for applet: {}", appletName);
            }
        }
        finally {
            LOCK.unlock();
        }
    }

    /**
     * Return the OPLauncher Applet frame
     * @return  the OPLauncher applet frame, or NULL if none was found (not initialized yet)
     */
    public JFrame getAppletFrame(String appletName) {
        LOCK.lock();
        try {
            AppletController controller = getControllerMap().get(appletName);
            if (controller != null) {
                return controller.getAppletFrame();
            }
            else {
                LOGGER.warn("(getAppletFrame) No controller found for applet: {}", appletName);
            }

            return _nullptr_();
        }
        finally {
            LOCK.unlock();
        }
    }

    public void setStatusBarMessage(String message, String appletName) {
        LOCK.lock();
        try {
            AppletController controller = getControllerMap().get(appletName);
            if (controller != null) {
                controller.getAppletStatusBarLabel().setText(message);
            }
            else {
                LOGGER.warn("(setStatusBarMessage) zNo controller found for applet: {}", appletName);
            }
        }
        finally {
            LOCK.unlock();
        }
    }

    /**
     * Verifies if the applet controller is initialized or not
     * @return  <i>True</i> if it's already initialized, <i>False</i> otherwise
     */
    public boolean isAppletControllerInitialized(String appletName) {
        LOCK.lock();
        try {
            AppletController controller = getControllerMap().get(appletName);
            return controller != null;
        }
        finally {
            LOCK.unlock();
        }
    }

    private Map<String, AppletController> getControllerMap() {
        return _controllerMap;
    }

    // class properties
    private Map<String, AppletController> _controllerMap = _nullptr_();
}
