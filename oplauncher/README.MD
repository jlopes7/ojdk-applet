# Build Instructions for the OJDK Plugin Launcher

### For MacOS
1. Make sure `CC` is installed from the XCode Developer toolkit
2. Install `cmake`
```bash
brew install cmake
```
3. Run the script to generate the `Makefile` in the `build/` folder
```bash
./createBuildFiles4Mac.sh
```
4. Build the `oplauncher` plugin
```bash
cmake --build $PWD/build
```

### For Windows
Install MS Visual C++ Redistributable (x64): https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170#visual-studio-2015-2017-2019-and-2022
Both `$%JRE_HOME%/bin` and `%JRE_HOME%\bin\server` need to be in the system path before running the code

1. Make sure that `CC` from Visual Studio 17 compiler tools are correctly installed
2. Simply run the build batch file
```bash
createBuildFiles4Windows.bat
```
3. The `oplauncher.exe` executable will be generated in the folder `build`

