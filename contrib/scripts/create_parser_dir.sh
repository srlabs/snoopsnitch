#!/bin/sh -e

echo "Creating parser directory"

PARSER_DIR=${OUTPUT_DIR}/parser

install -d ${PARSER_DIR}
install -m 755 ${OUTPUT_DIR}/out/lib/libasn1c.so.0         ${PARSER_DIR}/libasn1c.so
install -m 755 ${OUTPUT_DIR}/out/lib/libosmo-asn1-rrc.so.0 ${PARSER_DIR}/libosmo-asn1-rrc.so
install -m 755 ${OUTPUT_DIR}/out/lib/libosmocore.so.5      ${PARSER_DIR}/libosmocore.so
install -m 755 ${OUTPUT_DIR}/out/lib/libosmogsm.so.5       ${PARSER_DIR}/libosmogsm.so
install -m 755 ${OUTPUT_DIR}/out/metagsm/diag_import       ${PARSER_DIR}/libdiag_import.so
install -m 755 ${OUTPUT_DIR}/out/metagsm/libcompat.so      ${PARSER_DIR}/libcompat.so

# Really dirty hack: The Android build system and package installer require 
# all files in the native library dir to have a filename like libXXX.so. If
# the file extension ends with .so.5, it will not be copied to the APK file. 
# So the following line of perl patches all references so that the libraries
# are found with a .so extension instead of .so.[digit]
perl -i -pe 's/libasn1c\.so\.0/libasn1c.so\0\0/gs;s/libosmo-asn1-rrc\.so\.0/libosmo-asn1-rrc.so\0\0/gs;s/libosmocore\.so\.5/libosmocore.so\0\0/gs;s/libosmogsm\.so\.5/libosmogsm.so\0\0/gs' ${PARSER_DIR}/*.so
