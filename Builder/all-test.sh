ant clean-apk


rm /mount/extHDD/help/Dropbox/FREE_PDF_APK/testing/PDF\ Reader-*
rm /mount/extHDD/help/Dropbox/Nexus/AppVer/*

ant armeabi-v7a test

./remove_all.sh
./install_all.sh

./link_lang.sh
./zip-source.sh

rm /mount/extHDD/help/Dropbox/Nexus/AppVer/*-x86.apk