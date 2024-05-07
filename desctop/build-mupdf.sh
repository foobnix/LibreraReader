dir=$PWD

cd /Volumes/SSD-USB/git2/mupdf-1.23.11

sed -i -e 's/1.7/1.8/g' /Volumes/SSD-USB/git2/mupdf-1.23.11/platform/java/Makefile

make genarate
make java

cp /Volumes/SSD-USB/git2/mupdf-1.23.11/build/java/release/libmupdf_java64.jnilib $dir/build/libs