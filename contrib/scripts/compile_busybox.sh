#!/bin/sh

export ANDROID_VER="21"
export MACH=$(uname -m)
export KERN=$(uname -s)
export HOST="linux-${MACH}"

#export NDK_DIR="/home/user/android-ndk-r12b"
export SYSROOT="${NDK_DIR}/platforms/android-${ANDROID_VER}/arch-arm"
export MSD_CONFIGURE_OPTS="--host arm-linux-androideabi --prefix=${MSD_DESTDIR}"
export PATH=${PATH}:${NDK_DIR}/toolchains/arm-linux-androideabi-4.9/prebuilt/${HOST}/bin/
export CROSS_COMPILE=arm-linux-androideabi-
export RANLIB=arm-linux-androideabi-ranlib
export CFLAGS="--sysroot=${SYSROOT}"
export CPPFLAGS="-I${NDK_DIR}/platforms/android-${ANDROID_VER}/arch-arm/usr/include/ -fPIE"
export LDFLAGS="--sysroot=${SYSROOT} -Wl,-rpath-link=${NDK_DIR}/platforms/android-${ANDROID_VER}/arch-arm/usr/lib/,-L${NDK_DIR}/platforms/android-${ANDROID_VER}/arch-arm/usr/lib/"
export LIBS="-lc -lm"

HOST_TARGET_OPTS="--host arm-linux-androideabi --enable-targets=arm-linux-androideabi,aarch64-linux-gnu,aarch64_be-linux-gnu"

# copy make config
cp $BASE_DIR/scripts/busybox_make.conf ../busybox/.config

cd $BASE_DIR/busybox

# apply patches
echo "Applying patches:"
echo $BASEDIR/patches/busybox_name.patch
git reset --hard
git clean -f
patch -p1 < $BASE_DIR/patches/busybox_name.patch

#build
make -j4

cp busybox $BASE_DIR/prebuilt/libbusybox.so
