#!/usr/bin/env bash
JAVA_HOME=/home/dev/Downloads/AndroidStudio/android-studio-2022.2.1.18-linux/android-studio/jbr/
#./fonts.sh

#Builder folder
./link_to_mupdf_1.21.1.sh

cd ../

./gradlew clean incVersion

./gradlew assembleFdroidRelease
./gradlew assembleProRelease
./gradlew assembleLibreraRelease

#cd Builder
#./link_two_mupdf.sh
#cd ../
#./gradlew assembleProRelease

./gradlew copyApks -Pbeta
./gradlew -stop

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-arm.*

cd Builder
./remove_all.sh
./install_all.sh