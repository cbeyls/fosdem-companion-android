# FOSDEM Companion

Advanced native Android schedule browser application for the [FOSDEM](http://fosdem.org/) conference in Brussels, Belgium.

This is a new implementation of the [legacy FOSDEM app](https://github.com/rkallensee/fosdem-android/). The code has been rewritten from scratch and the features have been extended. It uses loaders and fragments extensively and is backward compatible up to Android 2.1 thanks to the support library.

To get more information and install the app, look at the [Google Play Store](https://play.google.com/store/apps/details?id=be.digitalia.fosdem) page.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## Build dependencies

The project is built with Eclipse and depends on the Android Support Library and the compatibility ActionBar. You need to import the appcompat project from your local SDK folder to your Eclipse workspace and add it as a dependency to this project. All other dependencies are included in the project.

## Used libraries

* [Android Support Library](http://developer.android.com/tools/support-library/) by The Android Open Source Project
* [PagerSlidingTabStrip](https://github.com/astuetz/PagerSlidingTabStrip) by Andreas Stuetz
* [ViewPagerIndicator](http://viewpagerindicator.com/) by Jake Wharton
* [PhotoView](https://github.com/chrisbanes/PhotoView) by Chris Banes

## Contributors

* Christophe Beyls
