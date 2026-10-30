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
  private boolean pending = false, second = false, backUp = false;
  private long first = 0, lastRepeat = 0;
  private final Runnable single =
      () -> {
        pending = false;
        lower();
      };

  protected void onServiceConnected() {
    instance = this;
    audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    Log.i("ScreenCover", "connected");
    if (getSharedPreferences("recovery", 0).getBoolean("restore", false)) {
      originalMute = getSharedPreferences("recovery", 0).getBoolean("mute", false);
      audioHost = getSharedPreferences("recovery", 0).getString("host", "");
      sound(false);
    }
  }

  private void lower() {
    audio.adjustStreamVolume(
        AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
  }

  private void flush() {
    if (pending) {
      h.removeCallbacks(single);
      pending = false;
      lower();
    }
  }

  protected boolean onKeyEvent(KeyEvent e) {
    int k = e.getKeyCode();
    boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
    if (k == KeyEvent.KEYCODE_BACK && (cover != null || backUp)) {
      if (down) {
        hide();
        backUp = true;
      } else backUp = false;
      return true;
    }
    if (k == KeyEvent.KEYCODE_VOLUME_UP && down && cover != null) hide();
    if (k != KeyEvent.KEYCODE_VOLUME_DOWN) {
      if (down) flush();
      return false;
    }
    if (down) {
      if (e.getRepeatCount() > 0) {
        if (second) return true;
        if (pending) {
          flush();
          lastRepeat = SystemClock.uptimeMillis();
          return true;
        }
        if (SystemClock.uptimeMillis() - lastRepeat > 110) {
          lower();
          lastRepeat = SystemClock.uptimeMillis();
        }
        return true;
      }

      if (pending && SystemClock.uptimeMillis() - first <= 300) {
        h.removeCallbacks(single);
        pending = false;
        second = true;
        toggle();
      } else {
        flush();
        second = false;
        pending = true;
        first = SystemClock.uptimeMillis();
        h.postDelayed(single, 300);
      }
    } else {
      if (second) second = false;
    }
    return true;
  }

  private void toggle() {
    if (cover == null) show();
    else hide();
  }

  private void show() {
    View v =
        new View(this) {
          protected void onDraw(Canvas c) {
            super.onDraw(c);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(
                Color.argb(
                    Math.round(
                        getSharedPreferences("settings", 0).getInt("opacity", 95) * 255f / 100f),
                    0,
                    0,
                    0));
            // Reference geometry is in 1920 x 1080 pixels and scales to the display.
            android.content.SharedPreferences cfg = getSharedPreferences("settings", 0);
            float sx = getWidth() / 1920f, sy = getHeight() / 1080f;
            float left = cfg.getInt("left", 1685) * sx, top = cfg.getInt("top", 72) * sy;
            RectF timer =
                new RectF(
                    left,
                    top,
                    left + cfg.getInt("width", 140) * sx,
                    top + cfg.getInt("height", 56) * sy);
            Path mask = new Path();
            mask.setFillType(Path.FillType.EVEN_ODD);
            mask.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
            if (cfg.getBoolean("timer", true))
              mask.addRoundRect(timer, timer.height() / 2f, timer.height() / 2f, Path.Direction.CW);
            c.drawPath(mask, paint);
            paint.setColor(Color.argb(110, 220, 225, 230));
            paint.setTextSize(18 * getResources().getDisplayMetrics().density);
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(
                "Double-tap Volume − or Back to return", getWidth() / 2f, getHeight() - 45, paint);
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

  public void onAccessibilityEvent(AccessibilityEvent e) {}

  public void onInterrupt() {
    hide();
  }

  public void onDestroy() {
    instance = null;
    h.removeCallbacksAndMessages(null);
    hide();
    super.onDestroy();
  }
}
