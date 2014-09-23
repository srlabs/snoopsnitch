#! /bin/bash -x
set -e
git clone ssh://gitolite@git.srlabs.de/metagsm.git
cd metagsm
make -f Makefile.Android -j 4
