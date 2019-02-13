#!/usr/bin/env bash

cd ../
./gradlew --no-daemon  clean assembleBetaRelease a_copyApks

cd Builder
./remove_all.sh
./install_all.sh
./clear-cache.sh