#include "jvm_launcher.h"

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
errorcode_t jvm_launcher_init(const char *class_name) {
	int res;
	char exec_dir[BUFFER_SIZE];
	char policy_path[BUFFER_SIZE];
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

	memset(classpath_option, 0, BUFFER_SIZE);
	memset(policy_option, 0, BUFFER_SIZE);
	memset(applet_policy_filepath, 0, BUFFER_SIZE);
	memset(jvm_launcher, 0, sizeof(jvm_launcher_t));

	if (!classpath_option || !policy_option) {
		fprintf(stderr, "Memory allocation failed\n");
		return RC_ERR_FAILED_TO_LAUNCHJVM;
	}

	// Get executable directory and construct paths
	get_executable_directory(exec_dir, BUFFER_SIZE);
	snprintf(policy_path, BUFFER_SIZE, "%s/applet.policy", exec_dir);
	snprintf(exec_dir, BUFFER_SIZE, "%s/%s", exec_dir, getOpLauncherCommanderJarFileName());

	// Validate paths
#ifdef _WIN32
	if (_access(policy_path, F_OK) != 0) {
#else
	if ( access(policy_path, F_OK) != 0 ) {
#endif
		fprintf(stderr, "Policy file not found: %s\n", policy_path);
		free(classpath_option);
		free(policy_option);
		return RC_ERR_POLICY_FILE_MISSING;
	}

	// Construct JVM options
	snprintf(classpath_option, BUFFER_SIZE, "-Djava.class.path=%s", exec_dir);
	snprintf(policy_option, BUFFER_SIZE, "-Djava.security.policy=%s", policy_path);

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
		fprintf(stderr, "JNI_CreateJavaVM failed with error code %d\n", res);
		return RC_ERR_FAILED_TO_LAUNCHJVM;
	}

	return EXIT_SUCCESS;
}

