package org.oplauncher.res;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;
import org.oplauncher.ErrorCode;
import org.oplauncher.OPLauncherException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.regex.Pattern.quote;

public class FileResource {
    static private final Logger LOGGER = LogManager.getLogger(FileResource.class);

    final int IDX_FILETYPE = 0;
    final int IDX_FILEDEF  = 1;

    public enum ResourceType {
        JAR_FILE,
        CLASS_FILE,
        ZIP_FILE,
        UNKNOWN
    }

    public FileResource(File maskedFile) {
        String filename = maskedFile.getName();

        setMaskedFile(maskedFile)
            .setFileHash(filename)
            .setTempClassPath(createTempDirectory())
            // unmask the file
            .unmaskFile();
    }
    private FileResource() {}
;
    private File createTempDirectory() {
        try {
            File tempDir = FileUtils.getTempDirectory();

            // Append a custom subdirectory to isolate your temporary files
            File customTempDir = new File(tempDir, ConfigurationHelper.genRandomString(16));

            // Ensure the directory exists
            if (customTempDir.exists()) {
                FileUtils.cleanDirectory(customTempDir); // Clean if already exists
            }
            else {
                FileUtils.forceMkdir(customTempDir); // Create the directory
            }

            /// Save the current temporary path
            ConfigurationHelper.saveFileResource(_fileHash, this);

            return customTempDir;
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to create temporary directory", e);
        }
    }

    private String getPackagePart(String className) {
        if (className == null || className.trim().equals("")) return "";

        className = className.replace(".class", "")
                             .replace("\\", "/")
                             .replace(".", "/");

        int idx = className.lastIndexOf('/');
        return idx > 0 ? className.substring(0, idx) : "";
    }
    private String getClassPart(String className) {
        if (className == null || className.trim().equals("")) return "";

        className = className.replace(".class", "")
                             .replace(".", "/");

        int idx = Math.max(className.lastIndexOf('/'), className.lastIndexOf('\\'));
        return idx > 0 ? className.substring(idx +1).concat(".class") : className.concat(".class");
    }
    protected FileResource unmaskFile() {
        ResourceType type = getResourceType();
        String filePart;

        String parts[] = getMaskedFile().getName().split(quote("_"));
        if (parts.length > IDX_FILEDEF) {
            filePart = parts[IDX_FILEDEF];
        }
        else {
            throw new RuntimeException(String.format("The cached file does not contain any class/file definitions: [%s]", getMaskedFile().getName()));
        }

        try {
            final int IDX_CLASSNAME = 0;
            String filePartPattern = new String(Hex.decodeHex(filePart), Charset.defaultCharset());
            switch (type) {
                case ZIP_FILE: {
                    LOGGER.info("Unmasking zip archive file...");
                    expandZIPContents();
                    break;
                }
                case JAR_FILE: {
                    _file = new File(getTempClassPath(), filePartPattern);
                    FileUtils.copyFile(getMaskedFile(), getUnmaskedFile());
                    break;
                }
                case CLASS_FILE: {
                    String fileParts[]  = filePartPattern.split(quote("!" /*reserved for future impl*/));
                    String klassPkgPath = getPackagePart(fileParts[IDX_CLASSNAME]);
                    String klassName    = getClassPart(fileParts[IDX_CLASSNAME]);

                    File klassPkgDir = klassPkgPath != null && !klassPkgPath.trim().equals("")
                                            ? new File(getTempClassPath(), klassPkgPath)
                                            : getTempClassPath();
                    _file = new File(klassPkgDir, klassName);

                    if (!klassPkgDir.exists()) FileUtils.forceMkdir(klassPkgDir);
                    FileUtils.copyFile(getMaskedFile(), getUnmaskedFile());
                    break;
                }
                default:
                    throw new RuntimeException("Unsupported resource type: " + type);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to unmask file", e);
        }

        return this;
    }

    static public FileResource ofResource(File file) {
        FileResource res = new FileResource();
        String filename = file.getName();

        if (file.isDirectory()) {
            throw new OPLauncherException("Cannot create a resource from a directory: " + file.getAbsolutePath(), ErrorCode.FAILED_TO_LOAD_RESOURCE);
        }

        res.setUnmaskedFile(file)
            .setFileHash(filename)
            .maskFile(file)
            .setTempClassPath(file.getParentFile());

        return res;
    }

    private FileResource maskFile(File file) {
        String hashedFileName = getFileHash();
        String cachePath = URLUtils.reverseUrlToPackageName(HttpSessionResourceRequest.getLastRegisteredURL());
        File cacheHome = new File(ConfigurationHelper.getCacheHomeDirectory(), cachePath);
        File hashFile = new File(cacheHome, hashedFileName);

        try {
            FileUtils.copyFile(file, hashFile);
            _maskedFile = hashFile;

            return this;
        }
        catch (IOException e) {
            throw new OPLauncherException("Failed to mask file: " + hashedFileName, ErrorCode.FAILED_TO_LOAD_RESOURCE);
        }
    }
    private FileResource setUnmaskedFile(File file) {
        _file = file;
        return this;
    }
    private FileResource setFileHash(String filename) {
        _fileHash = URLUtils.generateMD5FromFileName(filename);
        return this;
    }
    private FileResource setTempClassPath(File tempClassPath) {
        _tmpClassPathPath = tempClassPath;
        return this;
    }
    private FileResource setMaskedFile(File file) {
        _maskedFile = file;
        return this;
    }

    private void expandZIPContents() throws IOException {
        Path outputDir = getTempClassPath().toPath();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Expanding zip contents to [{}]", outputDir);
        }

        // Ensure output directory exists
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Open ZIP file
        try (ZipFile zip = new ZipFile(getMaskedFile())) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path entryPath = outputDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                }
                else {
                    // Ensure parent directories exist for the resource
                    Files.createDirectories(entryPath.getParent());

                    // Extract file to directory
                    try (InputStream inputStream = zip.getInputStream(entry)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("(expandZIPContents) +-> Expanding ZIP content to: [{}]", entryPath.toFile().getAbsolutePath());
                        }
                        Files.copy(inputStream, entryPath, REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    public File getMaskedFile() {
        return _maskedFile;
    }

    public File getUnmaskedFile() {
        return _file;
    }
    public File getFile() {
        return unmaskFile().getUnmaskedFile();
    }

    public String getFileHash() {
        return _fileHash;
    }

    public File getTempClassPath() {
        return _tmpClassPathPath;
    }

    public ResourceType getResourceType() {
        String parts[] = _maskedFile.getName().split(quote("_"));

        if (parts[IDX_FILETYPE].equalsIgnoreCase("J")) return ResourceType.JAR_FILE;
        else if (parts[IDX_FILETYPE].equalsIgnoreCase("C")) return ResourceType.CLASS_FILE;
        else if (parts[IDX_FILETYPE].equalsIgnoreCase("Z")) return ResourceType.ZIP_FILE;
        else {
            // No resolution found
            return ResourceType.UNKNOWN;
        }
    }

    ///  class properties
    private String _fileHash;
    private File _maskedFile;
    private File _file;
    private File _tmpClassPathPath;
}
