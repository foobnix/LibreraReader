#!/usr/bin/env bash
#./fonts.sh

#/usr/libexec/java_home -V
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=`/usr/libexec/java_home -v 24`
else
  export JAVA_HOME=/home/dev/.local/share/JetBrains/Toolbox/apps/android-studio/jbr
fi


./link_to_mupdf_1.23.7.sh

cd ../

./gradlew clean incVersion
./gradlew assembleProRelease
./gradlew assembleLibreraRelease
./gradlew assembleFdroidRelease

####################################

./gradlew copyApks -Pbeta
./gradlew -stop

####################################

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /home/dev/Dropbox/FREE_PDF_APK/testing/*-arm.apk

rm /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-x86*
rm /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-arm.apk

cd Builder
./remove_all.sh
./install_all.sh