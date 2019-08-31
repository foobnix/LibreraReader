#!/usr/bin/env bash





cd ../

./gradlew clean incVersion

# ==== BETA MUPDF  1.11 ========
./link_to_mupdf_1.11.sh
./gradlew assembleBetaRelease assembleProRelease

#./gradlew lintBetaRelease assembleBetaRelease assembleProRelease

# ==== BETA MUPDF  1.16.1 ========

./link_to_mupdf_1.16.1.sh.sh
./gradlew assembleBeta2Release



./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh

#dropbox stop
#sleep 10
#dropbox start