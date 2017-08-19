#PATH=/home/papa/dev/jdk1.8.0_45/bin:$PATH
#PATH=/home/papa/dev/apache-ant-1.9.4/bin:$PATH
#PATH=/home/papa/dev/android-sdk-linux/platform-tools:$PATH

./inc-index.sh

ant clean-apk

./remove_all.sh

ant armeabi-v7a pdf
ant x86 pdf

./install_all.sh
./link_lang.sh