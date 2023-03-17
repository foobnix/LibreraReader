#!/usr/bin/env bash

rm ../app/src/fdroid/assets/fonts.zip
rm ../app/src/main/assets/fonts.zip

zip -r ../app/src/fdroid/assets/fonts.zip fonts
#zip -r ../app/src/main/assets/fonts.zip   fonts/FreeSerif.ttf

