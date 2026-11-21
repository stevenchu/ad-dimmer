# Changelog

## 0.5.1 — 2026-11-21

- Android media control now runs on every cover, with connected Sonos control attempted as well. A Sonos setup failure allows the Android attempt to continue.
- Simplified Sound settings to one optional speaker-address field. Save an address to connect Sonos, or clear it to disconnect. Existing connection preferences are preserved.
- Saved Sonos volume and mute state are captured concurrently before changing audio. The normal path suppresses competing HDMI volume commands and has no fixed one-second delay. Background verification selects a settling fallback automatically when needed. Speaker volume and mute state restore last.
- Retain recovery records until both outputs finish restoration. Retry transient Android restoration failures.
- Preserve confirmed app identity across same-boot service reconnects. Apply pending app events before selecting a mask. An unavailable identity uses Full dimming and guidance instead of an unrelated saved fallback opening.
- On-device checks cover combined mute/restore cycles, exact Sonos volume restoration, already-muted state, continued Android attempts after Sonos setup failure, and process-death recovery.

## 0.5.0 — 2026-11-20

- Added Android TV media muting by saving the current media volume and setting it to zero while the cover is active. Uncovering restores the saved level. Already-muted or zero-volume audio stays silent, and a nonzero volume changed by the user is preserved.
- Audio work runs in sequence off the UI thread. Pending restoration is saved before muting and retried after a failed restore or service restart. Earlier mute-only preview journals remain supported.
- Volume Up restores sound before raising it. Existing Sonos configurations continue to use direct Sonos control.
- Verified audible mute and restoration on the Android 11 Halo+ built-in speakers with the physical double-press Menu shortcut and Volume Up. On-device checks also cover repeated cycles, already-muted state, service interruption, and process-death recovery. Bluetooth and the final volume-zero approach on HDMI ARC remain unverified.

## 0.4.0 — 2026-11-17

- Added a 95% HBO Max preset with a calibrated 226 × 43 px rectangular timer opening at left 106, top 74 on the 1920 × 1080 reference screen.
- Added Full dimming for apps without a configured opening.
- Left and Right switch presets while the cover is active. Automatic selection remembers overrides by app profile. Manual mode remembers the last selection across apps.
- Upgrades preserve custom timer geometry, opacity, and shortcut settings.

## 0.2.1 — 2026-10-30

- Renamed the project and app display name to Ad Dimmer; internal app ID and signing identity stay unchanged for upgrades.
- Shortened the README around downloading, installing, and using the remote.
- Added a clearly labeled fictional before/after illustration and retained the authentic capture separately.

## 0.2.0 — 2026-10-29

- Automatic app-identity-based visual presets, with Prime Video as the initial identity.
- Per-app opacity and timer geometry; Default fallback and manual override.
- Local custom preset creation, one-time legacy migration, and persistent edits.
- Ignore own/system events; freeze the active visual snapshot and uncover on confirmed app changes.
- Annotated preset architecture and an explicit tester checklist; other streaming apps remain unverified.

## 0.1.0 — 2026-10-29

First experimental community release, adapted from a personal Halo+ remote shortcut.

- Manual double-tap Volume Down screen cover, with Back and Volume Up escape paths.
- Adjustable opacity and pill-shaped timer opening.
- Optional user-configured Sonos mute with explicit state verification and restoration.
- Audio disabled by default; no private device addresses bundled.
- TV settings screen, installation/removal guide, source build script, validation tests, and compatibility-report template.

## 0.3.0 — 2026-10-30

- No fixed Volume Down shortcut; ordinary volume events pass through immediately.
- Select or record a button; double, hold, and cautioned single replacement modes.
- Adjustable double-press timing, default 650 ms.
- Protected essential controls, settings bypass, restore-normal-buttons action.
- Temporary 90-second tests with explicit confirmation and automatic rollback.
- Native actions remain enabled for double/hold; those combinations require device-specific testing.
