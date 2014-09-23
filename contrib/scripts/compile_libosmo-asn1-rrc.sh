#! /bin/bash -x
set -e
aunpack $BASE_DIR/libosmo-asn1-rrc.tar.bz2 
cd libosmo-asn1-rrc
cat $BASE_DIR/libosmo-asn1-rrc-jakob.patch|patch src/Makefile.am
autoreconf -fi
./configure --host $CROSS_COMPILE CC=$CROSS_COMPILE-gcc CPPFLAGS="-I$ANDROID_ROOT/platforms/android-19/arch-arm/usr/include/ -I $PREFIX/include"  CFLAGS="--sysroot=$SYSROOT -nostdlib" LDFLAGS="--sysroot=$SYSROOT  -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$ANDROID_ROOT/platforms/android-19/arch-arm/usr/lib/,-L$PREFIX/lib/" LIBS="-lc " --prefix=$PREFIX
make -j4 libosmo_asn1_rrc_la_LDFLAGS="-lasn1c -L$PREFIX/lib"
make install