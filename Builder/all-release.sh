#!/usr/bin/env bash

./fonts.sh

./link_to_mupdf_1.21.1.sh

cd ../

./gradlew clean incVersion

./gradlew assembleLibreraRelease
./gradlew assemblePdf_v2Release
./gradlew assembleEbookaRelease
./gradlew assemblePdf_classicRelease
./gradlew assembleFdroidRelease
./gradlew assembleTts_readerRelease
./gradlew assembleEpub_readerRelease
./gradlew assembleProRelease

./gradlew copyApks -Pbeta
./gradlew -stop

#rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
#rm /home/dev/Nextcloud/LibreraBeta/*-x86*

cd Builder
./remove_all.sh
./install_all.sh
