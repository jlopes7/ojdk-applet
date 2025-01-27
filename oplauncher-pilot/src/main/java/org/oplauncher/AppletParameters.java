package org.oplauncher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.regex.Pattern.quote;
import static org.oplauncher.IConstants.*;

public class AppletParameters {
    static private final Lock LOCK = new ReentrantLock();
    static private final AppletParameters INSTANCE = new AppletParameters();

    private AppletParameters() {
        _paramMap = new LinkedHashMap<>();
    }

    private AppletParameters set(String paramsDef) {
        if (paramsDef == null) {
            throw new RuntimeException("No parameter provided to the Applet runtime");
        }

        _paramMap.clear();

        String parameters[] = paramsDef.split(quote(";"));
        for (String paramSet : parameters) {
            final int IDX_KEY = 0;
            final int IDX_VAL = 1;
            String paramSetParts[] = paramSet.split(quote("="));

            if ( paramSetParts.length > IDX_VAL ) {
                _paramMap.put(paramSetParts[IDX_KEY].trim().toLowerCase()/*make case insensitive*/, paramSetParts[IDX_VAL].trim());
            }
        }

        return this;
    }

    static public final AppletParameters getInstance(String params) {
        LOCK.lock();
        try {
            return INSTANCE.set(params);
        }
        finally {
            LOCK.unlock();
        }
    }

    public final int getWidth() {
        LOCK.lock();
        try {
            String widthStr = _paramMap.get(APPLETPARAM_WIDTH);
            return widthStr == null ? APPLET_WIDTH_DEFAULT : Integer.parseInt(widthStr);
        }
        finally {
            LOCK.unlock();
        }
    }

    public final int getHeight() {
        LOCK.lock();
        try {
            String heightStr = _paramMap.get(APPLETPARAM_HEIGHT);
            return heightStr == null ? APPLET_HEIGHT_DEFAULT : Float.valueOf(Float.parseFloat(heightStr) * HEIGHT_RATE_FACTOR).intValue();
        }
        finally {
            LOCK.unlock();
        }
    }

    public final AppletParameters setPosition(int x, int y) {
        _paramMap.put(APPLETPARAM_POSX, String.valueOf(x));
        _paramMap.put(APPLETPARAM_POSY, String.valueOf(y));
        return this;
    }

    public final int getPositionX() {
        LOCK.lock();
        try {
            String posXStr = _paramMap.get(APPLETPARAM_POSX);
            return posXStr == null ? -1 : Integer.valueOf(posXStr).intValue();
        }
        finally {
            LOCK.unlock();
        }
    }

    public final int getPositionY() {
        LOCK.lock();
        try {
            String posYStr = _paramMap.get(APPLETPARAM_POSY);
            return posYStr == null ? -1 : Integer.valueOf(posYStr).intValue();
        }
        finally {
            LOCK.unlock();
        }
    }

    public final String getCustomParameter(String name) {
        LOCK.lock();
        try {
            return _paramMap.get(name);
        }
        finally {
            LOCK.unlock();
        }
    }

    /// class properties
    private Map<String,String> _paramMap;
}
