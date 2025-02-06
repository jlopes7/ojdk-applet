package org.oplauncher;

public enum OpCode {
    LOAD_APPLET("load_applet"),
    UNLOAD_APPLET("unload_applet"),
    CHANGE_POSTION("change_postion"),
    UNKNOWN("unknown")
      ;

    public String opcode() {
        return _opcode;
    }

    static public OpCode parse(String opcode) {
        if (opcode == null) return UNKNOWN;

        for (OpCode op : OpCode.values()) {
            if (op.opcode().equalsIgnoreCase(opcode.trim())) {
                return op;
            }
        }

        return UNKNOWN;
    }

    private OpCode(String opcd) {
        _opcode = opcd;
    }
    // class properties
    private String _opcode;
}
