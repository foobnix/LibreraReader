#!/usr/bin/env bash

./link_two_mupdf.sh

cd ../

./gradlew clean incVersion

./gradlew assembleOldRelease
./gradlew assembleTts_readerRelease
./gradlew assembleLibreraRelease
./gradlew assembleProRelease
./gradlew assembleEpub_readerRelease
./gradlew assemblePdf_v2Release
./gradlew assembleEbookaRelease
./gradlew assemblePdf_classicRelease

./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder


#rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
#rm /home/dev/Nextcloud/LibreraBeta/*-x86*

./remove_all.sh
./install_all.sh
