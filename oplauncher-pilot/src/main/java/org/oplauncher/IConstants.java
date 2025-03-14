package org.oplauncher;

public interface IConstants {
    static public final Object NULLPTR = null;
    static public final String EMPTY_STRING = "";

    static public final String DEFAULT_OPLAUNCHER_POOLNAME = "default-oplauncher-pool";

    static public final int NO_PROP_CODE = 0;
    static public final int EXIT_SUCCESS = 0;

    static public final float HEIGHT_RATE_FACTOR = 1.0f;
    static public final char DEFAULT_EOL = '\n';
    static public final char REGISTERED_CHAR = (char) 174;
    static public final String ON_VALUE = "on";
    static public final String OFF_VALUE = "off";

    static public final String SUCCESS_RESPONSE = "Success";
    static public final String FAILURE_RESPONSE = "Failure";
    static public final String OP_PARAM_LOW_VISIBILITY = "low_visibility";

    static public final int DEFAULT_INIT_POSX = 50;
    static public final int DEFAULT_INIT_POSY = 50;

    static public final int DEFAULT_CONNECTION_BACKLOG = 5;
    static public final int DEFAULT_CONNECTION_SETSOTIMEOUT_SEC = 5000;

    static public final String DEFAULT_OPSERVER_CTXROOT = "oplauncher-op";
    static public final String DEFAULT_HB_CTXROOT = "oplauncher-hb";
    static public final String HTTP_HEADER_CHROMEEXT_TKN = "X-Chrome-Extension-Tkn";

    static public final String WINREG_OPLAUNCHER_KEY = "SOFTWARE\\OPlauncher";
    static public final String WINREG_OPLAUNCHER_KEYVAL = "IniToken";

    static public final String CONFIG_FILENAME = "config.properties";
    static public final String CONFIG_LOG_FILENAME = "log4j2.xml";
    static public final String CONFIG_JAVACONSOLE_TEXT = "CONSOLE_TEXT";
    static public final String CONFIG_NATIVE_JAVACONSOLE_TYPE = "native";
    static public final String CONFIG_SWING_JAVACONSOLE_TYPE = "swing";

    static public final String CONFIG_JAVACONSOLE_FONTOPT1 = "Monospaced";
    static public final String CONFIG_JAVACONSOLE_FONTOPT2 = "Verdana";

    static public final String CONFIG_ICONRES_JAVACONSOLE = "/oplauncher_icon_32x32_2.png";
    static public final String CONFIG_ICONRES_INFO = "/info-button.png";
    static public final String CONFIG_ICONRES_REFRESH = "/refresh-button.png";
    static public final String CONFIG_ICONRES_CLEAR = "/clear-icon.png";
    static public final String CONFIG_ICONRES_SAVE = "/save-icon.png";
    static public final String CONFIG_ICONRES_INFO2 = "/info-icon.png";
    static public final String CONFIG_SPLASH_IMAGE = "/oplauncher_splash.png";

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
    static public final String CONFIG_PROP_APPLET_CLOSEWINDOW = "oplauncher.runtime.closewindow.active";
    static public final String CONFIG_PROP_APPLET_STATUSBAR = "oplauncher.runtime.statusbar.active";
    static public final String CONFIG_PROP_APPLET_TRACKWINPOS = "oplauncher.runtime.trackwindow.position";
    static public final String CONFIG_PROP_JAVACONSOLE = "oplauncher.config.java.console";
    static public final String CONFIG_PROP_JAVACONSOLE_TYPE = "oplauncher.config.java.console.type";
    static public final String CONFIG_PROP_APPLETFRAME_ICON = "oplauncher.runtime.icon";
    static public final String CONFIG_PROP_SPLASH_IMAGE = "oplauncher.splash.image";
    static public final String CONFIG_PROP_OP_SERVER_TYPE = "oplauncher.runtime.opserver.type";
    static public final String CONFIG_PROP_OP_SERVER_PORT = "oplauncher.runtime.opserver.port";
    static public final String CONFIG_PROP_OP_SERVER_IP = "oplauncher.runtime.opserver.registered_ip";
    static public final String CONFIG_PROP_OP_SERVER_CHROME_TOKEN = "oplauncher.runtime.opserver.chrome.token";
    static public final String CONFIG_PROP_OP_SERVER_CTXROOT = "oplauncher.runtime.opserver.ctxroot";
    static public final String CONFIG_PROP_OP_SERVER_APPTKN = "oplauncher.runtime.opserver.apptkn";
    static public final String CONFIG_PROP_OP_SERVER_APPTKN_ACTIVE = "oplauncher.runtime.opserver.apptkn.active";
    static public final String CONFIG_PROP_SECUR_ALGORITHM = "oplauncher.runtime.secur.alg";
    static public final String CONFIG_PROP_SECUR_MASK = "oplauncher.runtime.secur.mask";
    static public final String CONFIG_PROP_SECUR_ACTIVE = "oplauncher.runtime.secur.active";
    static public final String CONFIG_PROP_SECUR_ENCKEY = "oplauncher.runtime.secur.key";

    static public final String APPLETPARAM_WIDTH = "width";
    static public final String APPLETPARAM_HEIGHT = "height";
    static public final String APPLETPARAM_POSX = "posx";
    static public final String APPLETPARAM_POSY = "posy";

    static public final int APPLET_WIDTH_DEFAULT = 400;
    static public final int APPLET_HEIGHT_DEFAULT = 300;

    static public final int APPLET_ICON_SIZE_16 = 16;
}
