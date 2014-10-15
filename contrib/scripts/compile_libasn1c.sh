#! /bin/bash -eux

cd ${BASE_DIR}/libasn1c
git reset --hard
git clean -f
patch -p1 < ${BASE_DIR}/patches/libasn1c_jakob.patch

autoreconf -fi
./configure ${MSD_CONFIGURE_OPTS}
make -j4 LIBOSMOCORE_CFLAGS="-I${MSD_DESTDIR}/include/" LIBOSMOCORE_LIBS="-L${MSD_DESTDIR}/lib -losmocore"
make install
