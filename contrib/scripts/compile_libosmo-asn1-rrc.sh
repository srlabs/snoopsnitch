#! /bin/bash -eux

cd ${BASE_DIR}/libosmo-asn1-rrc

autoreconf -fi
./configure ${MSD_CONFIGURE_OPTS}
make -j4 libosmo_asn1_rrc_la_LDFLAGS="-lasn1c -L${MSD_DESTDIR}/lib"
make install
