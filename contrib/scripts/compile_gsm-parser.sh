#! /bin/bash -eux

cd ${BASE_DIR}/gsm-parser
make -j4 ${GSM_PARSER_MAKE_ARGS}
