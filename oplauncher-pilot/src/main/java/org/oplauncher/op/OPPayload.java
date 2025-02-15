package org.oplauncher.op;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.oplauncher.OpCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.oplauncher.OpCode.UNKNOWN;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OPPayload {

    public String getToken() {
        return Optional.ofNullable(_token).orElse("");
    }

    public Integer getPosX() {
        return _posx;
    }
    public Integer getPosY() {
        return _posy;
    }
    public Integer getWidth() {
        return _width;
    }
    public Integer getHeight() {
        return _height;
    }

    public OpCode getOpCode() {
        return Optional.ofNullable(_optype).orElse(UNKNOWN);
    }

    public List<String> getParameters() {
        return _parameters!=null ? _parameters : new ArrayList<String>();
    }

    public String getAppletName() {
        return _appletName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Payload: {")
                .append("\"_tkn_\"=\"")
                .append(getToken())
                .append("\",\"opcode\"=\"")
                .append(getOpCode().opname())
                .append("\",\"parameters\"=\"")
                .append(String.join(",", getParameters()));
        if ( getPosX()   != null ) sb.append(",\"px\"=").append(getPosX());
        if ( getPosY()   != null ) sb.append(",\"py\"=").append(getPosY());
        if ( getWidth()  != null ) sb.append(",\"w\"=").append(getWidth());
        if ( getHeight() != null ) sb.append(",\"h\"=").append(getHeight());
        sb.append("\"}");

        return sb.toString();
    }

    // class properties
    @JsonProperty("_tkn_")
    private String _token;
    @JsonProperty("op")
    private OpCode _optype;

    @JsonProperty("px")
    private Integer _posx;
    @JsonProperty("py")
    private Integer _posy;

    @JsonProperty("w")
    private Integer _width;
    @JsonProperty("h")
    private Integer _height;

    @JsonProperty("applet_name")
    private String _appletName;

    @JsonProperty("params")
    @JsonDeserialize( using = OPPayloadDeserializer.class, contentAs = String.class )
    private List<String> _parameters;
}
