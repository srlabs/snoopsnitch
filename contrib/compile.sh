#! /bin/bash

set -e

if test -z $1; then
    echo "Usage: $0 <compile directory>"
    exit 1;
fi

if test -e $1; then
    echo "Compile directory $1 already exists"
    exit 1;
fi

if test -z $NDK_DIR; then
    echo "No NDK_DIR set"
    exit 1;
fi

BASE_DIR="$( cd "$( dirname $0 )" && pwd )"

# update submodules if necessary
if [ -e libasn1c ];
then
	(cd .. && git submodule init contrib/libasn1c && git submodule update contrib/libasn1c)
fi

if [ -e libosmocore ];
then
	(cd .. && git submodule init contrib/libosmocore && git submodule update contrib/libosmocore)
fi

if [ -e metagsm ];
then
	(cd .. && git submodule init contrib/metagsm && git submodule update contrib/metagsm)
fi

mkdir $1
cd $1

OUTPUT_DIR=`pwd`

echo "export BASE_DIR=$BASE_DIR" > $OUTPUT_DIR/env.sh
echo "export OUTPUT_DIR=$OUTPUT_DIR" >> $OUTPUT_DIR/env.sh
echo "export NDK_DIR=/home/user/android-ndk-r10/" >> $OUTPUT_DIR/env.sh
echo "export NDK_DIR=$NDK_DIR" >> $OUTPUT_DIR/env.sh
echo 'export ANDROID_ROOT=$NDK_DIR' >> $OUTPUT_DIR/env.sh
echo 'export PATH=$PATH:$ANDROID_ROOT/toolchains/arm-linux-androideabi-4.8/prebuilt/linux-x86_64/bin/' >> $OUTPUT_DIR/env.sh
echo "export CROSS_COMPILE=arm-linux-androideabi" >> $OUTPUT_DIR/env.sh
echo 'export SYSROOT=$ANDROID_ROOT/platforms/android-19/arch-arm' >> $OUTPUT_DIR/env.sh
echo "export PREFIX=$OUTPUT_DIR/out/" >> $OUTPUT_DIR/env.sh

echo "Environment:"
cat $OUTPUT_DIR/env.sh
. $OUTPUT_DIR/env.sh

mkdir $PREFIX

for i in libosmocore libasn1c libosmo-asn1-rrc metagsm;do
    echo -n "Building $i..."
    cd $OUTPUT_DIR
    if $BASE_DIR/scripts/compile_$i.sh > $OUTPUT_DIR/$i.compile_log 2>&1;then
	echo OK
    else
	echo "Failed!"
	echo "Please view log file $OUTPUT_DIR/$i.compile_log"
	exit 1
    fi
done

$BASE_DIR/scripts/create_parser_dir.sh
echo DONE
