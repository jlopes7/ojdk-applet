package org.oplauncher;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.File;

public class ConfigurationHelperTest {

    @Test
    public void test_getHomeDirectory() {
        File homeDirectory = ConfigurationHelper.getHomeDirectory();
        String homeDirectoryPath = homeDirectory.getAbsolutePath();

        assertNotNull(homeDirectory);
        assertEquals("C:/Temp/oplauncher", ConfigurationHelper.getHomeDirectory().getAbsolutePath());
    }
}
