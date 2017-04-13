# Lirbi Reader

Lirbi Reader is a book reader and PDF Redaer for Android 
Supprt formats PDF, EPUB, MOBI, DjVu, FB2, TXT, RTF, AZW, AZW3, HTML, CBZ, CBR

Web: [http://lirbi.com/](http://lirbi.com/)

Android Play market apps:

[Lirbi Readear](https://play.google.com/store/apps/details?id=com.foobnix.pdf.reader)
[PRO Lirbi Readear](https://play.google.com/store/apps/details?id=com.foobnix.pro.pdf.reader)
[PDF Reader Clasic](https://play.google.com/store/apps/details?id=classic.pdf.reader.viewer.djvu.epub.fb2.txt.mobi.book.reader.lirbi.libri)

## How to install

Lirbi is developed on Eclise with ADT

INSTALL IDE

1) Install Eclipse
2) Install Android ADT
3) Install Android NDK
4) Install JAVA
5) Install ANT

DOWNLOAD LIRBI SOURCE CODE from GITHUB

1) git clone https://github.com/foobnix/LirbiReader.git

2) run ./pdf4/update_all.sh
Downlaod required packages (Google Play Service, RecicleView, CardView libraries ect)

3) Download Mupdf sources tar.gz from [http://mupdf.com/downloads](http://mupdf.com/downloads)

4) import all projects to Eclipse


## Build

1) Build NDK part
./pdf4/Builder/link_to_mupdf.sh

Specify you project path and MUPDF path
MUPDF=/dev/mupdf-test/platform/java
MY=/home/ivan-dev/git/pdf4

2) To build Lirbi reader
./pdf4/Builder/all-lirbi-free.sh

3) To build Lirbi PRO
./pdf4/Builder/all-pro.sh

4) To build PDF Classic
./pdf4/Builder/all-classic.sh

## Lirbi depend on

MuPDF - http://www.mupdf.com

EbookDroid
djvulibre
hpx

libmobi
commons-compress
eventbus
greendao
jsoup
juniversalchardet
rtfparserkit

Lirbi is distributed under the GPL

## License

See the [LICENSE](LICENSE.txt) file for license rights and limitations (GPL v.3).

