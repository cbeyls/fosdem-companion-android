[![Android CI](https://github.com/cbeyls/fosdem-companion-android/actions/workflows/android.yml/badge.svg)](https://github.com/cbeyls/fosdem-companion-android/actions/workflows/android.yml) [![F-Droid](https://img.shields.io/f-droid/v/be.digitalia.fosdem)](https://f-droid.org/packages/be.digitalia.fosdem/)

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

* [Android Jetpack](https://developer.android.com/jetpack) by The Android Open Source Project
* [Dagger Hilt](https://dagger.dev/hilt/) by The Dagger Authors
* [Material Components for Android](https://material.io/develop/android) by The Android Open Source Project
* [OkHttp](https://github.com/square/okhttp) by Square, Inc.
* [Moshi](https://github.com/square/moshi) by Square, Inc.
* [Kotlin Standard Library](https://github.com/JetBrains/kotlin) by JetBrains s.r.o. and Kotlin Programming Language contributors
* [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) by JetBrains s.r.o.
* [PhotoView](https://github.com/Baseflow/PhotoView) by Chris Banes, Marek Sebera and John Carlson

## Contributors

* Christophe Beyls
