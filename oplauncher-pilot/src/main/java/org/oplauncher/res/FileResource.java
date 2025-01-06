package org.oplauncher.res;

import java.io.File;

public class FileResource {

    protected FileResource(File file) {
        String filename = file.getName();

        _file = file;
        _fileHash = URLUtils.generateMD5FromFileName(filename);
    }

    ///  class properties
    private String _fileHash;
    private File _file;
}
