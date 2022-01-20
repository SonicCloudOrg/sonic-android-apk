#!/bin/bash
set -xe
pkg=jp.co.cyberagent.stf
cls=Agent
apk=$(adb shell pm path $pkg | dos2unix | cut -c9-)
adb shell "export CLASSPATH=$apk; exec app_process /system/bin $pkg.$cls" $@
