#! /bin/bash -x
set -e

cd $BASE_DIR/libasn1c
git reset --hard
git clean -f
patch -p1 < $BASE_DIR/patches/libasn1c_jakob.patch

autoreconf -fi
./configure --host $CROSS_COMPILE CC=$CROSS_COMPILE-gcc CPPFLAGS="-I$ANDROID_ROOT/platforms/android-19/arch-arm/usr/include/ -I $PREFIX/include"  CFLAGS="--sysroot=$SYSROOT -nostdlib" LDFLAGS="--sysroot=$SYSROOT  -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$PREFIX/lib/" LIBS="-lc -lm" --prefix=$PREFIX
make -j4 LIBOSMOCORE_CFLAGS=-I$PREFIX/include/ LIBOSMOCORE_LIBS="-L$PREFIX/lib -losmocore"
make install
