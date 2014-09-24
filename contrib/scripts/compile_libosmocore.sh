#! /bin/bash -x
set -e

cd ${BASE_DIR}/libosmocore
git reset --hard
git clean -f
patch -p1 < $BASE_DIR/patches/osmocore_luca.patch
patch -p1 < $BASE_DIR/patches/osmocore_jakob.patch

autoreconf -fi
./configure --host $CROSS_COMPILE CC=$CROSS_COMPILE-gcc CPPFLAGS="-I$ANDROID_ROOT/platforms/android-19/arch-arm/usr/include/"  CFLAGS="--sysroot=$SYSROOT -nostdlib" LDFLAGS="--sysroot=$SYSROOT -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/" LIBS="-lc " --prefix=$PREFIX --disable-utilities --disable-serial --enable-talloc
make -j4
make install
