package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
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

    private OPLauncherConfig() {}

    protected void registerController(final AppletController controller) {
        LOCK.lock();
        try {
            _controller = controller;
        }
        finally {
            LOCK.unlock();
        }
    }

    /**
     * Changes the ICON from the Applet frame
     * @param icon  the new ICON to be used by the OPLauncher
     */
    public void setFrameIcon(final ImageIcon icon) {
        LOCK.lock();
        try {
            if (getController() != null) {
                getController().getAppletFrame().setIconImage(icon.getImage());
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
    public void resizeFrame(final int width, final int height) {
        LOCK.lock();
        try {
            if (getController() != null) {
                getController().getAppletFrame().setSize(width, height);
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
    public JFrame getAppletFrame() {
        LOCK.lock();
        try {
            if (getController() != null) {
                return getController().getAppletFrame();
            }

            return _nullptr_();
        }
        finally {
            LOCK.unlock();
        }
    }

    /**
     * Verifies if the applet controller is initialized or not
     * @return  <i>True</i> if it's already initialized, <i>False</i> otherwise
     */
    public boolean isAppletControllerInitialized() {
        LOCK.lock();
        try {
            return getController() != null;
        }
        finally {
            LOCK.unlock();
        }
    }

    private AppletController getController() {
        return _controller;
    }

    // class properties
    private AppletController _controller = _nullptr_();
}
