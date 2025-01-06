package org.oplauncher;

import static org.oplauncher.ErrorCode.*;

public class OPLauncherException extends RuntimeException {
    public OPLauncherException(String message) {
        super(message);
        _errorCode = GENERAL_ERROR;
    }
    public OPLauncherException(String message, Throwable cause) {
        super(message, cause);
        _errorCode = GENERAL_ERROR;
    }
    public OPLauncherException(Throwable cause) {
        super(cause);
        _errorCode = GENERAL_ERROR;
    }
    public OPLauncherException(String message, ErrorCode errCd) {
        super(message);
        _errorCode = errCd;
    }
    public OPLauncherException(Throwable cause, ErrorCode errCd) {
        super(cause);
        _errorCode = errCd;
    }
    public OPLauncherException(String message, Throwable cause, ErrorCode errCd) {
        super(message, cause);
        _errorCode = errCd;
    }
    public OPLauncherException(ErrorCode errCd) {
        super("Unknonw error");
        _errorCode = errCd;
    }

    public ErrorCode getErrorCode() {
        return _errorCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(". Error code: (");
        sb.append(getErrorCode().name());
        sb.append(", ");
        sb.append(getErrorCode().code());
        sb.append(")");

        return sb.toString();
    }

    // class properties
    private ErrorCode _errorCode;
}
