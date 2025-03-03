package org.oplauncher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.res.FileResource;
import org.oplauncher.res.HttpSessionResourceRequest;
import org.oplauncher.res.ResourceRequestFactory;

import static java.util.Optional.ofNullable;
import static org.oplauncher.CommunicationParameterParser.IDX_RESURL;
import static org.oplauncher.res.ResourceType.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public abstract class AbstractAppletClassLoader<T> extends ClassLoader implements IAppletClassLoader<T> {
    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(AbstractAppletClassLoader.class);
    /*static {
        ///  Logger initialization
        ConfigurationHelper.intializeLog();
    }*/

    protected AbstractAppletClassLoader(ClassLoader parent) {
        super(parent);

        _fileMap = new LinkedHashMap<>();
        _loadedClassMap = new LinkedHashMap<>();
        _currentParameters = new ArrayList<>();
    }

    @Override
    public String processAppletC2A(List<T> parameters) {
        LOCK.lock();
        try {
            String opcode = CommunicationParameterParser.resolveOpCode(parameters);

            ///  switch the process based on the opcode given as parameter
            switch (OpCode.parse(opcode)) {
                ///  Load all operations as needed
                case LOAD_APPLET: {
                    return processLoadAppletOp(parameters);
                }
                case UNLOAD_APPLET: {
                    return processUnloadAppletOp(parameters);
                }
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

    protected boolean isClassLoaded(String name) {
        return _loadedClassMap.containsKey(name);
    }

    protected Class<?> getLoadedClass(String name) {
        return _loadedClassMap.get(name);
    }

    protected List<FileResource> loadAppletFromURL(List<T> parameters) throws OPLauncherException {
        LOCK.lock();
        try {
            boolean loadArchives = false;
            HttpSessionResourceRequest session = ResourceRequestFactory.getResourceRequest(HTTP_SESSION_REQUEST);

            List<FileResource> resources = new ArrayList<>();

            // Save the currently used parameters... may have to load additional classes..., ugly, but functional!
            saveCurrentUsedParameters(parameters);

            CommunicationParameterParser.AppletTagDef applTagDef = CommunicationParameterParser.resolveAppletTagDef(parameters);
            String loadSourceBaseURLPath = CommunicationParameterParser.resolveBaseUrl(parameters);
            String loadResApplType = CommunicationParameterParser.resolveAppletTag(parameters, applTagDef);
            String loadSourceResURLPath = CommunicationParameterParser.resolveLoadResourceURL(parameters);
            String loadJavaClass = CommunicationParameterParser.resolveAppletClass(parameters);
            List<String> archives = CommunicationParameterParser.resolveArchives(parameters);
            Map<String,String> cookies = CommunicationParameterParser.resolveCookies(parameters);
            String appletName = CommunicationParameterParser.resolveAppletName(parameters);

            ///  Load all applet parameters and save it to the base classloader to be access by the Applet Context
            ///  initialization at a later time
            _appletParameters = CommunicationParameterParser.resolveAppletParameters(appletName, parameters);

            // Load all the deps archives (JARS)
            for (String archive : archives) {
                loadArchives = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("(loadAppletFromURL) Loading applet archive [{}] from base URL: {}", archive, loadSourceBaseURLPath);
                }
                resources.add(_loadAppletFromURL(applTagDef, appletName, loadSourceBaseURLPath, loadResApplType,
                                                 archive, cookies, session, false));
            }

            _appletClassName = loadJavaClass;
            _appletName      = appletName;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Applet named {} to load the class: [{}]", _appletName, _appletClassName);
            }
            ///  We only load the Applet class from the URL if no archieves are present in the request
            if ( !loadArchives ) {
                resources.add(_loadAppletFromURL(applTagDef, appletName, loadSourceBaseURLPath, loadResApplType,
                                                 loadSourceResURLPath, cookies, session, true));
            }

            return resources;
        }
        finally {
            LOCK.unlock();
        }
    }
    protected FileResource loadAppletFromURL(URL loadSourceBaseURL, HttpSessionResourceRequest session, Map<String,String> cookies) {
        FileResource res = session.verifyCache(loadSourceBaseURL);
        if (res == null) {
            res = session.getResource(loadSourceBaseURL, cookies);
        }

        return res;
    }

    private FileResource _loadAppletFromURL(CommunicationParameterParser.AppletTagDef applTagDef,
                                                  String appletName,
                                                  String loadSourceBaseURLPath,
                                                  String loadResApplType,
                                                  String loadSourceResURLPath,
                                                  Map<String,String> cookies,
                                                  HttpSessionResourceRequest session, boolean loadApplet) throws OPLauncherException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Loading applet Base URL: [{}]", loadSourceBaseURLPath);
            LOGGER.debug("Loading applet resource URL: [{}]", loadSourceResURLPath);
            LOGGER.debug("Loading applet resource applet type: [{}]", loadResApplType);
        }

        try {
            String codeBase;
            String prePattUrlPath = ofNullable(loadSourceResURLPath).map(s->s.trim().toLowerCase()).orElse("");
            if ( !prePattUrlPath.startsWith("http://") && !prePattUrlPath.startsWith("https://") ) {
                StringBuilder sb = new StringBuilder(loadSourceBaseURLPath);
                if (!loadSourceBaseURLPath.endsWith("/")) sb.append('/');
                if (applTagDef == CommunicationParameterParser.AppletTagDef.CODEBASE)
                    sb.append(loadResApplType).append('/');
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Applet tag definition: [{}}]", applTagDef.name());
                }

                codeBase = sb.toString();
            }
            else codeBase = "";

            URL loadSourceBaseURL = new URL(codeBase.concat(loadSourceResURLPath));
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Applet name: [{}]", appletName);
                LOGGER.info("Loading applet from URL [{}]", loadSourceBaseURL);
            }
            if (loadApplet) {
                _appletDocumentBase = loadSourceBaseURL;
                _appletCodeBase = new URL(codeBase);
            }

            /// TODO: Missing the implementation of the JARs Applet parameter to download multiple jar deps
            FileResource res = loadAppletFromURL(loadSourceBaseURL, session, cookies);

            if (res != null) {
                // Add the loaded file to the control map
                addFileResource(loadSourceResURLPath, res);

                return res;
            }
            else {
                throw new OPLauncherException(String.format("Could not load the resource from the given URL: %s", loadSourceBaseURLPath), ErrorCode.FAILED_TO_DOWNLOAD_FILE);
            }
        }
        catch (MalformedURLException mex) {
            throw new OPLauncherException(mex, ErrorCode.MALFORMED_URL);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        LOCK.lock();
        try {
            if (isClassLoaded(name)) {
                return getLoadedClass(name);
            }
            else {
                try {
                    ArchiveClassLoaderType type = ConfigurationHelper.getArchiveClassLoaderType();
                    FileResource res = getResourceByName(name);
                    Class<?> klass = null;

                    if (res != null && res.getResourceType() == FileResource.ResourceType.CLASS_FILE) {
                        type = ArchiveClassLoaderType.SIMPLE;
                    }

                    switch (type) {
                        case SIMPLE:
                            byte[] klassBytes = loadClassData(name);
                            klass = defineClass(name, klassBytes, 0, klassBytes.length);
                            break;
                        case URL:
                            ClassLoader cl = getURLClassLoader(name);
                            if (cl != null) {
                                klass = cl.loadClass(name);
                            }
                            else {
                                LOGGER.warn("Could not find class [{}] using the conventinal ClassLoader, trying to search in the system loader", name);
                                try {
                                    klass = super.findClass(name);
                                }
                                catch (ClassNotFoundException cnfe) {
                                    LOGGER.warn("Could not find class [{}] using the system ClassLoader", name, cnfe);
                                    LOGGER.warn("Trying to load the class from the remote URL");

                                    //if (!name.endsWith(".class")) name = name.concat(".class");

                                    setCurrentParameterValue(IDX_RESURL, (T) name);
                                    List<FileResource> resources = loadAppletFromURL(getCurrentlyUsedParameters());
                                    for (FileResource resource : resources) {
                                        klass = this.findClass(name);
                                    }
                                }
                            }
                            break;
                        default: {
                            throw new RuntimeException(String.format("Could not load class with the class loader type [%s]. Class: %s", type.name(), name));
                        }
                    }

                    _loadedClassMap.put(name, klass);
                    return klass;
                } catch (IOException e) {
                    throw new ClassNotFoundException(String.format("Could not load class from Applet framework: [%s]", name), e);
                }
            }
        }
        finally {
            LOCK.unlock();
        }
    }

    private void _loadJarSimple(final File jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath)) {
            jarFile.stream()
                    .parallel()
                    .forEach(jarEntry -> {
                        // TODO: In the future is also important to have code that read load the dep jars inside the jar
                        //       file since the MANIFEST can also contain libs to be added to the classpath. If that's the
                        //       this will not work for the current implementation
                        try {
                            InputStream is = jarFile.getInputStream(jarEntry);
                            byte[] classBytes = IOUtils.toByteArray(is);
                            String entryName = jarEntry.getName();
                            String className = entryName.replace('/', '.')
                                    .replace(".class", "");

                            // Save the resource to the temp class path
                            File resFile = new File(jarPath.getParentFile(), entryName);
                            if (!resFile.getParentFile().exists()) {
                                FileUtils.forceMkdir(resFile.getParentFile());
                            }
                            FileUtils.copyInputStreamToFile(new ByteArrayInputStream(classBytes), resFile);

                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("Loaded resource [{}] from Jar [{}]", entryName, jarFile.getName());
                                LOGGER.info("+-> Cached to: [{}]", resFile.getAbsolutePath());
                            }

                            /// Define a new class if necessary
                            /*if (jarEntry.getName().endsWith(".class")) {
                                // Set the class into the classpath
                                defineClass(className, classBytes, 0, classBytes.length);
                            }*/
                        }
                        catch (IOException e) {
                            System.err.println(String.format("Failed to load Applet class from the Jar file(%s): [%s]", jarPath, jarEntry.getName()));
                        }
                    });
        }

        // STEP 2: Now that all the class were placed in the temp folder, lets load all the classes to the classpath
        File tempFolder = jarPath.getParentFile();
        Arrays.stream(tempFolder.listFiles())
                .parallel()
                .filter(f -> f.getName().endsWith(".class"))
                .forEach(f -> {
                    String rootPath = tempFolder.getAbsolutePath();
                    int startIndex = rootPath.length() +1;
                    String className = f.getAbsolutePath()
                            .substring(startIndex)
                            .replace("/", ".")
                            .replace(".class", "");

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Loaded class [{}] from: [{}]", className, tempFolder.getAbsolutePath());
                    }

                    try {
                        byte clzb[] = FileUtils.readFileToByteArray(f);
                        defineClass(className, clzb, 0, clzb.length);
                    }
                    catch (Throwable t) {
                        LOGGER.error(String.format("Failed to load class [%s]", className), t);
                    }
                });
    }
    private void _initURLClzLoader(final File file) throws IOException {
        if ( _urlClassLoader == null ) {
            _urlClassLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
        }
    }
    protected void loadJar(final File jarPath) throws IOException {
        ArchiveClassLoaderType type = ConfigurationHelper.getArchiveClassLoaderType();
        switch (type) {
            case SIMPLE: {
                _loadJarSimple(jarPath);
                break;
            }
            case URL: {
                _initURLClzLoader(jarPath);
                break;
            }
            default: {
                throw new OPLauncherException(String.format("Unsupported archive classloader type: [%s]", type.name()), ErrorCode.UNSUPPORTED_ARCHIVE_CLZLOADER);
            }
        }

    }

    protected URLClassLoader getURLClassLoader(final String name) {
        if (_urlClassLoader == null) {
            try {
                FileResource res = getResourceByName(name);
                if (res != null) {
                    _urlClassLoader = new URLClassLoader(new URL[]{res.getUnmaskedFile().toURI().toURL()}, this);
                }
                else {
                    LOGGER.warn("Could not find a class resource associated with the given the name: {}", name);
                    return null;
                }
            }
            catch (MalformedURLException mex) {
                throw new OPLauncherException(mex, ErrorCode.MALFORMED_URL);
            }
        }

        return _urlClassLoader;
    }

    private String getResourcePath(final String name) {
        return name.replace('.', '/') + ".class";
    }
    protected FileResource getResourceByName(final String name) {
        return getFileResource(getResourcePath(name));
    }

    private byte[] loadClassData(final String name) throws IOException {
        String path = getResourcePath(name);
        FileResource res = getFileResource(path);
        if (res == null) {
            throw new IOException(String.format("Could not find a class resource associated with the following class: [%s]", path));
        }

        if (res.getResourceType() == FileResource.ResourceType.CLASS_FILE) {
            Path classPath = Paths.get(res.getTempClassPath().getPath(), path);
            return Files.readAllBytes(classPath);
        }
        // Jar files
        else {
            ArchiveClassLoaderType type = ConfigurationHelper.getArchiveClassLoaderType();
            File klassFile = Arrays.stream(res.getTempClassPath().listFiles())
                                    .filter(f -> f.getName().equals(path))
                                    .findFirst().orElse(null);
            if (klassFile != null) {
                return Files.readAllBytes(klassFile.toPath());
            }

            LOGGER.warn("Could not find a class resource associated with the following class: {}", name);

            return new byte[0];
        }
    }

    private List<String> getJarArchives() {
        return _fileMap.keySet().stream().filter(name -> name.endsWith(".jar")).collect(Collectors.toList());
    }

    public URL getAppletDocumentBase() {
        return _appletDocumentBase;
    }
    public URL getAppletCodeBase() {
        return _appletCodeBase;
    }
    public String getAppletClassName() {
        return _appletClassName;
    }
    public String getAppletName() {
        return _appletName;
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
    protected boolean isFileInJar(File jarPath, String klassName) {
        if (!jarPath.exists() || !jarPath.isFile()) {
            throw new RuntimeException("The specified JAR file does not exist: " + jarPath.getAbsolutePath());
        }

        try (JarFile jarFile = new JarFile(jarPath)) {
            JarEntry entry = jarFile.getJarEntry(klassName);
            return entry != null;
        }
        catch (IOException e) {
            throw new RuntimeException("Error while accessing the JAR file: " + jarPath.getName(), e);
        }
    }
    protected FileResource getFileResource(final String klassName) {
        LOCK.lock();
        try {
            FileResource res = _fileMap.get(klassName);
            if (res == null) { // maybe it's a class within a Jar file
                List<String> jars = getJarArchives();
                for (String jar : jars) {
                    FileResource jarRes = _fileMap.get(jar);
                    if ( jarRes == null ) {
                        throw new RuntimeException("Could not find any resource associated with the jar: " + jar);
                    }

                    if ( isFileInJar(jarRes.getUnmaskedFile(), klassName) ) {
                        return jarRes;
                    }
                }
            }

            if (LOGGER.isDebugEnabled() && res != null) {
                LOGGER.debug("(getFileResource) Found resource: {}", klassName);
                LOGGER.debug("(getFileResource) Temp resource path: {}", res.getTempClassPath().getPath());
                LOGGER.debug("(getFileResource) Temp file masked path: {}", res.getMaskedFile().getAbsolutePath());
            }

            return res;
        }
        finally {
            LOCK.unlock();
        }
    }

    protected List<T> getCurrentlyUsedParameters() {
        return _currentParameters;
    }
    protected List<T> saveCurrentUsedParameters(List<T> param) {
        if ( param != null ) {
            if ( param.size() == getCurrentlyUsedParameters().size() ) {
                for (int i = 0; i < param.size(); i++) {
                    T givenParam = param.get(i);
                    T savedParam = getCurrentlyUsedParameters().get(i);

                    if (!givenParam.equals(savedParam)) {
                        getCurrentlyUsedParameters().set(i, givenParam);
                    }
                }
            }
            else {
                getCurrentlyUsedParameters().clear();
                getCurrentlyUsedParameters().addAll(param);
            }
        }

        return param;
    }
    private void setCurrentParameterValue(int codeIndex, T value) {
        if (codeIndex < _currentParameters.size()) {
            _currentParameters.set(codeIndex, value);
        }
    }

    public AppletParameters getAppletParameters() {
        return _appletParameters;
    }

    public abstract String processLoadAppletOp(List<T> parameters) throws OPLauncherException;
    public abstract String processUnloadAppletOp(List<T> parameters) throws OPLauncherException;

    // class properties
    private Map<String, FileResource> _fileMap;
    private Map<String, Class<?>> _loadedClassMap;
    private AppletParameters _appletParameters;

    private URL _appletDocumentBase;
    private URL _appletCodeBase;
    private String _appletClassName;
    private String _appletName;

    private URLClassLoader _urlClassLoader;

    private List<T> _currentParameters;
}
