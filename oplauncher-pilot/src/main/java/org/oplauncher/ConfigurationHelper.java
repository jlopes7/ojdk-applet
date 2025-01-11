package org.oplauncher;

import org.apache.commons.io.FileUtils;
import org.oplauncher.res.FileResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.oplauncher.IConstants.*;

public class ConfigurationHelper {
    static private final Lock LOCK = new ReentrantLock();
    static private final Map<String, FileResource> TEMP_CPS = new LinkedHashMap<>();
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

    static public final String getSavedResourceName(String defval) {
        if ( configPropAvailable(CONFIG_PROP_RESOURCENAME) ) {
            return CONFIG.getProperty(CONFIG_PROP_RESOURCENAME);
        }
        else if ( defval != null ) {
            return defval;
        }
        else {
            throw new RuntimeException("It was not possible to find the resource name. No resource name found or saved in the heap");
        }
    }

    static public final void saveFileResource(String hash, FileResource dir) {
        LOCK.lock();
        try {
            TEMP_CPS.put(hash, dir);
        }
        finally {
            LOCK.unlock();
        }
    }
    static public final FileResource getSavedFileResource(String hash) {
        LOCK.lock();
        try {
            if (hash == null) return null;

            return TEMP_CPS.get(hash);
        }
        finally {
            LOCK.unlock();
        }
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

    static public String genRandomString(int size) {
        final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random RANDOM = new SecureRandom();
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            int randomIndex = RANDOM.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(randomIndex));
        }
        return sb.toString();
    }
}
