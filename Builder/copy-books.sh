for DEVICE in $(adb devices | grep [0-9] | tr -s "\t " " " | cut -d " " -f 1)
do
		echo "copy to device >>> ${DEVICE} <<<"
		adb -s ${DEVICE} push /home/ivan-dev/Dropbox/Projects/BookTestingDB /sdcard/Download/BookTestingDB
done