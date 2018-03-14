#!/bin/sh

export ANDROID_VER="21"
export MACH=$(uname -m)
export KERN=$(uname -s)
export HOST="linux-${MACH}"

#export NDK_DIR="/home/user/android-ndk-r12b"
export SYSROOT="${NDK_DIR}/platforms/android-${ANDROID_VER}/arch-arm"
export MSD_CONFIGURE_OPTS="--host arm-linux-androideabi --prefix=${MSD_DESTDIR}"
export PATH=${PATH}:${NDK_DIR}/toolchains/arm-linux-androideabi-4.9/prebuilt/${HOST}/bin/
export CROSS_COMPILE=arm-linux-androideabi
export RANLIB=arm-linux-androideabi-ranlib
export CFLAGS="--sysroot=${SYSROOT}"
export CPPFLAGS="-I${NDK_DIR}/platforms/android-${ANDROID_VER}/arch-arm/usr/include/ -fPIE"
export LDFLAGS="--sysroot=${SYSROOT} -Wl,-rpath-link=${NDK_DIR}/platforms/android-${ANDROID_VER}/arch-arm/usr/lib/,-L${NDK_DIR}/platforms/android-${ANDROID_VER}/arch-arm/usr/lib/ -pie"
export LIBS="-lc -lm"

HOST_TARGET_OPTS="--host arm-linux-androideabi --enable-targets=arm-linux-androideabi,aarch64-linux-gnu,aarch64_be-linux-gnu"
cd $BASE_DIR/binutils-gdb/bfd
./configure $HOST_TARGET_OPTS --disable-option-checking  CXX=${CROSS_COMPILE}-g++ CC=${CROSS_COMPILE}-gcc  --disable-nls
make -j4
cd ../libiberty
./configure $HOST_TARGET_OPTS --disable-option-checking  CXX=${CROSS_COMPILE}-g++ CC=${CROSS_COMPILE}-gcc  --disable-nls
make -j4
cd ../opcodes
./configure $HOST_TARGET_OPTS --disable-option-checking  CXX=${CROSS_COMPILE}-g++ CC=${CROSS_COMPILE}-gcc  --disable-nls
make -j4
cd ../binutils
./configure $HOST_TARGET_OPTS --disable-option-checking  CXX=${CROSS_COMPILE}-g++ CC=${CROSS_COMPILE}-gcc  --disable-nls
make -j4

pwd
cp objdump $BASE_DIR/prebuilt/libobjdump.so

#perl -i.bak -pe '$_="// REMOVED\n if /^#include <stdio_ext.h>/' libiberty/fopen_unlocked.c
#perl -i.bak -pe '$_="// REMOVED\n if /^#include <sys\/sysctl.h.h>/' libiberty/physmem.c

#perl -i.bak -pe '$_="// REMOVED\n" if /^\s*#\s*include\s*<stdio_ext.h>/' intl/localealias.c


# export CFLAGS="$CFLAGS -DHAVE_STDIO_EXT_H=0"
