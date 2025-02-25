[CmdletBinding()]
Param (
    [Parameter(Mandatory = $true)]
    [string]$ReleaseType
)

function Get-ConfigValue {
    param (
        [switch]$MSVCCompilerHomePath,
        [switch]$MSVCToolsHomePath,
        [switch]$WiXHomePath,
        [switch]$MavenHomePath,
        [switch]$JavaHomePath,
        [string]$Property
    )

    [string]$filePath = "$PROJECT_ROOT\build.cfg"
    # Ensure the file exists
    if (!(Test-Path -Path $filePath -PathType Leaf)) {
        Write-Host "Error: Configuration file not found at $filePath" -ForegroundColor Red
        Exit $ERROR_CODE
    }

    # Read the file, ignoring comments and empty lines
    $config = Get-Content $FilePath | Where-Object { $_ -match "^\s*[^#]" }

    foreach ($line in $config) {
        if ($line -match "^\s*(\S+)\s*:\s*(.+?)\s*$") {
            $key = $matches[1].Trim()
            $val = $matches[2].Trim()

            if ( $MSVCCompilerHomePath ) {
                if ( $key -ieq "MSVCCompilerHomePath" ) {
                    return $val
                }
            }
            elseif ( $MSVCToolsHomePath ) {
                if ( $key -ieq "MSVCToolsHomePath" ) {
                    return $val
                }
            }
            elseif ( $WiXHomePath ) {
                if ( $key -ieq "WiXHomePath" ) {
                    return $val
                }
            }
            elseif ( $MavenHomePath ) {
                if ( $key -ieq "MavenHomePath" ) {
                    return $val
                }
            }
            elseif ( $JavaHomePath ) {
                if ( $key -ieq "JavaHomePath" ) {
                    return $val
                }
            }
            else {
                if ( -not([string]::IsNullOrEmpty($Property)) -and ($key -ieq $Property.Trim()) ) {
                    return $val
                }
            }
        }
    }

    # Key not found in the configuration file
    return $null
}

function Is-CompilerFlagActive() {
    param (
        [switch]$BuildNative,
        [switch]$BuildJava,
        [switch]$BuildMSI
    )

    if ( $BuildNative ) {
        return Is-True -Status $(Get-ConfigValue -Property "BuildNativeComp" )
    }
    elseif ( $BuildJava ) {
        return Is-True -Status $(Get-ConfigValue -Property "BuildJavaComp" )
    }
    elseif ( $BuildMSI ) {
        return Is-True -Status $(Get-ConfigValue -Property "BuildMSIComp" )
    }

    return $false
}
function Is-True() {
    param ( [string]$Status )

    return ( -not([string]::IsNullOrEmpty($Status)) -and ( ($Status.Trim() -ieq "yes" ) -or ($Status.Trim() -ieq "y" ) -or ($Status.Trim() -ieq "true" ) ))
}



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
[string]$WIX_CONFIG_OUTPUT_FILE="OPLauncherInstaller.wixobj"

[int16]$ERROR_CODE = 1
[int16]$SUCCESS_CODE = 0

[string]$MSVCDevTools = Get-ConfigValue -MSVCToolsHomePath
[string]$MSVCCompiler = Get-ConfigValue -MSVCCompilerHomePath
[string]$WiXToolkit   = Get-ConfigValue -WiXHomePath
[string]$MavenPath    = Get-ConfigValue -MavenHomePath
[string]$JavaHome     = Get-ConfigValue -JavaHomePath

[string]$DEPS_HOME = "$PROJECT_ROOT\deps"
[string]$NINJA_HOME = "$PROJECT_ROOT\build\ninja-win\x64"
[string]$CMAKE_HOME = "$PROJECT_ROOT\build\cmake\win\x64\bin"

Add-Type -AssemblyName System.IO.Compression.FileSystem

if ( Test-Path -Path $MavenPath ) {
    $env:MVN_HOME = $MavenPath
}
if ( Test-Path -Path $JavaHome ) {
    $env:JAVA_HOME = $JavaHome
}

Write-Host "Found MSVC tooling path: $MSVCDevTools" -ForegroundColor DarkCyan
Write-Host "Found MSVC compiler path: $MSVCCompiler" -ForegroundColor DarkCyan
Write-Host "Found WiX Toolkit path: $WiXToolkit" -ForegroundColor DarkCyan
Write-Host "Found Maven path: $MavenPath" -ForegroundColor DarkCyan
Write-Host "Found OJDK path: $JavaHome" -ForegroundColor DarkCyan

if ( -not(Test-Path $MSVCDevTools) ) {
    $MSVCDevTools = $env:MSVCENVTOOLS

    if ( [string]::IsNullOrEmpty($MSVCDevTools) ) {
        Write-Host "The MS VC env configuration tool needs to exist in the system. Current path could not be resolved: $MSVCDevTools" -ForegroundColor Red
        Exit $ERROR_CODE
    }
}
if ( -not(Test-Path -Path $MSVCCompiler) ) {
    Write-Host "The MSVC compiler path doesn't exist: $MSVCCompiler" -ForegroundColor Red
    Exit $ERROR_CODE
}
if ( -not(Test-Path -Path $WiXToolkit ) ) {
    Write-Host "The WiX env configuration toolkit needs to exist in the system. Current path could not be resolved: $WiXToolkit" -ForegroundColor Red
    Exit $ERROR_CODE
}

