#!/usr/bin/env bash

cd fonts/ttf
zip -FSr ../../../app/src/fdroid/assets/fonts.zip .
zip -FSr ../fonts.zip .

