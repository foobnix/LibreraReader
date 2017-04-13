screens=/home/ivan-dev/git/pdf4/Builder/screens/banners
rm -r $screens/*
for lang in en ru it fr de ja zh tr pt es ar fa 
do
for i in 1 2 3 4 
do
mkdir $screens/$i
		adb shell am broadcast -a MyAction --es locale "$lang,$i"
		echo test_locale "$lang,$i"
		sleep 2s
	
		adb shell screencap -p /sdcard/screen.png
		adb pull /sdcard/screen.png $screens/$i/$lang.png
		adb shell rm /sdcard/screen.png
		echo $i $lang.png
	done
done
