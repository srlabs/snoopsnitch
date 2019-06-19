#!/bin/bash

set -e
set -x
echo "$GSM_PARSER_MAKE_ARGS"

cd ${BASE_DIR}/gsm-parser
make -j4 ${GSM_PARSER_MAKE_ARGS} install EXTRA_CFLAGS="$CFLAGS" EXTRA_LDFLAGS="$LDFLAGS"
