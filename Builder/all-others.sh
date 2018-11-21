./link_ant.sh
./link_to_mupdf_1.11.sh
./copy-fonts.sh

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk


ant arm+arm64 ebooka
ant x86 ebooka

ant arm+arm64 pdf-v2
ant x86 pdf-v2

ant x86
ant version

./remove_all.sh
./install_all.sh
./link_eclipse.sh

cd /home/ivan-dev/Dropbox/FREE_PDF_APK/testing
md5sum *.apk > checksum.txt