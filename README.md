![Logo](https://raw.githubusercontent.com/foobnix/LirbiReader/master/logo.jpg)

# Librera Reader

**The development and support of Librera is frozen for an unpredictable time, there is a big war in my country.**
[Russian invasion of Ukraine](https://en.wikipedia.org/wiki/2022_Russian_invasion_of_Ukraine)

Librera Reader is an e-book reader for Android devices; 
it supports the following formats: PDF, EPUB, EPUB3, MOBI, DjVu, FB2, TXT, RTF, AZW, AZW3, HTML, CBZ, CBR, DOC, DOCX, and OPDS Catalogs

Web: [http://librera.mobi/](http://librera.mobi/)

FAQ: [Read](http://librera.mobi/faq/)

Android Play Market Apps:

[Librera](https://play.google.com/store/apps/details?id=com.foobnix.pdf.reader)

[Librera PRO](https://play.google.com/store/apps/details?id=com.foobnix.pro.pdf.reader)

Application Fonts (**fonts.zip** download to internal sd card, to [Downloads] folder)
[link](https://github.com/foobnix/LirbiReader/tree/master/Builder/fonts) 

[Telegram](https://t.me/LibreraReader)

[Telegram Download Beta apk](https://t.me/LibreraBeta)

[Beta .apk (latest build)](http://beta.librera.mobi)

[Please support Librera on Patreon](https://www.patreon.com/librera)


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
(replacing PASSWD by the password you typed while creating the keystore, ALIAS as before and using the path to your keystore):

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

## Librera Build on MuPdf 1.11 (Default)

~~~~
cd Builder
./link_to_mupdf_1.11.sh (Change the paths to mupdf and jniLibs folders)
./gradlew assembleLibrera
~~~~

## Librera Build on MuPdf 1.20.0 (Testing, F-Droid)

~~~~
cd Builder
./link_to_mupdf_1.20.0.sh
./gradlew assembleAlpha
~~~~

## Building for F-Droid

If you wish to build for F-Droid (e.g. not using google services) you can run the build with

~~~~
cd Builder
./link_to_mupdf_1.20.0.sh
./gradlew assembleFdroid
~~~~

F-Droid build does also not need a **google-services.json**

## Librera depends on:

MuPDF - (AGPL License) https://mupdf.com/downloads/archive/

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
