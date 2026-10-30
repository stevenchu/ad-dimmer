# Contributing

Compatibility reports are useful contributions—even if you do not write code.

Please include device model, Android version, remote type, video app, whether playback continued, which controls worked, and audio setup. Remove IP/MAC addresses, serial numbers, account details, personal photos, and viewing history before posting.

For changes, open an issue describing the problem, keep the pull request focused, run `scripts/test.sh` and `scripts/build.sh`, and describe real-device checks separately from builds. Preserve ordinary volume behavior, a reliable escape path, and restoration of the prior Sonos mute state.

Priorities: more device testing, a D-pad calibration preview, key-gesture tests, local speaker discovery, and network recovery improvements. Keep Accessibility access narrow. Do not add analytics, hidden recording, or claims of automatic ad detection without a separately reviewed implementation.

This is a spare-time experimental community tool. Reports and patches are welcome; no response-time commitment is implied.
