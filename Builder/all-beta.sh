#!/usr/bin/env bash



./link_to_mupdf_1.16.1.sh

cd ../

./gradlew clean incVersion


./gradlew assembleBetaRelease
./gradlew assembleProRelease

#./gradlew lintBetaRelease assembleBetaRelease assembleProRelease


./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.s

#dropbox stop
#sleep 10
#dropbox start