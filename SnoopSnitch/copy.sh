#!/bin/sh

set -e

JNI_DIR=./app/src/main/jniLibs/armeabi/
ASSETS_DIR=./app/src/main/assets/

cp ../contrib/prebuilt/libasn1c.so $JNI_DIR
cp ../contrib/prebuilt/libcompat.so $JNI_DIR
cp ../contrib/prebuilt/libdiag_import.so $JNI_DIR
cp ../contrib/prebuilt/libopenssl.so $JNI_DIR
cp ../contrib/prebuilt/libosmo-asn1-rrc.so $JNI_DIR
cp ../contrib/prebuilt/libosmocore.so $JNI_DIR
cp ../contrib/prebuilt/libosmogsm.so $JNI_DIR
cp ../contrib/prebuilt/libsmime_crt.so $JNI_DIR
cp ../contrib/prebuilt/libdiag-helper.so $JNI_DIR
cp ../contrib/prebuilt/libbusybox.so $JNI_DIR
cp ../contrib/prebuilt/libobjdump.so $JNI_DIR

cp ../contrib/prebuilt/anonymize.sql $ASSETS_DIR
cp ../contrib/prebuilt/si.sql $ASSETS_DIR
cp ../contrib/sql/si_loc.sql $ASSETS_DIR
cp ../contrib/prebuilt/cell_info.sql $ASSETS_DIR
cp ../contrib/prebuilt/sms.sql $ASSETS_DIR
cp ../contrib/prebuilt/sm_2g.sql $ASSETS_DIR
cp ../contrib/prebuilt/sm_3g.sql $ASSETS_DIR
cp ../contrib/prebuilt/mcc.sql $ASSETS_DIR
cp ../contrib/prebuilt/mnc.sql $ASSETS_DIR
cp ../contrib/prebuilt/hlr_info.sql $ASSETS_DIR
cp ../contrib/prebuilt/sm.sql $ASSETS_DIR
cp ../contrib/sql/files.sql $ASSETS_DIR
cp ../analysis/prebuilt/catcher_analysis.sql $ASSETS_DIR
cp ../analysis/prebuilt/config.sql $ASSETS_DIR
cp ../analysis/prebuilt/analysis_tables.sql $ASSETS_DIR
cp ../analysis/prebuilt/event_analysis.sql $ASSETS_DIR
cp ../analysis/prebuilt/location.sql $ASSETS_DIR

