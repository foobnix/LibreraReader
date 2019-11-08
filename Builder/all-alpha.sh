#!/usr/bin/env bash


### 1.16.1
./link_to_mupdf_master.sh

cd ../

./gradlew clean incVersion

./gradlew assembleAlphaRelease

./gradlew -stop


./gradlew copyApks -Pbeta
./gradlew -stop


cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh
