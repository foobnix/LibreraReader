#!/usr/bin/env bash
#./fonts.sh

#Builder folder
./link_to_mupdf_1.23.3.sh

java -version

cd ../

./gradlew clean incVersion

./gradlew assembleFdroidRelease
./gradlew assembleProRelease
#./gradlew assembleLibreraRelease

#cd Builder
#./link_two_mupdf.sh
#cd ../
#./gradlew assembleProRelease

./gradlew copyApks -Pbeta
./gradlew -stop

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-arm.*

rm /Users/dev/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /Users/dev/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-arm.*

cd Builder
./remove_all.sh
./install_all.sh