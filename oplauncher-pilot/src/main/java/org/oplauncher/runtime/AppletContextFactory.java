package org.oplauncher.runtime;

import org.oplauncher.OPLauncherException;

import java.applet.AppletContext;

import static org.oplauncher.ErrorCode.*;

public class AppletContextFactory {

    static public AppletContext newAppletContext(AppletContextType type) throws OPLauncherException {
        switch (type) {
            case DEFAULT: return new DefaultAppletContext();
            default: {
                throw new OPLauncherException(String.format("Unsupported AppletContextType: [%s]", type), UNSUPPORTED_APPLET_CONTEXT);
            }
        }
    }
}
