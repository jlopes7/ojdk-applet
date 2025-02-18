package org.oplauncher.runtime.secur;

import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;

import static org.oplauncher.ErrorCode.UNSUPPORTED_CIPHER_TYPE;

public class SecurityManager {

    static public final OPCipherProcessor getCipherProcessor() {
        OPCipherType type = ConfigurationHelper.getDefaultCipherType();
        switch (type) {
            case DES3: return new CipherOPMessage3DESProcessor();
            default:
                throw new OPLauncherException("Unsupported cipher type: " + type, UNSUPPORTED_CIPHER_TYPE);
        }
    }
}
