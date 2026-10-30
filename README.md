# Ad Dimmer

**Dim ad breaks with your Android TV remote.**

Choose a remote shortcut to darken the picture. Keep the ad countdown visible, then use the shortcut again—or press **Back**—to return.

Free and open source. Made for Android TV devices, including TVs, projectors, and streaming boxes. **Tested on Android 11 with Prime Video; other setups need testers.**

**[Download Ad Dimmer](https://github.com/stevenchu/ad-dimmer/releases/download/v0.3.0/AdDimmer.apk)** · [Installation help](docs/SETUP.md) · [Report a problem](https://github.com/stevenchu/ad-dimmer/issues)

![Before and after an illustrative 95% ad dimmer, with the countdown visible](docs/ad-dimmer-demo.png)

*Illustrative demo using an AI-generated fictional ad. [Actual projector screenshot](docs/SCREENSHOTS.md).*

## Set it up

1. **Install the app** on your Android TV device. [Need help?](docs/SETUP.md)
2. **Open Ad Dimmer** from your Apps list. Choose how dark the picture should be and adjust the timer opening.
3. **Enable Ad Dimmer remote shortcut** in Android's Accessibility settings.
4. Open **Choose / test remote shortcut**, try a 90-second temporary mapping, then confirm it works. Nothing is assigned until you choose it.

No paid app, account, root, or firmware change required. Disable other remote-button remappers to avoid conflicts.

Normal volume control is immediate. Single-press replacement shows a warning; Home, Back, and essential navigation are protected.

## Your controls

| Remote button | What happens |
| --- | --- |
| Your confirmed shortcut | Dim / restore the picture |
| Back while dimmed | Restore the picture |
| Volume Up while dimmed | Restore the picture and raise volume |
| Tap or hold Volume Down (default) | Normal immediate volume control |

[Shortcut safety and recovery](docs/SHORTCUTS.md) explains temporary tests and what happens to normal button actions.

## Make it yours

- Change the darkness; the default is **95%**.
- Move or resize the timer opening—or hide it entirely.
- Save a layout for each app, with automatic selection or a manual choice.
- Optionally connect **Sonos** to mute and restore sound too. Other audio outputs are not currently controlled.

## Before you install

**You trigger it yourself.** Ad Dimmer does not detect or skip ads. Playback is intended to continue underneath; press the shortcut again when the ad ends.

This is an **experimental release**. The tested setup is Prime Video on an Android 11 XGIMI Halo+ with a Bluetooth remote; optional audio was tested with a Sonos Arc Ultra. Requires Android TV. Other devices, Android versions, and streaming apps remain unverified.

It does not record your screen or send viewing history anywhere. Accessibility identifies the active app and remote buttons; optional Sonos control stays on your home network. [Privacy](PRIVACY.md) · [Known limits and testing](docs/VERIFICATION.md)

## Help it grow

Tried it on another device or streaming app? [Share what worked](https://github.com/stevenchu/ad-dimmer/issues/new?template=compatibility.md).

[Contribute](CONTRIBUTING.md) · [Build from source](BUILDING.md) · [Preset foundation](docs/PRESETS.md) · [Test checklist](docs/QA.md)

MIT licensed. Independent project; not affiliated with Amazon, Google, XGIMI, or Sonos.
