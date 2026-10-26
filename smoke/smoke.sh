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
adb shell am start -W -n $PKG/.ui.MainActivity
sleep 5; tap text "Wait"; adb shell am start -W -n $PKG/.ui.MainActivity; sleep 2; shot 01_tasks_empty

# Only what's new: widget + button, bigger widget text, widget taps
tap desc "Add task"; typew "Pay electricity bill tomorrow at 6 pm"; tap textprefix "Set reminder"; tap text "High"; tap text "Save"
tap desc "Add task"; typew "Buy milk and bread"; tap text "Medium"; tap text "Save"
tap desc "Add task"; typew "Call the bank"; tap text "Save"
shot 05_tasks_list

# 9. Home-screen widget: long-press home → Widgets → search → expand → drag preview onto home
adb shell input keyevent 3; sleep 2
adb shell input swipe 540 1200 540 1200 1500; sleep 2
tap text "Widgets"; sleep 2
tap text "Search"; adb shell input text "NotifyMe"; sleep 3; adb shell input keyevent 111; sleep 1
tap text "NotifyMe"; sleep 2; shot 20_widget_expanded
dump; cp ui.xml shots/widget_picker.xml
python3 - > /tmp/wc <<'PY'
import re, xml.etree.ElementTree as ET
best=None
for n in ET.parse('ui.xml').iter('node'):
    t=(n.get('text') or '')+'|'+(n.get('content-desc') or '')
    if 'NotifyMe sticky' in t:
        x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
        if best is None or y1>best[1]: best=((x1+x2)//2,y1)
if best: print(best[0], best[1])
PY
c=$(cat /tmp/wc)
if [ -n "$c" ]; then set -- $c; adb shell input draganddrop $1 $(( $2 - 120 )) 540 1000 3000; echo "DRAG: $c" >> shots/steps.txt; else echo "WIDGET NOT FOUND" >> shots/steps.txt; fi
sleep 4; shot 21_widget_drop
tap text "Add"; tap text "Add to home screen"; sleep 3
adb shell input keyevent 3; sleep 3; shot 22_widget_home
tap desc "Mark done" 0; sleep 3; shot 23_widget_ticked
tap text "Today"; sleep 3; shot 24_widget_today
tap text "Tasks"; sleep 2
tap desc "Add"; sleep 3; shot 25_widget_plus_opens_app
adb shell input keyevent 4; sleep 1; adb shell input keyevent 3; sleep 2

adb logcat -d > shots/logcat.txt
grep -n "FATAL EXCEPTION" -A 30 shots/logcat.txt > shots/crashes.txt || true
echo "done" >> shots/steps.txt
