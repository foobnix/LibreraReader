#git clone https://github.com/foobnix/LibreraReader.git --branch 7.12.67
git clone https://github.com/foobnix/LibreraReader.git
git clone --recursive git://git.ghostscript.com/mupdf.git --branch 1.11

mkdir release

cd LibreraReader
git fetch
git reset --hard origin/master
#git pull origin master
cd ..

cd mupdf
make
cd ..

LibreraReader/Builder/link_to_mupdf_1.11.sh mupdf LibreraReader

pwd

cd LibreraReader
./update_all.sh
./update_fix_build.sh

cd Builder
./update_jars.sh


ant clean-apk
ant arm+arm64 pro-fdroid
