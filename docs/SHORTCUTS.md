# Choose a shortcut without losing normal remote controls

v0.3.0 shortcut foundation. Double-press Menu was confirmed on the original Android 11 Halo+ / Prime Video setup. Other button/remote combinations require their own temporary test.

Open **Ad Dimmer → Choose / test remote shortcut** from Android TV's Apps list. No shortcut is assigned by default, including when upgrading from the old fixed Volume Down mapping. Volume Down and Volume Up run immediately unless you explicitly assign a supported gesture to Volume Down.

## Choose and test

1. Choose Menu, Mute, Input, Volume Down (advanced), or **Record another remote button**. Recording observes one button without swallowing its native action; that action may open another screen. Return to Ad Dimmer to see the recorded key.
2. Choose double press, hold, or single press. Double press defaults to a more forgiving **650 ms** and can be adjusted from 250–1200 ms. Hold is 800 ms.
3. Read the behavior below. Start a **90-second temporary test**.
4. Open your video app and physically try the shortcut. Uncover with Back. Return through Home → Apps → Ad Dimmer → shortcut setup and choose **It worked — save this shortcut** before time runs out.
5. If you do nothing, the previous confirmed mapping returns. A crash or process restart also discards the trial. The app refuses confirmation until the gesture has actually been observed and the cover removed.

## What happens to the button's ordinary action?

**Double press and hold observe native actions; they do not delay or suppress them.** This avoids the delayed Volume Down problem. It also means a menu may appear or native Mute may change sound before the dimmer activates. Try it on your particular remote; if that behavior interferes, use another button. This is not a promise that every double-Mute or hold combination preserves the audio state a user expects: native audio actions are outside Ad Dimmer's control.

**Single press replaces the button's normal action.** A warning and explicit acknowledgement of another route to the original function are required before the temporary test. Single Mute means dim-and-mute (if configured) replaces ordinary sound-only mute. Single Menu/Input can replace access to their normal screens. Use single press only after confirming the alternative route.

Home, Back, Power, D-pad navigation/OK, Enter, Volume Up, voice-assistant, and other key navigation controls are protected from remapping. Single-press Volume Down replacement is also blocked. Shortcut actions are disabled inside Ad Dimmer's own settings except during an explicit test.

## Recovery

- **Back** removes a cover; **Home** remains native.
- Open Ad Dimmer from Apps and choose **Restore normal remote buttons**. This cancels any trial, removes the cover, restores requested audio state, and deletes the shortcut assignment.
- Or disable Ad Dimmer's Accessibility service in Android settings.
- Sonos can always be unmuted directly in its app.

## Contribution boundaries

`ShortcutGesture` is pure timing logic, `ShortcutPolicy` lists protected keys, `ShortcutStore` persists confirmed mappings only, and `ShortcutActivity` handles warnings and test confirmation. `CoverService` observes events and manages temporary mapping expiry. No key-injection or native-action replay permission is needed.

Test native forwarding, protected keys, double timing, held-key repeats, confirmation gating, rollback, and process restart separately from the physical remote. Diagnostic calls to `onKeyEvent` are not evidence that a manufacturer's remote actually exposes that key to Accessibility. Add an actual device report before claiming support.
