# Portalissimo

English | [Français](README_FR.md)

Portalissimo is an experimental fork of [Jami for Android](https://github.com/savoirfairelinux/jami-client-android) for **Meta Portal TV running Android 9**. It keeps Jami's calling engine and TV interface, with automatic camera framing through Meta's Smart Camera SDK when the device allows access.

This is an independent project, not an official Meta or Jami distribution.

Download: [Portalissimo 1.0 — ARM64 APK](https://github.com/pintorus/Portalissimo/releases/tag/v1.0). This build uses the same local debug signing key as the development builds.

## Features and current status

- Jami video calls and conferencing, with a TV interface.
- Smart Camera `BasicSpotlight` subject tracking during Camera2 preview and video capture, with `DefaultAuto` fallback if the mode is rejected. The user confirmed satisfactory framing on their Portal TV; results remain dependent on the firmware.
- Standard camera capture remains available if the Meta service is absent, denies access or disconnects.
- Camera control is released when capture stops or is reconfigured.
- A `noPush` build variant without Google push services.

The base application was installed and its TV account wizard opened on a Portal TV running Android 9 (API 28). The Smart Camera integration builds successfully and has unit tests, but **end-to-end calls and meeting stability still need full validation on the device**. No manual mode selector is exposed.

## Build

Requirements:

- Full JDK 21, including `javac`.
- Android SDK 36, Build Tools 36.0.0, NDK 28.2.13676358 and CMake 4.1.2.
- SWIG 4.2 or newer, Autoconf, Automake, Autopoint, Libtool, Gettext, Meson, Yasm, Ninja, M4, pkg-config and a native build toolchain.
- Rust with `rustup`; the native build installs the required Android targets.
- Node.js 24 and npm for Jami's bundled collaborative editor.

Configure `JAVA_HOME` and `ANDROID_HOME` for your installations and put the tools on `PATH`. Then:

```sh
git clone --recurse-submodules --branch portal-tv https://github.com/pintorus/Portalissimo.git
cd Portalissimo
cd daemon/bin/jni
PACKAGEDIR="$PWD/../../../jami-android/libjamiclient/src/main/java" bash ./make-swig.sh
cd ../../../jami-android
./gradlew :app:assembleNoPushDebug -Parchs=arm64-v8a
```

For an existing checkout, run `git submodule update --init --recursive` before generating the JNI bindings. You can also open `jami-android` in Android Studio after configuring the dependencies.

The ARM64 APK is generated at:

```text
jami-android/app/build/outputs/apk/noPush/debug/app-noPush-debug.apk
```

The minimum Android API is 26, which includes Android 9/API 28. Compiling with SDK 36 does not upgrade the Portal's operating system.

## Tests

From `jami-android`:

```sh
./gradlew :app:testNoPushDebugUnitTest -Parchs=arm64-v8a
```

Validation performed: 15 JVM tests and 64 collaborative-editor JavaScript tests passed, and the ARM64 debug APK built successfully. The Smart Camera tests cover availability, permission failures, late callbacks, disconnections and resource cleanup.

## Install on Portal TV

Enable USB debugging and identify the Portal TV serial with `adb devices`. From `jami-android`, replace `PORTAL_SERIAL` with that serial:

```sh
adb -s PORTAL_SERIAL install -r app/build/outputs/apk/noPush/debug/app-noPush-debug.apk
adb -s PORTAL_SERIAL shell pm grant cx.ring android.permission.CAMERA
adb -s PORTAL_SERIAL shell pm grant cx.ring android.permission.RECORD_AUDIO
adb -s PORTAL_SERIAL shell am start -W -n cx.ring/.tv.main.HomeActivity
```

The package ID remains `cx.ring`. This debug build uses a local debug signing key and cannot replace an existing Jami installation signed with another key. Back up your accounts before removing an existing installation: uninstalling deletes its local accounts and data.

## Smart Camera

The integration uses `com.facebook.portal:smartcamera:1.4.0` and requests `com.facebook.portal.permission.SMART_CAMERA_CONTROL`. It requests `BasicSpotlight`, with `DefaultAuto` fallback, only when Meta's `com.facebook.portal.aiservice` service is present and grants control. Declaring the permission does not guarantee that a particular Portal firmware will grant it.

To inspect activation and fallback messages:

```sh
adb -s PORTAL_SERIAL logcat -s PortalSmartCamera
```

Further technical notes in French: [Portal TV build and installation](PORTAL-TV.md) and [Smart Camera SDK research and limitations](SMART-CAMERA.md). Meta's [SDK samples](https://github.com/fbsamples/portal-sdk-samples) provide the upstream usage examples.

## Upstream and license

Portalissimo is based on Jami by Savoir-faire Linux and its contributors. The Jami code is distributed under GPL-3.0-or-later; see [COPYING](COPYING). Third-party dependencies retain their respective licenses.

Report fork-specific problems in [Portalissimo issues](https://github.com/pintorus/Portalissimo/issues). The [upstream Jami project](https://git.jami.net/savoirfairelinux/jami-client-android) remains the source for Jami development.
