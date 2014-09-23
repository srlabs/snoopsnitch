#! /bin/bash -x
set -e

cd ${BASE_DIR}/metagsm
make -f Makefile.Android -j 4 DESTDIR=${OUTPUT_DIR}/out/metagsm install
