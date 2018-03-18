#! /bin/bash -eux

cd ${BASE_DIR}/sigtool
${NDK_DIR}/ndk-build

cp ${BASE_DIR}/sigtool/libs/armeabi/libsigtool.so ${BASE_DIR}/prebuilt/
