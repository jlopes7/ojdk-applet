package org.oplauncher.runtime;

import org.oplauncher.AppletClassLoader;
import org.oplauncher.OPLauncherException;

import java.applet.AppletContext;

public class DefaultAppletController extends AppletController {

    protected DefaultAppletController(AppletClassLoader clzloader, AppletContext context) throws OPLauncherException {
        super(clzloader, context);
    }

    @Override
    protected String loadAppletClass() throws OPLauncherException {
        return "";
    }
}
