package org.oplauncher.res;

import org.oplauncher.OPLauncherException;

import static org.oplauncher.ErrorCode.*;

public class ResourceRequestFactory {

    static public IResourceRequest getResourceRequest(ResourceType type) throws OPLauncherException {
        switch (type) {
            case HTTP_SESSION_REQUEST: {
                return new HttpSessionResourceRequest();
            }
            default: {
                throw new OPLauncherException(("Unsupported resource type: " + type.name()), UNSUPPORTED_OPERATION);
            }
        }
    }
}
