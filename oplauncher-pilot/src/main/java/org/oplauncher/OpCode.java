package org.oplauncher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OpCode {
    LOAD_APPLET("load_applet"),
    UNLOAD_APPLET("unload_applet"),
    CHANGE_POSTION("move_applet"),
    FOCUS_APPLET("focus_applet"),
    BLUR_APPLET("blur_applet"),
    UNKNOWN("unknown")
      ;

    public String opcode() {
        return _opcode;
    }

    @JsonValue
    public String opname() {
        return _opcode;
    }

    @JsonCreator
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
