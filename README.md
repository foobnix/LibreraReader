# Librera Reader

Librera Reader is a book reader and PDF Redaer for Android 
Supprt formats PDF, EPUB, MOBI, DjVu, FB2, TXT, RTF, AZW, AZW3, HTML, CBZ, CBR

Web: [http://librera.mobi/](http://librera.mobi/)

Android Play market apps:

[Librera](https://play.google.com/store/apps/details?id=com.foobnix.pdf.reader)

[Librera PRO](https://play.google.com/store/apps/details?id=com.foobnix.pro.pdf.reader)

[Arhive .apk](http://archive.librera.mobi)

[Beta(latest) .apk](http://beta.librera.mobi)

## How to install

Librera is developed on Eclise with ADT

INSTALL IDE

1) Install Eclipse
2) Install Android ADT
3) Install Android NDK
4) Install JAVA
5) Install ANT

DOWNLOAD Librera SOURCE CODE from GITHUB

1) git clone https://github.com/foobnix/LirbiReader.git

2) run ./LirbiReader/update_all.sh
Downlaod required packages (Google Play Service, RecicleView, CardView, etc)

3) Download Mupdf sources tar.gz from [http://mupdf.com/downloads](http://mupdf.com/downloads)

4) import all projects to Eclipse


## Build

1) Build NDK part
./LirbiReader/Builder/link_to_mupdf.sh

Specify you project path and MUPDF path
MUPDF=/dev/mupdf-test/platform/java
MY=/home/ivan-dev/git/LirbiReader

2) To build Librera Reader
./LirbiReader/Builder/all-lirbi-free.sh

3) To build Librera PRO
./LirbiReader/Builder/all-pro.sh

4) To build PDF Classic
./LirbiReader/Builder/all-classic.sh

## Librera depend on

MuPDF - (AGPL License) http://git.ghostscript.com/?p=mupdf.git;a=commit;h=0628a0b3d166543dbc1c346790014ff39ccf76b8

Librera Patch ./LirbiReader/Builder/jni/MupdfPatch.txt

Mupdf patch source ./LirbiReader/mupdf-source.tar.gz

EbookDroid
djvulibre
hpx

junrar
Universal Image Loader
libmobi
commons-compress
eventbus
greendao
jsoup
juniversalchardet
rtfparserkit

Librera is distributed under the GPL

## License

See the [LICENSE](LICENSE.txt) file for license rights and limitations (GPL v.3).

