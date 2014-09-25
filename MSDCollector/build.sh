#! /bin/sh -e

if [ x${NDK_DIR} = x ];
then
	echo "NDK_DIR not set!";
	exit 1;
fi

if [ x${ANDROID_HOME} = x ];
then
	echo "ANDROID_HOME not set!";
	exit 1;
fi

ant "-Djava.compilerargs=-Xlint:unchecked -Xlint:deprecation" -Dndk.dir=${NDK_DIR} debug
