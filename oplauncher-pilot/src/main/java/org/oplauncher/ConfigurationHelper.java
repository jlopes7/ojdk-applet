package org.oplauncher;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.oplauncher.res.FileResource;
import org.oplauncher.runtime.AppletContextType;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
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
            // 1st init step: Initialize/copy the oplauncher configuration file
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

            // 2nd init step: Check to see if the oplauncher LOG4J exists in the home folder
            // TODO: Deactivated for now, using the config details
            /*File log4jfile = new File(getHomeDirectory(), CONFIG_LOG_FILENAME);
            if (!log4jfile.exists()) {
                InputStream is = ConfigurationHelper.class.getClassLoader().getResourceAsStream(CONFIG_LOG_FILENAME);
                FileUtils.copyInputStreamToFile(is, log4jfile);
            }*/
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    static public final void intializeLog() throws OPLauncherException {
        if (ConfigurationHelper.isLoggingActive()) {
            File log4jfile = new File(getHomeDirectory(), CONFIG_LOG_FILENAME);
            if (!log4jfile.exists()) {
                manualInitializeLog();
            }
            else {
                xmlIntializeLog();
            }
        }
    }

    static private void xmlIntializeLog() throws OPLauncherException {
        String logPattern = ConfigurationHelper.getLogFileRotatePattern();
        File logFile = ConfigurationHelper.getLogConfigurationFile();
        String logLevel = ConfigurationHelper.getLogLevel();

        // Define the log system parameters
        System.setProperty("log.filename", logFile.getAbsolutePath());
        System.setProperty("log.filenamePattern", logPattern);
        System.setProperty("log.level", logLevel);

        // Reload the log4j config
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        // Set the configuration file
        context.setConfigLocation(logFile.toURI());
        context.reconfigure();
    }
    static private void manualInitializeLog() throws OPLauncherException {
        String logFilePattern = getLogFileRotatePattern();
        File logFile = getLogConfigurationFile();
        String logLevel = getLogLevel();
        String logPattern = getLogPattern();

        LoggerContext context = (LoggerContext) LogManager.getContext(false);

        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.add(builder.newAppender("FileAppender", "RollingFile")
                .addAttribute("fileName", logFile.getAbsolutePath())
                .addAttribute("filePattern", logFilePattern)
                .add(builder.newLayout("PatternLayout").addAttribute("pattern", logPattern))
                .addComponent(builder.newComponent("Policies")
                        .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", getLogFileRotationSize()))))
                .add(builder.newLogger("DynamicLogger", Level.toLevel(logLevel))
                        .add(builder.newAppenderRef("FileAppender"))
                        .addAttribute("additivity", false))
                .add(builder.newRootLogger(Level.toLevel(logLevel))
                        .add(builder.newAppenderRef("FileAppender")));

        context.start(builder.build());
    }

    static public final boolean isCacheActive() {
        return Boolean.parseBoolean(CONFIG.getProperty(CONFIG_PROP_CACHE_ACTIVEFLAG, "true"));
    }

    static private boolean configPropAvailable(String prop) {
        return CONFIG.getProperty(prop) != null;
    }

    static public ArchiveClassLoaderType getArchiveClassLoaderType() {
        return ArchiveClassLoaderType.parse(CONFIG.getProperty(CONFIG_PROP_ARCHIEVE_CLASSLOADER));
    }

    static public final String getLogPattern() {
        return CONFIG.getProperty(CONFIG_PROP_LOGPATTERN, "%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n").trim();
    }

    static public final String computeFileHashName(String resName) {
        String fileExt = FilenameUtils.getExtension(resName);
        String hexFileName = Hex.encodeHexString(resName.getBytes(Charset.defaultCharset()));
        String hashName = Character.valueOf(fileExt.charAt(0)).toString().concat("_").concat(hexFileName);

        return hashName;
    }

    static public final boolean isLoggingActive() {
        return Boolean.parseBoolean(CONFIG.getProperty(CONFIG_PROP_LOGACTIVE, "true").trim());
    }
    static public final File getLogConfigurationFile() {
        File logFile;
        if ( configPropAvailable(CONFIG_PROP_LOGFILENAME) ) {
            logFile = new File(CONFIG.getProperty(CONFIG_PROP_LOGFILENAME));
            File logDir = logFile.getParentFile();

            if (!logDir.exists()) {
                try {
                    FileUtils.forceMkdir(logDir);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            logFile = new File(getLogHomeDirectory(), CONFIG_LOG_DEF_RUNTIME_FILENAME);
        }

        return logFile;
    }
    static public final String getLogFileRotatePattern() {
        if ( configPropAvailable(CONFIG_PROP_LOGFILEPATTERN) ) {
            return CONFIG.getProperty(CONFIG_PROP_LOGFILEPATTERN).trim();
        }
        else {
            return CONFIG_LOG_DEF_RUNTIME_PATNAME;
        }
    }

    static public final String getLogLevel() {
        return CONFIG.getProperty(CONFIG_PROP_LOGLEVEL, "INFO").trim();
    }

    static public final String getLogFileRotationSize() {
        return CONFIG.getProperty(CONFIG_PROP_LOGRATATION_SZ, "2MB").trim();
    }

    static public final AppletContextType getAppletContextType() {
        String appletContextType = CONFIG.getProperty(CONFIG_PROP_APPLETCONTEXT, "default").trim();

        return AppletContextType.parse(appletContextType);
    }

    static private File _getHomeDirectory(File base, final String propname, final String structDir) {
        File homeDirectory;
        if ( configPropAvailable(propname) ) {
            homeDirectory = new File(CONFIG.getProperty(propname));
        }
        else homeDirectory = new File(base, structDir);

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

    static public final File getLogHomeDirectory() {
        return _getHomeDirectory(getHomeDirectory(), CONFIG_PROP_LOGDIR, "logs");
    }

    static public final File getHomeDirectory() {
        File userHomeDirectory = new File(System.getProperty("user.home"));
        return _getHomeDirectory(userHomeDirectory, CONFIG_PROP_CONFIG_ROOT, ".oplauncher");
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
        return _getHomeDirectory(getHomeDirectory(), CONFIG_PROP_CACHE_FILEPATH, "cache");
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
