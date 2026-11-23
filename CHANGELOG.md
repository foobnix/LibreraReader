# Changelog

All notable changes to Librera Reader.

Release builds: https://github.com/foobnix/LibreraReader/releases · [Unreleased APK Direct Download](http://beta.librera.mobi)

* Musician's mode: a round button beside the mode's name goes to page 1; it can be hidden in the mode's settings
* Smoother vertical scrolling: two-finger zoom and scroll follow the fingers frame by frame and zoom around the point between them; DjVu pages no longer freeze the screen while they render, and high zoom stays smooth with smaller page tiles
* Radio: the station playing, or the one chosen for the book, stays above the station list; tap it to pause or play
* The music file and the radio station are remembered separately for each book; MP3 opens in the folder of the book's music file
* Play and pause of the radio work when tapped quickly one after the other
* Removed the "Pause on incoming calls" option: reading always pauses for a call, unless it mixes with other apps' sound
* Changelog link at the bottom of the preferences, opens this page in the browser
* New option in Preferences › Advanced Settings: open books on page 1 instead of where you left off (also in Preferences › General, and for books opened in LibreraX)
* PDF search highlights the found word itself: with the PDFium engine the box was one word off on pages with math symbols
* PDF text can be selected and searched on a page the book opened on zoomed in
* Record the book read aloud to an MP3 file
* Pages with only a picture (an EPUB cover) are no longer drawn blank in the HTML engine
* In day mode the page colour is used for the whole page, also where the book sets its own background
* Internet radio: play M3U playlists, with a built-in list of recommended stations
* Music and radio buttons in the TTS panel, with seek buttons for streams
* Panels and buttons adjusted for landscape
* CHM and LIT books open, converted to EPUB
* MuPDF engine upgraded to 1.28.6
* Self test of the reading engines on your own books
* Fixed hyphenation, fonts and the e-ink theme
* Show or hide the Musician's mode button
* Fixed double tap and paged mode
* Draw shapes on PDF pages
* An animated sound icon shows while TTS is reading
* Fixed splitting pages and right-to-left reading
* Interface updates and fixes
* Page count shown for EPUB and other reflowable books (estimated, marked ≈)
* Draw on PDF pages in paged mode
* TIFF images and books open without MuPDF
* Fixed the TTS buttons, text selection and the library sort order
* TTS highlights the text being read with the HTML engine
* JS build: reads without MuPDF, with the HTML engine and PDFium
* Fixed Google Drive sync, SVG images and FB2 images
* Fonts included by default
* Comic books (CBZ, CBR) open from the archive without unpacking it first
* Home screen widget and shortcut for a book
* TTS buttons at the bottom of the reader
* Send a book with its bookmarks and progress to LibreraX; a book that crashed can be opened in LibreraX
* Fixed hyphenation, the dictionary and bookmark panels
* Reflowable books now render in a WebView with foliate-js, and PDFs with PDFium
* Icons are shown in the SAF folder browser
* SAF folders can be added to the library
* Underline and highlight in EPUB
* Open a book in LibreraX, our new modern reader for Android and iOS
* MuPDF engine upgraded to 1.28.5
* Pull to refresh in the library can be turned off
* Full published date shown and used for sorting
* The library shows its current sort
* MuPDF engine upgraded to 1.28.4
* Bookmarks are sent to LibreraX
* Cover corner radius option
* Open a book with LibreraX
* Faster footnotes (2.4x)
* BBCode formatting is off by default
* New design: floating rounded tabs, round buttons, rounded dialogs and menus, new theme colours.
* Redesigned settings, book info and TTS dialogs.
* Library button and one-tap Vertical/Paged switch in the reader.
* BBCode formatting in books.
* Bookmarks and reading position stay on the right page after font changes and sync.
* Favourites use a heart.
* Librera now appears as a media app in Android Auto
* Browse recent books in the car, with covers, titles and authors
* Selecting a book resumes it at its saved position
* Playback controls work from the car, the lock screen and Bluetooth
* TTS uses Android's standard media notification, with cover art, a progress bar and lock screen controls
* Fixed no TTS speech when the system default engine points at an uninstalled package
* Fixed the TTS notification disappearing, or staying stuck on "please wait", after a long time in the background
* Fixed audio focus not being released on pause, which left other apps muted
* Fixed a crash when requesting the notification permission from the TTS controls
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
* Google Drive sync is much faster: smaller listing requests, parallel downloads and a change check that skips work when nothing changed
* Deleting a book now syncs to Google Drive: it no longer comes back from another device
* Fixed Google Drive downloads failing into folders that did not exist locally
* Fixed a Google Drive sync that could stop early on files without a size
* Temporary download files are no longer uploaded to Drive
* Fixed text replacement for multiple words
* Updated the UI for changing text and background colors
* Added an alert for permanent file deletion
* Clicking on File Information metadata now navigates to the library
* Added the "iw" translation and fixed other translations
* Fixed the grid view widget
* Fixed the eye reset timer
* All-storage access is optional
* Apps can use the system file manager to open individual files
* Fixed choose profile
* Fixed contrast and brightness
* Advanced option to enable contrast and brightness for all reading modes
* Fixed Chinese language
* Fixed search in many PDF and EPUB books
* Improvements
* Fixes
* Fixes
* Librera for macOS, supports PDF, EPUB, FB2, CBZ, CBR (beta.librera.mobi for downloads)
