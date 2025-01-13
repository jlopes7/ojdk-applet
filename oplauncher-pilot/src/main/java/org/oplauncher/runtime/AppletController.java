package org.oplauncher.runtime;

import org.oplauncher.AppletClassLoader;
import org.oplauncher.OPLauncherException;
import org.oplauncher.OpCode;

import java.applet.AppletContext;

public abstract class AppletController {

    protected AppletController(AppletClassLoader clzloader, AppletContext context) throws OPLauncherException {
        _classLoader = clzloader;
        _context = context;
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

    // class properties
    private AppletClassLoader _classLoader;
    private AppletContext _context;
}
