package org.oplauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.oplauncher.IConstants.FAILURE_RESPONSE;
import static org.oplauncher.IConstants.SUCCESS_RESPONSE;

public class OPLauncherController {
    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(OPLauncherController.class);
    static {
        ///  Logger initialization
        ConfigurationHelper.intializeLog();
    }

    static public final String getDispatcherPatternID(final String kName) {
        return new StringBuilder("APPLETCTRL: ").append(kName).toString();
    }

    public String processLoadAppletOp(List<String> parameters) {
        try {
            final String kName = CommunicationParameterParser.resolveAppletName(parameters);
            final String kCtrlName = getDispatcherPatternID(kName);
            OPLauncherDispatcherPool pool = OPLauncherDispatcherPool.getActivePoolInstance(this);
            OPLauncherDispatcher dispatcher = pool.getInstance(kCtrlName);

            LOGGER.info("About to load an Applet from the Controller name: {}", kCtrlName);
            dispatcher.loadApplet(kName, parameters);

            return SUCCESS_RESPONSE;
        }
        catch (OPLauncherException e) {
            LOGGER.error("Failed to load the Applet", e);
            return FAILURE_RESPONSE;
        }
    }

    static public void main(String[] args) {
        OPLauncherController controller = new OPLauncherController();
        /*controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://javatester.org",
                "",
                "", "Java Tester Applet 1",
                "width=440;height=60;posx=1530;posy=420",
                "JavaVersionDisplayApplet.class"));
        controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://javatester.org",
                "",
                "", "Java Tester Applet 2",
                "width=440;height=60;posx=1530;posy=420",
                "JavaVersionDisplayApplet.class"));*/
        controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://www.w3.org/People/mimasa/test/object/java/",
                "applets/",
                "", "clock1",
                "width=81;height=120;posx=11.0000;posy=296.0000",
                "tz.class"));
    }
}
