#! /bin/sh
set -e

PARSER_DIR=/home/user/msd/parser/compile5/parser
OUT_DIR=libs/armeabi
cp $PARSER_DIR/diag_import $OUT_DIR/libdiag_import.so
#chmod 755 $OUT_DIR/libdiag_import.so
cp $PARSER_DIR/libasn1c.so.0 $OUT_DIR/libasn1c.so
cp $PARSER_DIR/libcompat.so $OUT_DIR/libcompat.so
cp $PARSER_DIR/libosmo-asn1-rrc.so.0 $OUT_DIR/libosmo-asn1-rrc.so
cp $PARSER_DIR/libosmocore.so.5 $OUT_DIR/libosmocore.so
cp $PARSER_DIR/libosmogsm.so.5 $OUT_DIR/libosmogsm.so
cd $OUT_DIR
cp /vbshared/strace-armv6l libstrace.so
chmod 755 *.so
ls -ls
# Really dirty hack: The Android build system and package installer require 
# all files in the native library dir to have a filename like libXXX.so. If
# the file extension ends with .so.5, it will not be copied to the APK file. 
# So the following line of perl patches all references so that the libraries
# are found with a .so extension instead of .so.[digit]
perl -i.bak -pe 's/libasn1c\.so\.0/libasn1c.so\0\0/gs;s/libosmo-asn1-rrc\.so\.0/libosmo-asn1-rrc.so\0\0/gs;s/libosmocore\.so\.5/libosmocore.so\0\0/gs;s/libosmogsm\.so\.5/libosmogsm.so\0\0/gs' *.so
ls -ls
