# Build from source

Requirements: a JDK 17, Python 3, `zip`, and official Android SDK **Platform 35** plus **Build-Tools 35.0.0**. Android Studio's SDK Manager can install these. No Gradle dependency resolution is needed.

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk
bash scripts/test.sh
bash scripts/build.sh
```

You can override `ANDROID_BUILD_TOOLS` and `ANDROID_PLATFORM_JAR` for a nonstandard SDK layout. Output: `build/ScreenCover.apk` and `build/SHA256SUMS`. The script compiles Java, converts it to DEX, packages resources, aligns, signs, and verifies the APK.

## Signing

By default the script creates a **development** signing key under ignored `build/signing/`. Keep that directory if you want to install your next build over the previous one. Never commit a signing key.

For a separately managed release identity, set `SCREEN_COVER_KEYSTORE`, `SCREEN_COVER_KEY_ALIAS`, and `SCREEN_COVER_STORE_PASS`. The keystore must already exist. A build with a different signing key cannot update an installed copy with the same application ID; uninstalling loses local settings.

The first GitHub release uses a dedicated local signing key, not the personal projector prototype's key. Its certificate fingerprint and APK SHA-256 checksum are included with the release. This is a community sideload release, not Play Store certification.

## Layout

- `MainActivity.java`: TV settings, optional audio destination, preview.
- `CoverService.java`: remote interception, translucent overlay, serialized Sonos commands and recovery.
- `Config.java`: private IPv4 and timer-geometry validation.
- `res/xml/service.xml`: narrow Accessibility capability declaration; no screen-content retrieval.
- `tests/ConfigTest.java`: validation boundary checks.

Application ID: `org.stevenchu.screencover`; Android minimum 26, target 30. The target follows the original Android 11 prototype and should be reviewed before broader distribution or a Play Store submission. Source builds with SDK 35. There is no exported diagnostic broadcast receiver in the public APK.

## Before a release

Build and verify the APK. Exercise setup with a D-pad, overlay on/off, the timer cutout, single/held volume, Back, already-muted restoration, failed network requests, service reconnection, and upgrade behavior. Record actual devices and results in `docs/VERIFICATION.md`. Do not infer remote or protected-video behavior from synthetic ADB key events.
