./inc-index.sh

ant clean-apk

./link_to_mupdf_1.11.sh

ant arm+arm64 droid
ant x86 droid

./remove_all.sh
./install_all.sh