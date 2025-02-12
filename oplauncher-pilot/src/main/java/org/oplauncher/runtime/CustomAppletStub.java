package org.oplauncher.runtime;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.net.URL;

public class CustomAppletStub implements AppletStub {

    public CustomAppletStub(AppletController controller) {
        _controller = controller;
    }

    @Override
    public boolean isActive() {
        return _controller.isAppletActive();
    }

    @Override
    public URL getDocumentBase() {
        return _controller.getAppletClassLoader().getAppletDocumentBase();
    }

    @Override
    public URL getCodeBase() {
        return _controller.getAppletClassLoader().getAppletCodeBase();
    }

    @Override
    public String getParameter(String name) {
        return _controller.getAppletClassLoader()
                .getAppletParameters()
                .getCustomParameter(_controller.getAppletClassLoader().getInstanceName(), name);
    }

    @Override
    public AppletContext getAppletContext() {
        return _controller.getAppletContext();
    }

    @Override
    public void appletResize(int width, int height) {
        _controller.getAppletFrame().setSize(width, height);
    }

    // class properties
    private AppletController _controller;
}
