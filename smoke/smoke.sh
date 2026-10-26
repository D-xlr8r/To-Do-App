#!/bin/bash
set -x
PKG=com.dxlr8r.notifyme
mkdir -p shots
shot() { sleep 1.5; adb exec-out screencap -p > "shots/$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1; }
tap() { dump; c=$(python3 smoke/ui.py ui.xml "$1" "$2" "${3:-0}"); if [ -n "$c" ]; then adb shell input tap $c; echo "TAP OK: $2" >> shots/steps.txt; else echo "NOT FOUND: $2" >> shots/steps.txt; cp ui.xml "shots/notfound-$(echo $2 | tr -c 'a-zA-Z0-9' '_').xml"; fi; sleep 2; }
type() { adb shell input text "$1"; sleep 1.5; }
back() { adb shell input keyevent 4; sleep 1.5; }

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r smoke/NotifyMe.apk || exit 1
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow
adb logcat -c
adb shell am start -W -n $PKG/.ui.MainActivity
sleep 5; shot 01_tasks_empty

tap desc "Add task"; shot 02_editor_open
type "Pay%selectricity%sbill%stomorrow%sat%s6%spm"
adb shell input keyevent 111   # hide keyboard (ESC)
sleep 1; shot 03_editor_suggestion
tap textprefix "Set reminder"; tap text "High"; shot 04_editor_filled
tap text "Save"; shot 05_tasks_one

tap desc "Add task"; type "Buy%smilk%sand%sbread"; adb shell input keyevent 111; tap text "Medium"; tap text "Save"
tap desc "Add task"; type "Call%sthe%sbank"; adb shell input keyevent 111; tap text "Low"; tap text "Save"
tap desc "Add task"; type "Read%sa%sbook"; adb shell input keyevent 111; tap text "Save"
shot 06_tasks_list
tap desc "Not done" 3; shot 07_task_done_snackbar
sleep 4; tap textprefix "Completed"; shot 08_completed_expanded

tap descprefix "Add reminder" 0; shot 09_task_reminder_sheet
tap text "Alarm"; shot 10_alarm_mode
tap text "Repeat"; shot 11_repeat_dialog
tap text "Every week"; shot 12_repeat_weekly
tap text "Done"; tap text "Cancel"

tap text "Calendar"; shot 13_calendar
tap desc "Add reminder"; type "Doctor%sappointment"; adb shell input keyevent 111; shot 14_calendar_editor
tap text "Save"; shot 15_calendar_with_dot
tap desc "Next month"; shot 16_next_month
tap text "Today"

tap desc "Focus mode"; shot 17_focus
tap text "Start focus"; shot 18_focus_running
adb shell input keyevent 4; sleep 2

tap desc "Settings"; shot 19_settings
back

# A real reminder firing one minute from now (notification + alarm screen)
tap text "Tasks"
tap desc "Add task"; type "Drink%swater%sin%s1%sminute"; adb shell input keyevent 111
tap textprefix "Set reminder"; tap text "Alarm"; tap text "Save"
shot 20_before_alarm
adb shell input keyevent 3   # home
sleep 75
shot 21_alarm_fired
adb shell dumpsys notification --noredact | grep -E "pkg=com.dxlr8r|android.title=" | head -20 > shots/notifications.txt
tap text "Mark done"; shot 22_after_alarm
adb shell cmd statusbar expand-notifications; sleep 2; shot 23_notification_shade
adb shell cmd statusbar collapse

# Dark mode + rotation of focus
adb shell cmd uimode night yes; sleep 2
adb shell am start -W -n $PKG/.ui.MainActivity; sleep 3; shot 24_dark_tasks
adb shell cmd uimode night no

# Voice shortcut / add intents
adb shell am start -W -a com.dxlr8r.notifyme.ADD_TASK -n $PKG/.ui.MainActivity; sleep 3; shot 25_shortcut_add

# Widget renders: bind it via a test host isn't possible headlessly; at least make sure the receiver starts
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE -n $PKG/.widget.NotifyWidgetReceiver

# Survives reboot-like rescheduling
adb shell am broadcast -a android.intent.action.MY_PACKAGE_REPLACED -n $PKG/.reminders.SystemEventsReceiver
sleep 3
adb logcat -d > shots/logcat.txt
grep -E "FATAL EXCEPTION|AndroidRuntime|$PKG.*(Exception|Error)" -A 15 shots/logcat.txt > shots/crashes.txt || true
echo "done" >> shots/steps.txt
