#!/bin/bash

set -e
set -x

# The OpenSSL build system requires CC="clang", not the full path with version
#export CC="clang"
# Overwrite
export CFLAGS="--sysroot=${SYSROOT}"

cd ${BASE_DIR}/openssl
# Make sure all artefacts from previous compilation runs are deleted
# Required when switching between different architectures (host, arm, aarch64)
git reset --hard
git clean -d -f -x
./Configure "$OPENSSL_TARGET" no-shared -fPIE -D__ANDROID_API__=22
make LDFLAGS="-fPIE -pie"
