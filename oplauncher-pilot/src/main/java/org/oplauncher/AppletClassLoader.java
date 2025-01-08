package org.oplauncher;

import org.oplauncher.res.FileResource;

import java.util.List;

public class AppletClassLoader extends AbstractAppletClassLoader<String> {

    public AppletClassLoader() {
        super(AppletClassLoader.getSystemClassLoader());

        _appletController = new AppletController(this);
    }

    @Override
    public String processLoadAppletOp(List<String> parameters) throws OPLauncherException {
        // Step 1: Load the applet source code and cache it (if enabled)
        List<FileResource> loadedResources = loadAppletFromURL(parameters);

        return "";
    }

    // class properties
    private AppletController _appletController;
}
