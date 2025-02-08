package org.oplauncher.op;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.oplauncher.ErrorCode.OPSERVER_NOT_AVAILABLE;

public class OPServerFactory {
    static private final Logger LOGGER = LogManager.getLogger(OPServerFactory.class);
    static private final Lock LOCK = new ReentrantLock();


    static public final OPServer newServer(OPCallback successcb, OPCallback failcb) {
        LOCK.lock();
        try {
            OPServerType type = ConfigurationHelper.getOPServerType();
            String ipAddress = ConfigurationHelper.getOPServerAddress();
            int port = ConfigurationHelper.getOPServerPort();

            switch (type) {
                case HTTP: {
                    return new HttpOPServer(ipAddress, port)
                            .registerSuccessCallback(successcb)
                            .registerFailureCallback(failcb);
                }
                default: {
                    throw new OPLauncherException("Unknown or unsupported OP server type: " + type, OPSERVER_NOT_AVAILABLE);
                }
            }
        }
        finally {
            LOCK.unlock();
        }
    }
}
