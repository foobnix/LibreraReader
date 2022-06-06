#!/usr/bin/env bash


### 1.20.0
./link_to_mupdf_1.20.0.sh

cd ../

./gradlew clean incVersion

#./gradlew assembleFdroidRelease
./gradlew assembleAlphaRelease
#./gradlew assembleLibreraRelease


### 1.11.1

cd Builder
./link_to_mupdf_1.11.sh

cd ../
./gradlew assembleProRelease
./gradlew assembleBetaRelease
#./gradlew assembleLibreraRelease
#./gradlew assembleHuaweiRelease

./gradlew copyApks -Pbeta
./gradlew -stop


cd Builder

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-arm*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*



./remove_all.sh
./install_all.sh
./clear-cache.sh


