#include "jvm_launcher.h"
#include "ini_config.h"
#include "logging.h"

#ifdef _WIN32
    #include <windows.h>
#elif __APPLE__
	#include <mach-o/dyld.h>
    #include <unistd.h>
    #include <libgen.h> // For dirname
#endif

/**
 * Path to the security policy file
 */
char *applet_policy_filepath;

/**
 * Global variable used to save information about the JVM execution
 */
jvm_launcher_t *jvm_launcher;

/**
 * Initializes the JVM
 */
returncode_t jvm_launcher_init(const char *class_name) {
	int res;
	returncode_t rc;
	char *oplauncher_cp;
	char policy_path[MAX_PATH];
	char oplauncher_libhome[MAX_PATH];
	JavaVMInitArgs vm_args;
#if defined(_DEBUG)
	JavaVMOption options[3];
#else
	JavaVMOption options[2];
#endif

	char *classpath_option = malloc(BUFFER_SIZE);
	char *policy_option = malloc(BUFFER_SIZE);
	applet_policy_filepath = malloc(BUFFER_SIZE);
	jvm_launcher = malloc(sizeof(jvm_launcher_t));

	_MEMZERO(oplauncher_libhome, MAX_PATH);
	_MEMZERO(policy_path, MAX_PATH);
	_MEMZERO(classpath_option, BUFFER_SIZE);
	_MEMZERO(policy_option, BUFFER_SIZE);
	_MEMZERO(applet_policy_filepath, BUFFER_SIZE);
	_MEMZERO(jvm_launcher, sizeof(jvm_launcher_t));

	if (!classpath_option || !policy_option) {
		logmsg(LOGGING_ERROR, "Failed to allocate memory for classpath options");
		return RC_ERR_FAILED_TO_LAUNCHJVM;
	}

	// Prepare the configuration files
	read_ini_value(INI_SECTION_JVM, INI_SECTION_JVM_PROP_LIBPATH, oplauncher_libhome, MAX_PATH);
	read_ini_value(INI_SECTION_JVM, INI_SECTION_JVM_PROP_POLICYFILE, policy_path, MAX_PATH);
	rc = format_get_classpath(oplauncher_libhome, &oplauncher_cp, MID_BUFFER_SIZE);
	if (rc != EXIT_SUCCESS) {
		logmsg(LOGGING_ERROR, "Failed to load the classpath");
		free(classpath_option);
		free(policy_option);
		free(oplauncher_cp);
		return rc;
	}

	// Validate paths
#ifdef _WIN32
	if (_access(policy_path, F_OK) != 0) {
#else
	if ( access(policy_path, F_OK) != 0 ) {
#endif
		logmsg(LOGGING_ERROR, "Policy file not found: %s\n", policy_path);
		free(classpath_option);
		free(policy_option);
		return RC_ERR_POLICY_FILE_MISSING;
	}

	logmsg(LOGGING_NORMAL, "-> Initializing JVM with Class-Path: %s", oplauncher_cp);
	logmsg(LOGGING_NORMAL, "-> Initializing the JVM with the Security Policy: %s", policy_path);

	// Construct JVM options
	snprintf(classpath_option, BUFFER_SIZE, "-Djava.class.path=%s", oplauncher_cp);
	snprintf(policy_option, BUFFER_SIZE, "-Djava.security.policy=%s", policy_path);
	free(oplauncher_cp);

	options[0].optionString = classpath_option;
	options[1].optionString = policy_option;
#if defined(_DEBUG)
	options[2].optionString = "-verbose:jni"; // Debug JNI
#endif

	JNI_GetDefaultJavaVMInitArgs(&vm_args);
	// Initialize JVM arguments
	vm_args.version = JNI_VERSION_1_8;
#if defined(_DEBUG)
	vm_args.nOptions = 3;
#else
	vm_args.nOptions = 2;
#endif
	vm_args.options = options;
	vm_args.ignoreUnrecognized = JNI_FALSE;

	// Create JVM
	res = JNI_CreateJavaVM(&jvm_launcher->jvm, (void **)&jvm_launcher->env, &vm_args);
	free(classpath_option);
	free(policy_option);

	if (res < 0) {
		logmsg(LOGGING_ERROR, "JNI_CreateJavaVM failed with error code %d\n", res);
		return RC_ERR_FAILED_TO_LAUNCHJVM;
	}

	logmsg(LOGGING_NORMAL, "Successfully initialized JVM: %p", PTR(jvm_launcher).jvm);

	return EXIT_SUCCESS;
}

void jvm_launcher_terminate() {
	if (PTR(jvm_launcher).env) {
		if (PTR(jvm_launcher).applet_classloader) {
			logmsg(LOGGING_NORMAL, "Terminating the JVM: %p", PTR(jvm_launcher).jvm);
			PTR(PTR(jvm_launcher).env)->DeleteLocalRef(PTR(jvm_launcher).env, PTR(jvm_launcher).applet_classloader);
		}

		// Destroys the JVM
		PTR(PTR(jvm_launcher).jvm)->DestroyJavaVM(PTR(jvm_launcher).jvm);

		free(jvm_launcher);
	}
	else {
		sendErrorMessage("The JVM launcher could not be found: java/lang/Error", RC_ERR_NO_JVMPROCESS_AVAIL);
	}
}

/**
 * Retrieves the directory of the executable
 */
void get_executable_directory(char *buffer, size_t size) {
#ifdef _WIN32
	GetModuleFileNameA(NULL, buffer, size); // Get full path of the executable
	char *last_backslash = strrchr(buffer, '\\');
	if (last_backslash) {
		*last_backslash = '\0';
	}
#elif __APPLE__
	uint32_t bufsize = (uint32_t)size;
	if (_NSGetExecutablePath(buffer, &bufsize) != 0) {
		char errMsg[BUFFER_SIZE];
		snprintf(errMsg, BUFFER_SIZE, "Buffer size too small; required size: %u\n", bufsize);
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_BUFFER_SZ_TOOSMALL);
	}
	char *dir = dirname(buffer);  // Get directory part of the path
	strncpy(buffer, dir, size);
#else
	ssize_t len = readlink("/proc/self/exe", buffer, size - 1);
	if (len != -1) {
		buffer[len] = '\0';
		dirname(buffer); // Get the directory name
	} 
	else {
		perror("readlink");
		exit(EXIT_FAILURE);
	}
#endif
	/// Just save the policy file
	strncpy (applet_policy_filepath, buffer, BUFFER_SIZE);
}

