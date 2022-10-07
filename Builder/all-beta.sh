#!/usr/bin/env bash


### Librera Old
./link_to_mupdf_1.11.sh

cd ../

./gradlew clean incVersion

./gradlew assembleOldRelease


### Librera Beta

cd Builder

#./link_to_mupdf_master.sh
./link_to_mupdf_1.20.2.sh

cd ../

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


