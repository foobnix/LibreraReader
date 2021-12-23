#!/usr/bin/env bash
APK=/home/dev/Dropbox/FREE_PDF_APK/testing

for DEVICE in $(adb devices | grep  -E -i '[abcdf0-9]' | tr -s "\t " " " | cut -d " " -f 1)
do

    if [[ $DEVICE == "List" ]]; then
        continue
    fi

	  echo "----------------------------------"
	  TYPE=$(adb -s ${DEVICE} shell getprop ro.product.cpu.abi)
	  MODEL=$(adb -s ${DEVICE} shell getprop ro.product.model)
	  echo "DEVICE: ${DEVICE}-[${MODEL}]"
	  echo "CPU: ${TYPE}"


	if [[ $TYPE == *"arm64"* ]]; then
		echo "TYPE: arm64"
	    FILES=$APK/*arm64.apk
	elif [[ $TYPE == *"armeabi"* ]]; then
		echo "TYPE: arm"
	    FILES=$APK/*arm.**
	else 
		echo "TYPE: x86"
	    FILES=$APK/*x86*.apk
	fi
	echo ""

  for f in $FILES
	do
		echo "Installing: $f"
		#adb -s ${DEVICE} install -r "$f"
		adb -s ${DEVICE} install "$f"
	done

    FILES=$APK/*uni*.apk
    for f in $FILES
	do
		echo "Installing universal: $f"
		adb -s ${DEVICE} install "$f"
	done

done


