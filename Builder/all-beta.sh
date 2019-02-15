#!/usr/bin/env bash

cd ../

./gradlew --no-daemon  clean incVersion assembleBetaRelease copyApks -Pbeta

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh