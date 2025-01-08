package org.oplauncher;

public enum ErrorCode {
    GENERAL_ERROR(7000),
    UNSUPPORTED_OPERATION(7001),
    MALFORMED_URL(7002),
    SECURITY_ERROR(7003),
    FAILED_TO_DOWNLOAD_FILE(7004),
    UNSUPPORTED_OPCODE(7005)
      ;

    private ErrorCode(int cd) {
        _code = cd;
    }

    public int code() { return _code; }
    // class properties
    private int _code;
}
