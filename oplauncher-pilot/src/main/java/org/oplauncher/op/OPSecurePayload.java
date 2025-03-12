package org.oplauncher.op;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OPSecurePayload implements OPPayload {

    public String getPayload() {
        return Optional.ofNullable(_payload).orElse("");
    }

    public int getMessageSize() {
        return Optional.ofNullable(_msgsize).orElse(0);
    }

    public boolean isSyncedResponse() {
        return _syncResponse;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Payload: {")
                .append("\"p\"=\"")
                .append(getPayload())
                .append("\",\"msz\"=\"")
                .append(getMessageSize())
                .append("\",\"syncresp\"=")
                .append(isSyncedResponse())
                .append("}");

        return sb.toString();
    }

    // class properties
    @JsonProperty("p")
    private String _payload;
    @JsonProperty("msz")
    private Integer _msgsize;
    @JsonProperty("syncresp")
    private boolean _syncResponse;
}
