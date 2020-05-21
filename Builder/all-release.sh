#!/usr/bin/env bash

#/home/ivan-dev/git/LibreraReader/Builder
### 1.11.1
./link_to_mupdf_1.11.sh

cd ../

./gradlew clean incVersion

./gradlew assembleProRelease
/.gradlew assembleBetaRelease
./gradlew assemblePdf_v2Release
./gradlew assembleEbookaRelease
./gradlew assembleTts_readerRelease
./gradlew assembleLibreraRelease
./gradlew assemblePdf_classicRelease
./gradlew assembleEpub_readerRelease
./gradlew assembleHuaweiRelease


### 1.16.1
cd Builder
./link_to_mupdf_1.16.1.sh
cd ../
./gradlew assembleAlphaRelease
./gradlew assembleFdroidRelease


./gradlew copyApks -Pbeta
./gradlew -stop


cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh
