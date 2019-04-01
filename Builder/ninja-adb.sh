#!/bin/bash

for DEVICE in $(adb devices | grep -E -i 'abcaced|[0-9]' | tr -s "\t " " " | cut -d " " -f 1)
do
  echo "--------------------------------------------------------"
  TYPE=$(adb -s ${DEVICE} shell getprop ro.product.cpu.abi)
  MODEL=$(adb -s ${DEVICE} shell getprop ro.product.model)
  echo "DEVICE: ${DEVICE} ${MODEL}"
  echo "CPU: ${TYPE}"
  
  adb -s ${DEVICE} "$@"
done