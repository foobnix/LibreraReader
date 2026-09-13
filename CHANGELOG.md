# Changelog

All notable changes to Librera Reader.

Release builds: https://github.com/foobnix/LibreraReader/releases · [Unreleased APK Direct Download](http://beta.librera.mobi)

## 9.6.7

* New design: floating rounded tabs, round buttons, rounded dialogs and menus, new theme colours.
* Redesigned settings, book info and TTS dialogs.
* Library button and one-tap Vertical/Paged switch in the reader.
* BBCode formatting in books.
* Bookmarks and reading position stay on the right page after font changes and sync.
* Favourites use a heart.

## 9.5.7 (2026-09-02)

**Android Auto**

* Librera now appears as a media app in Android Auto
* Browse recent books in the car, with covers, titles and authors
* Selecting a book resumes it at its saved position
* Playback controls work from the car, the lock screen and Bluetooth

**Text-to-speech**

* Uses Android's standard media notification, with cover art, a progress bar and lock screen controls
* Fixed no speech when the system default engine points at an uninstalled package
* Fixed the notification disappearing, or staying stuck on "please wait", after a long time in the background
* Fixed audio focus not being released on pause, which left other apps muted
* Fixed a crash when requesting the notification permission from the TTS controls

**Reading**

* Upgraded the MuPDF engine to 1.28.3 (Librera patches ported from 1.23.7)
* Text selection now works while "crop white space" is enabled, in both book and scroll modes
* Reading direction (RTL) can now be set in scroll mode, not only in book mode
* Annotating is allowed while crop is enabled
* Fixed reflow when toggling "crop white space"
* Book mode: the clock/battery ticker stops when the status bar is hidden
* New "Show progress slider" setting to toggle the seek bar row
* Improved jump history and "Back" arrow visibility when using the slider
* More consistent brightness values when adjusting by scroll
* Fixes for formatted .txt files

**Google Drive sync**

* Much faster: smaller listing requests, parallel downloads and a change check that skips work when nothing changed
* Deleting a book now propagates: it no longer comes back from another device
* Fixed downloads failing into folders that did not exist locally
* Fixed a sync that could stop early on files without a size
* Temporary download files are no longer uploaded to Drive

## 9.4.21 (2026-07-21)

* Fixed text replacement for multiple words
* Updated the UI for changing text and background colors
* Added an alert for permanent file deletion
* Clicking on File Information metadata now navigates to the library
* Added the "iw" translation and fixed other translations
* Fixed the grid view widget
* Fixed the eye reset timer

## 9.4.8 (2026-05-21)

* All-storage access is optional
* Apps can use the system file manager to open individual files
* Fixed choose profile

## 9.3.75 (2026-04-24)

* Fixed contrast and brightness
* Advanced option to enable contrast and brightness for all reading modes
* Fixed Chinese language
* Fixed search in many PDF and EPUB books

## 9.3.63 (2026-03-02)

* Improvements
* Fixes

## 9.3.55 (2026-01-29)

* Fixes
* Librera for macOS, supports PDF, EPUB, FB2, CBZ, CBR (beta.librera.mobi for downloads)

Older releases: https://github.com/foobnix/LibreraReader/releases
