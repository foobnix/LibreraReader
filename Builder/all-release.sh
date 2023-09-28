#!/usr/bin/env bash

./fonts.sh

./link_to_mupdf_1.23.3.sh

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

cd Builder
link_to_mupdf_1.11.sh

cd ../

./gradlew assembleTts_readerRelease
./gradlew assembleEpub_readerRelease

./gradlew copyApks -Pbeta -Prelesae
./gradlew -stop

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-arm.*

rm /Users/dev/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /Users/dev/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-arm.*

cd Builder
./remove_all.sh
./install_all.sh
