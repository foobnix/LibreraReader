#!/bin/bash -x

djvulibredir=..
tmpdir=./tmp$$
book=djvulibre-book-en.djvu

mkdir $tmpdir
trap "rm -rf $tmpdir" 0

## collect man pages

nnn=1
addpage()
{
  nn=$(printf "%03d" $nnn)
  man -t $1 > $tmpdir/$nn-$1.ps
  nnn=$(($nnn + 1))
}


## add man pages

addpage djvu

addpage djview
addpage nsdejavu

for f in $djvulibredir/tools/*.1 
do
  b=$(basename $f .1)
  [ $b == djvu ] || addpage $b
done

addpage djvutoxml
addpage djvuxml

## call djvudigital

cat $tmpdir/0*.ps > $tmpdir/book.ps
djvudigital --dpi=400 --words $tmpdir/book.ps $book

## prepare outline

echo "(bookmarks" > $tmpdir/outline.txt

p=1
for f in $tmpdir/0*.ps
do
   b=$(basename $f .ps | sed -e 's/^[-0-9]*//')
   c=$(grep '^%%Page:' $f | wc -l)
   echo '  ("'"$b"'" "#'"$p"'")' >> $tmpdir/outline.txt
   p=$(($p + $c))
done

echo ")" >> $tmpdir/outline.txt

## plug outline

djvused $book -e "set-outline $tmpdir/outline.txt" -s


