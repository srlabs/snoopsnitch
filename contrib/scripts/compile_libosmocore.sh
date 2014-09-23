#! /bin/bash -x
set -e
git clone git://git.osmocom.org/libosmocore/
cd libosmocore
cat $BASE_DIR/osmocore_luca.patch |patch -p1
cat $BASE_DIR/osmocore_jakob.patch |patch -p1
autoreconf -fi
./configure --host $CROSS_COMPILE CC=$CROSS_COMPILE-gcc CPPFLAGS="-I$ANDROID_ROOT/platforms/android-19/arch-arm/usr/include/"  CFLAGS="--sysroot=$SYSROOT -nostdlib" LDFLAGS="--sysroot=$SYSROOT -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/" LIBS="-lc " --prefix=$PREFIX --disable-utilities --disable-serial --enable-talloc
make -j4
make install