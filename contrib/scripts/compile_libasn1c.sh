#! /bin/bash -x
set -e
git clone git://git.osmocom.org/libasn1c
cd libasn1c
cat $BASE_DIR/libasn1c_jakob.patch |patch -p1
autoreconf -fi
./configure --host $CROSS_COMPILE CC=$CROSS_COMPILE-gcc CPPFLAGS="-I$ANDROID_ROOT/platforms/android-19/arch-arm/usr/include/ -I $PREFIX/include"  CFLAGS="--sysroot=$SYSROOT -nostdlib" LDFLAGS="--sysroot=$SYSROOT  -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$PREFIX/lib/" LIBS="-lc -lm" --prefix=$PREFIX
make -j4 LIBOSMOCORE_CFLAGS=-I$PREFIX/include/ LIBOSMOCORE_LIBS="-L$PREFIX/lib -losmocore"
make install