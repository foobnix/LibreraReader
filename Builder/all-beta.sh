#!/usr/bin/env bash

./link_merge.sh

cd ../

./gradlew clean incVersion

./gradlew assembleLibreraRelease
./gradlew assembleProRelease
./gradlew assembleFdroidRelease

./gradlew copyApks -Pbeta
./gradlew -stop

cd Builder


#rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
#rm /home/dev/Nextcloud/LibreraBeta/*-x86*

./remove_all.sh
./install_all.sh
./clear-cache.sh