# App presets: contributor foundation

The cover is still manually toggled. Automatic selection changes **which saved visual layout** is used; it never decides when an ad begins or ends.

## User flow

Open Ad Dimmer from Android TV's Apps list. Choose **Preset to edit**, adjust opacity and timer geometry, and save. Automatic app selection is on by default. Turn it off to always use the preset selected in the editor.

Prime Video is the only bundled app identity: `com.amazon.amazonvideo.livingroom`. Its initial layout copies your previous settings. Default handles everything without a saved profile. Audio settings are global, so changing video apps cannot redirect mute to another soundbar.

To add another app, use **Add an app preset (advanced)** with its Android package ID and a name. The new preset copies Default and starts unverified. Save edits before selecting a different profile. A name or saved profile is not evidence of playback compatibility.

## Responsibilities in the source

- **AppPresetPolicy.java**: pure selection rules. Tracks a current app in memory, rejects our own package and common system/keyboard events, selects automatic/manual/default. Unit tests live in `AppPresetPolicyTest.java`.
- **CoverService.java**: consumes only `TYPE_WINDOW_STATE_CHANGED` package identifiers. Accepts saved identities, launchable TV/mobile apps, and TV Settings. Short debounce coalesces transitions. No text or screen-node reads. Identity inferred from these events is best-effort, not an authoritative foreground-task API.
- **PresetStore.java**: schema version 1. Separate visual values per app, saved custom identities, automatic/manual choice. First migration copies legacy global visual settings to Default and Prime. Subsequent starts preserve edits.
- **MainActivity.java**: remote-accessible preset editor and validation. It keeps audio settings global.

When covering, the service freezes a visual snapshot. Our own overlay, keyboard, and transient system windows must not replace the video preset. A confirmed change to a different launchable app removes the cover and requests restoration of the saved audio state. Returning to Prime selects its layout again, but does not automatically cover it.

At service startup, app identity is unknown until a qualifying event arrives; Default applies in the meantime. Returning to the video app produces a new event on the tested device. Manual selection is the fallback where firmware does not emit reliable events.

## Adding a supported app

1. Capture its actual package ID and device/Android/app versions, without recording account details or viewing history.
2. Add a local preset in the UI and align the timer opening during a real ad.
3. Verify the physical remote, playback continuation, sound, app switching, and restart using `QA.md`.
4. Submit reference geometry (1920 × 1080 coordinates), versions, and results in an issue or PR. Do not label a simulated event as a tested streaming service.
5. If proposing a bundled identity or layout, add focused policy tests and document the scope of actual testing. Keep a Default fallback.

Only configured preset identities are saved. The current app is held in memory; no app-use history is recorded or uploaded. Key mapping, ad detection, and speaker discovery are separate extension areas.
