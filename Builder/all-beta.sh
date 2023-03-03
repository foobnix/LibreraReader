#!/usr/bin/env bash

./link_to_mupdf_1.21.1.sh

cd ../

./gradlew clean incVersion

./gradlew assembleFdroidRelease
./gradlew assembleLibreraRelease

cd Builder


./link_two_mupdf.sh

cd ../

./gradlew assembleProRelease

./gradlew copyApks -Pbeta
./gradlew -stop


#rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
#rm /home/dev/Nextcloud/LibreraBeta/*-x86*

cd Builder
./remove_all.sh
./install_all.sh