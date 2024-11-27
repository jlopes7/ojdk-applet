@echo off

:: Set variables
set BUILD_DIR=build
set GENERATOR="Visual Studio 17 2022"
set CONFIG=Debug

:: Step 1: Create the build directory if it doesn't exist
if not exist %BUILD_DIR% (
    mkdir %BUILD_DIR%
)

:: Step 2: Navigate to the build directory
cd %BUILD_DIR%

:: Step 3: Run CMake to configure the project
cmake .. -G %GENERATOR%
if %errorlevel% neq 0 (
    echo CMake configuration failed.
    exit /b %errorlevel%
)

:: Step 4: Build the project
cmake --build . --config %CONFIG%
if %errorlevel% neq 0 (
    echo Build failed.
    exit /b %errorlevel%
)

:: Step 5: Return to the original directory
cd ..

echo Build completed successfully.

