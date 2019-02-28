#!/usr/bin/env bash

./link_to_mupdf_1.11.sh

cd ../

./gradlew clean incVersion

./gradlew assembleProRelease
./gradlew assembleFdroidRelease

./gradlew assembleLibreraRelease
./gradlew assemblePdf_classicRelease
./gradlew assembleEpub_readerRelease
./gradlew assembleEbookaRelease
./gradlew assemblePdf_v2Release
./gradlew assembleTts_readerRelease

./gradlew assembleFdroidRelease

./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh