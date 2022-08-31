#!/usr/bin/env bash


### 1.20.0
./link_to_mupdf_1.20.2.sh

cd ../

./gradlew clean incVersion

./gradlew assembleProRelease
./gradlew assembleFdroidRelease


### 1.11.1

cd Builder
./link_to_mupdf_1.11.sh

cd ../
./gradlew assembleOldRelease

./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-arm*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*

./remove_all.sh
./install_all.sh
./clear-cache.sh


