package org.oplauncher;

import java.util.List;

public class OpHelper {
    private static final int IDX_OPCODE = 0x00;
    private static final int IDX_RESURL = 0x01;

    static private <T>String paramValue(List<T> params, int idx) {
        if ( params!=null && params.size() > idx ) {
            return (String) params.get(idx);
        }

        return null;
    }

    static protected <T>String getOpCode(List<T> params) {
        String val;
        if ( params!=null && (val = paramValue(params, IDX_OPCODE)) != null ) {
            return val;
        }

        throw new RuntimeException(String.format("No opcode found for params: %s", params));
    }

    static protected <T>String getLoadURL(List<T> params) {
        String val;
        if ( params!=null && (val = paramValue(params, IDX_RESURL)) != null ) {
            String opcode = getOpCode(params);
            if ( val != null && OpCode.parse(opcode) == OpCode.LOAD_APPLET ) {
                return val ;
            }
            else if (val != null) {
                throw new RuntimeException(String.format("Incorrect operation provided: %s. Expected op: load_applet", opcode));
            }
        }

        throw new RuntimeException(String.format("No URL found for params: %s", params));
    }
}
