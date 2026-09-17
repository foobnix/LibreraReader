#!/usr/bin/env bash

# Where copyApks puts the builds: librera_builds_dir in the global ~/.gradle/gradle.properties
BUILDS_DIR=$(sed -n 's/^librera_builds_dir=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
if [ "debug" != "$1" ] && [ -z "$BUILDS_DIR" ]; then
  echo "ERROR: librera_builds_dir is not set in ~/.gradle/gradle.properties"
  exit 1
fi

if [ "debug" == "$1" ]; then
    echo "==[Debug]=="
    APK="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/app/build/intermediates/apk/pro/debug"
else
  if [ -z "$1" ]; then
    echo "==[Testing]=="
    APK="$BUILDS_DIR"
  else
    echo "==[$1]=="
    APK="$BUILDS_DIR/$1"
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


	# The folder has a space in it ("My Drive"): the files are globbed into an array, not a string
	if [[ $TYPE == *"arm64"* ]]; then
		#echo "TYPE:[arm64]"
	    FILES=("$APK"/*arm64.apk)
	elif [[ $TYPE == *"armeabi"* ]]; then
		#echo "TYPE:[arm]"
	    FILES=("$APK"/*arm.**)
	else 
		#echo "TYPE:[x86]"
	    FILES=("$APK"/*x86*.apk)
	fi


  for f in "${FILES[@]}"
	do
		echo "Installing: $f"
		#adb -s ${DEVICE} install -r "$f"
		adb -s ${DEVICE} install -t "$f"
	done

FILES=("$APK"/*uni*.apk)
for f in "${FILES[@]}"
do
  echo "Installing universal: $f"
  adb -s ${DEVICE} install "$f"
done

done