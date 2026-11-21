# Project context for agents

## Audio behavior

Product direction confirmed on 2026-11-19.

- The intended experience is one remote shortcut to dim the picture and mute Android TV media audio, then restore the picture and previous mute state.
- General Android TV muting is part of the intended feature scope. Sonos is an optional integration for setups requiring direct speaker control.
- From v0.5.1, every cover attempts Android media muting. A connected Sonos speaker is controlled as well. Sound settings have one optional speaker-address field; saving an address connects Sonos and clearing it disconnects. Existing saved connection preferences are respected.
- Android muting saves the media volume, sets it to zero, and restores it on uncover. The mute-flag-only approach reported muted streams while playback remained audible on the Halo+. Preserve the volume-zero implementation and its durable recovery behavior.
- Capture Sonos mute and volume concurrently before changing either output. When Sonos is connected, Android volume changes use the AOSP internal HDMI-origin flag to avoid sending competing CEC commands. This flag is outside the public SDK contract. Background startup checks reapply mute and automatically enable the slower settling fallback if a device overrides it. Preserve this compatibility path and both recovery snapshots. Restore the speaker’s saved volume and mute state last.
- Audio work runs serially off the UI thread. A failed Sonos setup still permits the Android attempt. Each restoration can be retried after service reconnection. Older Android mute-only and Sonos mute-only recovery records remain supported.
- The Halo+ built-in speakers passed audible and physical-remote tests in v0.5.0. Combined Android and Sonos control over HDMI ARC passed sustained mute, volume restoration, already-muted preservation, and process-death checks in v0.5.1. The owner confirmed audible Sonos muting. Keep the precise test results in docs/VERIFICATION.md current.
- Universal Android TV/audio-route compatibility remains unverified. The normal path has no fixed settling delay; fallback devices may still need it. Bluetooth and other hardware still need their own tests.

## Installation assistance

For user installation requests, follow [the AI installation guide](docs/AI-INSTALL.md). It covers release selection, device connection, updates that preserve settings, physical remote testing, and recovery. Use the public release and confirm its current behavior before promising features.

## App detection after reconnects

Keep the latest confirmed app identifier and boot identifier in private preferences so same-boot service reconnects retain mask selection. Apply a pending app event before choosing a cover. If the app identity is unavailable, use Full dimming and explain how to reopen the streaming app rather than applying an unknown-app override with a misplaced timer opening. Preserve per-app layout geometry. From v0.5.2, Left/Right changes in automatic mode last only for the active cover. Every activation uses the detected app’s own preset, ignoring legacy automatic override entries. Manual mode retains its chosen preset across activations. A Full dimming label alone does not prove app detection failed; inspect both detected identity and saved/resolved selection.
