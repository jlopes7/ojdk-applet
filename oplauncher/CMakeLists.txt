cmake_minimum_required(VERSION 3.15)
project(oplauncher)

# Set the C standard
set(CMAKE_C_STANDARD 11)

# If no build type is provided, lets go DEBUG IT!
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Debug)
endif()

# Detect platform
if(WIN32)
    message(STATUS "Building on Windows")
    set(OS_PLATFORM "windows")

    if(MSVC)
        add_compile_options(/wd4244 /wd4267 /wd4996) # Disable specific warnings
        # Use /Zi for debugging symbols in MSVC
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /Zi")
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /Zi")
    endif()

    # Add cJSON library with an isolated build directory
    set(CJSON_BINARY_DIR "${CMAKE_BINARY_DIR}/deps/cJSON_build")
    #add_subdirectory(${CMAKE_SOURCE_DIR}/deps ${CJSON_BINARY_DIR})
elseif(APPLE)
    message(STATUS "Building on macOS")
    set(OS_PLATFORM "macos")
else()
    message(FATAL_ERROR "Unsupported platform")
endif()

# Add cJSON library (ensure it's built first)
add_subdirectory(deps)

# Locate JNI headers and libraries (cross-platform)
#find_package(JNI REQUIRED)
#include_directories(${JNI_INCLUDE_DIRS})

# Check if the build type is Debug
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    if(MSVC)
        set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} /Zi")
        set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /Zi")
    else()
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g")
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g")
        set(CMAKE_LINKER_FLAGS "${CMAKE_LINKER_FLAGS} -fsanitize=address")
    endif()
endif()

# build without any optimizations
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g -O0")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -O0")

# Define OPLAUNCHER_JAVA_HOME
if(NOT DEFINED OPLAUNCHER_JAVA_HOME)
    message(FATAL_ERROR "Please define OPLAUNCHER_JAVA_HOME to point to your Java installation. Example: -DOPLAUNCHER_JAVA_HOME=C:/Program Files/Zulu/zulu-11")
endif()

# Locate JNI headers and libraries
set(JAVA_INCLUDE_DIR "${OPLAUNCHER_JAVA_HOME}/include")
set(JAVA_INCLUDE_DIR2 "${OPLAUNCHER_JAVA_HOME}/include/win32")
set(JAVA_JVM_LIB "${OPLAUNCHER_JAVA_HOME}/lib/jvm.lib")
set(JVM_DLL "${OPLAUNCHER_JAVA_HOME}/jre/bin/server/jvm.dll")
set(JAVA_DLL "${OPLAUNCHER_JAVA_HOME}/jre/bin/java.dll")

# Validate the paths
if(NOT EXISTS "${JAVA_INCLUDE_DIR}/jni.h")
    message(FATAL_ERROR "Could not find jni.h in ${JAVA_INCLUDE_DIR}. Ensure OPLAUNCHER_JAVA_HOME is set correctly.")
endif()

if(NOT EXISTS "${JAVA_INCLUDE_DIR2}/jni_md.h")
    message(FATAL_ERROR "Could not find jni_md.h in ${JAVA_INCLUDE_DIR2}. Ensure OPLAUNCHER_JAVA_HOME is set correctly.")
endif()

# Include directories
include_directories(
        ${CMAKE_SOURCE_DIR}
        ${CMAKE_SOURCE_DIR}/deps
        ${JAVA_INCLUDE_DIR}
        ${JAVA_INCLUDE_DIR2}
)

# Add source files
set(SOURCES
        main.c
        oplauncher.c
        jvm_launcher.c
        utils.c
)

# Add header files (optional, for IDE indexing)
set(HEADERS
        oplauncher.h
        jvm_launcher.h
        utils.h
        errcodes.h
)

# Create the executable
add_executable(oplauncher ${SOURCES} ${HEADERS})

if(WIN32)
    # Debugging paths (optional)
    message(STATUS "JVM_DLL path: ${JVM_DLL}")
    message(STATUS "cJSON shared library path: $<TARGET_FILE:cjson>")

    # Copy cJSON.dll to the executable's directory after build
    add_custom_command(TARGET oplauncher POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
            $<TARGET_FILE:cjson>               # Source location of cjson.dll
            $<TARGET_FILE_DIR:oplauncher>      # Destination directory
    )
endif()

if(MSVC)
    # Remove the /Za flag to avoid conflict with /std:c11
    string(REPLACE "/Za" "" CMAKE_C_FLAGS "${CMAKE_C_FLAGS}")
    string(REPLACE "/Za" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
endif()

# Copy the applet.policy file to the build folder
set(POLICY_FILE applet.policy)
set(POLICY_DESTINATION ${CMAKE_BINARY_DIR})
file(COPY ${POLICY_FILE} DESTINATION ${POLICY_DESTINATION})

# Link libraries (JNI and cJSON)
target_link_libraries(oplauncher ${JAVA_JVM_LIB} cjson)

# Output message for the user
message(STATUS "Configuration complete for ${OS_PLATFORM}")
