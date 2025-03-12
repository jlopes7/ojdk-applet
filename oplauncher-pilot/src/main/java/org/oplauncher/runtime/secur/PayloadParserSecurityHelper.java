package org.oplauncher.runtime.secur;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;
import org.oplauncher.op.OPMessage;
import org.oplauncher.op.OPPlainPayload;
import org.oplauncher.op.OPSecurePayload;

import static org.oplauncher.ErrorCode.SECURE_PAYLOAD_PARSE_ERROR;

public class PayloadParserSecurityHelper {
    static private final Logger LOGGER = LogManager.getLogger(PayloadParserSecurityHelper.class);

    static public final OPPlainPayload decodeSecuredPayload(OPMessage message) {
        OPSecurePayload securePayload = (OPSecurePayload) message.getPayload();
        String encPayload = securePayload.getPayload();
        String encKey = ConfigurationHelper.getCipherKey();
        String decPayload = SecurityManager.getCipherProcessor().decryptPayload(encPayload, encKey);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processing secure payload...");
            LOGGER.info("Secure payload received from the Chrome extension: {}", encPayload);
            LOGGER.info("Secure payload deciphered from the Chrome extension: {}", decPayload);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(decPayload, OPPlainPayload.class);
        }
        catch (JsonProcessingException e) {
            throw new OPLauncherException("Failed to parse secure payload", e, SECURE_PAYLOAD_PARSE_ERROR);
        }
    }
}
