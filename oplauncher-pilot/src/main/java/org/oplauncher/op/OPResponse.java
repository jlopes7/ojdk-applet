package org.oplauncher.op;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OPResponse {

    protected OPResponse(String response, boolean success, int rc) {
        _message = response;
        _success = success;
        _errorCode = rc;
    }

    public String getMessage() {
        return _message;
    }
    public boolean isSuccess() {
        return _success;
    }
    public int getErrorCode() {
        return _errorCode;
    }

    protected OPResponse setUnsuccess() {
        _success = false;
        return this;
    }
    protected OPResponse setSuccess() {
        _success = true;
        return this;
    }
    protected OPResponse setErrorCode(int rc) {
        _errorCode = rc;
        return this;
    }
    protected OPResponse setMessage(String msg) {
        _message = msg;
        return this;
    }

    // class properties
    @JsonProperty("message")
    private String _message;
    @JsonProperty("succeed")
    private boolean _success;
    @JsonProperty("errorcode")
    private int _errorCode;
}