if ( Is-CompilerFlagActive -BuildNative ) {
    Write-Host "Loading MS VC toolling and preparing the environment..." -ForegroundColor White
    Write-Host "MS VC toolling user path: $MSVCDevTools" -ForegroundColor White

    # Execute VsDevCmd.bat and capture the environment variables - WORKAROUND FROM CMD TO PS ! ;)
    $envVars = cmd /c "`"$MSVCDevTools\VsDevCmd.bat`" -arch=x64 && set" | Out-String
    # Apply the environment variables to the current PowerShell session
    $envVars -split "`r`n" | ForEach-Object {
        if ($_ -match "^(.*?)=(.*)$") {
            Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
            Write-Host "Loading item: $($matches[1]) := $($matches[2])" -ForegroundColor White
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

    if ( Test-Path -Path $DEPS_HOME\cmake.zip -PathType Leaf ) {
        Write-Host "Preparing CMake... " -ForegroundColor White
        [System.IO.Compression.ZipFile]::ExtractToDirectory("$DEPS_HOME\cmake.zip", "$PROJECT_ROOT\build\")
        #Expand-Archive -Path "$DEPS_HOME\cmake.zip" -DestinationPath "$PROJECT_ROOT\build" -Force
    }
    else {
        Write-Host "Missing the CMake package file to produce the native build is missing from $DEPS_HOME\cmake.zip" -ForegroundColor Red
        Exit $ERROR_CODE
    }
    if ( Test-Path -Path $DEPS_HOME\ninja-win.zip -PathType Leaf ) {
        Write-Host "Preparing Ninja... " -ForegroundColor White
        [System.IO.Compression.ZipFile]::ExtractToDirectory("$DEPS_HOME\ninja-win.zip", "$PROJECT_ROOT\build\")
        #Expand-Archive -Path "$DEPS_HOME\ninja-win.zip" -DestinationPath "$PROJECT_ROOT\build" -Force
    }
    else {
        Write-Host "Missing the Ninja package file to produce the native build is missing from $DEPS_HOME\ninja-win.zip" -ForegroundColor Red
        Exit $ERROR_CODE
    }

    # Step 1: Run CMake Compilation
    Write-Host "Building the native components of OPLauncher..." -ForegroundColor White
    Push-Location . | Out-Null

    if ( -not([System.Environment]::GetEnvironmentVariable("OPLAUNCHER_JAVA_HOME", "Machine"))) {
        Write-Host "The system-wide environment variable OPLAUNCHER_JAVA_HOME is not defined." -ForegroundColor Red
        Exit $ERROR_CODE
    }

    Set-Location -Path $PROJECT_ROOT

    $CMakeDefinitions = @(
        "-DOPLAUNCHER_JAVA_HOME=$($env:OPLAUNCHER_JAVA_HOME)",
        "-DCMAKE_C_COMPILER=$MSVCCompiler\cl.exe",
        "-DCMAKE_LINKER=$MSVCCompiler\link.exe",
        "-DCMAKE_BUILD_TYPE=$ReleaseType",
        "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
        "-DCMAKE_MAKE_PROGRAM=$NINJA_HOME\ninja.exe",
        "-DWIN32=1"
    )

    & $CMAKE_HOME\cmake -G "Ninja" oplauncher -B $BUILD_DIR $CMakeDefinitions
    & $CMAKE_HOME\cmake --build build --target clean -j $env:NUMBER_OF_PROCESSORS
    & $CMAKE_HOME\cmake --build build --target all -j $env:NUMBER_OF_PROCESSORS
    if ($LASTEXITCODE -ne 0) {
        Write-Host "CMake Build Failed!" -ForegroundColor Red
        Exit $LASTEXITCODE
    }
    Pop-Location
    Write-Host "The native components of OPLauncher were build successfully." -ForegroundColor Green
}
else {
    Write-Host "Native component build is disabled" -ForegroundColor Yellow
}

# Step 2: Run Maven Build
if ( Is-CompilerFlagActive -BuildJava ) {
    Write-Host "Creating the JVM components of OPLauncher... Maven build..." -ForegroundColor White
    Push-Location $MAVEN_DIR
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Maven Build Failed!" -ForegroundColor Red
        Pop-Location
        Exit $LASTEXITCODE
    }
    Pop-Location
    Write-Host "The JVM components of OPLaucnher were completed successfully." -ForegroundColor Green
}
else {
    Write-Host "Java component build is disabled" -ForegroundColor Yellow
}

# Step 3: Run WiX Build
if ( Is-CompilerFlagActive -BuildMSI ) {
    Write-Host "Building the OPLauncher MSI Installer with WiX..." -ForegroundColor White
    Push-Location $WIX_DIR

    # Push WiX into the Path
    $env:Path += ";$WiXToolkit\bin"

    # Compile WiX Source Files
    candle -dOP_BINARIES_DIR="$CMAKE_DIR" $WIX_CONFIG_FILE -o $WIX_CONFIG_OUTPUT_FILE
    if ($LASTEXITCODE -ne 0) {
        Write-Host "WiX Compilation Failed!" -ForegroundColor Red
        Pop-Location
        Exit $LASTEXITCODE
    }

    # Link to create MSI
    light $WIX_CONFIG_OUTPUT_FILE -o $MSI_OUTPUT
    if ($LASTEXITCODE -ne 0) {
        Write-Host "WiX Linking Failed!" -ForegroundColor Red
        Pop-Location
        Exit $LASTEXITCODE
    }
    Pop-Location
    Write-Host "MSI Created: $MSI_OUTPUT" -ForegroundColor Green
}
else {
    Write-Host "MSI component build is disabled" -ForegroundColor Yellow
}

# Step 4: Success Message
Write-Host "Build Process Completed Successfully!" -ForegroundColor Green

Exit $SUCCESS_CODE
