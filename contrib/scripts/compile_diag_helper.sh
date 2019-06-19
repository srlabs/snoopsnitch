#!/bin/bash

set -e
set -x

cd ${BASE_DIR}/diag_helper
${NDK_DIR}/ndk-build
