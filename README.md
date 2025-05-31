# Waterfox for Android

The all-new Waterfox for Android browser is based on [GeckoView](https://mozilla.github.io/geckoview/) and [Mozilla Android Components](https://mozac.org/).

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/

## Building with Local Android Components

To build Waterfox for Android with local Android components - as opposed to via Android Studio and Mozilla's Maven respository - you'll first need to build GeckoView from source and then build Waterfox for Android itself, pointing it to your local GeckoView build.

These instructions are based on if you were to start from a clean Linux distribution installation.

### 1. Clone and Prepare GeckoView

First, clone the Firefox repository (which contains GeckoView). These instructions assume you will clone it into a directory named `firefox`. We do not care about the commit history, so we are shallow cloning.

```shell
git clone --depth 1 --branch release https://github.com/mozilla-firefox/firefox/
cd firefox
```

### 2. Install Python Dependencies

Ensure you have `python3-pip` installed, which is required by Mozilla's build scripts.

```shell
sudo apt-get install python3-pip
```

### 3. Bootstrap GeckoView

Run the `mach` bootstrap command to set up the build environment for GeckoView specifically for Android.

```shell
./mach --no-interactive bootstrap --application-choice="GeckoView/Firefox for Android"
```

### 4. Configure the GeckoView Build

Create a `.mozconfig` file in the `firefox` directory with the necessary build options. This configuration enables the Android mobile project, optimizes the build, disables debug symbols, and enables GeckoView Lite.

```shell
rm -f mozconfig
cat > .mozconfig << 'EOF'
ac_add_options --enable-project=mobile/android
ac_add_options --enable-optimize
ac_add_options --disable-debug
ac_add_options --enable-geckoview-lite
EOF
```

### 5. Build GeckoView

Compile GeckoView and its binaries. This can take a significant amount of time.

```shell
./mach build && ./mach build binaries
```

### 6. Publish GeckoView to Maven Local

Publish the GeckoView and Exoplayer2 artifacts to your local Maven repository. This makes them accessible to the Waterfox for Android build system.

```shell
./mach gradle geckoview:publishDebugPublicationToMavenLocal
./mach gradle exoplayer2:publishDebugPublicationToMavenLocal
```

### 7. Clone Waterfox-Android

Navigate out of the `firefox` directory (or whatever you named your GeckoView clone) and clone the Waterfox-Android repository.

```shell
cd ../
git clone git@github.com:BrowserWorks/Waterfox-Android.git
cd Waterfox-Android
```

### 8. Configure Local Properties for Waterfox-Android

Create a `local.properties` file in the root of the `Waterfox-Android` project. This file tells the Waterfox build system where to find your locally built GeckoView.

Execute the following command in the `Waterfox-Android` directory:

```shell
cat > local.properties << 'EOF'
dependencySubstitutions.geckoviewTopsrcdir=/home/$USER/firefox
dependencySubstitutions.geckoviewTopobjdir=/home/$USER/firefox/obj-$TRIPLET
EOF
```

The `dependencySubstitutions.geckoviewTopsrcdir` path should point to the root of your GeckoView source code (e.g., the `firefox` directory you cloned in Step 1). The `dependencySubstitutions.geckoviewTopobjdir` path should point to the object directory within your GeckoView build, which is typically `obj-` followed by your architecture triplet (e.g., `obj-x86_64-unknown-linux-android`).

For example, if you cloned GeckoView to `/workspace/firefox` and your object directory is `/workspace/firefox/obj-x86_64-unknown-linux-android`, your `local.properties` would look like:

```Waterfox-Android/local.properties#L1-2
dependencySubstitutions.geckoviewTopsrcdir=/workspace/firefox
dependencySubstitutions.geckoviewTopobjdir=/workspace/firefox/obj-x86_64-unknown-linux-android
```

### 9. Set Environment Variables

Set the `JAVA_HOME` and `ANDROID_HOME` environment variables. The paths shown below are typical for a `.mozbuild` setup (created during GeckoView bootstrap) but might differ on your system. Ensure these point to a JDK 17 and a valid Android SDK.

```shell
# These paths might differ based on your .mozbuild setup
export JAVA_HOME=$HOME/.mozbuild/jdk/jdk-17.0.13+11/ # Or your JDK 17 path
export ANDROID_HOME=$HOME/.mozbuild/android-sdk-linux/ # Or your Android SDK path
```

### 10. Build Waterfox-Android

Finally, clean and build the Waterfox for Android application.

To build a **debug** version:

```shell
./gradlew clean app:assembleDebug
```

To build a **release** version:

```shell
./gradlew clean app:assembleRelease
```

Note: For release builds, you will need to have set up signing configurations as per standard Android development practices.
