#!/usr/bin/env bash

./link_to_mupdf_1.11.sh
./link_to_mupdf_master.sh

MASTER=/home/dev/git/LibreraReader/Builder/mupdf-master/platform/librera
OLD=/home/dev/git/LibreraReader/Builder/mupdf-1.11/platform/librera/libs

cp -a $OLD $MASTER

cd ../

./gradlew clean incVersion

./gradlew assembleLibreraRelease
./gradlew assembleProRelease
./gradlew assembleFdroidRelease

./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-arm*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*

rm /home/dev/Nextcloud/LibreraBeta/*-arm*
rm /home/dev/Nextcloud/LibreraBeta/*-x86*

./remove_all.sh
./install_all.sh
./clear-cache.sh