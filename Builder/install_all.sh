#!/usr/bin/env bash

if [ "debug" == "$1" ]; then
    echo "==[Debug]=="
    APK=/Users/dev/git/LibreraReader/app/build/intermediates/apk/pro/debug
else
  if [ -z "$1" ]; then
    echo "==[Testing]=="
    APK=/Users/dev/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing
    APK=/Users/dev/git/LibreraReader/app/build/intermediates/apk/pro/debug
  else
    echo "==[$1]=="
      if [ "$(uname)" == "Darwin" ]; then
       APK=/Users/dev/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/$1
        else
       APK=/home/dev/Dropbox/FREE_PDF_APK/testing/$1
      fi
  fi
fi

for DEVICE in $(adb devices | grep  -E -i '[abcdf0-9]' | tr -s "\t " " " | cut -d " " -f 1)
do

    if [[ $DEVICE == "List" ]]; then
        continue
    fi

	  echo "----------------------------------"
	  TYPE=$(adb -s ${DEVICE} shell getprop ro.product.cpu.abi)
	  MODEL=$(adb -s ${DEVICE} shell getprop ro.product.model)
	  A_V=$(adb -s ${DEVICE} shell getprop ro.build.version.release )
	  echo "--------------------------------------------------------"
	  echo "DEVICE:[${DEVICE}-${MODEL}] CPU:[${TYPE}] ANDROID:[${A_V}]"
	  echo "--------------------------------------------------------"


	if [[ $TYPE == *"arm64"* ]]; then
		#echo "TYPE:[arm64]"
	    FILES=$APK/*arm64.apk
	elif [[ $TYPE == *"armeabi"* ]]; then
		#echo "TYPE:[arm]"
	    FILES=$APK/*arm.**
	else 
		#echo "TYPE:[x86]"
	    FILES=$APK/*x86*.apk
	fi


  for f in $FILES
	do
		echo "Installing: $f"
		#adb -s ${DEVICE} install -r "$f"
		adb -s ${DEVICE} install -t "$f"
	done

FILES=$APK/*uni*.apk
for f in $FILES
do
  echo "Installing universal: $f"
  adb -s ${DEVICE} install "$f"
done

done