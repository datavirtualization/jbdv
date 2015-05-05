#!/bin/sh
set -e
BASEDIR=$(dirname $0)
if [ ! -d "target/executables" ]; then
    mkdir target/executables
fi
if [ -f "target/executables/run.rc" ]; then
    rm target/executables/run.rc
fi
echo "#include \"winuser.h\"" >> target/executables/run.rc
echo "1 RT_MANIFEST  \"$BASEDIR/run.exe.manifest\"" >> target/executables/run.rc
i686-pc-mingw32-windres --input "target/executables/run.rc" --output "target/executables/run.res" --output-format=coff
i686-pc-mingw32-gcc -o target/executables/run.exe "$BASEDIR/run.c" target/executables/run.res;
