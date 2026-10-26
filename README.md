# NotifyMe: ToDo List & Reminder

A calm, ad-free Android to-do and reminder app. Kotlin + Jetpack Compose, everything stored on the phone.

## Features
- **Tasks**: add with + (type or speak), soft pastel priority colours (High / Medium / Low), tick the box to complete, collapsible *Completed* list with Undo, swipe left to delete.
- **Reminders on any task**: clock icon → date, time, repeat, and *Notification* or full-screen *Alarm*.
- **Calendar**: month view, red dot on every date with a reminder (past or future), tap a date to add/edit.
- **Smart voice** (English, Hindi, Hinglish), works offline: "Pay electricity bill tomorrow at 6 pm", "कल शाम 6 बजे दवाई", "har somvar subah 9 baje meeting".
- **Repeat**: daily, weekdays, weekly (pick days), monthly, yearly, every N days/weeks; ends never / on a date / after N times.
- **Notification actions**: Done, Snooze 10 min, 1 hour (alarm screen also has Tomorrow).
- **Focus mode**: full-screen clock, 25 / 50 / custom-minute timer, optional Do Not Disturb, night clock.
- **Sticky-note widget**: resizable, switches between Tasks and Today, tick tasks and add from the home screen.
- Reminders survive restarts and time-zone changes; a battery guide for Xiaomi, Oppo, Vivo, Realme and other brands.

## Build
Open in Android Studio (JDK 17), or:

```
./gradlew assembleRelease bundleRelease
```

To sign locally, put `keystore.properties` (git-ignored) in the project root:

```
storeFile=notifyme-release.jks
storePassword=…
keyAlias=notifyme
keyPassword=…
```

The GitHub workflow in `.github/workflows/android.yml` runs the unit tests and builds an unsigned release APK and AAB on every push.
