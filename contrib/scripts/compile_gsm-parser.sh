#!/bin/bash

set -e
set -x
echo "$GSM_PARSER_MAKE_ARGS"

cd ${BASE_DIR}/gsm-parser

# Make sure all artefacts from previous compilation runs are deleted
# Required when switching between different architectures (host, arm, aarch64)
git reset --hard
git clean -d -f -x

make -j4 ${GSM_PARSER_MAKE_ARGS} install EXTRA_CFLAGS="$CFLAGS" EXTRA_LDFLAGS="$LDFLAGS" CC="$CC"
