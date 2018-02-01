#PATH=/home/papa/dev/jdk1.8.0_45/bin:$PATH
#PATH=/home/papa/dev/apache-ant-1.9.4/bin:$PATH
#PATH=/home/papa/dev/android-sdk-linux/platform-tools:$PATH

./link_to_mupdf_1.11.sh
./inc-index.sh

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

ant arm+arm64 pdf-beta
#ant arm64 pdf-beta
#ant x86 pdf-beta

ant x86

ant version

./remove_all.sh
./install_all.sh
