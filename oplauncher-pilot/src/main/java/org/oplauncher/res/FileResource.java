package org.oplauncher.res;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.oplauncher.ConfigurationHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static java.util.regex.Pattern.quote;

public class FileResource {
    final int IDX_FILETYPE = 0;
    final int IDX_FILEDEF  = 1;

    public enum ResourceType {
        JAR_FILE,
        CLASS_FILE,
        UNKNOWN
    }

    protected FileResource(File maskedFile) {
        String filename = maskedFile.getName();

        _maskedFile = maskedFile;
        _fileHash = URLUtils.generateMD5FromFileName(filename);

        _tmpClassPathPath = createTempDirectory();

        // unmask the file
        unmaskFile();
    }
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

    public File getMaskedFile() {
        return _maskedFile;
    }

    private File getUnmaskedFile() {
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
        else return ResourceType.UNKNOWN;
    }

    ///  class properties
    private String _fileHash;
    private File _maskedFile;
    private File _file;
    private File _tmpClassPathPath;
}
