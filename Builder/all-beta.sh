ant -f build_index.xml index
ant -f build_index.xml index-beta

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

#./link_to_mupdf_1.12.sh
#ant arm+arm64 pdf-beta-112

#./link_to_mupdf_1.11.sh
#ant arm+arm64 pdf-beta-111


./link_to_mupdf_1.11.sh
#ant arm pdf-beta

ant arm+arm64 pdf-beta
ant arm+arm64 pdf
ant arm+arm64 pro

ant x86 pdf

ant x86
ant version

./remove_all.sh
./install_all.sh

#./copy-fonts.sh