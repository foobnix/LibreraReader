#!/usr/bin/env bash



./link_to_mupdf_1.11.sh

cd ../

./gradlew clean incVersion

#./gradlew lintBetaRelease
./gradlew lintBetaRelease assembleBetaRelease
#./gradlew assembleProRelease


./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh

#dropbox stop
#sleep 10
#dropbox start