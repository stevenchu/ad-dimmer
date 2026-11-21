package org.stevenchu.screencover;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class CoverService extends AccessibilityService {
  public static CoverService instance;

  public void preview() {
    toggle();
  }

  public boolean isCovered() {
    return cover != null;
  }

  private Boolean originalMute = null;
  private int originalSonosVolume = -1;
  // AOSP uses this internal flag for incoming HDMI volume updates to avoid forwarding them back
  // over CEC. It is outside the public SDK contract; startup checks select the settling fallback
  // when a device still undoes the speaker mute.
  private static final int FLAG_HDMI_SYSTEM_AUDIO_VOLUME = 1 << 8;
  private int mediaVolumeFlags;
  private volatile long audioGeneration;
  private String audioHost = null;

  private String sonos(String action, String extra) throws Exception {
    URL u = new URL("http://" + audioHost + ":1400/MediaRenderer/RenderingControl/Control");
    HttpURLConnection c = (HttpURLConnection) u.openConnection();
    c.setInstanceFollowRedirects(false);
    c.setConnectTimeout(2000);
    c.setReadTimeout(2000);
    c.setRequestMethod("POST");
    c.setDoOutput(true);
    c.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
    c.setRequestProperty(
        "SOAPACTION", "\"urn:schemas-upnp-org:service:RenderingControl:1#" + action + "\"");
    String body =
        "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:"
            + action
            + " xmlns:u=\"urn:schemas-upnp-org:service:RenderingControl:1\"><InstanceID>0</InstanceID><Channel>Master</Channel>"
            + extra
            + "</u:"
            + action
            + "></s:Body></s:Envelope>";
    try {
      c.getOutputStream().write(body.getBytes("UTF-8"));
      if (c.getResponseCode() != 200) throw new IOException("Sonos HTTP " + c.getResponseCode());
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      InputStream in = c.getInputStream();
      byte[] buf = new byte[2048];
      int n;
      while ((n = in.read(buf)) != -1) b.write(buf, 0, n);
      return b.toString("UTF-8");
    } finally {
      c.disconnect();
    }
  }

  private boolean getMute() throws Exception {
    String reply = sonos("GetMute", "");
    if (reply.contains("<CurrentMute>1</CurrentMute>")) return true;
    if (reply.contains("<CurrentMute>0</CurrentMute>")) return false;
    throw new IOException("Invalid Sonos mute response");
  }

  private void setMute(boolean value) throws Exception {
    sonos("SetMute", "<DesiredMute>" + (value ? "1" : "0") + "</DesiredMute>");
    if (getMute() != value) throw new IOException("Sonos mute did not match");
  }

  private int getSonosVolume() throws Exception {
    java.util.regex.Matcher match =
        java.util.regex.Pattern.compile("<CurrentVolume>([0-9]{1,3})</CurrentVolume>")
            .matcher(sonos("GetVolume", ""));
    if (match.find()) {
      int volume = Integer.parseInt(match.group(1));
      if (volume <= 100) return volume;
    }
    throw new IOException("Invalid Sonos volume response");
  }

  private void setSonosVolume(int volume) throws Exception {
    sonos("SetVolume", "<DesiredVolume>" + volume + "</DesiredVolume>");
    if (getSonosVolume() != volume) throw new IOException("Sonos volume did not match");
  }

  // Serialize audio changes across service reconnections and keep them off the UI thread.
  private static final ExecutorService mediaAudio = Executors.newSingleThreadExecutor();
  private static final ExecutorService speakerReads = Executors.newFixedThreadPool(2);

  private void sound(boolean covered) {
    boolean useSonos = getSharedPreferences("settings", 0).getBoolean("sonos", false);
    String host = getSharedPreferences("settings", 0).getString("host", "");
    long generation = ++audioGeneration;
    mediaAudio.execute(
        () -> {
          long started = SystemClock.elapsedRealtime();
          if (covered) {
            mediaVolumeFlags =
                useSonos && !getSharedPreferences("settings", 0).getBoolean("hdmi_settle", false)
                    ? FLAG_HDMI_SYSTEM_AUDIO_VOLUME
                    : 0;
          }
          boolean sonosReady = false;
          if (covered && useSonos) {
            try {
              // Capture the speaker before Android volume changes can affect it through HDMI.
              restoreSonos();
              audioHost = host;
              if (!Config.validHost(audioHost)) throw new IOException("Invalid Sonos address");
              Future<Boolean> muteRead = speakerReads.submit(this::getMute);
              Future<Integer> volumeRead = speakerReads.submit(this::getSonosVolume);
              boolean previous;
              int previousVolume;
              try {
                previous = muteRead.get();
                previousVolume = volumeRead.get();
              } finally {
                muteRead.cancel(true);
                volumeRead.cancel(true);
              }
              if (!getSharedPreferences("recovery", 0)
                  .edit()
                  .putBoolean("restore", true)
                  .putString("host", audioHost)
                  .putBoolean("mute", previous)
                  .putInt("volume", previousVolume)
                  .putInt("flags", mediaVolumeFlags)
                  .commit()) {
                throw new IOException("Could not save Sonos recovery state");
              }
              originalMute = previous;
              originalSonosVolume = previousVolume;
              sonosReady = true;
            } catch (Exception e) {
              mediaVolumeFlags = 0;
              audioFailure(
                  "Could not reach Sonos. Android TV audio control will still be attempted.", e);
            }
          }

          try {
            if (covered) mediaMute.mute();
            else {
              try {
                mediaMute.restore();
              } catch (RuntimeException first) {
                // HDMI feedback may still be settling when the first read-back is checked.
                SystemClock.sleep(250);
                mediaMute.restore();
              }
            }
          } catch (RuntimeException e) {
            audioFailure(
                covered
                    ? "Could not mute Android TV audio. Check your audio output."
                    : "Could not restore Android TV audio. Use your remote to raise the volume.",
                e);
          }

          try {
            if (needsHdmiSettle() && (sonosReady || (!covered && originalMute != null))) {
              // HDMI volume feedback can arrive after setStreamVolume returns and undo a speaker
              // mute. Let that response settle, then apply the speaker's final mute state.
              SystemClock.sleep(1000);
            }
            if (covered) {
              if (sonosReady) {
                setMute(true);
                Log.i(
                    "ScreenCover",
                    "Sonos mute verified in " + (SystemClock.elapsedRealtime() - started) + " ms");
                checkSonosMute(generation, 750);
                checkSonosMute(generation, 1500);
              }
            } else {
              restoreSonos();
            }
          } catch (Exception e) {
            audioFailure(covered ? "Could not mute Sonos." : "Could not restore Sonos sound.", e);
          }
        });
  }

  private boolean needsHdmiSettle() {
    return mediaVolumeFlags == 0
        || getSharedPreferences("settings", 0).getBoolean("hdmi_settle", false);
  }

  private void restoreSonos() throws Exception {
    if (originalMute == null) return;
    if (originalSonosVolume >= 0 && getSonosVolume() != originalSonosVolume) {
      setSonosVolume(originalSonosVolume);
      if (needsHdmiSettle()) SystemClock.sleep(500);
    }
    setMute(originalMute);
    // A later Android recovery can affect the speaker through HDMI. Keep its snapshot until both
    // outputs have finished restoring, so a reconnect can apply the speaker state last again.
    if (mediaMute.pending()) return;
    if (!getSharedPreferences("recovery", 0).edit().clear().commit()) {
      throw new IOException("Could not clear Sonos recovery state");
    }
    originalMute = null;
    originalSonosVolume = -1;
    Log.i("ScreenCover", "Sonos mute state restored and verified");
  }

  private void checkSonosMute(long generation, long delay) {
    h.postDelayed(
        () -> {
          if (generation != audioGeneration) return;
          mediaAudio.execute(
              () -> {
                if (generation != audioGeneration || originalMute == null) return;
                try {
                  if (!getMute() && generation == audioGeneration) {
                    getSharedPreferences("settings", 0)
                        .edit()
                        .putBoolean("hdmi_settle", true)
                        .apply();
                    setMute(true);
                    Log.i("ScreenCover", "HDMI overrode mute; enabled settling fallback");
                  }
                } catch (Exception e) {
                  audioFailure("Could not verify Sonos mute.", e);
                }
              });
        },
        delay);
  }

  private void audioFailure(String message, Exception error) {
    Log.e("ScreenCover", message, error);
    h.post(() -> toast(message));
  }

  private final Handler h = new Handler(Looper.getMainLooper());
  private AudioManager audio;
  private MediaMute mediaMute;
  private WindowManager wm;
  private View cover;
  private String coverProfile, coverDetectedProfile;
  private PresetStore.Visual coverVisual;
  private final Set<Integer> profileKeysDown = new HashSet<>();
  private final AppPresetPolicy appPolicy = new AppPresetPolicy();
  private PresetStore presets;
  private String pendingPackage;
  private final Runnable confirmPackage =
      () -> {
        String pkg = pendingPackage;
        pendingPackage = null;
        if (!appPolicy.observe(pkg, true, getPackageName())) return;
        getSharedPreferences("app_detection", 0)
            .edit()
            .putString("package", pkg)
            .putInt(
                "boot",
                android.provider.Settings.Global.getInt(
                    getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, -1))
            .apply();
        if (cover != null) hide();
      };

  public String detectedApp() {
    return appPolicy.currentApp();
  }

  public String selectedPreset() {
    if (presets.automatic()) {
      // An override for unknown apps cannot identify an app after a fresh service connection.
      if (appPolicy.currentApp().isEmpty()) return AppPresetPolicy.FULL_DIM;
      return presets.automaticProfile(detectedProfile());
    }
    return appPolicy.select(false, presets.manual(), presets.apps());
  }

  private String detectedProfile() {
    return appPolicy.select(true, AppPresetPolicy.DEFAULT, presets.apps());
  }

  private boolean confirmedApp(String pkg) {
    if (pkg == null || pkg.equals(getPackageName())) return false;
    if (pkg.equals("com.android.tv.settings")) return true;
    if (presets.apps().contains(pkg)) return true;
    // Query only launchable TV/mobile apps, not window text. Volume/IME overlays are not apps here.
    return getPackageManager().getLeanbackLaunchIntentForPackage(pkg) != null
        || getPackageManager().getLaunchIntentForPackage(pkg) != null;
  }

  private ShortcutStore shortcutStore;
  private ShortcutStore.Mapping active, trial;
  private ShortcutGesture gesture;
  private long trialEnds;
  private boolean trialTriggered = false, recording = false;
  private int recorded = -1;
  private boolean backUp = false, volumeUp = false;
  private final Runnable holdCheck =
      () -> {
        if (gesture != null && gesture.tick(SystemClock.uptimeMillis())) fireShortcut();
      };
  private final Runnable delayedToggle = () -> toggle();
  private final Runnable expireTrial =
      () -> {
        hide();
        trial = null;
        loadShortcut();
        toast("Shortcut test expired; previous mapping restored");
      };

  public void loadShortcut() {
    h.removeCallbacks(holdCheck);
    h.removeCallbacks(delayedToggle);
    active = trial != null ? trial : shortcutStore.read();
    if (!ShortcutPolicy.allowed(active.key, active.mode))
      active = new ShortcutStore.Mapping(-1, "double", 650, 800);
    gesture = new ShortcutGesture(active.mode, active.doubleMs, active.holdMs);
  }

  private void toast(String value) {
    Toast.makeText(this, value, Toast.LENGTH_LONG).show();
  }

  public void startRecording() {
    recorded = -1;
    recording = true;
    h.postDelayed(() -> recording = false, 20000);
    toast("Press the button to use. Home, Back and navigation are protected.");
  }

  public int recordedKey() {
    return recorded;
  }

  public boolean startTrial(ShortcutStore.Mapping candidate) {
    if (!ShortcutPolicy.allowed(candidate.key, candidate.mode)) return false;
    cancelTrial();
    hide();
    recording = false;
    trial = candidate;
    trialTriggered = false;
    trialEnds = SystemClock.uptimeMillis() + 90000;
    loadShortcut();
    h.postDelayed(expireTrial, 90000);
    return true;
  }

  public boolean trialActive() {
    return trial != null && SystemClock.uptimeMillis() < trialEnds;
  }

  public boolean trialWorked() {
    return trialActive() && trialTriggered;
  }

  public long trialSeconds() {
    return Math.max(0, (trialEnds - SystemClock.uptimeMillis()) / 1000);
  }

  public boolean confirmTrial() {
    if (!trialWorked() || cover != null) return false;
    shortcutStore.save(trial);
    trial = null;
    h.removeCallbacks(expireTrial);
    loadShortcut();
    return true;
  }

  public void cancelTrial() {
    h.removeCallbacks(expireTrial);
    h.removeCallbacks(delayedToggle);
    if (trial != null) {
      hide();
      trial = null;
    }
    if (shortcutStore != null) loadShortcut();
  }

  public void restoreNormalButtons() {
    cancelTrial();
    hide();
    recording = false;
    shortcutStore.disable();
    loadShortcut();
  }

  private void fireShortcut() {
    if (trial != null) {
      if (!trialActive()) {
        expireTrial.run();
        return;
      }
      trialTriggered = true;
    }
    // Native double/hold actions run first; there is no interception delay on ordinary controls.
    h.removeCallbacks(delayedToggle);
    h.postDelayed(delayedToggle, active.mode.equals("single") ? 0 : 180);
  }

  protected void onServiceConnected() {
    instance = this;
    presets = new PresetStore(this);
    android.content.SharedPreferences detection = getSharedPreferences("app_detection", 0);
    int boot =
        android.provider.Settings.Global.getInt(
            getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, -1);
    String lastPackage = detection.getString("package", "");
    if (boot >= 0 && detection.getInt("boot", -1) == boot && confirmedApp(lastPackage)) {
      appPolicy.observe(lastPackage, true, getPackageName());
    }
    shortcutStore = new ShortcutStore(this);
    loadShortcut();
    audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    android.content.SharedPreferences recovery = getSharedPreferences("android_audio_recovery", 0);
    mediaVolumeFlags = recovery.getInt("flags", 0);
    mediaMute =
        new MediaMute(
            new MediaMute.Audio() {
              public boolean isMuted() {
                return audio.isStreamMute(AudioManager.STREAM_MUSIC);
              }

              public int volume() {
                return audio.getStreamVolume(AudioManager.STREAM_MUSIC);
              }

              public void setVolume(int volume) {
                if (audio.isVolumeFixed()) {
                  throw new UnsupportedOperationException("This output has fixed volume");
                }
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, mediaVolumeFlags);
                long deadline = SystemClock.elapsedRealtime() + 1000;
                while (audio.getStreamVolume(AudioManager.STREAM_MUSIC) != volume
                    && SystemClock.elapsedRealtime() < deadline) {
                  SystemClock.sleep(25);
                }
              }

              public void setMuted(boolean muted) {
                if (audio.isVolumeFixed()) {
                  throw new UnsupportedOperationException("This output has fixed volume");
                }
                audio.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    muted ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE,
                    0);
                // AudioService can apply the change asynchronously. Verification runs on our
                // worker.
                long deadline = SystemClock.elapsedRealtime() + 1000;
                while (audio.isStreamMute(AudioManager.STREAM_MUSIC) != muted
                    && SystemClock.elapsedRealtime() < deadline) {
                  SystemClock.sleep(25);
                }
              }
            },
            new MediaMute.Recovery() {
              public boolean pending() {
                return recovery.getBoolean("restore", false);
              }

              public int savedVolume() {
                return recovery.getInt("volume", -1);
              }

              public void save(boolean pending, int volume) {
                if (!recovery
                    .edit()
                    .putBoolean("restore", pending)
                    .putInt("volume", volume)
                    .putInt("flags", mediaVolumeFlags)
                    .commit()) {
                  throw new IllegalStateException("Could not save Android audio recovery state");
                }
              }
            });
    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    Log.i("ScreenCover", "connected");
    if (getSharedPreferences("recovery", 0).getBoolean("restore", false)) {
      originalMute = getSharedPreferences("recovery", 0).getBoolean("mute", false);
      originalSonosVolume = getSharedPreferences("recovery", 0).getInt("volume", -1);
      if (!mediaMute.pending())
        mediaVolumeFlags = getSharedPreferences("recovery", 0).getInt("flags", 0);
      audioHost = getSharedPreferences("recovery", 0).getString("host", "");
    }
    // Recover each output independently, even if the sound settings have changed.
    sound(false);
  }

  protected boolean onKeyEvent(KeyEvent e) {
    int key = e.getKeyCode();
    boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
    if (down && (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_HOME)) {
      h.removeCallbacks(delayedToggle);
      h.removeCallbacks(holdCheck);
    }
    if (key == KeyEvent.KEYCODE_BACK && (cover != null || backUp)) {
      if (down) {
        hide();
        backUp = true;
      } else backUp = false;
      return true;
    }
    if (key == KeyEvent.KEYCODE_VOLUME_UP && (cover != null || volumeUp)) {
      if (down) {
        hide();
        volumeUp = true;
        mediaAudio.execute(
            () ->
                audio.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI));
      } else volumeUp = false;
      return true;
    }
    if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
      // Consume the matching release even if another action has already removed the cover.
      if (!down && profileKeysDown.remove(key)) return true;
      if (down && profileKeysDown.contains(key)) return true;
      if (down && cover != null && e.getRepeatCount() == 0) {
        profileKeysDown.add(key);
        if (gesture != null) gesture.reset();
        h.removeCallbacks(holdCheck);
        switchProfile(key == KeyEvent.KEYCODE_DPAD_RIGHT);
        return true;
      }
    }
    // Recording is observational: essential buttons and native single actions remain available.
    if (recording && !down && ShortcutPolicy.allowed(key, "double")) {
      recorded = key;
      recording = false;
      toast("Recorded " + RemoteLabels.button(key));
      return false;
    }
    if (recording) return false;
    // Settings and navigation must always remain operable, except during an explicit temporary
    // test.
    if (!trialActive() && (MainActivity.visible || ShortcutActivity.visible)) return false;
    if (active == null || key != active.key) {
      if (down && gesture != null) {
        gesture.reset();
        h.removeCallbacks(holdCheck);
      }
      return false;
    }
    long now = SystemClock.uptimeMillis();
    if (down) {
      gesture.down(now, e.getRepeatCount());
      if (e.getRepeatCount() == 0) {
        h.removeCallbacks(holdCheck);
        h.postDelayed(holdCheck, active.holdMs);
      }
    } else {
      h.removeCallbacks(holdCheck);
      if (gesture.up(now)) fireShortcut();
    }
    // Double and hold never swallow native events. Single replacement requires a confirmed test.
    return active.mode.equals("single");
  }

  private void toggle() {
    if (cover == null) show();
    else hide();
  }

  private void switchProfile(boolean right) {
    coverProfile = AppPresetPolicy.cycle(coverProfile, right, presets.apps());
    coverVisual = presets.read(coverProfile);
    presets.saveSelection(coverDetectedProfile, coverProfile);
    cover.invalidate();
  }

  private void show() {
    // A shortcut can arrive before the app-event debounce expires.
    if (pendingPackage != null) {
      h.removeCallbacks(confirmPackage);
      confirmPackage.run();
    }
    if (presets.automatic() && appPolicy.currentApp().isEmpty()) {
      toast("App not detected yet. Go Home, then reopen your streaming app to select its mask.");
    }
    // Keep the chosen layout until a remote arrow switches it or the cover closes.
    coverDetectedProfile = detectedProfile();
    coverProfile = selectedPreset();
    coverVisual = presets.read(coverProfile);
    View v =
        new View(this) {
          protected void onDraw(Canvas c) {
            super.onDraw(c);
            PresetStore.Visual visual = coverVisual;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.argb(Math.round(visual.opacity * 255f / 100f), 0, 0, 0));
            // Reference geometry is in 1920 x 1080 pixels and scales to the display.

            float sx = getWidth() / 1920f, sy = getHeight() / 1080f;
            float left = visual.left * sx, top = visual.top * sy;
            RectF timer = new RectF(left, top, left + visual.width * sx, top + visual.height * sy);
            if (visual.timer && AppPresetPolicy.HBO_MAX.equals(coverProfile)) {
              // Leave the opening unpainted. Nonoverlapping strips keep the opacity uniform.
              paint.setAntiAlias(false);
              c.drawRect(0, 0, getWidth(), timer.top, paint);
              c.drawRect(0, timer.bottom, getWidth(), getHeight(), paint);
              c.drawRect(0, timer.top, timer.left, timer.bottom, paint);
              c.drawRect(timer.right, timer.top, getWidth(), timer.bottom, paint);
              paint.setAntiAlias(true);
            } else {
              Path mask = new Path();
              mask.setFillType(Path.FillType.EVEN_ODD);
              mask.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
              if (visual.timer)
                mask.addRoundRect(timer, timer.height() / 2f, timer.height() / 2f, Path.Direction.CW);
              c.drawPath(mask, paint);
            }
            paint.setColor(Color.argb(110, 220, 225, 230));
            paint.setTextSize(18 * getResources().getDisplayMetrics().density);
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(
                presets.name(coverProfile) + "   ·   ◀ ▶ Change preset   ·   Back to return",
                getWidth() / 2f, getHeight() - 45, paint);
          }
        };
    WindowManager.LayoutParams p =
        new WindowManager.LayoutParams(
            -1,
            -1,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
    p.gravity = Gravity.TOP | Gravity.LEFT;
    try {
      wm.addView(v, p);
      cover = v;
      sound(true);
      Log.i("ScreenCover", "cover on");
    } catch (Exception ex) {
      Log.e("ScreenCover", "show failed", ex);
    }
  }

  private void hide() {
    if (cover != null) {
      try {
        wm.removeViewImmediate(cover);
      } catch (Exception ignored) {
      }
      cover = null;
      Log.i("ScreenCover", "cover off");
    }
    if (mediaMute != null) sound(false);
  }

  public void onAccessibilityEvent(AccessibilityEvent e) {
    if (presets == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
    CharSequence source = e.getPackageName();
    String pkg = source == null ? null : source.toString();
    if (!confirmedApp(pkg)) return;
    // Debounce launch transitions. Ignore our own cover, keyboard, and transient system windows.
    pendingPackage = pkg;
    h.removeCallbacks(confirmPackage);
    h.postDelayed(confirmPackage, 200);
  }

  public boolean onUnbind(android.content.Intent intent) {
    hide();
    return super.onUnbind(intent);
  }

  public void onInterrupt() {
    hide();
  }

  public void onDestroy() {
    instance = null;
    h.removeCallbacksAndMessages(null);
    trial = null;
    hide();
    super.onDestroy();
  }
}
