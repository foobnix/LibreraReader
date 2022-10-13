#!/usr/bin/env bash

./link_to_mupdf_1.11.sh
./link_to_mupdf_master.sh

MASTER=mupdf-master/platform/librera
OLD=mupdf-1.11/platform/librera/libs

cp -a $OLD $MASTER

