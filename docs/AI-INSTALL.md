# Install with an AI assistant

An AI assistant can walk you through installation. An assistant with permission to run commands on your computer can also download the app, connect to your Android TV, and install it. You will need to use your TV remote to approve the connection, enable the Accessibility service, and test your shortcut.

Copy this request into your assistant and include your TV model and computer operating system if you know them.

> Help me install Ad Dimmer on my Android TV. Read https://github.com/stevenchu/ad-dimmer/blob/main/docs/AI-INSTALL.md and follow its process. Use the official release linked from the current README. Help me connect my computer to my TV, preserve any existing settings, and verify installation and my physical remote shortcut. Explain which steps I need to perform on the TV.

## Instructions for the assistant

Read the current [README](../README.md), [setup guide](SETUP.md), [shortcut guide](SHORTCUTS.md), and [project context](../AGENTS.md). Check the release notes for the APK you will install. The current implementation and release determine available features. The unreleased v0.5.0 source preview implements Android TV media muting with local recovery tests passing and live device verification pending. The current public v0.4.0 release controls Sonos audio when configured.

### 1. Establish the setup

Identify the TV or streaming-device model, Android version, computer operating system, and whether Ad Dimmer is already installed. Ask for missing information as it becomes needed. Use the user's selected device when several devices are connected.

If you can run commands on the user's computer, perform the computer steps within their installation request. If you can only provide advice, give commands suited to their operating system and inspect the results they provide. A remote cloud computer usually cannot reach the TV's home network. In that case, guide the user on their local computer.

### 2. Get the official app and tools

Follow the Download link in the current repository README to choose the public release. A release can be marked pre-release, so check and communicate its status. Download `AdDimmer.apk` and its `SHA256SUMS` from that same release. Use a dedicated download folder to avoid confusing older files with the selected APK.

Compare the APK's SHA-256 checksum with the matching entry in `SHA256SUMS`. Use the appropriate command for the computer.

| Computer | Command |
| --- | --- |
| macOS | `shasum -a 256 AdDimmer.apk` |
| Linux | `sha256sum AdDimmer.apk` |
| Windows PowerShell | `Get-FileHash .\AdDimmer.apk -Algorithm SHA256` |

A mismatch means the downloaded file needs investigation or a fresh download before installation. If a release has no checksum, report that verification gap. If Android SDK Build Tools are already available, `apksigner verify --print-certs AdDimmer.apk` can also verify the APK signature and compare its signing certificate with the release's `RELEASE_CERTIFICATE.txt`. The checksum and signing-certificate digest identify different things.

Reuse an installed `adb` when available. Otherwise download Google's SDK Platform Tools for the user's operating system and extract them. Use the executable's full path if it is outside PATH. In PowerShell, use `.\adb.exe` for an executable in the current directory, or use `& "C:\path\to\platform-tools\adb.exe"` for an absolute path with spaces. Commands below use `adb` as shorthand for the selected executable.

