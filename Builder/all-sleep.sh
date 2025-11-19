
adb shell am force-stop com.foobnix.pdf.reader
sleep 1
adb shell am start-activity -S -W -n com.foobnix.pdf.reader/com.foobnix.ui2.MainTabs2