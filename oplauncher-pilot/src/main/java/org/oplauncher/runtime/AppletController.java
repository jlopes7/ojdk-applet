package org.oplauncher.runtime;

import org.oplauncher.AppletClassLoader;
import org.oplauncher.OPLauncherException;
import org.oplauncher.OpCode;

import javax.swing.*;
import java.applet.AppletContext;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AppletController {
    static private final Lock LOCK = new ReentrantLock();

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

    protected AppletController setAppletFrame(String title) {
        _appletFrame = new JFrame(title);
        return this;
    }
    protected AppletController setStatusBarLabel(String text) {
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
