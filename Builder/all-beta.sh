#PATH=/home/papa/dev/jdk1.8.0_45/bin:$PATH
#PATH=/home/papa/dev/apache-ant-1.9.4/bin:$PATH
#PATH=/home/papa/dev/android-sdk-linux/platform-tools:$PATH

./inc-index.sh
./link_lang.sh

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

ant armeabi-v7a pdf-beta
ant x86 pdf-beta

#ant armeabi-v7a pdf-ink
#ant x86 pdf-ink

ant version

./remove_all.sh
./install_all.sh
