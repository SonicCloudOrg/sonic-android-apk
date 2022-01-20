# layoutlib.jar

A stripped down version of the Android SDK's `layoutlib.jar`.

## Why do we need this?

Unfortunately, the private APIs we need access to include an abstract class which we must subclass. This is not possible via reflection. Instead, we'll simply provider a JAR with the necessary methods, making the whole thing a breeze. The APIs have so far stayed almost entirely stable.

## How to generate the JAR

Copy one of the `layoutlib.jar` files in your Android SDK folder to the `libs` folder in this module. You can then optionally run `./generate.sh <PATH_TO_ORIGINAL_LAYOUTLIB_JAR>` to copy the original `layoutlib.jar` and remove any unnecessary content from it, slimming the archive down considerably.

The current base file is `$ANDROID_SDK/platforms/android-21/data/layoutlib.jar`.
