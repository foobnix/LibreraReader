MPATH=/home/ivan-dev/Dropbox/Обзоры/GeekTimes
convert  -bordercolor white -border 20x20 $MPATH/$1.png $MPATH/$2.png +append $MPATH/img2_$1$2.png
echo $MPATH/img2_$1$2.png
 
