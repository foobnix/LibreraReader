#!/usr/bin/env bash
#./fonts.sh

#/usr/libexec/java_home -V
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=`/usr/libexec/java_home -v 17`
else
  export JAVA_HOME=/home/dev/.local/share/JetBrains/Toolbox/apps/android-studio/jbr
fi
####################################

git pull


./link_to_mupdf_1.23.7.sh fdroid
cd ../



./gradlew clean incVersion
./gradlew assembleFdroidRelease

####################################

cd Builder
./link_to_mupdf_1.23.7.sh
cd ../

./gradlew assembleProRelease

####################################

./gradlew copyApks -Pbeta
./gradlew -stop

####################################

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-arm.apk

rm /Users/dev/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /Users/dev/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-arm.apk

cd Builder
./remove_all.sh
./install_all.sh