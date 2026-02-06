![Logo](https://raw.githubusercontent.com/foobnix/LirbiReader/master/logo.jpg)

**The development and support of Librera is frozen for an unpredictable time, there is a big war in my country
Ukraine.**
[Russian invasion of Ukraine](https://en.wikipedia.org/wiki/2022_Russian_invasion_of_Ukraine)

🇺🇦 To help Ukraine, please donate to these funds 💙💛

[OFFICIAL FUNDRAISING PLATFORM OF UKRAINE](https://u24.gov.ua/)

[Повернись Живим - Come Back Alive](https://savelife.in.ua/en/)

[Фонд Сергія Стерненка - Foundation Sternenko Community](https://www.sternenkofund.org/en)

[Фонд Сергія Притули - Serhiy Prytula Charity Foundation](https://prytulafoundation.org/en/)

# Librera Reader

Librera Reader is an e-book reader for Android devices;
it supports the following formats: PDF, EPUB, EPUB3, MOBI, DjVu, FB2, TXT, RTF, AZW, AZW3, HTML, CBZ, CBR, DOC, DOCX,
and OPDS Catalogs

# Download application

[Librera Reader on Google Play](https://play.google.com/store/apps/details?id=com.foobnix.pdf.reader)

[Librera PRO on Google Play](https://play.google.com/store/apps/details?id=com.foobnix.pro.pdf.reader)

[Librera F-Droid](https://f-droid.org/en/packages/com.foobnix.pro.pdf.reader/)

[Beta testing .apk](http://beta.librera.mobi/)

[Application Fonts.zip](https://github.com/foobnix/LirbiReader/tree/master/Builder/fonts)


[Web browser Librera Book Reader](https://librera.mobi/online-book-reader/)

https://librera.mobi/online-book-reader/?file=https://pdfobject.com/pdf/sample.pdf

[Google Chrome Extension](https://chromewebstore.google.com/detail/epub-reader-librera/kfpiokccdkdlbjmgiajpfcdefcbdbphe)

[zip Chrome Extension](https://github.com/foobnix/LibreraReader/raw/master/Builder/librara-chrome-extension.zip)

[MacOS DMG / Desktop version](http://beta.librera.mobi/)

Librera for macOS, supports PDF, EPUB, FB2, CBZ, CBR (beta.librera.mobi for downloads)

![img1](https://raw.githubusercontent.com/foobnix/LirbiReader/master/iosOS/img1.png)

![img2](https://raw.githubusercontent.com/foobnix/LirbiReader/master/iosOS/img2.png)

### Links

[web: https://librera.mobi/](https://librera.mobi/)

[What is new/Changes](https://librera.mobi/what-is-new/)

[FAQ](https://librera.mobi/faq/)

[Telegram Info](https://t.me/LibreraReader)

[Telegram Chat](https://t.me/librera_reader_chat)

♥︎♥︎♥︎ [Support & Donation on Patreon](https://www.patreon.com/librera) ♥︎♥︎♥︎

[Email: librera.reader@gmail.com](mailto:librera.reader@gmail.com)

## Required build libs

~~~~
mesa-common-dev libxcursor-dev libxrandr-dev libxinerama-dev libglu1-mesa-dev libxi-dev pkg-config libgl-dev
~~~~

You also need the Android NDK in version 20+
Please ensure to download it using android studio and add the NDK to your PATH.

## Create a keystore

Even if you do not plan to upload a version yourself you need a keystore with a certificate to build.
The keystore needs to be in PKCS12 format.
You can create a keystore in your actual directory using the following call
(replace ALIAS by your alias, it is just a name):

~~~~
keytool -genkey -v -storetype PKCS12 -keystore keystore.pkcs12 -alias ALIAS -keyalg RSA -keysize 2048 -validity 10000
~~~~

Now edit or create the file ~/.gradle/gradle.properties and set following values
(replacing PASSWD by the password you typed while creating the keystore, ALIAS as before and using the path to your
keystore):

~~~~
RELEASE_STORE_FILE=/PATH/TO/YOUR/keystore.pkcs12
RELEASE_STORE_PASSWORD=PASSWD
RELEASE_KEY_PASSWORD=PASSWD
RELEASE_KEY_ALIAS=ALIAS
~~~~

## Create Firebase Authentication file

To build with firebase support (all version but the ones for Fdroid) you need to get an
authentication file for firebase services offered by google. Therefore please follow
https://firebase.google.com/docs/android/setup to create your own project. You need to
register for the packages com.foobnix.pdf.info and com.foobnix.pdf.reader.a1. This way
you will get a google-services.json file that you have to place in the app folder of
the repository.

For this project only Analytics is used, so a spakling plan is all you need.

## Librera Build on MuPdf

~~~~
cd Builder
./link_to_mupdf_x.x.x.sh (Change the paths to mupdf and jniLibs folders)
cd ..
./gradlew assembleLibrera
~~~~

## Building for F-Droid for Android

If you wish to build for F-Droid (e.g. not using google services, Internet) you can run the build with

~~~~
cd Builder
./link_to_mupdf_x.x.x.sh
cd ..
./gradlew assembleFdroid
~~~~

F-Droid build does also not need a **google-services.json**

## Librera depends on:

MuPDF - (AGPL License) https://github.com/ArtifexSoftware/mupdf

* ebookdroid
* djvulibre
* hpx
* junrar
* glide
* libmobi
* commons-compress
* eventbus
* greendao
* jsoup
* juniversalchardet
* commons-compress
* okhttp3
* okhttp-digest
* okio
* rtfparserkit
* java-mammoth
* zip4j

Librera is distributed under the GPL

## License

See the [LICENSE](LICENSE.txt) file for license rights and limitations (GPL v.3).
