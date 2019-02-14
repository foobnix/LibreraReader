#!/usr/bin/env bash

cd ../
./gradlew --no-daemon  incVersion clean assembleBetaRelease copyApks

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh