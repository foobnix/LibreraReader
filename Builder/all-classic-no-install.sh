./inc-index.sh

ant clean-apk


rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk


ant armeabi-v7a classic
ant x86 classic

./link_lang.sh
./zip-source.sh