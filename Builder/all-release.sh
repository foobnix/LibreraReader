#!/usr/bin/env bash


./link_to_mupdf_1.11.sh

cd ../
./gradlew clean incVersion

./gradlew assembleOldRelease

cd Builder

./link_to_mupdf_master.sh

cd ../


./gradlew assembleLibreraRelease
./gradlew assembleProRelease
./gradlew assembleEpub_readerRelease
./gradlew assemblePdf_v2Release
./gradlew assembleEbookaRelease
./gradlew assembleTts_readerRelease
./gradlew assemblePdf_classicRelease
./gradlew assembleFdroidRelease
./gradlew assembleHuaweiRelease


./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder

./remove_all.sh
./install_all.sh
./clear-cache.sh
