#!/usr/bin/env bash

cd ../
./gradlew clean assembleBetaRelease a_copyApks

./remove_all.sh
./install_all.sh