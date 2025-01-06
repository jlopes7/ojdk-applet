package org.oplauncher;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class ConfigurationHelper {

    static public final File getHomeDirectory() {
        final String kCacheHome = ".oplauncher";
        File homeDirectory = new File(System.getProperty("user.home"), kCacheHome);

        if (!homeDirectory.exists()) {
            try {
                FileUtils.forceMkdir(homeDirectory);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return homeDirectory;
    }

    static public final File getCacheHomeDirectory() {
        final String kCacheHome = "cache";
        File cacheHomeDirectory = new File(getHomeDirectory(), kCacheHome);

        if (!cacheHomeDirectory.exists()) {
            try {
                FileUtils.forceMkdir(cacheHomeDirectory);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return cacheHomeDirectory;
    }
}
