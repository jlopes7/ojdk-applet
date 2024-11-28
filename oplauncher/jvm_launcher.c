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
int jvm_launcher_init(const char *class_name) {
	char exec_dir[BUFFER_SIZE];
	char policy_path[BUFFER_SIZE];
	JavaVMInitArgs vm_args;
	JavaVMOption options[2];

	char classpath_option[BUFFER_SIZE];
	char policy_option[BUFFER_SIZE];

	applet_policy_filepath = malloc(BUFFER_SIZE);
	jvm_launcher = malloc(sizeof(jvm_launcher_t));

	// Sets all the memory pointers to '\0'
	memset(applet_policy_filepath, 0, BUFFER_SIZE);
	memset(jvm_launcher, 0, sizeof(jvm_launcher_t));

	// Get the directory of the executable
	get_executable_directory(exec_dir, BUFFER_SIZE);

	// Construct the path to the applet.policy file
	snprintf(policy_path, BUFFER_SIZE, "%s/applet.policy", exec_dir);

	// Set JVM options
	snprintf(classpath_option, BUFFER_SIZE, "-Djava.class.path=%s", exec_dir);
	snprintf(policy_option, BUFFER_SIZE, "-Djava.security.policy=%s", policy_path);
	options[0].optionString = classpath_option;
	options[1].optionString = policy_option;


	// TODO: Hardcoded for version 8 now, needs to change in the future
	vm_args.version = JNI_VERSION_1_8;
	vm_args.nOptions = 2;
	vm_args.options = options;
	vm_args.ignoreUnrecognized = JNI_FALSE;

	// Create the JVM
	int res = JNI_CreateJavaVM(&jvm_launcher->jvm, (void **)&jvm_launcher->env, &vm_args);
	if (res < 0 || !PTR(jvm_launcher).env) {
		fprintf(stderr, "Failed to create JVM\n");
		return RC_ERR_FAILED_TO_LAUNCHJVM;
	}

	return EXIT_SUCCESS;
}

void jvm_launcher_terminate() {
	if (PTR(jvm_launcher).env) {
		// Destroys the JVM
		PTR(PTR(jvm_launcher).jvm)->DestroyJavaVM(PTR(jvm_launcher).jvm);
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
 * Trigger the applet execution code
 */
void trigger_applet_execution(const char *class_name, data_tuplet_t *params) {
	const int kNumOfParameters = MAXARRAYSIZE;

	// Load the custom classloader
	jclass classloader_class = PTR(PTR(jvm_launcher).env)->FindClass(PTR(jvm_launcher).env, CL_APPLET_CLASSLOADER);
	if (classloader_class == NULL) {
		char *errMsg = "Failed to find the AppletClassLoader class";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_FIND_APPCLLOADER);
		return;
	}

	// Call the AppletClassLoader constructor
	jmethodID constructor = PTR(PTR(jvm_launcher).env)->GetMethodID(PTR(jvm_launcher).env, classloader_class, "<init>", "()V");
	jobject classloader = PTR(PTR(jvm_launcher).env)->NewObject(PTR(jvm_launcher).env, classloader_class, constructor);
	if (classloader == NULL) {
		char *errMsg = "Failed to create AppletClassLoader instance";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_CRE_APPCLLOADER);
		return;
	}

	// Prepare the parameters as a Java String[]
	jclass string_class = PTR(PTR(jvm_launcher).env)->FindClass(PTR(jvm_launcher).env, "java/lang/String");
	jobjectArray java_params = PTR(PTR(jvm_launcher).env)->NewObjectArray(PTR(jvm_launcher).env, kNumOfParameters * 2, string_class, NULL);
	if (java_params == NULL) {
		char *errMsg = "Failed to create parameter array";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_CREATE_PARAM_ARRAY);
		return;
	}

	// Fill the array with name-value pairs
	for (int i = 0; i < kNumOfParameters; i++) {
		jstring key = PTR(PTR(jvm_launcher).env)->NewStringUTF(PTR(jvm_launcher).env, params[i].name);
		jstring value = PTR(PTR(jvm_launcher).env)->NewStringUTF(PTR(jvm_launcher).env, params[i].value);
		PTR(PTR(jvm_launcher).env)->SetObjectArrayElement(PTR(jvm_launcher).env, java_params, i * 2, key);
		PTR(PTR(jvm_launcher).env)->SetObjectArrayElement(PTR(jvm_launcher).env, java_params, i * 2 + 1, value);
		PTR(PTR(jvm_launcher).env)->DeleteLocalRef(PTR(jvm_launcher).env, key);
		PTR(PTR(jvm_launcher).env)->DeleteLocalRef(PTR(jvm_launcher).env, value);
	}

	// Load the applet class and pass parameters
	// Load the applet class and pass parameters
	jmethodID load_applet_method = PTR(PTR(jvm_launcher).env)->GetMethodID(
		PTR(jvm_launcher).env,
		classloader_class,
		"loadApplet",
		"(Ljava/lang/String;[Ljava/lang/String;)V"
	);
	if (load_applet_method == NULL) {
		char *errMsg = "Failed to find loadApplet method";
		fprintf(stderr, "%s\n", errMsg);
		sendErrorMessage(errMsg, RC_ERR_FAILED_FIND_APPCLLOADMETHOD);
		return;
	}

	jstring applet_class_name = PTR(PTR(jvm_launcher).env)->NewStringUTF(PTR(jvm_launcher).env, class_name);
	/// Call the method to trigger the execution of the JVM with the class loader for the applet
	PTR(PTR(jvm_launcher).env)->CallVoidMethod(
		PTR(jvm_launcher).env,
		classloader,
		load_applet_method,
		applet_class_name,
		java_params
	);

	// Clean up local references
	PTR(PTR(jvm_launcher).env)->DeleteLocalRef(PTR(jvm_launcher).env, applet_class_name);
	PTR(PTR(jvm_launcher).env)->DeleteLocalRef(PTR(jvm_launcher).env, java_params);
}

