package org.oplauncher.runtime.secur;

import org.oplauncher.OPLauncherException;

public interface OPCipherProcessor {
    public String decryptPayload(String encryptedBase64Json, String base64Key) throws OPLauncherException;
}
