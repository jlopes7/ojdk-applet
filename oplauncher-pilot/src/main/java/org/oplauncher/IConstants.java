package org.oplauncher;

public interface IConstants {
    static public final Object NULLPTR = null;

    static public final float HEIGHT_RATE_FACTOR = 1.0f;
    static public final char DEFAULT_EOL = '\n';
    static public final char REGISTERED_CHAR = (char) 174;

    static public final String CONFIG_FILENAME = "config.properties";
    static public final String CONFIG_LOG_FILENAME = "log4j2.xml";

    static public final String CONFIG_LOG_DEF_RUNTIME_FILENAME = "oplauncher-pilot.log";
    static public final String CONFIG_LOG_DEF_RUNTIME_PATNAME = "oplauncher-pilot-%d{yyyy-MM-dd}-%i.log.gz";

    static public final String CONFIG_PROP_CACHE_ACTIVEFLAG = "oplauncher.cache.active";
    static public final String CONFIG_PROP_CACHE_FILEPATH = "oplauncher.cache.directory";
    static public final String CONFIG_PROP_CONFIG_ROOT = "oplauncher.config.root";
    static public final String CONFIG_PROP_RESOURCENAME = "oplauncher.runtime.res_name";
    static public final String CONFIG_PROP_CURTEMPCP = "oplauncher.runtime.current.classpath";
    static public final String CONFIG_PROP_LOGACTIVE = "plauncher.log.enabled";
    static public final String CONFIG_PROP_LOGFILENAME = "oplauncher.log.filepath";
    static public final String CONFIG_PROP_LOGDIR = "oplauncher.log.dir";
    static public final String CONFIG_PROP_LOGFILEPATTERN = "oplauncher.log.logfile.pattern";
    static public final String CONFIG_PROP_LOGLEVEL = "oplauncher.log.level";
    static public final String CONFIG_PROP_LOGPATTERN = "oplauncher.log.logpattern";
    static public final String CONFIG_PROP_LOGRATATION_SZ = "oplauncher.log.rotationsize";
    static public final String CONFIG_PROP_APPLETCONTEXT = "oplauncher.runtime.applet.context";
    static public final String CONFIG_PROP_ARCHIEVE_CLASSLOADER = "oplauncher.config.archive.classloader";
    static public final String CONFIG_PROP_APPLET_ALWAYSONTOP = "oplauncher.runtime.alwaysontop";
    static public final String CONFIG_PROP_APPLET_RESIZEFLAG = "oplauncher.runtime.resize.active";

    static public final String APPLETPARAM_WIDTH = "width";
    static public final String APPLETPARAM_HEIGHT = "height";

    static public final int APPLET_WIDTH_DEFAULT = 400;
    static public final int APPLET_HEIGHT_DEFAULT = 300;

    static public final int APPLET_ICON_SIZE_16 = 16;
}
