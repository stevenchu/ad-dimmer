# Set up Screen Cover

## Install

Download `ScreenCover.apk` from the project's GitHub release onto your Android TV device. Use a file-transfer app or USB drive and open it with a file manager. Android may ask you to allow that file manager to install unknown apps. Turn that permission back off afterward if you no longer need it.

If your TV cannot open APK files, use the computer method below. A Google Play account, paid Button Mapper upgrade, root, and a new projector OS are not required.

## Choose your settings

Open Screen Cover from the TV's Apps list. Move between fields and buttons with the D-pad and OK button.

- **Opacity:** 95 means a 95% opaque black cover; 100 is completely opaque.
- **Timer opening:** leave it on for a pill-shaped clear area; turn it off for a full cover.
- **Left / Top / Width / Height:** pixels relative to a 1920 × 1080 reference screen. They scale with the display. Smaller Left moves it left; smaller Top moves it up. The opening must fit on the reference screen.
- **Sonos:** leave off for visual cover only. To also mute, enter the private IPv4 address of the soundbar playing your TV audio, then enable the option. Find its address in the Sonos app's system information or your router's device list. A router address reservation helps keep it stable. This version does not discover speakers automatically.

Save settings. Sonos control uses local HTTP on port 1400 and supports private IPv4 addresses only. Do not forward this port through your router.

## Enable the remote shortcut

The typical path on Android TV is **Settings → Device Preferences → Accessibility → Screen Cover remote shortcut → Enable**. The app also has an Open Accessibility settings button; some XGIMI firmware opens the general settings screen instead, so navigate there manually.

Read Android's permission notice. This service is for intercepting remote buttons and displaying the cover. It does not request screen-content retrieval. Only enable apps you trust.

Disable other remote key-filtering services, such as Button Mapper, before enabling this one. On newer Android versions a sideloaded app's Accessibility access may be restricted; follow the device's own instructions if it offers “Allow restricted settings.” If it does not, report the model and version rather than bypassing device security.

## First test

1. Play something at a comfortable volume.
2. Double-tap Volume Down quickly. The picture should dim.
3. If Sonos is enabled, confirm sound stops.
4. Press Back. Picture and prior sound state should return, without navigating away from the video.
5. Repeat using double-tap to uncover. Test a single Volume Down tap, a hold, and Volume Up.
6. During an actual ad, fit the timer opening. If it misses the timer, uncover and adjust settings.
7. Repeat after restarting the device; Accessibility-service persistence varies by firmware.

If you were already muted before covering, you should remain muted afterward.

## Computer installation (ADB)

Install Google's [SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools) on the computer. Enable the TV's Developer options and the debugging connection supported by its firmware. Put both devices on the same home network and approve the computer on the TV when prompted.

Older firmware may expose network ADB directly; other versions need USB initialization or a wireless pairing code. Do not assume that enabling USB debugging opens a network port. Follow [Android's connection instructions](https://developer.android.com/tools/adb) for your version and manufacturer.

Once connected and authorized:

```sh
adb devices
adb -s YOUR_DEVICE install --no-incremental -r ScreenCover.apk
```

Replace `YOUR_DEVICE` with the exact authorized entry shown by `adb devices`. For a TV that already exposes network debugging, connect with `adb connect YOUR_TV_IP:PORT`. For firmware that offers pairing, use its displayed pairing and connection ports with `adb pair` and `adb connect`.

## Stop or remove

Uncover first, then disable **Screen Cover remote shortcut** in Accessibility. Uninstall Screen Cover from Apps if you want to remove it. You can then re-enable your old button remapper.

If sound stays muted, unmute directly in the Sonos app. Reconnecting Screen Cover's service also attempts to restore a saved mute state. Do not reset the projector or flash firmware to remove this app.

## Problems

- **Double-tap does nothing:** check Accessibility is still enabled and another key-filtering service is not competing.
- **Single taps feel slower:** the 300 ms wait distinguishes a tap from a double-tap.
- **Sound does not mute:** check Sonos is enabled, the correct soundbar address is saved, and both devices can reach each other. Wi-Fi guest isolation can block local control.
- **Picture returns but no sound:** use Sonos to unmute; report the sequence and any error message.
- **Timer opening is wrong:** adjust its geometry; the app does not recognize the timer automatically.
- **Video pauses:** report your device, Android version, and video app. Keeping playback active is not universal.
