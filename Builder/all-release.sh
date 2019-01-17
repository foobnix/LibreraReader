./link_ant.sh
./link_to_mupdf_1.11.sh
./copy-fonts.sh

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

ant arm+arm64 pro
ant x86 pro

ant arm pro
ant arm64 pro

ant arm pdf
ant arm64 pdf
ant x86 pdf

ant arm classic
ant arm64 classic

ant arm ebooka
ant arm64 ebooka

ant arm pdf-v2
ant arm64 pdf-v2

ant arm tts-reader
ant arm64 tts-reader

ant x86

./remove_all.sh
./install_all.sh
./link_eclipse.sh

cd /home/ivan-dev/Dropbox/FREE_PDF_APK/testing
md5sum *.apk > checksum.txt