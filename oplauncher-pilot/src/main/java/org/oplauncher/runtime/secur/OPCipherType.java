package org.oplauncher.runtime.secur;

public enum OPCipherType {
    DES3,
    AES,
    UNKNOWN
      ;

    static public OPCipherType from(final String s) {
        if (s == null) return UNKNOWN;

        for (OPCipherType c : OPCipherType.values()) {
            if (c.name().equalsIgnoreCase(s.trim())) return c;
        }

        return UNKNOWN;
    }
}
