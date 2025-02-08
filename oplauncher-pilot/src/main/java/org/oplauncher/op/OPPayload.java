package org.oplauncher.op;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.oplauncher.OpCode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OPPayload {

    public String getToken() {
        return _token;
    }

    public OpCode getOpCode() {
        return _optype;
    }

    public List<String> getParameters() {
        return _parameters;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Payload {")
                .append("\"_tkn_\"=\"")
                .append(_token)
                .append("\",\"opcode\"=\"")
                .append(getOpCode().opname())
                .append("\",\"parameters\"=\"")
                .append(String.join(",", _parameters))
                .append("\"}");

        return sb.toString();
    }

    // class properties
    @JsonProperty("_tkn_")
    private String _token;
    @JsonProperty("opcode")
    private OpCode _optype;

    @JsonProperty("params")
    private List<String> _parameters;
}
