cmake_minimum_required(VERSION 3.15)
project(oplauncher)

# Set the C standard
set(CMAKE_C_STANDARD 11)

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

# Add source files
set(SOURCES
        main.c
        oplauncher.c
        jvm_launcher.c
)

# Add header files (optional, for IDE indexing)
set(HEADERS
        oplauncher.h
        jvm_launcher.h
        utils.h
        utils.c
        errcodes.h
)

# Create the executable
add_executable(oplauncher ${SOURCES} ${HEADERS})

# Link libraries (JNI and cJSON)
target_link_libraries(oplauncher ${JNI_LIBRARIES} cjson)

# Output message for the user
message(STATUS "Configuration complete for ${OS_PLATFORM}")
