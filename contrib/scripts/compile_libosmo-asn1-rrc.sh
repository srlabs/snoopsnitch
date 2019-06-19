#!/bin/bash

set -e
set -x

cd ${BASE_DIR}/libosmo-asn1-rrc

autoreconf -fi
./configure ${MSD_CONFIGURE_OPTS} LDFLAGS="$LDFLAGS -L${MSD_DESTDIR}/lib"

# Make sure all artefacts from previous compilation runs are deleted
# Required when switching between different architectures (host, arm, aarch64)
# Do not use git clean since libosmo-asn1-rrc is not a submodule!
make clean

make -j4
make install
