#!/usr/bin/env bash

./link_to_mupdf_1.11.sh

cd ../

./gradlew --no-daemon incVersion
./gradlew --no-daemon clean assembleBetaRelease copyApks -Pbeta
./gradlew --no-daemon clean assembleEbookaRelease copyApks -Pbeta

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh