package org.oplauncher;

import org.oplauncher.res.FileResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.oplauncher.ErrorCode.*;

public class AppletClassLoader extends AbstractAppletClassLoader<String> {

    public AppletClassLoader() {
        super(AppletClassLoader.getSystemClassLoader());

        _appletController = new AppletController(this);
    }

    @Override
    public String processLoadAppletOp(List<String> parameters) throws OPLauncherException {
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
                    case UNKNOWN: {
                        throw new OPLauncherException(String.format("Unknown resource type: [%s]", resource.getFile().getName()), CLASSPATH_LOAD_ERROR);
                    }
                }
            } catch (IOException e) {
                throw new OPLauncherException(String.format("Failed to load the resource: [%s]",
                                              resource.getFile().getName()), e, CLASSPATH_LOAD_ERROR);
            }
        }

        return "";
    }

    static public void main(String[] args) {
        try {
            AppletClassLoader appletClassLoader = new AppletClassLoader();
            appletClassLoader.loadAppletFromURL(Arrays.asList("load_applet",
                    "https://www.cs.fsu.edu/~jtbauer/cis3931/tutorial/applet/overview",
                    "codebase=example",
                    "width=100;height=100",
                    "Simple.class"));
            Class<?> klass = appletClassLoader.findClass("Simple");
            System.out.printf("Class name: %s%n", klass.getCanonicalName());
            Arrays.asList(klass.getDeclaredMethods()).forEach(System.out::println);
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    // class properties
    private AppletController _appletController;
}
