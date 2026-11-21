# Privacy

Ad Dimmer has no accounts, developer backend, analytics, advertisements, or third-party app SDKs.

- Accessibility receives remote key events and window-state notifications with the source app identifier to select a preset. The latest confirmed app identifier and device boot identifier are saved locally to restore mask selection after a same-boot service reconnect. No viewing or app-use history is saved. The app does not retrieve window content, inspect video, recognize ads, or read text on screen.
- Settings and pending audio-restoration records are stored in private app preferences. Cloud backup is disabled.
- Optional Sonos control sends `GetMute`, `SetMute`, `GetVolume`, and `SetVolume` SOAP requests to the user-entered private IPv4 address on port 1400. Android media audio control runs automatically. Saving a Sonos address connects direct speaker control alongside it. No automatic network discovery occurs in this version.
- No microphone, camera, location, screen capture, or recording permission is requested.
- Local Sonos traffic is unencrypted, as required by this integration. Use it only on a trusted home network. Never expose the speaker's control port to the internet.
- Errors log operational status. There is no log upload. Review and redact logs before posting them publicly.

Uninstalling removes app settings. A soundbar's mute state is external to the app; if necessary, restore it in the Sonos app.
