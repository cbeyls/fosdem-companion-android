Change Log
==========

## Version 2.2.9

_2025-01-23_

- Fix: FOSDEM logo padding and alignment
- Fix: more links to speaker info web pages
- Fix: on-site navigation room links (regression introduced in 2.2.8).

## Version 2.2.8

_2025-01-16_

- Fix: some links to speaker info web pages
- Fix: link to stands web page
- Enhancement: remove event title from sharing text body
- Upgrade libraries and small performance improvements.

## Version 2.2.7

_2024-06-28_

- Feature: enable predictive back animations
- Fix: restored the ability to connect to FOSDEM servers on Android < 7.1 by including a root certificate
- Fix: proper detection of granting the schedule alarms permission on Android 14+
- Fix: search menu item always showing in the overflow menu instead of the main toolbar when possible
- Fix: blinking menu items when swapping between event details
- Upgrade libraries, Kotlin, Gradle plugin and build tools to their latest versions.

## Version 2.2.6

_2024-02-02_

- Fix: speaker info button not visible on tablets
- Fix: feedback link not properly removed when a feedback button is available.

## Version 2.2.5

_2024-02-01_

- Feature: add "Submit feedback" button in event details screen
- Fix: no image placeholder shown in room image dialog launched from notification
- Enhancement: don't display blue image square in place of `<img>` tags in HTML descriptions
- Enhancement: take manual database updates into account when scheduling the next automatic update.

## Version 2.2.4

_2024-01-16_

- Enhancement: make use of new fields in the XML schedule file format (based on PretalX), allowing the removal of some hardcoded values
- Enhancement: add support for the "Junior" track type
- Enhancement: trigger an automatic download on app startup after clearing the schedule during a database schema upgrade
- Enhancement: use `ContextCompat.registerReceiver()` API to ensure the time zone change broadcast will be received on API 33+
- Reimplement database tables version tracking using `invalidationTrackerFlow()` to avoid blocking the main thread during initialization.

## Version 2.2.1

_2024-01-12_

- Enhancement: always enable the room image dialog and show a placeholder text if no image is available, instead of disabling the dialog. This makes the on-site navigation action available for all rooms
- Enhancement: disable bookmarks import and export when the database has not yet been loaded. This saves the user's time by prohibiting a multiple steps action that would always end up in failure
- Fix: duplicate menu items shown in session details screen (regression introduced in 2.2.0)
- Upgrade AndroidX Lifecycle library to 2.7.0.

## Version 2.2.0

_2024-01-08_

- Updated campus map image to 2024 version
- Convert all map images to lossless WebP (uses 20% less space than PNG)
- Remove unused Android 4.x resources
- Fix: bookmarks menu items are shown even when the bookmarks screen is not visible (regression introduced in 2.1.9).

## Version 2.1.9

_2024-01-06_

