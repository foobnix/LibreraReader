#!/usr/bin/env bash


### 1.16.1
./link_to_mupdf_1.16.1.sh

cd ../

./gradlew clean incVersion

./gradlew assembleFdroidRelease
./gradlew assembleAlphaRelease


### 1.11.1

cd Builder
./link_to_mupdf_1.11.sh

cd ../
./gradlew assembleProRelease
./gradlew assembleBetaRelease
./gradlew assembleHuaweiRelease

./gradlew copyApks -Pbeta
./gradlew -stop


cd Builder

rm /home/dev/Dropbox/FREE_PDF_APK/testing/Librera\ Pro-*-arm*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/Librera\ Pro-*-x*

rm /home/dev/Nextcloud/LibreraBeta/Librera\ Pro-*-arm*
rm /home/dev/Nextcloud/LibreraBeta/Librera\ Pro-*-x*


./remove_all.sh
./install_all.sh
./clear-cache.sh


