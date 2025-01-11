package org.oplauncher;

import org.apache.commons.io.IOUtils;
import org.oplauncher.res.FileResource;
import org.oplauncher.res.HttpSessionResourceRequest;
import org.oplauncher.res.ResourceRequestFactory;
import static org.oplauncher.res.ResourceType.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;

public abstract class AbstractAppletClassLoader<T> extends ClassLoader implements IAppletClassLoader<T> {
    static private final Lock LOCK = new ReentrantLock();

    protected AbstractAppletClassLoader(ClassLoader parent) {
        super(parent);

        _fileMap = new LinkedHashMap<>();
    }

    @Override
    public String processAppletC2A(List<T> parameters) {
        LOCK.lock(); // only 1 op at a time!
        try {
            String opcode = CommunicationParameterHelper.resolveOpCode(parameters);

            ///  switch the process based on the opcode given as parameter
            switch (OpCode.parse(opcode)) {
                ///  Load all operations as needed
                case LOAD_APPLET: return processAppletC2A(parameters);
                default:
                    throw new OPLauncherException(String.format("Unknown or unsupported opcode: [%s]", opcode), ErrorCode.UNSUPPORTED_OPCODE);
            }
        }
        catch (Exception e) {
            e.printStackTrace(System.err);

            return e.getMessage();
        }
        finally {
            LOCK.unlock();
        }
    }

    protected List<FileResource> loadAppletFromURL(List<T> parameters) throws OPLauncherException {
        HttpSessionResourceRequest session = ResourceRequestFactory.getResourceRequest(HTTP_SESSION_REQUEST);
        CommunicationParameterHelper.AppletTagDef applTagDef = CommunicationParameterHelper.resolveAppletTagDef(parameters);
        String loadSourceBaseURLPath = CommunicationParameterHelper.resolveBaseUrl(parameters);
        String loadResApplType = CommunicationParameterHelper.resolveAppletTag(parameters, applTagDef);
        String loadSourceResURLPath  = CommunicationParameterHelper.resolveLoadResourceURL(parameters);
        try {
            StringBuilder sb = new StringBuilder(loadSourceBaseURLPath);
            if ( !loadSourceBaseURLPath.endsWith("/") ) sb.append('/');
            if ( applTagDef == CommunicationParameterHelper.AppletTagDef.CODEBASE ) sb.append(loadResApplType).append('/');

            URL loadSourceBaseURL = new URL(sb.append(loadSourceResURLPath).toString());

            FileResource res = session.verifyCache(loadSourceBaseURL);
            if (res == null) {
                res = session.getResource(loadSourceBaseURL, CommunicationParameterHelper.resolveCookies(parameters));
            }

            // Add the loaded file to the control map
            addFileResource(loadSourceResURLPath, res);

            if (res != null) {
                return new ArrayList<>(Arrays.asList(res));
            }

            throw new OPLauncherException(String.format("Could not load the resource from the given URL: %s", loadSourceBaseURLPath), ErrorCode.FAILED_TO_DOWNLOAD_FILE);
        }
        catch (MalformedURLException mex) {
            throw new OPLauncherException(mex, ErrorCode.MALFORMED_URL);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] klassBytes = loadClassData(name);
            return defineClass(name, klassBytes, 0, klassBytes.length);
        }
        catch (IOException e) {
            throw new ClassNotFoundException(String.format("Could not load class from Applet framework: [%s]", name), e);
        }
    }

    protected void loadJar(File jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath)) {
            jarFile.stream()
                    .parallel()
                    .forEach(jarEntry -> {
                        // TODO: In the future is also important to have code that read load the dep jars inside the jar
                        //       file since the MANIFEST can also contain libs to be added to the classpath. If that's the
                        //       this will not work for the current implementation
                        if (jarEntry.getName().endsWith(".class")) {
                            try {
                                InputStream is = jarFile.getInputStream(jarEntry);
                                byte[] classBytes = IOUtils.toByteArray(is);
                                String className = jarEntry.getName()
                                        .replace('/', '.')
                                        .replace(".class", "");

                                // Set the class into the classpath
                                defineClass(className, classBytes, 0, classBytes.length);
                            }
                            catch (IOException e) {
                                System.err.println(String.format("Failed to load Applet class from the Jar file(%s): [%s]", jarPath, jarEntry.getName()));
                            }
                        }
                    });
        }
    }

    private byte[] loadClassData(String name) throws IOException {
        String path = name.replace('.', '/') + ".class";
        FileResource res = getFileResource(path);
        if (res == null) {
            throw new IOException(String.format("Could not find a class resource associated with the following class: [%s]", path));
        }

        Path classPath = Paths.get(res.getTempClassPath().getPath(), path);
        return Files.readAllBytes(classPath);
    }

    private void addFileResource(final String klassName, FileResource resource) {
        LOCK.lock();
        try {
            _fileMap.put(klassName, resource);
        }
        finally {
            LOCK.unlock();
        }
    }
    protected FileResource getFileResource(final String klassName) {
        LOCK.lock();
        try {
            return _fileMap.get(klassName);
        }
        finally {
            LOCK.unlock();
        }
    }

    public abstract String processLoadAppletOp(List<T> parameters) throws OPLauncherException;

    // class properties
    private Map<String, FileResource> _fileMap;
}
