package org.oplauncher.res;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.regex.Pattern.quote;
import static org.oplauncher.ErrorCode.*;

public class HttpSessionResourceRequest implements IResourceRequest<FileResource> {
    static private final Logger LOGGER = LogManager.getLogger(HttpSessionResourceRequest.class);
    static private final Lock LOCK = new ReentrantLock();

    static private URL _lastRegisteredURL;

    protected HttpSessionResourceRequest() {}

    public FileResource verifyCache(URL url) throws OPLauncherException {
        if (!ConfigurationHelper.isCacheActive()) return null; // Cache system needs to be active

        String cachePath = URLUtils.reverseUrlToPackageName(url);
        File cacheHome = new File(ConfigurationHelper.getCacheHomeDirectory(), cachePath);
        if ( LOGGER.isInfoEnabled() ) {
            LOGGER.info(String.format("Verifying cache for resource [%s] path defined as : [%s]", URLUtils.getFileNameFromURL(url), cacheHome.getAbsolutePath()));
        }

        if (cacheHome.exists()) {
            String resName = URLUtils.getFileNameFromURL(url);
            String hashName = Hex.encodeHexString(resName.getBytes(Charset.defaultCharset()));

            if ( LOGGER.isDebugEnabled() ) {
                LOGGER.debug("(verifyCache) Chache home exists: [{}]", cacheHome.getAbsolutePath());
                LOGGER.debug("(verifyCache) File name: [{}]", resName);
                LOGGER.debug("(verifyCache) Hash file name: [{}]", hashName);
            }

            for (File file : cacheHome.listFiles()) {
                String fileNameParts[] = file.getName().split(quote("_"));
                if (fileNameParts.length >= 2 && fileNameParts[1].equals(hashName)) {
                    return new FileResource(file);
                }
            }
        }

        ///  Default behaviour
        return null;
    }

    static public <T>T registerLastURL(URL url, T instance) {
        LOCK.lock();
        try {
            _lastRegisteredURL = url;

            return instance;
        }
        finally {
            LOCK.unlock();
        }
    }

    static public URL getLastRegisteredURL() {
        LOCK.lock();
        try {
            return _lastRegisteredURL;
        }
        finally {
            LOCK.unlock();
        }
    }

    @Override
    public <K, V>FileResource getResource(URL url, Map<K, V> cookieParameters) throws OPLauncherException {
        // Check the cache first
        FileResource cachedResource = registerLastURL(url, this).verifyCache(url);
        if (cachedResource != null) return cachedResource;

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Resource [{}] is not cached. Loading from HTTP", url);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("(getResource) Cache for the resource [{}] doesn't exist. It needs to be loaded", URLUtils.getFileNameFromURL(url));
        }

        // Build the cookie header
        StringBuilder cookieHeader = new StringBuilder();
        for (Map.Entry<?, ?> entry : cookieParameters.entrySet()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("(getResource) Adding cookie for the request: KEY[{}]-->VAL[{}]", entry.getKey(), entry.getValue());
            }
            cookieHeader.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
        }
        String cookieHeaderString = cookieHeader.toString().trim();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("(getResource) File cookie header: [{}]", cookieHeaderString);
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url.toString());
            // If there are any cookies available
            if (!cookieHeaderString.equals("")) {
                httpGet.addHeader("Cookie", cookieHeaderString);
            }

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String cachePath = URLUtils.reverseUrlToPackageName(url);
                File cacheHome = new File(ConfigurationHelper.getCacheHomeDirectory(), cachePath);
                String resName = URLUtils.getResourceName(url, response);
                //String fileName = URLUtils.getFileNameFromURL(url);
                String savedResName = ConfigurationHelper.getSavedResourceName(resName);
                String fileExt = FilenameUtils.getExtension(resName);
                String hashName = ConfigurationHelper.computeFileHashName(resName);
                //Character.valueOf(fileExt.charAt(0)).toString().concat("_").concat(hexFileName);
                //String hashName = URLUtils.generateMD5FromFileName(resName);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("(getResource) Resource details before caching:");
                    LOGGER.debug("(getResource) -> Cache path: [{}]", cachePath);
                    LOGGER.debug("(getResource) -> Cache home: [{}]", cacheHome);
                    LOGGER.debug("(getResource) -> Resource name: [{}]", resName);
                    LOGGER.debug("(getResource) -> Saved Resource name: [{}]", savedResName);
                    LOGGER.debug("(getResource) -> File extension: [{}]", fileExt);
                    LOGGER.debug("(getResource) -> File hash name: [{}]", hashName);
                }

                if (response.getCode() <= 304 && response.getCode() >= 200) { /// between [200, 305[ OK
                    InputStream inputStream = response.getEntity().getContent();

                    File cachedFile = new File(cacheHome, hashName);
                    FileUtils.copyInputStreamToFile(inputStream, cachedFile);

                    return new FileResource(cachedFile);
                }
                else {
                    throw new OPLauncherException(String.format("Failed to download resource(%s): HTTP code %d", resName, response.getCode()), FAILED_TO_DOWNLOAD_FILE);
                }
            }
        }
        catch (Exception e) {
            throw new OPLauncherException("Error downloading resource: " + e.getMessage(), e);
        }
    }
}
