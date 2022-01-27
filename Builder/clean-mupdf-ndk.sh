BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

whereis ndk-build
echo "================== "

cd ${BUILD_DIR}/mupdf-1.11
make clean
cd ${BUILD_DIR}/mupdf-1.11/platform/librera
ndk-build clean

echo "================== "

cd ${BUILD_DIR}/mupdf-1.16.1
make clean
cd ${BUILD_DIR}/mupdf-1.16.1/platform/librera
ndk-build clean

echo "================== "

cd ${BUILD_DIR}/mupdf-1.19.0
make clean
cd ${BUILD_DIR}/mupdf-1.19.0/platform/librera
ndk-build clean

echo "================== "

link_to_mupdf_1.11.sh &
link_to_mupdf_1.16.1.sh &
link_to_mupdf_1.19.0.sh &