/**
 * Calls the "processLoadAppletOp" method of the "com.oplauncher.AppletClassLoader".
 *
 * @param class_name The fully qualified name of the class to load (e.g. "com.oplauncher.AppletClassLoader").
 * @param jar_file Path to the jar file that contains the class.
 * @param params List of parameters to pass to the applet (as a C array of strings).
 * @param param_count Number of parameters in the "params" array.
 *
 * @return int Status of the function call (0 for success, non-zero for failure).
 */
returncode_t trigger_applet_execution(const char *class_name, char **params, int param_count) {
	const int kNumOfParameters = MAXARRAYSIZE;

	logmsg(LOGGING_NORMAL, "Trigging the Applet execution in the newly created JVM...");
	if (!jvm_launcher || !jvm_launcher->jvm || !jvm_launcher->env) {
		char *errMsg = "Error: JVM is not initialized.";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_FIND_APPCLLOADER);
		return RC_ERR_JVMNOTLOADED;
	}

	JNIEnv *env = jvm_launcher->env;
	JavaVM *jvm = jvm_launcher->jvm;

	logmsg(LOGGING_NORMAL, "Attaching the JVM to the current Thread...");
	// Attach the current thread to the JVM if needed
	int attachResult = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
	if (attachResult != JNI_OK) {
		char *errMsg = "Error: Failed to attach current thread to JVM.";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_FIND_APPCLLOADER);
		return RC_ERR_FAILEDJVM_ATTACH;
	}

	logmsg(LOGGING_NORMAL, "Loading the Applet loader class: %s", CL_APPLET_CLASSLOADER);
	// Load the custom classloader
	jclass appletClassLoaderClass = PTR(env)->FindClass(env, CL_APPLET_CLASSLOADER);
	if (appletClassLoaderClass == NULL) {
		char *errMsg = "Failed to find the AppletClassLoader class";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_FIND_APPCLLOADER);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_FAILED_FIND_APPCLLOADER;
	}

	// Call the AppletClassLoader constructor
	jmethodID appletClassLoaderInstance = PTR(env)->GetMethodID(env, appletClassLoaderClass, "<init>", "()V");
	jobject classloader = PTR(env)->NewObject(env, appletClassLoaderClass, appletClassLoaderInstance);
	if (classloader == NULL) {
		char *errMsg = "Failed to create AppletClassLoader instance";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_CRE_APPCLLOADER);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_FAILED_CRE_APPCLLOADER;
	}

	// Find the method ID for "processLoadAppletOp"
	jmethodID loadAppletMethodID = PTR(env)->GetMethodID(env, appletClassLoaderClass,
												    CL_APPLET_CLASSLOADER_METHOD,
														 CL_APPLET_CLASSLOADER_PARAMTYPES);

	if (loadAppletMethodID == NULL) {
		char *errMsg = "Error: Method processLoadAppletOp not found.";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_CLLOADER_METHOD_NOTFOUND);

		PTR(jvm)->DetachCurrentThread(jvm);
		return RC_ERR_CLLOADER_METHOD_NOTFOUND;
	}

	// Create a Java ArrayList and add all parameters to it
	jclass arrayListClass = PTR(env)->FindClass(env, "java/util/ArrayList");
	if (arrayListClass == NULL) {
		char *errMsg = "Error: Class ArrayList not found.";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_CLLOADER_METHOD_NOTFOUND);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_CLLOADER_METHOD_NOTFOUND;
	}
	jmethodID arrayListConstructor = PTR(env)->GetMethodID(env, arrayListClass, "<init>", "()V");
	jobject parameterList = PTR(env)->NewObject(env, arrayListClass, arrayListConstructor);
	if (parameterList == NULL) {
		char *errMsg = "Error: Could not create ArrayList instance.";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_CANNOT_CLASS_INSTANCE);

		PTR(jvm)->DetachCurrentThread(jvm);
		return RC_ERR_CANNOT_CLASS_INSTANCE;
	}

	/// Add the parameters to the list
	jmethodID addMethod = PTR(env)->GetMethodID(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");
	if (addMethod == NULL) {
		char *errMsg = "Error: Method add not found in ArrayList.";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_CLLOADER_METHOD_NOTFOUND);

		PTR(jvm)->DetachCurrentThread(jvm);
		return RC_ERR_CLLOADER_METHOD_NOTFOUND;
	}

	logmsg(LOGGING_NORMAL, "Creating the parameters to run the Applet loader:");
	// iterate all and populate parameters
	for (int i = 0; i < param_count; i++) {
		char *currentParam = params[i];
		logmsg(LOGGING_NORMAL, "+-> Param[%d]: %s", (i + 1), currentParam);
		jstring paramString = PTR(env)->NewStringUTF(env, currentParam);
		if (paramString == NULL) {
			char *errMsg = "Error: Could not create Java string for parameter";
			snprintf(errMsg, MAXPATHLEN, "Error: Could not create Java string for parameter %d", i);
			logmsg(LOGGING_ERROR, errMsg);
			sendErrorMessage(errMsg, RC_ERR_CANNOT_CLASS_INSTANCE);

			PTR(jvm)->DetachCurrentThread(jvm);
			return RC_ERR_CANNOT_CLASS_INSTANCE;
		}
		PTR(PTR(jvm_launcher).env)->CallBooleanMethod(env, parameterList, addMethod, paramString);
	}

	logmsg(LOGGING_NORMAL, "Calling the applet loader method: %s <-> ParamTypes: %s", CL_APPLET_CLASSLOADER_METHOD, CL_APPLET_CLASSLOADER_PARAMTYPES);
	// Call the processLoadAppletOp method
	jstring resultString = (jstring)PTR(env)->CallObjectMethod(env, classloader, loadAppletMethodID, parameterList);
	if (resultString == NULL) {
		char *errMsg = "Error: Applet trigger returned null.";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_WRONG_RESULT_CLLOADER);

		PTR(jvm)->DetachCurrentThread(jvm);
		return RC_ERR_WRONG_RESULT_CLLOADER;
	}

	// Convert Java string result to a C string
	const char *resultCStr = PTR(env)->GetStringUTFChars(env, resultString, 0);
	if (resultCStr == NULL) {
		char *errMsg = "Error: Could not convert result to C string.";
		logmsg(LOGGING_ERROR, errMsg);
		sendErrorMessage(errMsg, RC_ERR_TYPECONVERTING_FAILED);

		// Release resources
		PTR(jvm)->DetachCurrentThread(jvm);
		PTR(env)->ReleaseStringUTFChars(env, resultString, resultCStr);
		PTR(jvm)->DetachCurrentThread(jvm);
		// Clean up local references
		PTR(env)->DeleteLocalRef(env, classloader);
		PTR(env)->DeleteLocalRef(env, parameterList);

		return RC_ERR_TYPECONVERTING_FAILED;
	}

	logmsg(LOGGING_NORMAL, "JVM Applet loader response: (%s)", resultCStr);
	logmsg(LOGGING_NORMAL, "Saving the Applet loader instance for future OP executions: %p (JVM: %p)", classloader, jvm);

	// Saves the classloader
	PTR(jvm_launcher).applet_classloader = &classloader;

	// Release resources
	PTR(env)->ReleaseStringUTFChars(env, resultString, resultCStr);
	PTR(jvm)->DetachCurrentThread(jvm);
	//PTR(env)->DeleteLocalRef(env, parameterList);

	return EXIT_SUCCESS;
}

