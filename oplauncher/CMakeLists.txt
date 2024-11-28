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
elseif(APPLE)
    message(STATUS "Building on macOS")
    set(OS_PLATFORM "macos")
else()
    message(FATAL_ERROR "Unsupported platform")
endif()

# Add cJSON library (ensure it's built first)
add_subdirectory(deps)

# Include directories
include_directories(${CMAKE_SOURCE_DIR})
include_directories(${CMAKE_SOURCE_DIR}/deps)

# Locate JNI headers and libraries (cross-platform)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# Check if the build type is Debug
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    message(STATUS "Configuring Debug build: Adding debug symbols (-g)")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g")
endif()


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

# Copy the applet.policy file to the build folder
set(POLICY_FILE applet.policy)
set(POLICY_DESTINATION ${CMAKE_BINARY_DIR})
file(COPY ${POLICY_FILE} DESTINATION ${POLICY_DESTINATION})

# Link libraries (JNI and cJSON)
target_link_libraries(oplauncher ${JNI_LIBRARIES} cjson)

# Output message for the user
message(STATUS "Configuration complete for ${OS_PLATFORM}")
