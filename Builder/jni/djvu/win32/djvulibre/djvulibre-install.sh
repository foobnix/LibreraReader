#!/bin/sh

c=/c
qtdir=$c/Qt/5.15.1/msvc2019
djdir=$HOME/djvu/djvulibre-3.5
dwdir=$djdir/win32/djvulibre/Release
djsrc=$HOME/djvu/djvulibre-djview/src
msvc="$c/Program Files/Microsoft Visual Studio/2019/Community/VC"
msredist="$msvc/Redist/MSVC/14.23.27820/vcredist_x86.exe"
ssl="$c/Qt/Tools/OpenSSL/win_x86_pre/lib*-1_1.dll"

target=$HOME/djvu/DjVuLibre

if [ ! -d $target ] ; then
    mkdir $target
fi

function run() {
    echo "$@"
    "$@"
    if test $? -ne 0 ; then
      echo "FAILED: " "$@"
    fi
}

## Djvulibre tools
echo ---- DjVuLibre tools

djexe="bzz.exe c44.exe cjb2.exe cpaldjvu.exe csepdjvu.exe
       ddjvu.exe djvm.exe djvmcvt.exe djvudump.exe djvuextract.exe 
       djvumake.exe djvups.exe djvused.exe djvutoxml.exe
       djvutxt.exe djvuxmlparser.exe"
djdll="libdjvulibre.dll libjpeg.dll libtiff.dll libz.dll"
for n in $djdll $djexe ; do 
    run cp $dwdir/$n $target ; done

## Qt libs
echo ---- Qt libs

qtdll="Qt5Core.dll Qt5Gui.dll Qt5Widgets.dll Qt5Network.dll Qt5OpenGL.dll Qt5PrintSupport.dll"
qtplug="platforms styles imageformats"
for n in $qtdll ; do 
    run cp $qtdir/bin/$n $target ; done
test -d $target/plugins || run mkdir $target/plugins
for n in $qtplug ; do 
    test -d $target/plugins/$n || run mkdir $target/plugins/$n 
    for m in $qtdir/plugins/$n/*.dll ; do
        run cp $m $target/plugins/$n ; done
    run chmod 0755 $target/plugins/$n/*.dll 
    for m in $target/plugins/$n/*.dll; do
	md="`dirname $m`/`basename $m .dll`d.dll"
	test -r $md && run rm $md
    done
done
    run rm $target/plugins/imageformats/qsvg*
run find $target -name '*.dll' -exec chmod 0755 {} \;

echo '[Paths]' > $target/qt.conf

## MS libs
echo ---- MS libs

for n in "$msredist" ; do
    run cp "$n" $target; done

## DjVuLibre shared files
echo ---- DjVuLibre shared files

test -d $target/share || mkdir $target/share
run cp -r $djdir/share/djvu $target/share
run find $target/share -name CVS -exec rm -rf {} \; -prune

## DjVuLibre dev files
echo ---- DjVuLibre dev files

run cp $dwdir/libdjvulibre.lib $target
test -d $target/include || run mkdir $target/include
test -d $target/include/libdjvu || run mkdir $target/include/libdjvu
run cp $djdir/libdjvu/miniexp.h $target/include/libdjvu
run cp $djdir/libdjvu/ddjvuapi.h $target/include/libdjvu

## DjView exe
echo ---- DjView exe

if test -r $dwdir/djview.exe ; then
  run cp $dwdir/djview.exe $target
else
  run cp $djsrc/release/djview.exe $target
fi
( cd $djsrc; run $qtdir/bin/lrelease djview.pro )
test -d $target/share/djvu/djview4 || run mkdir $target/share/djvu/djview4
run cp $djsrc/*.qm $target/share/djvu/djview4
run cp $qtdir/translations/qt_*.qm  $target/share/djvu/djview4
run chmod 0644 $target/share/djvu/djview4/qt*.qm
run rm -f $target/share/djvu/djview4/qt_help_*.qm

## OpenSSL

for n in $ssl ; do
   run cp "$n" "$target"
done

## Doc
echo ---- Doc

run cp $djdir/win32/djvulibre/djvulibre.nsi $target
run cp $djdir/win32/djvulibre/djvulibre*.nsh $target

test -d $target/doc || run mkdir $target/doc
run cp $djdir/doc/*.djvu $target/doc
run cp $djdir/doc/*.txt $target/doc
run unix2dos $target/doc/*.txt
run cp $djdir/COPYING $target/COPYING.txt
run unix2dos $target/COPYING.txt
