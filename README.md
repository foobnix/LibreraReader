![Logo](https://raw.githubusercontent.com/foobnix/LirbiReader/master/logo.jpg)

# Librera Reader

Librera Reader is a book reader and PDF Redaer for Android 
Supprt formats PDF, EPUB, MOBI, DjVu, FB2, TXT, RTF, AZW, AZW3, HTML, CBZ, CBR and OPDS Catalogs

Web: [http://librera.mobi/](http://librera.mobi/)

Android Play market apps:

[Librera](https://play.google.com/store/apps/details?id=com.foobnix.pdf.reader)

[Librera PRO](https://play.google.com/store/apps/details?id=com.foobnix.pro.pdf.reader)

[Arhive .apk](http://archive.librera.mobi)

[Beta(latest) .apk](http://beta.librera.mobi)

[Telegram] (https://t.me/LibreraReader)

## Support intents

intent.putExtra("page", int); //to open doc on page number

intent.putExtra("percent", float); //to open doc page by percent 

intent.putExtra("password", String); //to open password protected PDF

## How to install

Librera is developed on Eclise with ADT (Android Developer Tools Plugin)

INSTALL
0) Linux is prefer (Windows never tested)
1) Install Eclipse
2) Install Android ADT (from the Eclipse markeplace)
3) Install Android NDK
4) Install JAVA
5) Install ANT

DOWNLOAD Librera SOURCE CODE from GITHUB

1) git clone https://github.com/foobnix/LirbiReader.git

2) run ./LirbiReader/update_all.sh
Downlaod required packages (Google Play Service, RecicleView, CardView, etc)

3) Download Mupdf sources tar.gz from [http://mupdf.com/downloads](http://mupdf.com/downloads)

4) Import all projects to Eclipse


## Build

1) Build NDK part
./LirbiReader/Builder/link_to_mupdf.sh

Specify you project path and MUPDF path
MUPDF=/dev/mupdf-test/platform/java
MY=/home/ivan-dev/git/LirbiReader

2) To build Librera Reader
./LirbiReader/Builder/all-beta.sh


## Librera depend on

MuPDF - (AGPL License) https://mupdf.com/downloads/archive/ (mupdf-1.12.0-source.tar.xz)

MuPDF changed source ./LirbiReader/jni-1.12/~mupdf

* EbookDroid
* djvulibre
* hpx
* junrar
* Universal Image Loader
* libmobi
* commons-compress
* eventbus
* greendao
* jsoup
* juniversalchardet
* rtfparserkit
* okhttp

commons-compress-1.14.jar
eventbus-3.0.0.jar
greendao-3.2.0.jar
greendao-api-3.2.0.jar
jsoup-1.8.3.jar
juniversalchardet-1.0.3.jar
okhttp-3.9.1.jar
okhttp-digest-1.15.jar
okio-1.13.0.jar
rtfparserkit-1.10.0.jar

Librera is distributed under the GPL

## License

See the [LICENSE](LICENSE.txt) file for license rights and limitations (GPL v.3).

