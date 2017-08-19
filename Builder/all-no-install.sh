./inc-index.sh

ant clean-apk


rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk


ant armeabi-v7a pdf
ant x86 pdf

./link_lang.sh
./zip-source.sh