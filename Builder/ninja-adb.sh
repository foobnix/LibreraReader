#!/bin/bash

for DEVICE in $(adb devices | grep -E -i 'abcaced|[0-9]' | tr -s "\t " " " | cut -d " " -f 1)
do
  echo "--------------------------------------------------------"
  TYPE=$(adb -s ${DEVICE} shell getprop ro.product.cpu.abi)
  MODEL=$(adb -s ${DEVICE} shell getprop ro.product.model)
  A_V=$(adb -s ${DEVICE} shell getprop ro.build.version.release )
  echo "DEVICE:[${DEVICE} ${MODEL}] CPU:[${TYPE}] ANDROID:[${A_V}]"

  adb -s ${DEVICE} "$@"
done