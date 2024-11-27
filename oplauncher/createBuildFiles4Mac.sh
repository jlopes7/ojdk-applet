#!/bin/bash

set -Eo pipefail

BASEDIR="$(dirname `readlink -f $0`)"

# Create a build directory
mkdir -p $BASEDIR/build
pushd . &>/dev/null
cd $BASEDIR/build

# Configure with CMake
cmake .. -G "Unix Makefiles"

popd &>/dev/null

echo -e "\n - To build the source for Mac, simply run:"
echo -e "\n     cmake --build build/\n"

exit $?

