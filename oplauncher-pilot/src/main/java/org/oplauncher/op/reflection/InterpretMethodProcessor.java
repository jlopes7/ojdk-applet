package org.oplauncher.op.reflection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.OPLauncherException;
import org.oplauncher.runtime.OPLauncherConfig;

import java.applet.Applet;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.oplauncher.ErrorCode.GENERAL_ERROR;

public class InterpretMethodProcessor {
    static private final Logger LOGGER = LogManager.getLogger(InterpretMethodProcessor.class);

    public InterpretMethodProcessor(InterpretMethodDef imgdef, Applet applet) {
        _interpretMethodDef = imgdef;
        _applet = applet;
    }

    protected InterpretMethodDef getMethodDefinition() {
        return _interpretMethodDef;
    }
    protected Applet getApplet() {
        return _applet;
    }

    @SuppressWarnings("unchecked")
    public <T>T interpret(String ...args) {
        switch (getMethodDefinition()) {
            case GETVERSION: return (T) getJavaVersion();
            case GETVENDOR:  return (T) getJavaVendor();
            case GETPROP:    return (T) getProperty(args);
            case STATUSBAR:  return (T) setStaturBar(args);
            default:
                LOGGER.warn("Unknown method to interpret: {}. No change was applied to environment. Applet name/class: {} --> {}", getMethodDefinition().methodName(), getApplet().getName(), getApplet().getClass().getName());
        }

        return null;
    }

    protected String getJavaVersion() {
        return System.getProperty("java.version");
    }
    protected String getJavaVendor() {
        return System.getProperty("java.vendor");
    }
    protected String getProperty(String ...args) {
        String key = null;
        if (args!=null && args.length > 0 && (key = args[0]) != null) {
            return System.getProperty(key);
        }
        else {
            throw new OPLauncherException("No property key specified", GENERAL_ERROR);
        }
    }
    protected Boolean setStaturBar(String ...args) {
        String statusMsg = null;
        if (args!=null && args.length > 0 && (statusMsg = args[0]) != null) {
            OPLauncherConfig.instance.setStatusBarMessage(statusMsg, getApplet().getName());
            return TRUE;
        }

        return FALSE;
    }

    // class properties
    private InterpretMethodDef _interpretMethodDef;
    private Applet _applet;
}
