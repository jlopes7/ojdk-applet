package org.oplauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.load.SplashScreen;
import org.oplauncher.res.FileResource;
import org.oplauncher.runtime.AppletController;
import org.oplauncher.runtime.AppletControllerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.oplauncher.ErrorCode.*;
import static org.oplauncher.IConstants.EXIT_SUCCESS;
import static org.oplauncher.IConstants.SUCCESS_RESPONSE;

public class AppletClassLoader extends AbstractAppletClassLoader<String> {
    static private final Logger LOGGER = LogManager.getLogger(AppletClassLoader.class);

    public AppletClassLoader(OPLauncherDispatcher dispatcherRef) {
        super(AppletClassLoader.getSystemClassLoader());

        // Saves the applet reference
        _dispatcherRef = dispatcherRef;

        // Trigger the splash
        SplashScreen.instance.showSplash();

        // Creates the controller
        _appletController = AppletControllerFactory.createAppletController(this);
    }

    @Override
    public String processLoadAppletOp(List<String> parameters) throws OPLauncherException {
        try {
            // Step 0: Name this loader instance with the Applet name
            _instanceName = CommunicationParameterParser.resolveAppletName(parameters);

            // Step 1: Load the applet source code and cache it (if enabled)
            List<FileResource> loadedResources = loadAppletFromURL(parameters);

            // Lets load all resources
            for (FileResource resource : loadedResources) {
                try {
                    switch (resource.getResourceType()) {
                        /// Jar file to be loaded
                        case JAR_FILE: {
                            loadJar(resource.getFile());
                            break;
                        }
                        /// Zip content to be loaded
                        case ZIP_FILE: {
                            loadZip(resource.getTempClassPath()); // Load all the classes from the temp folder
                            break;
                        }
                        case UNKNOWN: {
                            throw new OPLauncherException(String.format("Unknown resource type: [%s]", resource.getFile().getName()), CLASSPATH_LOAD_ERROR);
                        }
                    }
                }
                catch (IOException e) {
                    throw new OPLauncherException(String.format("Failed to load the resource: [%s]",
                            resource.getFile().getName()), e, CLASSPATH_LOAD_ERROR);
                }
            }

            /// Load the Applet class !!!
            getAppletController().executeOP(OpCode.LOAD_APPLET);

            return SUCCESS_RESPONSE;
        }
        catch (Throwable t) {
            LOGGER.error("An error occurred while loading the applet", t);
            SplashScreen.instance.closeSplash();

            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, "An error occurred: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
            );
            throw t;
        }
    }

    @Override
    public String processUnloadAppletOp(List<String> parameters) throws OPLauncherException {
        if ( getAppletController()!=null && getAppletController().getAppletFrame().isVisible() ) {
            getAppletController().disposeAllApplets();
        }

        return SUCCESS_RESPONSE;
    }

    public AppletClassLoader disposeApplets() {
        getAppletController().disposeAllApplets();
        // Stops the OP server
        /*if ( getAppletController().getOPServer().isOPServerRunning() ) {
            getAppletController().getOPServer().stopOPServer();
        }*/
        System.exit(EXIT_SUCCESS);
        return this;
    }

    public String successResponse() {
        return SUCCESS_RESPONSE;
    }

    static public void main(String[] args) {
        try {
            AppletClassLoader appletClassLoader = new AppletClassLoader(null);
            /*appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                    "https://www.cs.fsu.edu/~jtbauer/cis3931/tutorial/applet/overview",
                    "codebase=example",
                    "", "",
                    "width=500;height=100",
                    "Simple.class"));*/
            appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                    "https://javatester.org",
                    "",
                    "", "Java Tester Applet",
                    "width=440;height=60;posx=1530;posy=420",
                    "JavaVersionDisplayApplet.class"));
            /*appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                    "https://www.math.uh.edu/mathonline/JavaTest",
                    "",
                    "HappyApplet.jar", "TestApplet",
                    "width=100;height=100",
                    "javadetectapplet.JavaDetectApplet.class"));*/
            /*appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                    "https://www.caff.de/applettest",
                    "",
                    "dxf-applet-signed.jar", "DXF Applet",
                    "width=1024;height=400",
                    "de.caff.dxf.applet.DxfApplet"));*/
            /*appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                    "https://imagej.net/ij",
                    "",
                    "ij.jar", "ImageJ Applet",
                    "width=300;height=100",
                    "ij.ImageJApplet"));*/
            /*appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                    "https://web.mit.edu/java_v1.0.2/www/tutorial/java/threads/",
                    "codebase=example",
                    "", "MiT Time/Date Applet",
                    "width=60;height=10",
                    "Clock.class"));*/
            /*appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet", //NOT WORKING!
                    "https://www.physics.purdue.edu/class/applets/phe/",
                    "codebase=../jarph",
                    "Kepler1.jar", "Box Modes Simulation",
                    "language=English;width=700;height=360",
                    "Kepler1.class"));*/
            /*appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                    "https://www.physics.purdue.edu/class/applets/phe/",
                    "codebase=../jarph",
                    "MFStab.jar", "Magnetic field of a bar magnet",
                    "language=English;width=600;height=420",
                    "MFStab.class"));*/
            /*Class<?> klass = appletClassLoader.findClass("Simple");
            System.out.printf("Class name: %s%n", klass.getCanonicalName());
            Arrays.asList(klass.getDeclaredMethods()).forEach(System.out::println);*/
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public AppletController getAppletController() {
        return _appletController;
    }

    public OPLauncherDispatcher getOPLauncherDispatcher() {
        return _dispatcherRef;
    }

    public String getInstanceName() {
        return _instanceName;
    }

    // class properties
    private AppletController _appletController;
    private OPLauncherDispatcher _dispatcherRef;
    private String _instanceName;
}
