#! /bin/bash
set -e
if test -z $1;then
    echo "Usage: $0 <compile directory>"
fi

if test -e $1;then
    echo "Compile directory $1 already exists"
fi
BASE_DIR="$( cd "$( dirname $0 )" && pwd )"
mkdir $1
cd $1
OUTPUT_DIR=`pwd`
if test -z $NDK_DIR;then
    export NDK_DIR=/home/user/android-ndk-r10/
    echo "Using default NDK_DIR=$NDK_DIR"
fi
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
    echo "Downloading and compiling $i..."
    cd $OUTPUT_DIR
    if $BASE_DIR/compile_$i.sh > $OUTPUT_DIR/$i.compile_log 2>&1;then
	echo OK
    else
	echo "Failed!"
	echo "Please view log file $OUTPUT_DIR/$i.compile_log"
	exit 1
    fi
done

$BASE_DIR/create_parser_dir.sh
echo EVERYTHING OK