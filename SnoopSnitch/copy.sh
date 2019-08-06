#!/bin/sh

set -e

JNI_32_DIR=./app/src/main/jniLibs/armeabi-v7a/
JNI_64_DIR=./app/src/main/jniLibs/arm64-v8a/
ASSETS_DIR=./app/src/main/assets/

cp ../contrib/prebuilt/32/* $JNI_32_DIR
cp ../contrib/prebuilt/64/* $JNI_64_DIR
cp ../contrib/prebuilt/*.sql $ASSETS_DIR
cp ../contrib/sql/*.sql $ASSESTS_DIR
