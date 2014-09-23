#!/bin/sh
set -e

echo "Creating parser directory"
cd $OUTPUT_DIR
mkdir parser
cp ./out/lib/libosmogsm.so.5 ./out/lib/libosmocore.so.5 ./out/lib/libasn1c.so.0 ./out/lib/libosmo-asn1-rrc.so.0 ./out/metagsm/libcompat.so ./out/metagsm/diag_import parser/
