#!/usr/bin/env bash


### 1.16.1
./link_to_mupdf_1.11.sh

cd ../

./gradlew clean incVersion

#./gradlew assembleBetaRelease
#./gradlew assemblePdf_v2Release
#./gradlew assembleEbookaRelease
#./gradlew assembleTts_readerRelease

./gradlew assembleLibreraRelease
./gradlew assembleProRelease

#./gradlew assembleFdroidRelease


./gradlew copyApks -Pbeta
./gradlew -stop


cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh
