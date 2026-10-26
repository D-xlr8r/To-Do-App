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
sleep 5; shot 01_tasks_empty

# 1. Smart text → reminder suggestion
tap desc "Add task"; sleep 2
typew "Pay electricity bill tomorrow at 6 pm"; fieldtext; hidekb
shot 02_editor_suggestion
tap textprefix "Set reminder"; fieldtext; tap text "High"; shot 03_editor_applied
tap text "Save"; shot 04_tasks_one

tap desc "Add task"; typew "Buy milk and bread"; hidekb; tap text "Medium"; tap text "Save"
tap desc "Add task"; typew "Call the bank"; hidekb; tap text "Low"; tap text "Save"
shot 05_tasks_list

# 2. A reminder one minute from now with Alarm mode — must ring on the lock screen
tap desc "Add task"; typew "Drink water in 1 minute"; fieldtext; hidekb
tap textprefix "Set reminder"; tap text "Alarm"; shot 06_alarm_setup; tap text "Save"
shot 07_before_alarm
adb shell input keyevent 26   # screen off (lock)
sleep 80
shot 08_alarm_fired
adb shell dumpsys notification --noredact | grep -E "pkg=com.dxlr8r|android.title=|android.text=" | head -20 > shots/notifications.txt
tap text "Mark done"; sleep 2; shot 09_after_alarm
adb shell input keyevent 82; adb shell wm dismiss-keyguard; sleep 2
adb shell am start -W -n $PKG/.ui.MainActivity; sleep 3; shot 10_tasks_after_alarm

# 3. Notification-style reminder + snooze from notification
tap desc "Add task"; typew "Stretch in 1 minute"; hidekb; tap textprefix "Set reminder"; tap text "Save"
adb shell input keyevent 3; sleep 130
adb shell cmd statusbar expand-notifications; sleep 2; shot 11_notification
tap text "Snooze 10 min"; sleep 2; shot 12_after_snooze
adb shell cmd statusbar collapse
adb shell am start -W -n $PKG/.ui.MainActivity; sleep 3; shot 13_tasks_snoozed

# 4. Tick a task, undo, completed list
tap desc "Mark done" 1; adb exec-out screencap -p > shots/14_done_snackbar.png
sleep 5; tap textprefix "Completed"; shot 15_completed

# 5. Hindi phrase parsing via typed text
tap desc "Add task"; adb shell input text "kal%sshaam%s7%sbaje%sdawai%slena"; sleep 2; fieldtext; hidekb; shot 16_hinglish_suggestion
tap text "Cancel"

# 6. Settings
tap desc "Settings"; shot 17_settings; adb shell input keyevent 4; sleep 1

# 7. Focus mode (clock ticks, so no UI dumps there)
adb shell am start -W -n $PKG/.ui.focus.FocusActivity; sleep 3; shot 18_focus
adb shell input keyevent 4; sleep 2

# 9. Home-screen widget: long-press home, open Widgets, add NotifyMe
adb shell input keyevent 3; sleep 2
adb shell input swipe 540 1200 540 1200 1500; sleep 2; shot 19_home_longpress
tap text "Widgets"; sleep 2
for i in 1 2 3 4 5 6; do dump; if python3 smoke/ui.py ui.xml text "NotifyMe" | grep -q .; then break; fi; adb shell input swipe 540 1800 540 700 400; sleep 1; done
tap text "NotifyMe"; sleep 2; shot 20_widget_picker
tap textprefix "NotifyMe sticky"; sleep 1
tap text "Add to home screen"; tap text "Add"; sleep 3
shot 21_widget_home

# 8. Rescheduling after "update" and widget update broadcast
adb shell am broadcast -a android.intent.action.MY_PACKAGE_REPLACED -n $PKG/.reminders.SystemEventsReceiver
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE -n $PKG/.widget.NotifyWidgetReceiver
sleep 3
adb shell dumpsys alarm | grep -A3 "com.dxlr8r" | head -30 > shots/alarms.txt
adb logcat -d > shots/logcat.txt
grep -n "FATAL EXCEPTION" -A 30 shots/logcat.txt > shots/crashes.txt || true
echo "done" >> shots/steps.txt
