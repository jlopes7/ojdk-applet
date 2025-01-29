package org.oplauncher.runtime;

import org.eclipse.swt.SWT;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.ErrorCode;
import org.oplauncher.OPLauncherException;

import javax.swing.*;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.oplauncher.IConstants.*;

public class JavaConsoleBuilder {
    static private final Lock LOCK = new ReentrantLock();
    public enum ConsoleType {
        SWING, SWT, UNKNOWN;

        static public ConsoleType parse(String type) {
            if (type == NULLPTR) return UNKNOWN;

            if (type.trim().equalsIgnoreCase(CONFIG_SWING_JAVACONSOLE_TYPE)) return SWING;
            else if (type.trim().equalsIgnoreCase(CONFIG_NATIVE_JAVACONSOLE_TYPE)) return SWT;
            else return UNKNOWN;
        }
    }

    static public final JavaConsoleBuilder.Builder newConsole(AppletController controller) {
        LOCK.lock();
        try {
            return new Builder(ConfigurationHelper.getConsoleType(), controller);
        }
        finally {
            LOCK.unlock();
        }
    }

    static public class Builder {
        private Builder(ConsoleType consoleType, AppletController controller) {
            _consoleType = consoleType;
            _controller = controller;
        }

        public Builder load() {
            LOCK.lock();
            try {
                switch (_consoleType) {
                    case SWING: {
                        SwingUtilities.invokeLater(() -> {
                            _javaConsole = new SwingJavaConsole(_controller).display(DEFAULT_INIT_POSX, DEFAULT_INIT_POSY);
                        });
                        break;
                    }
                    case SWT: {
                        _javaConsoleMonitor = new SWTJavaConsoleMonitor(_controller);
                        _javaConsoleMonitor.start();

                        break;
                    }
                    default:
                        throw new OPLauncherException("Unsupported console type: " + _consoleType.name(), ErrorCode.FAILED_TO_LOAD_RESOURCE);
                }

                return this;
            }
            finally {
                LOCK.unlock();
            }
        }

        public JavaConsole show() {
            LOCK.lock();
            try {
                return _javaConsoleMonitor!=null ? _javaConsoleMonitor.getJavaConsole() : _javaConsole;
            }
            finally {
                LOCK.unlock();
            }
        }

        // class properties
        private JavaConsole _javaConsole;
        private ConsoleType _consoleType;
        private SWTJavaConsoleMonitor _javaConsoleMonitor;

        private AppletController _controller;
    }
}
