package org.oplauncher.runtime;

import java.applet.Applet;

public enum AppletContextType {
    DEFAULT, UNKOWN
      ;

    static public AppletContextType parse(final String type) {
        if (type == null) return UNKOWN;

        for (AppletContextType t : AppletContextType.values()) {
            if (t.name().equalsIgnoreCase(type.trim())) return t;
        }

        return UNKOWN;
    }
}
