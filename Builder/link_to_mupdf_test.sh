#!/usr/bin/env bash

git clone --recursive git://git.ghostscript.com/mupdf.git --branch master mupdf-test

cd mupdf-test

make generate
make release