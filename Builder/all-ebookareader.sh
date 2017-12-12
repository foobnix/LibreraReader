./inc-index.sh

ant clean-apk

ant x86 droid
ant armeabi-v7a droid

./remove_all.sh
./install_all.sh