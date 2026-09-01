![Logo](https://raw.githubusercontent.com/foobnix/LirbiReader/master/logo.jpg)

**The development and support of Librera is frozen for an unpredictable time, there is a big war in my country
Ukraine.**
[Russian invasion of Ukraine](https://en.wikipedia.org/wiki/2022_Russian_invasion_of_Ukraine)

# Librera Reader

Librera Reader is an e-book reader for Android devices;
it supports the following formats: PDF, EPUB, EPUB3, MOBI, DjVu, FB2, TXT, RTF, AZW, AZW3, HTML, CBZ, CBR, DOC, DOCX,
and OPDS Catalogs

# Download application

[Librera on Google Play](https://play.google.com/store/apps/details?id=com.foobnix.pdf.reader)

[Librera PRO on Google Play](https://play.google.com/store/apps/details?id=com.foobnix.pro.pdf.reader)

[Librera F-Droid](https://f-droid.org/en/packages/com.foobnix.pro.pdf.reader/)


### All Librera Applications

[librera.app](https://librera.app)

![librera](https://raw.githubusercontent.com/foobnix/LibreraReader/refs/heads/master/librera-round-128.png)

[Librera1 Reader (New Librera)](https://librera1.com)



### Links

[FAQ](https://librera.mobi/faq/)

[Telegram Chat](https://t.me/librera_reader_chat)

[Support on Patreon](https://www.patreon.com/librera)

[librera.reader@gmail.com](mailto:librera.reader@gmail.com)

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

## What's new

Full release notes: https://github.com/foobnix/LibreraReader/releases

### 9.5.0 (unreleased)
[APK Direct Download](http://beta.librera.mobi)

Taken from the commits since the 9.4.21 tag; there is no release entry for it yet.

* Upgraded the MuPDF engine to 1.28.3 (Librera patches ported from 1.23.7)
* Text selection now works while "crop white space" is enabled, in both book and scroll modes
* Reading direction (RTL) can now be set in scroll mode, not only in book mode
* Annotating is allowed while crop is enabled
* Fixed reflow when toggling "crop white space"
* Book mode: the clock/battery ticker stops when the status bar is hidden
* New "Show progress slider" setting to toggle the seek bar row
* Improved jump history and "Back" arrow visibility when using the slider
* More consistent brightness values when adjusting by scroll

### 9.4.21

* Fixed text replacement for multiple words.
* Updated the UI for changing text and background colors.
* Added an alert for permanent file deletion.
* Clicking on File Information metadata now navigates to the library.
* Added the "iw" translation and fixed other translations.
* Fixed the grid view widget.
* Fixed the eye reset timer.

### 9.4.8

* All-storage access is optional.
* Apps can use the system file manager to open individual files.
* Fixed choose profile.

### 9.3.75

* Fixed Contrast and Brigtness
* Advanced option to Enable Contrast and Brigtness for all reading mods
* Fixed Chinese lang
* Fixed search in many pdf, epub

### 9.3.63

* Improvements
* Fixes

### 9.3.55

* Fixes
* Librera for macOS, supports PDF, EPUB, FB2, CBZ, CBR (beta.librera.mobi for downloads)

## Android versions

| App Version | Librera PRO  | Librera F-Droid |
|-------------|--------------|-----------------|
| 9.4.21      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.4.8       | 7.0 (API 24) | 7.0 (API 24)    |
| 9.4.5       | 7.0 (API 24) | 7.0 (API 24)    |
| 9.3.75      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.3.63      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.3.55      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.3.35      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.3.19      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.3.10      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.3.1       | 7.0 (API 24) | 7.0 (API 24)    |
| 9.2.40      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.2.31      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.2.21      | 7.0 (API 24) | 7.0 (API 24)    |
| 9.2.4       | 7.0 (API 24) | 7.0 (API 24)    |
| 9.1.35      | 7.0 (API 24) | 5.0 (API 21)    |
| 9.1.29      | 7.0 (API 24) | 5.0 (API 21)    |
| 9.1.14      | 7.0 (API 24) | 4.1 (API 16)    |
| 9.1.7       | 7.0 (API 24) | 4.1 (API 16)    |
| 9.0.5       | 7.0 (API 24) | 4.1 (API 16)    |
| 8.9.182     | 5.0 (API 21) | 4.1 (API 16)    |

## Librera depends on

| Library    | Version          | License              |
|------------|------------------|----------------------|
| MuPDF      | 1.28.3 (1.23.7)  | AGPL-3.0             |
| DjVuLibre  | 3.5.28           | GPL-2.0-or-later     |
| antiword   | 1.3.1            | GPL-2.0              |
| libmobi    | 0.12             | LGPL-3.0-or-later    |
| hqx        | 1.2              | LGPL-2.1-or-later    |
| LAME       | 3.100            | LGPL-2.0-or-later    |
| libwebp    | 1.3.2            | BSD-3-Clause         |

| Component                                 | License                       |
|-------------------------------------------|-------------------------------|
| EBookDroid (`org.ebookdroid`, `org.emdev`) | GPL-3.0 <sup>1</sup>          |
| wmf2svg (`net.arnx.wmf2svg`)               | Apache-2.0                    |
| PBKDF2 (`de.rtner`)                        | LGPL-2.1-or-later             |
| opendocument.java (`at.stefl`)             | not declared <sup>1</sup>     |
| DragLinearLayout (`com.jmedeisis`)         | Apache-2.0 <sup>1</sup>       |
| HSV colour picker (`com.buzzingandroid`)   | Apache-2.0 <sup>1</sup>       |
| android-lame JNI (`com.github.axet`)       | Apache-2.0 <sup>1</sup>       |

| Dependency                                  | Version        | License                     |
|---------------------------------------------|----------------|-----------------------------|
| androidx.appcompat:appcompat                | 1.7.1          | Apache-2.0                  |
| androidx.cardview:cardview                  | 1.0.0          | Apache-2.0                  |
| androidx.legacy:legacy-support-v4           | 1.0.0          | Apache-2.0                  |
| androidx.multidex:multidex                  | 2.0.1          | Apache-2.0                  |
| androidx.recyclerview:recyclerview          | 1.4.0          | Apache-2.0                  |
| androidx.work:work-runtime                  | 2.11.2         | Apache-2.0                  |
| org.greenrobot:eventbus                     | 3.3.1          | Apache-2.0                  |
| org.greenrobot:greendao                     | 3.3.0          | Apache-2.0                  |
| org.greenrobot:greendao-api                 | 3.3.0          | Apache-2.0                  |
| org.jsoup:jsoup                             | 1.22.2         | MIT                         |
| com.github.albfernandez:juniversalchardet   | 2.5.0          | MPL-1.1 / GPL-3.0 / LGPL-3.0 |
| com.squareup.okhttp3:okhttp                 | 3.12.6         | Apache-2.0                  |
| io.github.rburgst:okhttp-digest             | 3.1.1          | Apache-2.0                  |
| com.squareup.okio:okio                      | 1.17.6         | Apache-2.0                  |
| com.github.joniles:rtfparserkit             | 1.16.0         | Apache-2.0                  |
| org.zwobble.mammoth:mammoth                 | 1.5.0          | BSD-2-Clause                |
| javax.xml.stream:stax-api                   | 1.0-2          | CDDL-1.0 / GPL-2.0          |
| net.lingala.zip4j:zip4j                     | 2.11.6         | Apache-2.0                  |
| com.github.bumptech.glide:glide             | 4.16.0         | BSD-2-Clause and Apache-2.0 |
| commons-logging:commons-logging-api         | 1.1            | Apache-2.0                  |
| com.google.guava:guava                      | 33.6.0-android | Apache-2.0                  |
| org.commonmark:commonmark                   | 0.29.0         | BSD-2-Clause                |
| org.commonmark:commonmark-ext-gfm-tables    | 0.29.0         | BSD-2-Clause                |
| com.github.junrar:junrar                       | 7.6.0                    | UnRar License                    |

Librera is distributed under the GPL v3

## License

See the [LICENSE](LICENSE.txt) file for license rights and limitations (GPL v.3).
