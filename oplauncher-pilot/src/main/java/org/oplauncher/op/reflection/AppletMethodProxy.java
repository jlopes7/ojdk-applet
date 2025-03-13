package org.oplauncher.op.reflection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.OPLauncherException;
import org.oplauncher.op.*;
import org.oplauncher.runtime.AppletRegistry;
import org.oplauncher.runtime.secur.PayloadParserSecurityHelper;

import java.applet.Applet;
import java.lang.reflect.Method;
import java.util.List;

import static org.oplauncher.ErrorCode.FAILED_TO_FIND_APPLET_INSTANCE;
import static org.oplauncher.ErrorCode.REFLECTION_ERROR;
import static org.oplauncher.IConstants.NO_PROP_CODE;
import static org.oplauncher.op.reflection.InterpretMethodDef.NO_INTERPRETATION;

public class AppletMethodProxy<P extends OPPayload> extends AppletReflection {
    static private final Logger LOGGER = LogManager.getLogger(AppletMethodProxy.class);


    public AppletMethodProxy(OPMessage<P> message, OPHandler<P> handler) {
        _messagePayload = message;
        _handler = handler;
    }

    protected OPMessage<P> getMessage() {
        return _messagePayload;
    }
    protected OPHandler<P> getHandler() {
        return _handler;
    }

    protected boolean isInterpretMethod(String methodName) {
        return InterpretMethodDef.of(methodName) != NO_INTERPRETATION;
    }

    public OPResponse invoke() {
        OPPlainPayload payload = PayloadParserSecurityHelper.decodeSecuredPayload(getMessage());
        if ( payload != null ) {
            String appletName = payload.getAppletName();
            String methodName = payload.getMethodName();
            Applet applet = AppletRegistry.get(appletName);
            List<String> objGivenParams = payload.getParameters();

            if (applet == null) {
                throw new OPLauncherException(String.format("No Applet registered with the given name: %s", appletName), FAILED_TO_FIND_APPLET_INSTANCE);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("About to invoke the method '{}' on the applet: {}", methodName, appletName);
            }

            if ( !isInterpretMethod(methodName) ) {
                Method method = getMethodFromNumOfParameters(applet, methodName, objGivenParams.size());
                if (method == null) {
                    throw new OPLauncherException(String.format("No method found in the Applet instance (%s --> %s) for the given name: %s", appletName, applet.getClass().getName(), methodName), REFLECTION_ERROR);
                }
                List<?> objParams = convertClientParametersToMethodParameters(objGivenParams, method);

                ///  LETS INVOKE THE APPLET CLASS EXECUTION  ///
                try {
                    /*
                     * TODO: Currently this invoke operation only supports native types as a return
                     *       meaning Custom object types (!= from String) will throw an exception in the current
                     *       version
                     */
                    Object result = method.invoke(applet, objParams.toArray(new Object[0]));
                    /*if ( result != null && ( !result.getClass().isPrimitive() ) {
                        throw new OPLauncherException(String.format("Currently the remote method invocation only support primitive return types. Provided type: %s", result.getClass().getName()));
                    }*/

                    return new OPResponse("Method execution succeeded!", true, NO_PROP_CODE).setReturnData(result);
                }
                catch (Exception e) {
                    LOGGER.error("Error while invoking the method: {}", methodName, e);
                    throw new OPLauncherException(e, REFLECTION_ERROR);
                }
            }
            // Interpreted methods ....
            else {
                Object result = null;
                InterpretMethodDef imdef = InterpretMethodDef.of(methodName);
                InterpretMethodProcessor imp = new InterpretMethodProcessor(imdef, applet);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("+++ Method '{}' on the applet '{}' is interpret", methodName, appletName);
                }

                result = imp.interpret();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Interpretation for the method '{}' completed successful. Result: {} (it could be null since it could 'void')", methodName, result);
                }

                return new OPResponse("Method execution succeeded!", true, NO_PROP_CODE).setReturnData(result);
            }
        }
        else {
            throw new OPLauncherException(String.format("Unsupported payload type: %s", getMessage().getPayload().getClass().getName()));
        }
    }

    // class properties
    private OPMessage<P> _messagePayload;
    private OPHandler<P> _handler;
}
