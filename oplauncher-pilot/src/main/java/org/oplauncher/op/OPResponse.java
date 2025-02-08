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

    protected String getMessage() {
        return _message;
    }
    public boolean isSuccess() {
        return _success;
    }
    public int getErrorCode() {
        return _errorCode;
    }

    // class properties
    @JsonProperty("message")
    private String _message;
    @JsonProperty("succeed")
    private boolean _success;
    @JsonProperty("errorcode")
    private int _errorCode;
}
