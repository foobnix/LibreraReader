screens=/home/ivan-dev/git/pdf4/Builder/screens/all

for lang in en
do
rm -r $screens/$lang
mkdir $screens/$lang
	for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19
	do 
		adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE_TEST --es test_locale "$lang,$i"
		echo test_locale "$lang,$i"
		sleep 3s

		adb shell screencap -p /sdcard/screen.png
		
		adb shell am broadcast -a ScreenActivity --es locale "$lang,$i"
		sleep 1s
		adb shell screencap -p /sdcard/screen.png
		
		adb pull /sdcard/screen.png $screens/$lang/$i.png
		
		adb shell rm /sdcard/screen.png
		echo $screens/$lang/$i.png
	done
done

