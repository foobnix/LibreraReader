./link_to_mupdf_1.11.sh
./copy-fonts.sh

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

ant arm pro
ant arm64 pro
ant arm+arm64 pro
ant x86 pro


./remove_all.sh
./install_all.sh

sh ninja-adb.sh shell pm clear com.foobnix.pro.pdf.reader

ant arm pdf
ant arm64 pdf
ant x86 pdf

ant arm classic
ant arm64 classic
ant x86 classic

ant arm+arm64 droid
ant x86 droid

ant arm+arm64 pdf-ink
ant x86 pdf-ink

ant version

./remove_all.sh
./install_all.sh

