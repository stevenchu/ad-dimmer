# Screen Cover

**A quieter, less distracting ad break—using the remote you already have.**

Screen Cover is a small, free Android TV app. Double-tap **Volume Down** to put a dark cover over the picture. Double-tap again, or press **Back**, when you want to return. A small pill-shaped opening can leave the ad countdown visible.

With an optional Sonos connection, covering the picture also mutes your soundbar. Uncovering restores its previous mute state.

**This is manually triggered. It does not detect, skip, shorten, or block ads.** Video is intended to keep playing underneath the cover. There is no timer that automatically brings the picture back.

[Download the early release](https://github.com/stevenchu/screen-cover/releases/tag/v0.2.0) · [Setup guide](docs/SETUP.md) · [Report a problem](https://github.com/stevenchu/screen-cover/issues)

![Direct projector screenshot during a Prime Video ad break with the dimmer active and countdown visible](docs/prime-ad-dimmer.png)

*Actual Prime Video ad break captured from the projector with the dimmer active. The protected video layer appears black in direct screenshots; this image does not demonstrate the faint picture visible through the 95% cover on the physical projection.*

[View the illustrated controls overview](docs/overview.svg)

## What you can do

| Control | Result |
| --- | --- |
| Double-tap Volume Down within 300 ms | Cover / uncover |
| Back while covered | Uncover without sending Back to the video app |
| Volume Up while covered | Uncover, then normal volume up |
| Single press or hold Volume Down | Lower volume, with a 300 ms initial delay |

- Choose how dark the cover is; the default is **95% opacity**.
- Move and resize the rounded timer opening, or turn it off.
- Save a visual preset per app, with automatic selection or a manual override. Prime is the initial identity; other layouts need testers.
- Optionally mute a **Sonos home-theater soundbar** over your home network.
- Use it without a soundbar connection for a visual cover only.

## Start here

1. Download `ScreenCover.apk` from [Releases](https://github.com/stevenchu/screen-cover/releases/tag/v0.2.0) and install it on your Android TV device. It is not a Play Store app.
2. Open **Screen Cover** and review the settings. Audio control is off until you configure it.
3. Enable **Screen Cover remote shortcut** under Android's Accessibility settings. Disable other services that remap the same remote buttons.
4. Play a video and test cover / uncover, volume control, and sound before relying on it.

See the [step-by-step setup and removal guide](docs/SETUP.md), including ADB installation when needed. No root or firmware flashing is required.

## Where it has been tried

This started as a personal tool for an **original XGIMI Halo+ (2023), Android 11, Bluetooth remote, Prime Video, and Sonos Arc Ultra over HDMI ARC**. The original prototype's remote gesture, visual cover, and direct Sonos mute were exercised on that setup. The community version makes those settings editable.

**v0.2.0 is an early experimental release.** Android 8.0+ is the build minimum, not a promise of compatibility. Other TVs, projectors, apps, remotes, Sonos models, and newer Android restrictions need community testing. Check [verification notes](docs/VERIFICATION.md) for exactly what was tested.

## Limits worth knowing

- The timer opening is a fixed position, not automatic timer detection. Its default was fitted to one 1920 × 1080 setup. Change its position for your app and display.
- There is **no live background blur**. This build uses a translucent black layer. Android 11 lacks the supported Android 12+ cross-window blur API. [Android documentation](https://source.android.com/docs/core/display/window-blurs)
- Some video apps may pause, hide overlays, or handle remote buttons differently. Playback continuing underneath needs testing on your setup.
- Sonos mute requires a reachable compatible local speaker and may trail the picture slightly due to Wi-Fi. Network failures show a message; the visual cover may still work.
- If Android kills the app while it has muted Sonos, sound can stay muted. Reconnecting the service attempts restoration; you can always unmute in the Sonos app.
- Native projector mute was unreliable on the original HDMI ARC setup. This release does **not** promise built-in speaker, Bluetooth audio, or universal HDMI mute support.
- Only one Accessibility service can reliably filter the remote's keys. Button Mapper and similar remappers may conflict.

## Privacy and permissions

There are **no accounts, analytics, advertising SDKs, microphone access, screen recording, or screen-content retrieval**. The Accessibility service receives button events. It does not inspect what you watch or read the timer.

Optional network access sends local Sonos `GetMute` / `SetMute` commands to the private IPv4 address **you enter**. Audio is off by default. Settings and recovery state stay on the device. There are no developer servers. See [Privacy](PRIVACY.md).

## Build or contribute

The source is plain Java with Android's built-in widgets and APIs; there are no third-party app libraries. Start with [BUILDING.md](BUILDING.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

Useful contributions include compatibility reports, better TV setup flows, timer calibration controls, remote-gesture tests, and reliable speaker discovery. Please keep automatic ad detection separate from claims about the manual cover that exists today.

The [preset architecture](docs/PRESETS.md) explains where to contribute. Use the [QA checklist](docs/QA.md) to report actual device and app coverage.

MIT licensed. Independent community project; not affiliated with XGIMI, Amazon, Google, or Sonos.
