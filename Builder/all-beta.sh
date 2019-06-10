#!/usr/bin/env bash

dropbox stop

./link_to_mupdf_1.11.sh

cd ../

./gradlew clean incVersion
./gradlew assembleBetaRelease
#./gradlew assembleEbookaRelease
./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh

dropbox start