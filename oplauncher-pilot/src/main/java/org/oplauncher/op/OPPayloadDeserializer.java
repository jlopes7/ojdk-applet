package org.oplauncher.op;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OPPayloadDeserializer extends JsonDeserializer<List<String>> {
    static private final Logger LOGGER = LogManager.getLogger(OPPayloadDeserializer.class);
    @Override
    public List<String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("(deserialize) JSON current token: {}", jsonParser.getCurrentToken());
        }
        // Move to the first token if it's not set properly
        JsonToken token = jsonParser.getCurrentToken();
        if (token == null) {
            token = jsonParser.nextToken();
        }
        // If params is a JSON array, parse it normally
        if (token == JsonToken.START_ARRAY) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("(deserialize) JSON parameter is an array, parsing it...");
            }
            ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            return mapper.readValue(jsonParser, new TypeReference<List<String>>() {});
        }

        // If params is a single string, split it
        if (token == JsonToken.VALUE_STRING) {
            String value = jsonParser.getValueAsString();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("(deserialize) JSON parameter is given as a string: {}", value);
            }
            if (value == null || value.isEmpty()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(Arrays.asList(value.split(";")));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("(deserialize) Unexpected type: returning an empty list to avoid crashes");
        }
        // Unexpected type: return an empty list to avoid crashes
        return new ArrayList<>();
    }
}