- New feature: session attachments, in addition to links
- New feature: link to stands web page in main menu (#49)
- Enhancement: remember scroll position when switching back to bookmarks list (#81)
- Enhancement: "Upcoming only" option in bookmarks list renamed to "Hide past sessions". Sessions in progress are now always visible (#78)
- Enhancement: add colored warning icon next to full rooms in session lists (#32)
- Enhancement: display the next sessions of the coming hour instead of the coming 30 minutes in the "Live" screen
- Translation: use the word "session" everywhere instead of "event"
- Remove Android Beam sharing feature (no longer supported since Android 14 SDK)
- Fix: scrolling jumps back to top in the Live screen
- Fix: room status is not displayed in bookmarks list (regression introduced in 2.1.3)
- Various internal refactorings
- Change min SDK version to 21 (Android 5.0) and target SDK version to 33 (Android 13).

## Version 2.1.3

_2023-03-21_

- Add native support for the `POST_NOTIFICATIONS` permission in Android 13 and change target SDK version to 33
- Fix toolbar colors in light theme (a dark ThemeOverlay is needed for the Toolbar)
- Upgrade libraries, Kotlin, Gradle plugin and build tools to their latest versions.

## Version 2.1.2

_2023-02-03_

- Fix crash related to missing `PendingIntent.FLAG_IMMUTABLE` required since Android 12, when building a notification.

## Version 2.1.1

_2023-01-18_

- Fix crash related to missing `SCHEDULE_EXACT_ALARM` permission which is now required when targeting Android 12+ to be able to schedule exact alarms
- Rewrote some Kotlin Flow code related to autorefresh timers
- Upgrade libraries, Gradle plugin and build tools to their latest versions
- Change compile SDK version to 33 (Android 13) but keep target SDK version to 31 until code is added to properly handle the new notifications permission.

## Version 2.1.0

_2022-02-11_

- Add dependency injection using Hilt
- Refactor code to use Kotlin Flow everywhere and remove LiveData
- Replace some SharedPreferences with the new DataStore API
- Rewrite all date and time code to use the `java.time` APIs (through desugaring)
- Add support for device timezone in addition to conference timezone for displayed dates and times (selectable in settings)
- Allow alarms to go off while the device is in low-power idle mode
- Refactor code to use the new Activity Result, Fragment Result and `MenuProvider` APIs
- Replace custom progress bar with `LinearProgressIndicator` from the Material Components library
- Change min SDK version to 19 (Android 4.4) and target SDK version to 31 (Android 12).

## Version 2.0.5

_2021-02-06_

- Added import bookmarks feature to read bookmarks list from a previously exported file in iCalendar format
- Updated room map images (add a few missing rooms)
- Change target SDK version to 30
- Many code tweaks and library updates.

## Version 2.0.1

_2020-01-29_

- Fix: initial section display in the toolbar (regression introduced in 2.0.0)

## Version 2.0.0

_2020-01-28_

- Rewrote the entire app using the Kotlin programming language. For more details, see the [video recording of the FOSDEM 2020 presentation](https://ftp.belnet.be/mirror/FOSDEM/video/2020/UA2.118/kotlin_migrating_fosdem_companion.mp4)

## Version 1.7.3

_2020-01-26_

- Remove usage of `app:drawableTint` attribute from layouts to tint icons, since it shows problems in some Android versions
- Fix notification colors when using native Dark mode in Android 10+
- Add support for other rooms than Janson in building J
- Upgrade Material Components, Lifecycle and Fragment libraries
- Remove legacy non-RTL attributes from layouts since the minSDKVersion is now 17
- Minor code tweaks.

## Version 1.7.2

_2020-01-06_

- Fix bookmarks export feature: file was corrupted.

## Version 1.7.1

_2020-01-05_

- Fix external web browser toolbar color when switching between light and dark theme.

## Version 1.7.0

_2020-01-04_

- Add dark theme, configurable in the settings screen
- Polished material design
- The app now uses `MaterialComponent` themes instead of AppCompat themes
- Replace `ViewPager` with `ViewPager2`. RTL is now properly supported in pagers as well
- Remove schedule update prompt on app startup. Updates are now performed automatically
- Update campus map image to 2020 version.

The app now requires API 17 (Android 4.2) or more recent.

## Version 1.6.2

_2019-02-02_

- Prevent crash on some devices when trying to add the same bookmark twice.

## Version 1.6.1

_2019-02-02_

- Update campus map image to show F1 building.

## Version 1.6.0

_2019-01-29_

- Migrate from support libraries to AndroidX
- Refactor the entire database code to use Room (Google's ORM for Android), LiveData and the paging library
- Replace custom widgets with Material components for SnackBar, NavigationView, Floating Action Button and TabLayout
- The bookmark floating action button no longer swipes along with the content in the track event details screen
- Auto-hide the top toolbar on scroll in event details screens
- Implement collapsing toolbar in the speaker details screen
- Add material style fast scroll thumb and track drawable on API<21
- Replace remaining ListView instances with `RecyclerView`
- Display search error messages using a Snackbar instead of a dialog
- Remove the legacy underline paging indicator (swiping it still possible)
- Replace the legacy horizontal progress bar with a custom material horizontal progress bar backport implementation on API < 21
- Change the highlighted session background color to use accent color (blue)
- Fix the drawer menu shadow image for RTL configuration
- Improve RTL support in all layouts
- Prevent `LinkClickListener` from leaking memory
- Replace `LocalBroadcastManager` with a consumable `LiveData`
- Fix links to speakers with a '@' in their name.