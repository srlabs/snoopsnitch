#!/bin/bash

set -e
set -x

cd ${BASE_DIR}/libasn1c
# Make sure all artefacts from previous compilation runs are deleted
# Required when switching between different architectures (host, arm, aarch64)
git reset --hard
git clean -d -f -x
patch -p1 < ${BASE_DIR}/patches/libasn1c_jakob.patch
patch -p1 < ${BASE_DIR}/patches/libasn1c_luca.patch

autoreconf -fi
./configure ${MSD_CONFIGURE_OPTS}
make -j4 LIBOSMOCORE_CFLAGS="-I${MSD_DESTDIR}/include/" LIBOSMOCORE_LIBS="-L${MSD_DESTDIR}/lib -losmocore"
make install
