package org.oplauncher.op.reflection;

public enum InterpretMethodDef {
    GETVERSION("getVersion", 0),
    GETVENDOR("getVendor", 1),
    GETPROP("getProp", 2),
    STATUSBAR("statusbar", 3),
    NO_INTERPRETATION("unknown", 999),
      ;

    static public InterpretMethodDef of(String methodName) {
        if ( methodName == null ) return NO_INTERPRETATION;

        for (InterpretMethodDef def : values()) {
            if ( methodName.trim().equalsIgnoreCase(def.methodName()) ) return def;
        }

        return NO_INTERPRETATION;
    }

    private InterpretMethodDef(String methodName, int idx) {
        _methodName = methodName;
        _idx = idx;
    }

    public int index() {
        return _idx;
    }
    public String methodName() {
        return _methodName;
    }

    // class properties
    private String _methodName;
    private int _idx;
}
