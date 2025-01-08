package org.oplauncher;

import java.util.List;

public class OpHelper {
    private static final int IDX_OPCODE = 0x01;

    static protected <T>String getOpCode(List<T> params) {
        if ( params!=null && params.size() > IDX_OPCODE ) {
            return (String) params.get(IDX_OPCODE);
        }

        throw new RuntimeException(String.format("No opcode found for params %s", params));
    }
}
