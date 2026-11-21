# Validation and tester checklist

Report **device + Android version + streaming app/version + remote + audio route** with each result. A pass on Prime Video / Android 11 is evidence for that combination, not all Android 11 devices or streaming platforms.

## Automated / developer checks

- Run `scripts/test.sh`: valid/rejected addresses, geometry boundaries, Prime/default/manual selection, duplicate and ignored events, unknown-app fallback, and custom profile selection.
- Run `scripts/build.sh`: compile and verify APK signature.
- Device checks: legacy settings migrate once; profiles remain isolated; saving survives a service/process restart; actual Prime events select Prime; switching to TV Settings uncovers and restores sound.
- Open HBO Max and confirm the upper-left opening shows both the Ad badge and countdown during an ad. Press Left/Right while covered and confirm the preset changes once per press, the arrows stay out of the streaming app, and the cover stays on. Close and reopen the cover, restart the service, and confirm the choice persists. Switch to Prime and confirm its separate preset. With automatic selection off, confirm the last manually selected preset applies in both apps.
- In the v0.5.0 source preview with Sonos disabled, verify Android media mute and restoration using the physical shortcut, Back, Volume Up, app changes, Accessibility disable/re-enable, and process restart. Start once already muted and verify it stays muted. Start once unmuted and verify normal volume is preserved. Test repeated cover/uncover cycles and a failed or unsupported audio route. Report both Android mute state and audible speaker behavior.
- With Sonos enabled, verify direct Sonos control and preservation of the Android media state. Test initially muted and unmuted speaker states and report unreachable-speaker behavior separately.

Synthetic app identifiers exercise the policy and storage. They do not establish Netflix, Hulu, Disney+, or other streaming compatibility. Synthetic key events do not establish Bluetooth gesture behavior.

## Prime Video acceptance test — physical remote

| Step | Pass condition |
| --- | --- |
| Open Prime and play content | Prime preset selected after returning from Ad Dimmer settings |
| During an actual ad, double-tap Volume Down | Correct 95% cover/cutout appears; sound mutes if configured; ad continues |
| Use the shortcut again | Picture returns and original sound state is restored |
| Repeat; use Back to uncover | Restores picture/sound without navigating away from playback |
| Tap / hold Volume Down normally | Lowers sound; holding does not toggle the cover |
| Change volume to show its panel, then double-tap | Prime profile still used |
| Switch to Android Settings while covered | Cover clears and original sound state is restored |
| Return to Prime | Prime layout returns on next manual cover, not automatically |
| Save a small Prime geometry change, leave and return | Prime change persists; Default is unchanged |
| Restart app/service, then return to Prime | Saved settings persist; detection works after a new app event |
| Restart device | Confirm Accessibility service and shortcut still work; report if re-enabling is needed |

Repeat at least 5 cover/uncover cycles across two real ad breaks. Include one cycle starting already muted. Also verify the opening stays readable through different countdown lengths.

Do not restart the device during someone's viewing without their agreement. Process/service restart and full-device restart are distinct checks.

## Other platforms

Use the same checklist on each streaming app and device. Start by calibrating a local preset. Mark unperformed checks as **not tested**, and attach only redacted screenshots. We need testers with access to those apps before advertising support.
