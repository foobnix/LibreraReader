#PATH=/home/papa/dev/jdk1.8.0_45/bin:$PATH
#PATH=/home/papa/dev/apache-ant-1.9.4/bin:$PATH
#PATH=/home/papa/dev/android-sdk-linux/platform-tools:$PATH

./inc-index.sh

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

#./link_to_mupdf_1.12.sh
#ant arm+arm64 pdf-beta-112

#./link_to_mupdf_1.11.sh
#ant arm+arm64 pdf-beta-111


./link_to_mupdf_1.11.sh
ant arm+arm64 pdf-beta
ant arm pdf-beta
#ant x86 pdf-beta

ant x86
ant version

./remove_all.sh
./install_all.sh
