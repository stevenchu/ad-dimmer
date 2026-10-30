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
import java.util.concurrent.*;

public class CoverService extends AccessibilityService {
  public static CoverService instance;

  public void preview() {
    toggle();
  }

  public boolean isCovered() {
    return cover != null;
  }

  private final ExecutorService network = Executors.newSingleThreadExecutor();
  private Boolean originalMute = null;
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

  private void sound(boolean covered) {
    network.execute(
        () -> {
          try {
            if (covered) {
              if (!getSharedPreferences("settings", 0).getBoolean("sonos", false)) return;
              if (originalMute == null) {
                audioHost = getSharedPreferences("settings", 0).getString("host", "");
                if (!Config.validHost(audioHost))
                  throw new IOException("Enter a private IPv4 address in settings");
                originalMute = getMute();
                getSharedPreferences("recovery", 0)
                    .edit()
                    .putBoolean("restore", true)
                    .putString("host", audioHost)
                    .putBoolean("mute", originalMute)
                    .commit();
              }
              setMute(true);
            } else if (originalMute != null) {
              setMute(originalMute);
              originalMute = null;
              getSharedPreferences("recovery", 0).edit().clear().commit();
            }
            Log.i("ScreenCover", "Sonos mute state verified");
          } catch (Exception e) {
            Log.e("ScreenCover", "Sonos control failed");
            h.post(
                () ->
                    Toast.makeText(this, "Could not control Sonos sound", Toast.LENGTH_LONG)
                        .show());
          }
        });
  }

  private final Handler h = new Handler(Looper.getMainLooper());
  private AudioManager audio;
  private WindowManager wm;
  private View cover;
  private final AppPresetPolicy appPolicy = new AppPresetPolicy();
  private PresetStore presets;
  private String pendingPackage;
  private final Runnable confirmPackage =
      () -> {
        if (appPolicy.observe(pendingPackage, true, getPackageName()) && cover != null) hide();
      };

  public String detectedApp() {
    return appPolicy.currentApp();
  }

  public String selectedPreset() {
    return appPolicy.select(presets.automatic(), presets.manual(), presets.apps());
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
  private boolean backUp = false;
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
    shortcutStore = new ShortcutStore(this);
    loadShortcut();
    audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    Log.i("ScreenCover", "connected");
    if (getSharedPreferences("recovery", 0).getBoolean("restore", false)) {
      originalMute = getSharedPreferences("recovery", 0).getBoolean("mute", false);
      audioHost = getSharedPreferences("recovery", 0).getString("host", "");
      sound(false);
    }
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
    if (key == KeyEvent.KEYCODE_VOLUME_UP && down && cover != null) hide();
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

  private void show() {
    // Freeze this immutable visual snapshot until uncovered; system popups cannot change it.
    String selected = selectedPreset();
    final PresetStore.Visual visual = presets.read(selected);
    View v =
        new View(this) {
          protected void onDraw(Canvas c) {
            super.onDraw(c);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.argb(Math.round(visual.opacity * 255f / 100f), 0, 0, 0));
            // Reference geometry is in 1920 x 1080 pixels and scales to the display.

            float sx = getWidth() / 1920f, sy = getHeight() / 1080f;
            float left = visual.left * sx, top = visual.top * sy;
            RectF timer = new RectF(left, top, left + visual.width * sx, top + visual.height * sy);
            Path mask = new Path();
            mask.setFillType(Path.FillType.EVEN_ODD);
            mask.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
            if (visual.timer)
              mask.addRoundRect(timer, timer.height() / 2f, timer.height() / 2f, Path.Direction.CW);
            c.drawPath(mask, paint);
            paint.setColor(Color.argb(110, 220, 225, 230));
            paint.setTextSize(18 * getResources().getDisplayMetrics().density);
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText("Press Back to return", getWidth() / 2f, getHeight() - 45, paint);
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
      sound(false);
      Log.i("ScreenCover", "cover off");
    }
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
