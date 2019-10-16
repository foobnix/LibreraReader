#!/usr/bin/env bash

echo "kill-server"
adb kill-server
echo "sleep"
sleep 2
echo "start-server"
adb start-server
adb devices