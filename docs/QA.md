# Validation and tester checklist

Report **device + Android version + streaming app/version + remote + audio route** with each result. A pass on Prime Video / Android 11 is evidence for that combination, not all Android 11 devices or streaming platforms.

## Automated / developer checks

- Run `scripts/test.sh`: valid/rejected addresses, geometry boundaries, Prime/default/manual selection, duplicate and ignored events, unknown-app fallback, and custom profile selection.
- Run `scripts/build.sh`: compile and verify APK signature.
- Device checks: legacy settings migrate once; profiles remain isolated; saving survives a service/process restart; actual Prime events select Prime; switching to TV Settings uncovers and restores sound. Verify HBO and Prime remain correctly identified after a same-boot app update or service reconnect. When no app identity is available, verify Full dimming is used even if an unknown-app override points to Default.
- For combined Sonos control, simulate an early speaker unmute during a cover and verify startup checks reapply mute and learn the compatibility fallback. Confirm an uncover invalidates delayed checks so they cannot remute afterward.
- Open HBO Max and confirm the upper-left opening shows both the Ad badge and countdown during an ad. Press Left/Right while covered and confirm the preset changes once per press, the arrows stay out of the streaming app, and the cover stays on. In automatic mode, close and reopen the cover and confirm HBO’s own preset returns. Restart the service and confirm the app identity is retained. Seed an old HBO-to-Full-dimming override and confirm it is ignored. Switch to Prime and confirm its separate preset. With automatic selection off, confirm the last manually selected preset applies in both apps.
- From v0.5.0 with Sonos disabled, verify Android media mute and restoration using the physical shortcut, Back, Volume Up, app changes, Accessibility disable/re-enable, and process restart. Start once already muted and verify it stays muted. Start once unmuted and verify normal volume is preserved. Test repeated cover/uncover cycles and a failed or unsupported audio route. Report both Android mute state and audible speaker behavior.
- From v0.5.1, connect Sonos by saving its address. Verify Android audio is attempted alongside Sonos, and the actual speaker volume and mute state restore correctly after HDMI feedback settles. Test initially muted and unmuted speaker states, a failed Sonos setup, and process-death recovery of both outputs. Clear the address to disconnect and verify Android audio control still runs.

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
