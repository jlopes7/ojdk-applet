#include "jvm_launcher.h"

#ifdef _WIN32
    #include <windows.h>
#else
    #include <unistd.h>
    #include <libgen.h> // For dirname
#endif

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
	strncpy (_applet_policy_filepath_, buffer, BUFFER_SIZE);
}

/**
 * Configures the JVM with the Applet policy and loads the Applet class
 */
void configure_jvm_and_load_applet(const char *class_name, const char *params) {
	char exec_dir[BUFFER_SIZE];
	char policy_path[BUFFER_SIZE];
	JavaVM *jvm;
	JNIEnv *env;
	JavaVMInitArgs vm_args;
	JavaVMOption options[2];

	// Get the directory of the executable
	get_executable_directory(exec_dir, BUFFER_SIZE);

	// Construct the path to the applet.policy file
	snprintf(policy_path, BUFFER_SIZE, "%s/applet.policy", exec_dir);

	// Set JVM options
	char classpath_option[BUFFER_SIZE];
	snprintf(classpath_option, BUFFER_SIZE, "-Djava.class.path=%s", exec_dir);
	options[0].optionString = classpath_option;

	char policy_option[BUFFER_SIZE];
	snprintf(policy_option, BUFFER_SIZE, "-Djava.security.policy=%s", policy_path);
	options[1].optionString = policy_option;

	// TODO: Hardcoded for version 8 now, needs to change in the future
	vm_args.version = JNI_VERSION_1_8;
	vm_args.nOptions = 2;
	vm_args.options = options;
	vm_args.ignoreUnrecognized = JNI_FALSE;

	// Create the JVM
	int res = JNI_CreateJavaVM(&jvm, (void **)&env, &vm_args);
	if (res < 0 || !env) {
		fprintf(stderr, "Failed to create JVM\n");
		return;
	}

	// Load the custom classloader
	jclass classloader_class = PTR(*env).FindClass(env, CL_APPLET_CLASSLOADER);
	if (classloader_class == NULL) {
		fprintf(stderr, "Failed to find the AppletClassLoader class\n");
		PTR(*jvm).DestroyJavaVM(jvm);
		return;
	}

	// Call the AppletClassLoader constructor
	jmethodID constructor = PTR(*env).GetMethodID(env, classloader_class, "<init>", "()V");
	jobject classloader = PTR(*env).NewObject(env, classloader_class, constructor);
	if (classloader == NULL) {
		fprintf(stderr, "Failed to create AppletClassLoader instance\n");
		PTR(*jvm).DestroyJavaVM(jvm);
		return;
	}

	// Load the applet class and pass parameters
	jmethodID load_applet_method = PTR(*env).GetMethodID(env, classloader_class, "loadApplet", "(Ljava/lang/String;Ljava/lang/String;)V");
	if (load_applet_method == NULL) {
		fprintf(stderr, "Failed to find loadApplet method\n");
		PTR(*jvm).DestroyJavaVM(jvm);
		return;
	}

	jstring applet_class_name = PTR(*env).NewStringUTF(env, class_name);
	jstring applet_params = PTR(*env).NewStringUTF(env, params);

	/// Call the method to trigger the execution of the JVM with the class loader for the applet
	PTR(*env).CallVoidMethod(env, classloader, load_applet_method, applet_class_name, applet_params);

	// Destroys the JVM
	PTR(*jvm).DestroyJavaVM(jvm);
}

