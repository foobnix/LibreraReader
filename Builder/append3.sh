MPATH=/home/ivan-dev/Dropbox/Обзоры/GeekTimes
convert  -bordercolor white -border 10x10 $MPATH/$1.png $MPATH/$2.png $MPATH/$3.png +append $MPATH/img_$1$2$3.png
echo $MPATH/img_$1$2$3.png
 
