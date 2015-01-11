#! /bin/sh

# The openssl build system does not add a minus between ${CROSS_COMPILE} and the tool to call (e.g. gcc).
export CROSS_COMPILE=${CROSS_COMPILE}-
export CC="gcc --sysroot=${SYSROOT}"

tar xzf $BASE_DIR/openssl-1.0.1i.tar.gz
cd openssl-1.0.1i
./Configure android -fPIE
make LDFLAGS="-fPIE -pie"
