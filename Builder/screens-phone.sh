#1080x1920-420
screens=/home/ivan-dev/git/pdf4/Builder/screens/phone
rm -r $screens/*
for lang in en ar de es fa fr hi it ja ko nl pl pt ro ru sv tr uk vi zh
do
mkdir $screens/$lang
	for i in 1 2 3 4 5 6 7 8
	do 
		adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE_TEST --es test_locale "$lang,$i"
		echo test_locale "$lang,$i"
		sleep 2s

		adb shell screencap -p /sdcard/screen.png
		adb pull /sdcard/screen.png $screens/$lang/$i.png
		adb shell rm /sdcard/screen.png
		echo $screens/$lang/$i.png
	done
done

