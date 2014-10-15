#! /bin/bash -eux

cd ${BASE_DIR}/metagsm
make -j4 ${METAGSM_MAKE_ARGS}
