#!/bin/bash

set -e
set -x

cd ${BASE_DIR}/libosmocore
# Make sure all artefacts from previous compilation runs are deleted
# Required when switching between different architectures (host, arm, aarch64)
git reset --hard
git clean -d -f -x
patch -p1 < ${BASE_DIR}/patches/osmocore_luca.patch
patch -p1 < ${BASE_DIR}/patches/osmocore_jakob.patch

autoreconf -fi
./configure ${MSD_CONFIGURE_OPTS} --disable-utilities --disable-serial --disable-pcsc --enable-talloc 
make -j4
make install
