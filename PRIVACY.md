# Privacy

Ad Dimmer has no accounts, developer backend, analytics, advertisements, or third-party app SDKs.

- Accessibility receives remote key events and window-state notifications with the source app identifier to select a preset. Current app identity stays in memory; no viewing or app-use history is saved. The app does not retrieve window content, inspect video, recognize ads, or read text on screen.
- Settings and a pending audio-restoration record are stored in private app preferences. Cloud backup is disabled.
- Optional Sonos control sends `GetMute` and `SetMute` SOAP requests to the user-entered private IPv4 address on port 1400. Audio control is disabled by default. No automatic network discovery occurs in this version.
- No microphone, camera, location, screen capture, or recording permission is requested.
- Local Sonos traffic is unencrypted, as required by this integration. Use it only on a trusted home network. Never expose the speaker's control port to the internet.
- Errors log operational status. There is no log upload. Review and redact logs before posting them publicly.

Uninstalling removes app settings. A soundbar's mute state is external to the app; if necessary, restore it in the Sonos app.
