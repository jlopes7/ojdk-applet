package org.oplauncher.res;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.OPLauncherException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import static org.oplauncher.ErrorCode.*;

public class HttpSessionResourceRequest implements IResourceRequest<FileResource> {

    protected HttpSessionResourceRequest() {}

    public FileResource verifyCache(URL url) throws OPLauncherException {
        String cachePath = URLUtils.reverseUrlToPackageName(url);
        File cacheHome = new File(ConfigurationHelper.getCacheHomeDirectory(), cachePath);

        if (cacheHome.exists()) {
            String resName = URLUtils.getFileNameFromURL(url);
            String hashName = URLUtils.generateMD5FromFileName(resName);

            File cacheFile = new File(cacheHome, hashName);
            if (cacheFile.exists()) {
                return new FileResource(cacheFile);
            }
        }

        ///  Default behaviour
        return null;
    }

    @Override
    public <K, V>FileResource getResource(URL url, Map<K, V> cookieParameters) throws OPLauncherException {
        // Check the cache first
        FileResource cachedResource = verifyCache(url);
        if (cachedResource != null) return cachedResource;

        // Build the cookie header
        StringBuilder cookieHeader = new StringBuilder();
        for (Map.Entry<?, ?> entry : cookieParameters.entrySet()) {
            cookieHeader.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url.toString());
            httpGet.addHeader("Cookie", cookieHeader.toString());

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String cachePath = URLUtils.reverseUrlToPackageName(url);
                File cacheHome = new File(ConfigurationHelper.getCacheHomeDirectory(), cachePath);
                String resName = URLUtils.getResourceName(url, response);
                String hashName = URLUtils.generateMD5FromFileName(resName);

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
