#!/usr/bin/env bash


./link_to_mupdf_master.sh

cd ../

./gradlew clean incVersion

./gradlew assembleAlphaRelease

./gradlew copyApks -Pbeta
./gradlew -stop


cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh
