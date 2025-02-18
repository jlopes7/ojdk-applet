package org.oplauncher.runtime.secur;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.codec.binary.Base64;
import org.oplauncher.OPLauncherException;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.oplauncher.ErrorCode.ERROR_SECURITY_ERROR;

public class CipherOPMessage3DESProcessor implements OPCipherProcessor {
    static private final Logger LOOGER = LogManager.getLogger(CipherOPMessage3DESProcessor.class);
    private static final String ALGORITHM = "DESede/ECB/PKCS5Padding"; // Triple DES in ECB mode

    protected CipherOPMessage3DESProcessor() {}

    @Override
    public String decryptPayload(String encryptedBase64Json, String base64Key) throws OPLauncherException {
        // Decode the 24-bytes Base64 Key
        byte[] keyBytes = Base64.decodeBase64(base64Key);

        if (LOOGER.isInfoEnabled()) {
            LOOGER.info("Encoded Key: {}", base64Key);
            LOOGER.info("Encoded Payload: {}", encryptedBase64Json);
        }

        // Ensure key is exactly 24 bytes (DES3 requires a 192-bit key)
        if (keyBytes.length != 24) {
            throw new OPLauncherException("Invalid DES3 key length. Expected 24 bytes.", ERROR_SECURITY_ERROR);
        }

        // Decode the Base64 Encrypted JSON payload
        byte[] encryptedBytes = Base64.decodeBase64(encryptedBase64Json);

        try {
            // Configure Cipher for DES3 (Triple DES)
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "DESede");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            // Decrypt the data
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedPayload = new String(decryptedBytes, UTF_8);
            if (LOOGER.isDebugEnabled()) {
                LOOGER.debug("Cipher encoded size: {} bytes", encryptedBytes.length);
                LOOGER.debug("Cipher decoded size: {} bytes", decryptedBytes.length);
                LOOGER.debug("Decrypted payload: {}", decryptedPayload);
            }

            return decryptedPayload;
        }
        catch (Exception e) {
            throw new OPLauncherException("Decryption process failed for the payload:" + encryptedBase64Json, e, ERROR_SECURITY_ERROR);
        }
    }
}