void jvm_launcher_terminate() {
	if (PTR(jvm_launcher).env) {
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
		fprintf(stderr, "%s\n", errMsg);
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
 * Calls the "loadApplet" method of the "com.oplauncher.AppletClassLoader".
 *
 * @param class_name The fully qualified name of the class to load (e.g. "com.oplauncher.AppletClassLoader").
 * @param jar_file Path to the jar file that contains the class.
 * @param params List of parameters to pass to the applet (as a C array of strings).
 * @param param_count Number of parameters in the "params" array.
 *
 * @return int Status of the function call (0 for success, non-zero for failure).
 */
errorcode_t trigger_applet_execution(const char *class_name, const char *jar_file, char **params, int param_count) {
	const int kNumOfParameters = MAXARRAYSIZE;

	if (!jvm_launcher || !jvm_launcher->jvm || !jvm_launcher->env) {
		char *errMsg = "Error: JVM is not initialized.";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_FIND_APPCLLOADER);
		return RC_ERR_JVMNOTLOADED;
	}

	JNIEnv *env = jvm_launcher->env;
	JavaVM *jvm = jvm_launcher->jvm;

	// Attach the current thread to the JVM if needed
	int attachResult = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
	if (attachResult != JNI_OK) {
		char *errMsg = "Error: Failed to attach current thread to JVM.";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_FIND_APPCLLOADER);
		return RC_ERR_FAILEDJVM_ATTACH;
	}

	// Load the custom classloader
	jclass appletClassLoaderClass = PTR(PTR(jvm_launcher).env)->FindClass(PTR(jvm_launcher).env, CL_APPLET_CLASSLOADER);
	if (appletClassLoaderClass == NULL) {
		char *errMsg = "Failed to find the AppletClassLoader class";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_FIND_APPCLLOADER);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_FAILED_FIND_APPCLLOADER;
	}

	// Call the AppletClassLoader constructor
	jmethodID appletClassLoaderInstance = PTR(PTR(jvm_launcher).env)->GetMethodID(PTR(jvm_launcher).env, appletClassLoaderClass, "<init>", "()V");
	jobject classloader = PTR(PTR(jvm_launcher).env)->NewObject(PTR(jvm_launcher).env, appletClassLoaderClass, appletClassLoaderInstance);
	if (classloader == NULL) {
		char *errMsg = "Failed to create AppletClassLoader instance";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_CRE_APPCLLOADER);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_FAILED_CRE_APPCLLOADER;
	}

	// Find the method ID for "loadApplet"
	jmethodID loadAppletMethodID = PTR(PTR(jvm_launcher).env)->GetMethodID(env, appletClassLoaderClass, "loadApplet", "(Ljava/util/List;)Ljava/lang/String;");
	if (loadAppletMethodID == NULL) {
		char *errMsg = "Error: Method loadApplet not found.";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_CLLOADER_METHOD_NOTFOUND);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_CLLOADER_METHOD_NOTFOUND;
	}

	// Create a Java ArrayList and add all parameters to it
	jclass arrayListClass = PTR(PTR(jvm_launcher).env)->FindClass(env, "java/util/ArrayList");
	if (arrayListClass == NULL) {
		char *errMsg = "Error: Class ArrayList not found.";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_CLLOADER_METHOD_NOTFOUND);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_CLLOADER_METHOD_NOTFOUND;
	}
	jmethodID arrayListConstructor = PTR(PTR(jvm_launcher).env)->GetMethodID(env, arrayListClass, "<init>", "()V");
	jobject parameterList = PTR(PTR(jvm_launcher).env)->NewObject(env, arrayListClass, arrayListConstructor);
	if (parameterList == NULL) {
		char *errMsg = "Error: Could not create ArrayList instance.";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_CANNOT_CLASS_INSTANCE);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_CANNOT_CLASS_INSTANCE;
	}

	/// Add the parameters to the list
	jmethodID addMethod = PTR(PTR(jvm_launcher).env)->GetMethodID(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");
	if (addMethod == NULL) {
		char *errMsg = "Error: Method add not found in ArrayList.";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_CLLOADER_METHOD_NOTFOUND);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_CLLOADER_METHOD_NOTFOUND;
	}
	// iterate all and populate parameters
	for (int i = 0; i < param_count; i++) {
		jstring paramString = PTR(PTR(jvm_launcher).env)->NewStringUTF(env, params[i]);
		if (paramString == NULL) {
			char *errMsg = "Error: Could not create Java string for parameter";
			snprintf(errMsg, MAXPATHLEN, "Error: Could not create Java string for parameter %d", i);
			fprintf(stderr, "%s\n", errMsg);
			sendErrorMessage(errMsg, RC_ERR_CANNOT_CLASS_INSTANCE);

			PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
			return RC_ERR_CANNOT_CLASS_INSTANCE;
		}
		PTR(PTR(jvm_launcher).env)->CallBooleanMethod(env, parameterList, addMethod, paramString);
	}

	// Call the loadApplet method
	jstring resultString = (jstring)PTR(PTR(jvm_launcher).env)->CallObjectMethod(env, appletClassLoaderInstance, loadAppletMethodID, parameterList);
	if (resultString == NULL) {
		char *errMsg = "Error: Applet trigger returned null.";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_WRONG_RESULT_CLLOADER);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_WRONG_RESULT_CLLOADER;
	}

	// Convert Java string result to a C string
	const char *resultCStr = PTR(PTR(jvm_launcher).env)->GetStringUTFChars(env, resultString, 0);
	if (resultCStr == NULL) {
		char *errMsg = "Error: Could not convert result to C string.";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_TYPECONVERTING_FAILED);

		PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);
		return RC_ERR_TYPECONVERTING_FAILED;
	}

	printf("loadApplet result: %s\n", resultCStr);

	// Release resources
	PTR(PTR(jvm_launcher).env)->ReleaseStringUTFChars(env, resultString, resultCStr);
	PTR(PTR(jvm_launcher).jvm)->DetachCurrentThread(jvm);

	// Clean up local references
	/*PTR(PTR(jvm_launcher).env)->DeleteLocalRef(PTR(jvm_launcher).env, applet_class_name);
	PTR(PTR(jvm_launcher).env)->DeleteLocalRef(PTR(jvm_launcher).env, java_params);*/

	return EXIT_SUCCESS;
}