Official references are [SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools) and [Android Debug Bridge](https://developer.android.com/tools/adb).

### 3. Connect to the TV

Keep the computer and TV on the same trusted home network for a network connection. Guide the user to Developer options, usually enabled by repeatedly selecting the Android build entry in About. Menu names and debugging support vary by device.

Use whichever connection the device actually supports.

- **Wireless debugging with pairing.** Have the user open the pairing-code screen. Run `adb pair TV_IP:PAIRING_PORT` and enter the code interactively. Then use `adb connect TV_IP:CONNECTION_PORT` if pairing has not connected the device automatically. Pairing and connection ports can differ. Read them from the TV. Android documents this wireless workflow for Android TV 13 and newer, and firmware availability varies.
- **Manufacturer-provided network debugging.** Enable it on the TV, then run `adb connect TV_IP:PORT` using the device's documented port. Some older TVs expose port 5555, including the previously tested Halo+ setup. Confirm this for the current device.
- **USB debugging.** Use a supported data connection and the manufacturer's instructions. Some firmware requires an initial USB connection before network ADB can be enabled. Check its documentation before using `adb tcpip 5555`. Many TV USB ports are intended for accessories and cannot provide this connection.

Have the user approve the computer's debugging authorization prompt on the TV. Then run the following.

```sh
adb devices -l
```

The chosen entry must show `device`. Replace `YOUR_DEVICE` in all subsequent commands with that exact serial or network address. Verify its identity before installing.

```sh
adb -s YOUR_DEVICE shell getprop ro.product.model
adb -s YOUR_DEVICE shell getprop ro.build.version.release
adb -s YOUR_DEVICE shell getprop ro.build.version.sdk
```

Ad Dimmer currently requires Android 8 / API 26 or newer. Connection approval and an authorized device listing establish access. They do not establish that installation or the remote shortcut works.

### 4. Inspect and install

Check the installed public package before changing it.

```sh
adb -s YOUR_DEVICE shell pm path org.stevenchu.screencover
adb -s YOUR_DEVICE shell dumpsys package org.stevenchu.screencover
```

An installed package has an APK path. Record its version name and code from the package report. An absent package can produce an empty result or an error. Installation of an update should preserve existing settings.

```sh
adb -s YOUR_DEVICE install --no-incremental -r AdDimmer.apk
```

Require `Success`, then repeat the package inspection to confirm the expected version. Open the app from the TV's Apps list or launch it with the following command.

```sh
adb -s YOUR_DEVICE shell am start -W -n org.stevenchu.screencover/.MainActivity
```

Earlier personal testing used a separate `local.halocover` package. If that app is present, identify which app the user wants to use and which Accessibility service is enabled. Installing the public package creates a separate app with its own settings. Personal builds, signing keys, IP addresses, and temporary test receivers from development chats are specific to that setup.

### 5. Complete setup on the TV

Guide the user through [the setup guide](SETUP.md) using their physical remote.

1. Open Ad Dimmer and choose the desired picture preset and darkness.
2. Enable **Ad Dimmer remote shortcut** in Android Accessibility settings after the user reviews the permission notice. Other remote-button remappers may compete for the same key events. Have the user choose which service to use.
3. Open **Shortcut → Change shortcut** and choose a button and gesture. Explain its normal button behavior using the shortcut guide. Double press and hold can also trigger the button's ordinary action.
4. Start the 90-second temporary test. Have the user physically trigger the shortcut, remove the cover, and return to confirm **Keep this shortcut** before the trial expires. An unconfirmed trial returns to the previous mapping.
5. Configure Sonos only if the user wants it and has compatible equipment. Read the selected release's sound behavior before promising muting.

If Android restricts Accessibility access for a sideloaded app, follow the device's own Allow restricted settings flow when available. Explain a device limitation if the option is unavailable. Let the user approve Accessibility through Android's interface.

### 6. Verify the experience

Have the user play a video and physically try the following checks.

- Their confirmed shortcut dims the picture and removes the cover when used again.
- Back removes the cover without leaving the video. Home remains usable.
- Normal volume controls behave as expected.
- Prime Video and HBO Max select their respective presets. Left and Right switch presets while dimmed.
- During an actual ad, the timer opening fits the countdown. Adjust it in Advanced when needed.
- If Sonos is configured, covering mutes it and uncovering restores its previous mute state, including an already-muted starting state.
- After restarting the TV, confirm Accessibility is still enabled and the shortcut works.

Record separately what command output confirmed and what the user confirmed on the TV. A successful installation proves the APK installed. Physical remote behavior, timer alignment, protected-video playback, and audio need their own checks. Existing Halo+ test results describe that tested setup.

Finish with the device model, installed version, selected shortcut, passed checks, and anything still awaiting confirmation.

### 7. Recover from problems

| Problem | Next step |
| --- | --- |
| `adb` unavailable | Locate the extracted Platform Tools executable and use its full path. |
| `unauthorized` | Ask the user to approve the prompt on the TV, then recheck the device list. |
| `offline`, connection refused, or timeout | Recheck the TV's address, displayed ports, debugging setting, and network isolation. Reconnect the selected device. Pair again if its pairing session expired. |
| Multiple devices | Select the confirmed TV and use `-s YOUR_DEVICE` for every device command. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Explain the signing-key mismatch. Keep the installed app and settings while identifying the correct signed release. Removing the old app loses local settings and requires the user's explicit choice. |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Compare installed and downloaded versions. Use a compatible newer release. A downgrade requires a separate decision about settings and compatibility. |
| Storage or SDK error | Read the exact installation error and compare free space and Android version with the release requirements. |
| App installed but launcher tile is missing or stale | Confirm the package/version, try the launch command above, and reopen the launcher or restart the TV with the user's agreement. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | The installed app may use a different signing key. Preserve the installed copy and settings while identifying its source. Explain that uninstalling to switch signatures loses settings and get the user’s agreement before doing so. |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Compare installed and downloaded version codes. Keep the newer installed build unless the user chooses a downgrade after reviewing its consequences. |
| `INSTALL_FAILED_OLDER_SDK` | Check the device API level against the APK requirement. Report an unsupported Android version. |
| Storage or transfer failure | Inspect the error, free space with the user’s guidance if necessary, and retry the selected APK. |
| Shortcut fails or trial expires | Check Accessibility, competing button remappers, and physical button events. Repeat the temporary test and confirmation. |
| Cover or sound remains active | Try Back or the shortcut, then open Ad Dimmer and disable the shortcut or its Accessibility service. For Sonos, restore sound directly in the Sonos app if needed. |

For removal, uncover first, disable the service in Accessibility, and uninstall through Android Apps settings. Warn that uninstalling deletes local settings. Firmware changes and a factory reset are unnecessary for ordinary app installation or removal.

### 8. Finish the debugging session

After verification, disconnect the selected network session with `adb disconnect TV_IP:PORT` if applicable. Let the user know they can disable debugging on the TV and revoke this computer's debugging authorization when finished. Explain how to reconnect for future updates if they choose to disable it.
