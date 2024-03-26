#!/usr/bin/env bash
echo "=================="
echo "shell am force-stop"
sh ninja-adb.sh shell am force-stop com.foobnix.pro.pdf.reader
sh ninja-adb.sh shell am force-stop com.foobnix.pdf.reader
sh ninja-adb.sh shell am force-stop classic.pdf.reader.viewer.djvu.epub.fb2.txt.mobi.book.reader.lirbi.libri
sh ninja-adb.sh shell am force-stop droid.reader.book.epub.mobi.pdf.djvu.fb2.txt.azw.azw3
sh ninja-adb.sh shell am force-stop mobi.librera.book.reader
sh ninja-adb.sh shell am force-stop pdf.pdf.reader
sh ninja-adb.sh shell am force-stop tts.reader
sh ninja-adb.sh shell am force-stop epub.reader
sh ninja-adb.sh shell am force-stop com.foobnix.pdf.reader.a1
sh ninja-adb.sh shell am force-stop com.artifex.mupdf.mini.app
sh ninja-adb.sh shell am force-stop com.artifex.mupdf.viewer.app
sh ninja-adb.sh shell am force-stop mobi.librera.epub
echo "=================="


