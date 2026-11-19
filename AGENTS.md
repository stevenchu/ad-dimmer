# Project context for agents

## Audio behavior

Product direction confirmed on 2026-11-19.

- The intended experience is one remote shortcut to dim the picture and mute Android TV media audio, then restore the picture and previous mute state.
- General Android TV muting is part of the intended feature scope. Sonos is an optional integration for setups requiring direct speaker control.
- As inspected on 2026-11-19 at commit a117809, `CoverService.sound(boolean)` implements Sonos muting only. The app requests `MODIFY_AUDIO_SETTINGS` and initializes `AudioManager`, but general Android media muting still needs implementation and device testing.
- Treat the current Sonos-only behavior as an implementation gap when planning audio functionality. Check the current code and verification records before describing shipped behavior or changing product copy.
- Advertise general muting once implemented and verified. Android audio control depends on device and output configuration, including fixed-volume outputs. Compatibility across all Android TV devices and audio routes remains unverified.
