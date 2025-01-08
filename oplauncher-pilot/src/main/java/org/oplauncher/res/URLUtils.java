package org.oplauncher.res;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.oplauncher.ErrorCode;
import org.oplauncher.OPLauncherException;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.oplauncher.ErrorCode.*;

public class URLUtils {
    public static final String reverseUrlToPackageName(URL url) {
        String host = url.getHost();

        // Split the host into parts
        String[] parts = host.split("\\.");

        // Exclude "www" if present
        int startIndex = (parts[0].equalsIgnoreCase("www")) ? 1 : 0;

        // Reverse the parts and join with a dot
        StringBuilder reversedPackage = new StringBuilder();
        for (int i = parts.length - 1; i >= startIndex; i--) {
            reversedPackage.append(parts[i]);
            if (i > startIndex) {
                reversedPackage.append(".");
            }
        }

        return reversedPackage.toString();
    }

    public static String getFileNameFromURL(URL url) {
        // Get the path component of the URL
        String path = url.getPath();

        // Check if the path contains a file name
        if (path != null && !path.isEmpty()) {
            // Extract everything after the last slash
            return path.substring(path.lastIndexOf('/') + 1);
        }

        // Should never get here!
        throw new OPLauncherException(String.format("Invalid URL: [%s]",url), MALFORMED_URL);
    }

    public static String generateMD5FromFileName(String filename) {
        try {
            String filenameExt = FilenameUtils.getExtension(filename);
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Compute the hash
            byte[] hashBytes = md.digest(filename.getBytes());

            // Convert to hexadecimal
            return String.format("%s_%s", filenameExt.toLowerCase(), Hex.encodeHexString(hashBytes));
        }
        catch (NoSuchAlgorithmException e) {
            throw new OPLauncherException(String.format("Error processing the hashing algorithm for the file: [%s]",filename), e, SECURITY_ERROR);
        }
    }

    public static final String getResourceName(URL url, CloseableHttpResponse response) {
        // Try to get the file name from the Content-Disposition header
        Header contentDisposition = response.getFirstHeader("Content-Disposition");
        if (contentDisposition != null) {
            String dispositionValue = contentDisposition.getValue();
            if (dispositionValue != null && dispositionValue.contains("filename=")) {
                // Extract file name from Content-Disposition
                String[] parts = dispositionValue.split("filename=");
                if (parts.length > 1) {
                    return parts[1].replace("\"", "").trim();
                }
            }
        }

        // Fall back to extracting the file name from the URL
        String path = url.getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }
}
