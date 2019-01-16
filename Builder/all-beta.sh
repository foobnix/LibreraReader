./link_ant.sh
./link_to_mupdf_1.11.sh
./copy-fonts.sh

ant -f build_index.xml index
ant -f build_index.xml index-beta

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk


ant arm+arm64 tts-reader

#ant beta-cpu pdf-beta

#ant arm+arm64 pdf
#ant x86 pdf

#ant arm+arm64 droid
#ant x86 droid

ant x86
ant version

./remove_all.sh
./install_all.sh
./link_eclipse.sh

cd /home/ivan-dev/Dropbox/FREE_PDF_APK/testing
md5sum *.apk > checksum.txt