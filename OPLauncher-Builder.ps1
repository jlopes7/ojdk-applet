# Root configuration Paths
[string]$PROJECT_ROOT = "$PSScriptRoot"
[string]$OPLAUNCHER_NATIVE_ROOT = "$PROJECT_ROOT\oplauncher"
[string]$OPLAUNCHER_JAVA_ROOT = "$PROJECT_ROOT\oplauncher-pilot"
[string]$OPLAUNCHER_CHROMEEXT_ROOT = "$PROJECT_ROOT\chrome-ext"
[string]$BUILD_DIR = "$PROJECT_ROOT\build"
[string]$CMAKE_DIR = "$OPLAUNCHER_NATIVE_ROOT\cmake-build-release"
[string]$MSI_OUTPUT = "$BUILD_DIR\amt-oplauncher-v1.0b.msi"
[string]$WIX_DIR = "$PROJECT_ROOT"
[string]$MAVEN_DIR = "$OPLAUNCHER_JAVA_ROOT"
[string]$WIX_CONFIG_FILE="OPLauncherInstaller.wxs"

[int16]$ERROR_CODE = 1
[int16]$SUCCESS_CODE = 0

[string]$MSVCDevTools = "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\Common7\Tools"

if ( -not(Test-Path $MSVCDevTools) ) {
    $MSVCDevTools = $env:MSVCENVTOOLS

    if ( [string]::IsNullOrEmpty($MSVCDevTools) ) {
        Write-Host "The MS VC env configuration tool needs to exist in the system. Current path could not be resolved: $MSVCDevTools" -ForegroundColor Red
        Exit $ERROR_CODE
    }
}

Write-Host "Loading MS VC toolling and preparing the environment..." -ForegroundColor White
# Execute VsDevCmd.bat and capture the environment variables - WORKAROUND FROM CMD TO PS ! ;)
$envVars = cmd /c "`"$MSVCDevTools\VsDevCmd.bat`" && set" | Out-String
# Apply the environment variables to the current PowerShell session
$envVars -split "`r`n" | ForEach-Object {
    if ($_ -match "^(.*?)=(.*)$") {
        Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
    }
}
Write-Host "MSVC environment loaded successfully." -ForegroundColor Green

if ( Test-Path -Path $BUILD_DIR ) {
    Remove-Item -Path $BUILD_DIR\* -Recurse -Force | Out-Null
}
else {
    New-Item -ItemType Directory -Force -Path $BUILD_DIR | Out-Null
}
Copy-Item -Path $PROJECT_ROOT\logo\oplauncher_icon_32x32.ico -Destination $BUILD_DIR\ -Force

# Step 1: Run CMake Compilation
Write-Host "Building the native components of OPLauncher..." -ForegroundColor White
Push-Location . | Out-Null

if ( -not([System.Environment]::GetEnvironmentVariable("OPLAUNCHER_JAVA_HOME", "Machine"))) {
    Write-Host "The system-wide environment variable OPLAUNCHER_JAVA_HOME is not defined." -ForegroundColor Red
    Exit $ERROR_CODE
}

Set-Location -Path $PROJECT_ROOT
& cmake -G "Ninja" oplauncher -B $BUILD_DIR -DOPLAUNCHER_JAVA_HOME="$($env:OPLAUNCHER_JAVA_HOME)"
& cmake --build build --target clean -j $env:NUMBER_OF_PROCESSORS
& cmake --build build --target all -j $env:NUMBER_OF_PROCESSORS
if ($LASTEXITCODE -ne 0) {
    Write-Host "CMake Build Failed!" -ForegroundColor Red
    Exit $LASTEXITCODE
}
Pop-Location
Write-Host "The native components of OPLauncher were build successfully." -ForegroundColor Green

# Step 2: Run Maven Build
Write-Host "Creating the JVM components of OPLauncher... Maven build..." -ForegroundColor White
Push-Location $MAVEN_DIR
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven Build Failed!" -ForegroundColor Red
    Exit $LASTEXITCODE
}
Pop-Location
Write-Host "The JVM components of OPLaucnher were completed successfully." -ForegroundColor Green

# Step 3: Run WiX Build
Write-Host "Building the OPLauncher MSI Installer with WiX..." -ForegroundColor White
Push-Location $WIX_DIR

# Compile WiX Source Files
candle -dOP_BINARIES_DIR="$CMAKE_DIR" $WIX_CONFIG_FILE
if ($LASTEXITCODE -ne 0) {
    Write-Host "WiX Compilation Failed!" -ForegroundColor Red
    Exit $LASTEXITCODE
}

# Link to create MSI
light $WIX_CONFIG_FILE -o $MSI_OUTPUT
if ($LASTEXITCODE -ne 0) {
    Write-Host "WiX Linking Failed!" -ForegroundColor Red
    Exit $LASTEXITCODE
}
Pop-Location
Write-Host "MSI Created: $MSI_OUTPUT" -ForegroundColor Green

# Step 4: Success Message
Write-Host "Build Process Completed Successfully!" -ForegroundColor Green

Exit $SUCCESS_CODE
