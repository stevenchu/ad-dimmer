# Project context for agents

## Audio behavior

Product direction confirmed on 2026-11-19.

- The intended experience is one remote shortcut to dim the picture and mute Android TV media audio, then restore the picture and previous mute state.
- General Android TV muting is part of the intended feature scope. Sonos is an optional integration for setups requiring direct speaker control.
- As of 2026-11-20, the 0.5.0 source preview implements Android media muting through `AudioManager.STREAM_MUSIC` when Sonos is disabled. `MediaMute` persists recovery before changing audio, preserves an already-muted starting state, and retries interrupted restoration on service reconnection. Sonos uses the existing direct-control path when enabled.
- Local recovery tests and APK compilation/signature verification pass. Live device testing is pending. The public v0.4.0 APK still implements Sonos muting only. Keep release availability, source behavior, and device verification distinct when changing product copy.
- Fixed-volume outputs and external audio routes can prevent Android media muting. A failed operation shows a message. Compatibility across all Android TV devices and audio routes remains unverified.

## Installation assistance

For user installation requests, follow [the AI installation guide](docs/AI-INSTALL.md). It covers release selection, device connection, updates that preserve settings, physical remote testing, and recovery. Use the public release and confirm its current behavior before promising features.
