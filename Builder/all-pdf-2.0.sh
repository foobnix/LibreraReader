ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

ant arm+arm64 pdf-ink

./remove_all.sh
./install_all.sh
