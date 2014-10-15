#! /bin/sh

# The openssl build system does not add a minus between ${CROSS_COMPILE} and the tool to call (e.g. gcc).
export CROSS_COMPILE=${CROSS_COMPILE}-

tar xzf $BASE_DIR/openssl-1.0.1i.tar.gz
cd openssl-1.0.1i
./Configure android

# The first call to make fails for some unknown reason. However, the compilation does finish correclty when funning the same make command again.
make -j4 CPPFLAGS="-I$ANDROID_ROOT/platforms/android-19/arch-arm/usr/include/" LDFLAGS="--sysroot=$SYSROOT -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/"
make -j4 CPPFLAGS="-I$ANDROID_ROOT/platforms/android-19/arch-arm/usr/include/" LDFLAGS="--sysroot=$SYSROOT -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/"


