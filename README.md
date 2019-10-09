[![Build Status](https://travis-ci.org/cbeyls/fosdem-companion-android.svg?branch=master)](https://travis-ci.org/cbeyls/fosdem-companion-android)

# FOSDEM Companion

Advanced native Android schedule browser application for the [FOSDEM](http://fosdem.org/) conference in Brussels, Belgium.

This is a new implementation of the [legacy FOSDEM app](https://github.com/rkallensee/fosdem-android/). The code has been rewritten from scratch and the features have been extended. It is built on top of the latest [Jetpack](https://developer.android.com/jetpack/) libraries by Google.

The name FOSDEM and the gear logo are registered trademarks of FOSDEM VZW. Used with permission.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
   height="80">](https://f-droid.org/packages/be.digitalia.fosdem/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=be.digitalia.fosdem)

## How to build

All dependencies are defined in ```app/build.gradle```. Import the project in Android Studio or use Gradle in command line:

```
./gradlew assembleRelease
```

The result apk file will be placed in ```app/build/outputs/apk/```.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## Used libraries

* [AndroidX](https://developer.android.com/jetpack/androidx/) by The Android Open Source Project
* [Android Architecture Components](https://developer.android.com/topic/libraries/architecture/) by The Android Open Source Project
* [Material Components for Android](https://material.io/develop/android/) by The Android Open Source Project
* [PhotoView](https://github.com/chrisbanes/PhotoView) by Chris Banes, Marek Sebera and John Carlson

## Contributors

* Christophe Beyls
