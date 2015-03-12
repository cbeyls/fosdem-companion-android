# FOSSASIA Companion

Advanced native Android schedule browser application for the [FOSSASIA](http://fossasia.org/) conference in Singapore.

This is build upon the [FOSDEM Companion For android](https://github.com/cbeyls/fosdem-companion-android). It uses loaders and fragments extensively and is backward compatible up to Android 2.1 thanks to the support library.

To get more information and install the app, look at the [Google Play Store](https://play.google.com/store/apps/details?id=be.digitalia.fosdem) page.

The name FOSDEM and the gear logo are registered trademarks of FOSDEM VZW. Used with permission.

## How to build

All dependencies are defined in ```app/build.gradle```. Import the project in Android Studio or use Gradle in command line:

```
./gradlew assembleRelease
```

The result apk file will be placed in ```app/build/outputs/apk/```.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## Used libraries

* [Android Support Library](http://developer.android.com/tools/support-library/) by The Android Open Source Project
* [ViewPagerIndicator](http://viewpagerindicator.com/) by Jake Wharton
* [PhotoView](https://github.com/chrisbanes/PhotoView) by Chris Banes
* [Volley Library](https://android.googlesource.com/platform/frameworks/volley)

## Contributors

* Christophe Beyls
* Abhishek Batra
* Manan Wason
* Pratik Todi
* Mario Behling
* Tymon Radzik

