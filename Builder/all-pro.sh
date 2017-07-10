./inc-index.sh

ant clean-apk

ant x86 pro
#ant armeabi pro
ant armeabi-v7a pro

./remove_all.sh
./install_all.sh


./link_lang.sh
./zip-source.sh
