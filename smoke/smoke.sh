#!/bin/bash
set -x
PKG=com.dxlr8r.notifyme
mkdir -p shots
shot() { sleep 1.5; adb exec-out screencap -p > "shots/$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1; }
tap() { dump; c=$(python3 smoke/ui.py ui.xml "$1" "$2" "${3:-0}"); if [ -n "$c" ]; then adb shell input tap $c; echo "TAP OK: $2" >> shots/steps.txt; else echo "NOT FOUND: $2" >> shots/steps.txt; cp ui.xml "shots/notfound-$(echo $2 | tr -c 'a-zA-Z0-9' '_').xml"; fi; sleep 2; }
# type word by word, like a person
typew() { first=1; for w in $1; do [ $first = 0 ] && adb shell input keyevent 62; adb shell input text "$w"; first=0; sleep 0.4; done; sleep 1.5; }
fieldtext() { dump; python3 - <<'PY' >> shots/steps.txt
import xml.etree.ElementTree as ET
for n in ET.parse('ui.xml').iter('node'):
    if n.get('class','').endswith('EditText'): print('FIELD TEXT:', repr(n.get('text')))
PY
}
hidekb() { adb shell input keyevent 111; sleep 1; }

adb shell settings put secure immersive_mode_confirmations confirmed
adb install -r smoke/NotifyMe.apk || exit 1
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow
adb shell appops set $PKG USE_FULL_SCREEN_INTENT allow
adb logcat -c
# Only the new first-run tour
adb shell am start -W -n $PKG/.ui.MainActivity
sleep 5; tap text "Wait"; adb shell am start -W -n $PKG/.ui.MainActivity; sleep 2
shot 01_tour_welcome
tap text "Next"; shot 02_tour_add
tap text "Next"; shot 03_tour_tasks
tap text "Next"; shot 04_tour_calendar
tap text "Next"; shot 05_tour_focus
tap text "Next"; shot 06_tour_widget
adb shell input swipe 540 1500 540 700 300; sleep 1; shot 07_tour_widget_scrolled
tap text "Add widget to home screen"; sleep 3; shot 08_pin_dialog
dump; cp ui.xml shots/pin_dialog.xml
tap text "Add to home screen"; tap text "Add automatically"; tap text "Add"; sleep 3; shot 09_after_pin
adb shell am start -W -n $PKG/.ui.MainActivity; sleep 2; shot 10_back_in_tour
tap text "Get started"; sleep 2; shot 11_main_after_tour
adb shell am force-stop $PKG; adb shell am start -W -n $PKG/.ui.MainActivity; sleep 3; shot 12_relaunch_no_tour
tap desc "Settings"; sleep 2; adb shell input swipe 540 1800 540 900 300; sleep 1; shot 13_settings_tour_row
tap text "App tour"; sleep 2; shot 14_tour_from_settings
adb shell input keyevent 3; sleep 3; shot 15_home_widget

adb logcat -d > shots/logcat.txt
grep -n "FATAL EXCEPTION" -A 30 shots/logcat.txt > shots/crashes.txt || true
echo "done" >> shots/steps.txt
