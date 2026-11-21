# Project context for agents

## Audio behavior

Product direction confirmed on 2026-11-19.

- The intended experience is one remote shortcut to dim the picture and mute Android TV media audio, then restore the picture and previous mute state.
- General Android TV muting is part of the intended feature scope. Sonos is an optional integration for setups requiring direct speaker control.
- In v0.5.0, Android audio control saves the current media volume, sets it to zero while covered, and restores it afterward. The earlier mute-flag-only approach reported muted streams while playback remained audible on the tested Halo+. Use the volume-zero implementation and preserve its recovery behavior.
- `MediaMute` persists recovery before changing audio, preserves already-muted/zero-volume starting states, retries interrupted restoration on service reconnection, and preserves a nonzero volume the user changed while covered. Earlier mute-only recovery journals remain supported. Sonos uses direct control when enabled.
- On 2026-11-20, the Halo+ built-in speakers passed sustained cover/uncover and actual process-death recovery checks. The owner confirmed audible silence and restoration using physical double-press Menu and Volume Up. The final volume-zero approach has not been verified on HDMI ARC or Bluetooth. Universal Android TV/audio-route compatibility remains unverified.
- Keep release availability and verified device/audio-output combinations accurate when changing product copy. Fixed-volume outputs may reject volume changes and require separate audio control.

## Installation assistance

For user installation requests, follow [the AI installation guide](docs/AI-INSTALL.md). It covers release selection, device connection, updates that preserve settings, physical remote testing, and recovery. Use the public release and confirm its current behavior before promising features.
