package org.oplauncher;

public class AppletController {

    public AppletController(AppletClassLoader clzloader) {
        _classLoader = clzloader;
    }

    // class properties
    private AppletClassLoader _classLoader;
}
