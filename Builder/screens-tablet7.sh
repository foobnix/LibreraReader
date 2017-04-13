screens=/home/ivan-dev/git/LirbiReader/Builder/screens/tablet7
rm -r $screens/*
for lang in en ru it fr de ja zh tr pt es ar fa
do
mkdir $screens/$lang
	for i in 1 2 3 4 6 7 9 10
	do 
		adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE_TEST --es test_locale "$lang,$i"
		echo test_locale "$lang,$i"
		sleep 3s

		adb shell screencap -p /sdcard/screen.png
		adb pull /sdcard/screen.png $screens/$lang/$i.png
		adb shell rm /sdcard/screen.png
		echo $screens/$lang/$i.png
	done
done

