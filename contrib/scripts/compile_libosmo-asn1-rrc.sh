#! /bin/bash -eux

cd ${BASE_DIR}/libosmo-asn1-rrc

autoreconf -fi
./configure ${MSD_CONFIGURE_OPTS} LDFLAGS="-lasn1c -L${MSD_DESTDIR}/lib" 
make -j4
make install
