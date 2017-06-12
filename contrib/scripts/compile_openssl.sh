#! /bin/sh

# The openssl build system does not add a minus between ${CROSS_COMPILE} and the tool to call (e.g. gcc).
export CROSS_COMPILE=${CROSS_COMPILE}-
export CROSS_SYSROOT="$NDK_DIR/platforms/android-19/arch-arm"
export CC="gcc --sysroot=${SYSROOT}"

echo "CROSS_COMPILE: $CROSS_COMPILE"
echo "CROSS_SYSROOT: $CROSS_SYSROOT"

tar xzf $BASE_DIR/openssl-1.1.0f.tar.gz
cd openssl-1.1.0f
./Configure android no-shared -fPIE 
make LDFLAGS="-fPIE -pie"
