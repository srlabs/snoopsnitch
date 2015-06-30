#! /bin/bash -eux

cd ${BASE_DIR}/libosmocore
git reset --hard
git clean -f
patch -p1 < ${BASE_DIR}/patches/osmocore_luca.patch
patch -p1 < ${BASE_DIR}/patches/osmocore_jakob.patch

autoreconf -fi
./configure ${MSD_CONFIGURE_OPTS} --disable-utilities --disable-serial --disable-pcsc --enable-talloc 
make -j4
make install
