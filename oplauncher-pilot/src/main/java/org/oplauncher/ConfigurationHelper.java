package org.oplauncher;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import static org.oplauncher.IConstants.*;

public class ConfigurationHelper {
    static public final Properties CONFIG = new Properties();
    static {
        try {
            FileInputStream fis;
            File configFile = new File(getHomeDirectory(), CONFIG_FILENAME);
            if (!configFile.exists()) {
                InputStream is = ConfigurationHelper.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME);
                FileUtils.copyInputStreamToFile(is, configFile);
            }
            fis = new FileInputStream(configFile);

            try {
                CONFIG.load(fis);
            }
            finally {
                fis.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    static public final boolean isCacheActive() {
        return Boolean.parseBoolean(CONFIG.getProperty(CONFIG_PROP_CACHE_ACTIVEFLAG, "true"));
    }

    static private boolean configPropAvailable(String prop) {
        return CONFIG.getProperty(prop) != null;
    }

    static public final File getHomeDirectory() {
        final String kCacheHome = ".oplauncher";
        File homeDirectory;
        if ( configPropAvailable(CONFIG_PROP_CONFIG_ROOT) ) {
            homeDirectory = new File(CONFIG.getProperty(CONFIG_PROP_CONFIG_ROOT));
        }
        else homeDirectory = new File(System.getProperty("user.home"), kCacheHome);

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
        File cacheHomeDirectory;
        if ( configPropAvailable(CONFIG_PROP_CACHE_FILEPATH) ) {
            cacheHomeDirectory = new File(CONFIG.getProperty(CONFIG_PROP_CACHE_FILEPATH));
        }
        else cacheHomeDirectory = new File(getHomeDirectory(), kCacheHome);

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
