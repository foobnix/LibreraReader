echo "Command am kill"
sh ninja-adb.sh shell am kill com.foobnix.pdf.reader
sh ninja-adb.sh shell am kill com.foobnix.pdf.reader.a1
sh ninja-adb.sh shell am kill com.foobnix.pro.pdf.reader

sh ninja-adb.sh shell ps | grep foobnix
#sh ninja-adb.sh shell am force-stop com.foobnix.pdf.reader

#sh ninja-adb.sh shell am kill com.foobnix.pro.pdf.reader

