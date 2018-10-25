echo "pull file" $1


adb pull /sdcard/Android/data/com.foobnix.pdf.reader/cache/Book/$1 /home/ivan-dev/Downloads/$1
echo /home/ivan-dev/Downloads/$1