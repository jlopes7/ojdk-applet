package org.oplauncher;

import java.applet.AppletContext;
import java.util.List;

public class AppletClassLoader extends ClassLoader {

    public AppletClassLoader() {
        super(AppletClassLoader.getSystemClassLoader());
    }

    public <T>String loadApplet(List<T> parameters) {
        return "Dummy Response";
    }
}
