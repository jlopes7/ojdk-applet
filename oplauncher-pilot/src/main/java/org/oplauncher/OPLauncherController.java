package org.oplauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
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
            e.printStackTrace(System.err);

            return FAILURE_RESPONSE;
        }
    }

    static public void main(String[] args) {
        OPLauncherController controller = new OPLauncherController();
        controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://javatester.org",
                "",
                "", "applet_oGiA6GdfoOSwz6Ca",
                "width=440;height=60;posx=1530;posy=420",
                "JavaVersionDisplayApplet.class"));
        /*controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://javatester.org",
                "",
                "", "Java Tester Applet 2",
                "width=440;height=60;posx=1530;posy=420",
                "JavaVersionDisplayApplet.class"));*/
        /*controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://www.w3.org/People/mimasa/test/object/java/",
                "applets/",
                "", "clock1",
                "width=81;height=120;posx=11.0000;posy=296.0000",
                "tz.class"));*/
        /*controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://download.java.net/media/jogl/demos/www/",
                "",
                "http://download.java.net/media/applet-launcher/applet-launcher.jar,\n" +
                        "               http://download.java.net/media/jogl/jsr-231-2.x-webstart/nativewindow.all.jar,\n" +
                        "               http://download.java.net/media/jogl/jsr-231-2.x-webstart/jogl.all.jar,\n" +
                        "               http://download.java.net/media/gluegen/webstart-2.x/gluegen-rt.jar,\n" +
                        "               http://download.java.net/media/jogl/jsr-231-2.x-demos-webstart/jogl-demos.jar",
                "Java_Net_Test",
                "width=600;height=400;posx=8.0000;posy=163.0000;codebase_lookup=false;subapplet.classname=demos.applets.GearsApplet;subapplet.displayname=JOGL Gears Applet;noddraw.check=true;jnlpNumExtensions=1;jnlpExtension1=http://download.java.net/media/jogl/jsr-231-2.x-webstart/jogl-core.jnlp",
                "org.jdesktop.applet.util.JNLPAppletLauncher"));*/
        /*controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://courses.worldcampus.psu.edu/public/diagnostics/",
                "",
                "tract10.zip", "Java Test",
                "width=71;height=71;posx=11.0000;posy=296.0000;cabbase=tract10.cab;corners=0,0|70,70|70,0||0,0|0,70|70,70;creep=2,20;delay=250;fgcolor=FF0000;debug=0",
                "Tractrix"));*/
        /*controller.processLoadAppletOp(Arrays.asList("load_applet",
                "http://www.pinlady.net/PluginDetect/JavaTester/",
                "../files/",
                "getJavaInfo.jar",
                "Java Plugin Verification",
                "width=350;height=80;posx=8.0000;posy=163.0000;scriptable=true;codebase_lookup=false;mayscript=true;plugindetect",
                "A"));*/
        /*controller.processLoadAppletOp(Arrays.asList("load_applet",
                "https://areo.info/mpf/",
                ".",
                "imagefinder.jar",
                "Pathfinder ImageFinder Apple",
                "width=630;height=550;posx=8.0000;posy=163.0000;case=lower;cd1=data/cdrom1;cd2=data/cdrom2;cd3=data/cdrom3",
                "ImageFinder.class"))*/;
    }
}
