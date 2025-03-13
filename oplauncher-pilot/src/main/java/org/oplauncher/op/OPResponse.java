package org.oplauncher.op;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OPResponse {

    public OPResponse(String response, boolean success, int rc) {
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
    public Object getReturnData() {
        return _returnData;
    }

    public OPResponse setUnsuccess() {
        _success = false;
        return this;
    }
    public OPResponse setSuccess() {
        _success = true;
        return this;
    }
    public OPResponse setErrorCode(int rc) {
        _errorCode = rc;
        return this;
    }
    public OPResponse setMessage(String msg) {
        _message = msg;
        return this;
    }
    public OPResponse setReturnData(Object data) {
        _returnData = data;
        return this;
    }

    // class properties
    @JsonProperty("message")
    private String _message;
    @JsonProperty("succeed")
    private boolean _success;
    @JsonProperty("errorcode")
    private int _errorCode;
    @JsonProperty("methodResp")
    private Object _returnData;
}
