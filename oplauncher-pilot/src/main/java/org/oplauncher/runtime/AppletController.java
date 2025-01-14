package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.AppletClassLoader;
import org.oplauncher.OPLauncherException;
import org.oplauncher.OpCode;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AppletController {
    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(AppletController.class);

    protected AppletController(AppletClassLoader clzloader, AppletContext context) throws OPLauncherException {
        _classLoader = clzloader;
        _context = context;

        deactivateApplet();
    }

    public String execute(OpCode opcode) throws OPLauncherException {
        /// Visitor pattern for the respective operation code
        switch (opcode) {
            case LOAD_APPLET: return loadAppletClass();
            default: {
                throw new OPLauncherException(String.format("Unsupported operational code: [%s]", opcode.name()));
            }
        }
    }

    protected String renderApplet(Applet applet) throws Exception {
        LOCK.lock();
        try {
            defineAppletFrame("OPLauncher Applet Window").defineStatusBarLabel("Ready!");
            getAppletFrame().setSize(getAppletClassLoader().getAppletParameters().getWidth(), getAppletClassLoader().getAppletParameters().getHeight());
            getAppletFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            getAppletFrame().add(applet);

            getAppletStatusBar().setBorder(BorderFactory.createEtchedBorder());

            // Add the status bar to the bottom
            getAppletFrame().add(getAppletStatusBar(), BorderLayout.SOUTH);
            // Center the frame on the screen
            getAppletFrame().setLocationRelativeTo(null);

            getAppletClassLoader().getAppletController().activateApplet(); /// Mark the applet as active
            LOGGER.info("Calling applet STARTED");
            applet.start();

            // SHOW THE APPLET !!!
            getAppletFrame().setVisible(true);

            LOGGER.info("Applet successfully loaded !");

            return "";
        }
        finally {
            LOCK.unlock();
        }
    }
    protected String parseClassName(final String klass) {
        return klass.replace("/", ".")
                .replace("\\", ".")
                .replace(".class", "");
    }

    abstract protected String loadAppletClass() throws OPLauncherException;

    protected AppletClassLoader getAppletClassLoader() {
        return _classLoader;
    }

    protected AppletContext getAppletContext() {
        return _context;
    }

    public AppletController activateApplet() {
        LOCK.lock();
        try {
            _appletActive = true;

            return this;
        }
        finally {
            LOCK.unlock();
        }
    }
    public AppletController deactivateApplet() {
        LOCK.lock();
        try {
            _appletActive = false;

            return this;
        }
        finally {
            LOCK.unlock();
        }
    }

    public boolean isAppletActive() {
        LOCK.lock();
        try {
            return _appletActive;
        }
        finally {
            LOCK.unlock();
        }
    }

    protected AppletController defineAppletFrame(String title) {
        _appletFrame = new JFrame(title);
        return this;
    }
    protected AppletController defineStatusBarLabel(String text) {
        _appletStatusBarLabel = new JLabel(text);
        return this;
    }

    public JFrame getAppletFrame() {
        return _appletFrame;
    }
    public JLabel getAppletStatusBar() {
        return _appletStatusBarLabel;
    }

    // class properties
    private AppletClassLoader _classLoader;
    private AppletContext _context;

    private boolean _appletActive;

    // class properties
    private JFrame _appletFrame;
    private JLabel _appletStatusBarLabel;
}
