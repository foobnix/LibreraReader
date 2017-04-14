#!/bin/bash

SOURCE=/home/ivan-dev/git/LirbiReader/EBookDroid/res
DEST=/home/ivan-dev/Dropbox/FREE_PDF_APK/testing/languages


rm -rf $DEST/*


for item in $SOURCE/values-*; do
	 NAME=$(basename $item)
	echo $SOURCE/$NAME $DEST/$NAME
	cp -rf $SOURCE/$NAME $DEST/$NAME
done
cp $SOURCE/values/strings.xml $DEST/values-English.xml

rm -r $DEST/values-large
rm -r $DEST/values-xlarge
rm -r $DEST/values-v14
rm -r $DEST/values-v21
