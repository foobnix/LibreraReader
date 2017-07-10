#PATH=/home/papa/dev/jdk1.8.0_45/bin:$PATH
#PATH=/home/papa/dev/apache-ant-1.9.4/bin:$PATH
#PATH=/home/papa/dev/android-sdk-linux/platform-tools:$PATH

./inc-index.sh

ant clean-apk

ant x86 droid
ant armeabi-v7a droid


ant x86 pro
ant armeabi-v7a pro

ant x86 pdf
ant armeabi-v7a pdf

ant armeabi-v7a classic
ant x86 classic

./remove_all.sh
./install_all.sh


./link_lang.sh
./zip-source.sh