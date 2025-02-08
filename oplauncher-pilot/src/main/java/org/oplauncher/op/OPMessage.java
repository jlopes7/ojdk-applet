package org.oplauncher.op;

import org.oplauncher.ErrorCode;

import javax.lang.model.type.ErrorType;

public class OPMessage {

    protected OPMessage(OPPayload payload) {
        _payload = payload;
    }

    protected OPMessage setErrorCode(ErrorCode code) {
        _errorCode = code;
        return this;
    }
    protected OPMessage setError() {
        _haveError = true;
        return this;
    }
    protected OPMessage setErrorDetails(Exception e) {
        _error = e;
        return this;
    }

    public OPPayload getPayload() {
        return _payload;
    }

    public boolean haveError() {
        return _haveError;
    }

    public Exception getError() {
        return _error;
    }

    public ErrorCode getErrorType() {
        return _errorCode;
    }

    // class properties
    private OPPayload _payload;
    private boolean _haveError;
    private Exception _error;
    private ErrorCode _errorCode;
}
