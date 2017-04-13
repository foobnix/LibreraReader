ant clean-apk


rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/PDF\ Reader-*
rm /home/ivan-dev/Dropbox/Nexus/AppVer/*

ant armeabi-v7a classic
ant x86 classic

./remove_all.sh
./install_all.sh

./link_lang.sh
./zip-source.sh

rm /home/ivan-dev/Dropbox/Nexus/AppVer/*-x86.apk