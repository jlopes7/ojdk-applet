package org.oplauncher.op;

public enum OPServerType {
    HTTP("http"),
    WEBSOCKET("websocket2"),
    UNKNOWN("unknown")
      ;

    private OPServerType(String name) {
        _typename = name;
    }

    static public OPServerType from(String name) {
        if (name == null) return UNKNOWN;

        for (OPServerType type : OPServerType.values()) {
            if (type._typename.equalsIgnoreCase(name.trim())) return type;
        }

        return UNKNOWN;
    }

    // enum properties
    private String _typename;
}
