package org.oplauncher;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

public class AppletClassLoaderTest {
    AppletClassLoader _appletClassLoader = new AppletClassLoader();

    @BeforeTest
    public void setUp() throws Exception {

    }

    public void test_appletClassLoading() throws Exception {

    }

    @Test
    public void test_floatPointParameters() throws Exception {
        _appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                "https://www.cs.fsu.edu/~jtbauer/cis3931/tutorial/applet/overview",
                "codebase=example",
                "", "",
                "width=500.100;height=100.5643;posx=100.33;posy=100.33;",
                "Simple.class"));
    }

    @Test
    public void test_loadApplet() throws Exception {
        String result;
        /*result = appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                "https://www.cs.fsu.edu/~jtbauer/cis3931/tutorial/applet/overview",
                "codebase=example",
                "", "",
                "width=500;height=100",
                "Simple.class"));*/
        /*result = appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                "https://javatester.org",
                "",
                "", "Java Tester Applet",
                "width=440;height=60;posx=100;posy=100",
                "JavaVersionDisplayApplet.class"));*/
        /*result = appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                "https://www.math.uh.edu/mathonline/JavaTest",
                "",
                "HappyApplet.jar", "TestApplet",
                "width=100;height=100",
                "javadetectapplet.JavaDetectApplet.class"));*/
        /*result = appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                "https://www.caff.de/applettest",
                "",
                "dxf-applet-signed.jar", "DXF Applet",
                "width=1024;height=400",
                "de.caff.dxf.applet.DxfApplet"));*/
        /*result = _appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                "https://imagej.net/ij",
                "",
                "ij.jar", "ImageJ Applet",
                "width=300;height=100",
                "ij.ImageJApplet"));*/
        result = _appletClassLoader.processLoadAppletOp(Arrays.asList("load_applet",
                "https://web.mit.edu/java_v1.0.2/www/tutorial/java/threads/",
                "codebase=example",
                "", "MiT Time/Date Applet",
                "width=60;height=10",
                "Clock.class"));


        assertNotNull(result, "The result cannot be null");

        _appletClassLoader.disposeApplets();

        Class<?> klass = _appletClassLoader.findClass("Clock");
        System.out.printf("Class name: %s%n", klass.getCanonicalName());
        Arrays.asList(klass.getDeclaredMethods()).forEach(System.out::println);
        assertNotNull(klass, "The class needs to be loaded");
    }
}
