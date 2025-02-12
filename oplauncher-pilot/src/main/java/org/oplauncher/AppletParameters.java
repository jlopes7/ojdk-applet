package org.oplauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.regex.Pattern.quote;
import static org.oplauncher.IConstants.*;

public class AppletParameters {
    static private final Logger LOGGER = LogManager.getLogger(AppletParameters.class);
    static private final Lock LOCK = new ReentrantLock();
    static private final AppletParameters INSTANCE = new AppletParameters();

    private AppletParameters() {
        _paramMap = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    private AppletParameters set(String appletName, String paramsDef) {
        if (paramsDef == null) {
            throw new RuntimeException("No parameter provided to the Applet runtime");
        }
        if (appletName == null) {
            throw new RuntimeException("No Applet name provided");
        }

        List<ParamPair> pairs = getPairs(appletName);

        _paramMap.clear();

        String parameters[] = paramsDef.split(quote(";"));
        for (String paramSet : parameters) {
            final int IDX_KEY = 0;
            final int IDX_VAL = 1;
            String paramSetParts[] = paramSet.split(quote("="));

            if ( paramSetParts.length > IDX_VAL ) {
                ParamPair paramPair = new ParamPair(paramSetParts[IDX_KEY].trim().toLowerCase()/*make case insensitive*/, paramSetParts[IDX_VAL].trim());

                if ( !pairs.contains(paramPair) ) {
                    pairs.add(paramPair);
                }
            }
        }
        // Update the pair
        _paramMap.put(appletName, pairs);

        return this;
    }

    static public final AppletParameters getInstance(String appletName, String params) {
        LOCK.lock();
        try {
            return INSTANCE.set(appletName, params);
        }
        finally {
            LOCK.unlock();
        }
    }

    public final int getWidth(String appletName) {
        LOCK.lock();
        try {
            String widthStr = getParameterValue(appletName, APPLETPARAM_WIDTH);
            if ( LOGGER.isDebugEnabled() ) {
                LOGGER.debug("(getWidth) Applet({}}) width being retrieved: {}}", appletName, widthStr);
            }
            return widthStr == null ? APPLET_WIDTH_DEFAULT : Float.valueOf(widthStr).intValue();
        }
        finally {
            LOCK.unlock();
        }
    }

    public final int getHeight(String appletName) {
        LOCK.lock();
        try {
            String heightStr = getParameterValue(appletName, APPLETPARAM_HEIGHT);
            if ( LOGGER.isDebugEnabled() ) {
                LOGGER.debug("(getHeight) Applet({}) height being retrieved: {}", appletName, heightStr);
            }
            return heightStr == null ? APPLET_HEIGHT_DEFAULT : Float.valueOf(Float.parseFloat(heightStr) * HEIGHT_RATE_FACTOR).intValue();
        }
        finally {
            LOCK.unlock();
        }
    }

    public final AppletParameters setPosition(String appletName, int x, int y) {
        setParameterValue(appletName, APPLETPARAM_POSX, String.valueOf(x));
        setParameterValue(appletName, APPLETPARAM_POSY, String.valueOf(y));
        return this;
    }

    public final int getPositionX(String appletName) {
        LOCK.lock();
        try {
            String posXStr = getParameterValue(appletName, APPLETPARAM_POSX);
            if ( LOGGER.isDebugEnabled() ) {
                LOGGER.debug("(getPositionX) Applet({}) X-axis being retrieved: {}", appletName, posXStr);
            }
            return posXStr == null ? -1 : Float.valueOf(posXStr).intValue();
        }
        finally {
            LOCK.unlock();
        }
    }

    public final int getPositionY(String appletName) {
        LOCK.lock();
        try {
            String posYStr = getParameterValue(appletName, APPLETPARAM_POSY);
            if ( LOGGER.isDebugEnabled() ) {
                LOGGER.debug("(getPositionY) Applet({}) Y-axis being retrieved: {}", appletName, posYStr);
            }
            return posYStr == null ? -1 : Float.valueOf(posYStr).intValue();
        }
        finally {
            LOCK.unlock();
        }
    }

    public final String getCustomParameter(String appletName, String name) {
        LOCK.lock();
        try {
            return getParameterValue(appletName, name);
        }
        finally {
            LOCK.unlock();
        }
    }

    /*public List<String> getAllParameterKeyValuePairs() {
        return _paramMap.entrySet()
                .stream()
                .map(e -> (e.getKey()
                        .concat("=")
                        .concat(e.getValue())
                        .concat(",")))
                .collect(Collectors.toList());
    }*/

    static public class ParamPair {
        ParamPair(String key, String value) {
            _name = key;
            _value = value;
        }

        public String getName() {
            return _name;
        }
        public String getValue() {
            return _value;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            ParamPair paramPair = (ParamPair) o;
            return Objects.equals(_name, paramPair._name) && Objects.equals(_value, paramPair._value);
        }

        @Override
        public String toString() {
            return String.format("%s=%s", _name, _value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_name, _value);
        }

        // class properties
        private String _name;
        private String _value;
    }

    private List<ParamPair> getPairs(String name) {
        return Optional.ofNullable(_paramMap.get(name)).orElse(Collections.synchronizedList(new ArrayList<>()));
    }

    protected String getParameterValue(String appletName, String name) {
        return getPairs(appletName)
                .stream()
                .filter(pp -> pp.getName().trim().equalsIgnoreCase(name.trim()))
                .map(ParamPair::getValue)
                .findFirst().orElse(EMPTY_STRING);
    }
    protected AppletParameters setParameterValue(String appletName, String name, String value) {
        List<ParamPair> pairs = getPairs(appletName);
        ParamPair paramPair = new ParamPair(name.toLowerCase().trim(), value.trim());
        if ( pairs.isEmpty() || !pairs.contains(paramPair) ) {
            pairs.add(paramPair);
        }

        _paramMap.put(appletName, pairs);

        return this;
    }

    /// class properties
    private Map<String,List<ParamPair>> _paramMap;
}
