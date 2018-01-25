#PATH=/home/papa/dev/jdk1.8.0_45/bin:$PATH
#PATH=/home/papa/dev/apache-ant-1.9.4/bin:$PATH
#PATH=/home/papa/dev/android-sdk-linux/platform-tools:$PATH

./inc-index.sh

ant clean-apk

rm /home/ivan-dev/Dropbox/FREE_PDF_APK/testing/*.apk

ant arm pro
ant arm64 pro
ant x86 pro

./remove_all.sh
./install_all.sh

ant arm pdf
ant arm64 pdf
ant x86 pdf

ant arm classic
ant arm64 classic
ant x86 classic

ant arm droid
ant arm64 droid
ant x86 droid

ant arm pdf-ink
ant arm64 pdf-ink
ant x86 pdf-ink

ant version

./remove_all.sh
./install_all.sh