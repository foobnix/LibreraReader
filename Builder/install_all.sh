for DEVICE in $(adb devices | grep [0-9] | tr -s "\t " " " | cut -d " " -f 1)
do
	  echo "--------------------------------------------------------"
	  TYPE=$(adb -s ${DEVICE} shell getprop ro.product.cpu.abi)
	  MODEL=$(adb -s ${DEVICE} shell getprop ro.product.model)
	  echo "DEVICE: ${DEVICE} ${MODEL}"
	  echo "CPU: ${TYPE}"

	if [[ $TYPE == *"arm"* ]]; then
		echo "TYPE: arm"
	    FILES=apk/*arm64*.apk
	else
		echo "TYPE: x86"
	    FILES=apk/*x86*.apk
	fi

  for f in $FILES
	do
		echo "Installing: $f"
		#adb -s ${DEVICE} install -r "$f"
		adb -s ${DEVICE} install "$f"
	done

done

