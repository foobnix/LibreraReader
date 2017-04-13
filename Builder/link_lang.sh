#!/bin/bash

SOURCE=/home/ivan-dev/git/LirbiReader/EBookDroid/res
DEST=/home/ivan-dev/Dropbox/FREE_PDF_APK/testing/languages


rm -rf $DEST/*


for item in $SOURCE/values-*; do
 NAME=$(basename $item)
 ln -s $SOURCE/$NAME $DEST/$NAME
done
ln -s $SOURCE/values/strings.xml $DEST/values-English.xml

rm $DEST/values-large
rm $DEST/values-xlarge
rm $DEST/values-v14
rm $DEST/values-v21
