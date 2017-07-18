./inc-index.sh

ant clean-apk


rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

ant armeabi-v7a classic
ant x86 classic

./remove_all.sh
./install_all.sh

sh ninja-adb.sh shell pm clear classic.pdf.reader.viewer.djvu.epub.fb2.txt.mobi.book.reader.lirbi.libri

./link_lang.sh
./zip-source.sh