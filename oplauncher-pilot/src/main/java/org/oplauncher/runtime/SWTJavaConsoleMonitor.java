package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Event;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.oplauncher.IConstants.DEFAULT_INIT_POSX;
import static org.oplauncher.IConstants.DEFAULT_INIT_POSY;

public class SWTJavaConsoleMonitor extends Thread {
    static private final Logger LOGGER = LogManager.getLogger(SWTJavaConsoleMonitor.class);
    static private final Lock LOCK = new ReentrantLock();

    static private boolean _inited = false;

    public SWTJavaConsoleMonitor(AppletController controller) {
        super("SWTJavaConsoleMonitor");
        _controller = controller;
        resetInited();
        setDaemon(true);
        setContextClassLoader(SWTJavaConsoleMonitor.class.getClassLoader());
        setPriority(Thread.MIN_PRIORITY);
        setDefaultUncaughtExceptionHandler((thread, t) -> {
            LOGGER.error("The thread monitor failed", t);
        });
    }

    static public final boolean isInited() {
        LOCK.lock();
        try {
            return _inited;
        }
        finally {
            LOCK.unlock();
        }
    }

    static protected final void setInited(boolean inited) {
        LOCK.lock();
        try {
            _inited = inited;
        }
        finally {
            LOCK.unlock();
        }
    }

    static protected final void resetInited() {setInited(false);}
    public static void resetInited(Event event) {
        resetInited();
    }
    static protected final void setInited() {setInited(true);}

    @Override
    public void run() {
        _javaConsole = new SWTJavaConsole(getController()).display(DEFAULT_INIT_POSX, DEFAULT_INIT_POSY); // Position on screen
        setInited();

        SWTJavaConsole swtConsole = (SWTJavaConsole) _javaConsole;
        while (!swtConsole.getShell().isDisposed()) {
            if (!swtConsole.getDisplay().readAndDispatch()) {
                swtConsole.getDisplay().sleep();
            }
        }
        swtConsole.getDisplay().dispose();
    }

    public JavaConsole getJavaConsole() {
        return _javaConsole;
    }

    protected AppletController getController() {
        return _controller;
    }

    // class properties
    private JavaConsole _javaConsole;
    private AppletController _controller;
}
