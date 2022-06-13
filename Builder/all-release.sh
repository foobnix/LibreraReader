#!/usr/bin/env bash

#/home/ivan-dev/git/LibreraReader/Builder
### 1.11.1
./link_to_mupdf_1.11.sh

cd ../

./gradlew clean incVersion
./gradlew assembleLibreraRelease
./gradlew assembleProRelease
./gradlew assembleEpub_readerRelease

### 1.20.0
cd Builder
./link_to_mupdf_1.20.0.sh
cd ../
./gradlew assemblePdf_v2Release
./gradlew assembleEbookaRelease
./gradlew assembleTts_readerRelease
./gradlew assemblePdf_classicRelease
./gradlew assembleFdroidRelease

./gradlew copyApks -Pbeta
./gradlew -stop


cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh
