./inc-index.sh

ant clean-apk

ant arm droid
ant arm64 droid
ant x86 droid

./remove_all.sh
./install_all.sh