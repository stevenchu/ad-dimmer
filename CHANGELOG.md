# Changelog

## 0.5.0 preview — unreleased

- Added Android TV media muting when the cover appears, with restoration on uncover, service interruption, and service reconnection. Already-muted audio stays muted. The pending restoration is saved before muting and retained for retry after a failed restore.
- Existing Sonos configurations continue to use direct Sonos control. Android audio failures show a message while the picture cover remains usable.
- Local recovery tests and APK build pass. Device and audio-output verification is pending. The public download remains v0.4.0.

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
