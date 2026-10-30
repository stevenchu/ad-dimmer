package org.stevenchu.screencover;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;

/** Native widgets keep the settings usable with a TV D-pad. */
public class MainActivity extends Activity {
  private LinearLayout form;
  private SharedPreferences prefs;
  private EditText opacity, left, top, width, height, host;
  private CheckBox timer, sonos;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    prefs = getSharedPreferences("settings", 0);
    ScrollView scroll = new ScrollView(this);
    form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(48, 32, 48, 48);
    scroll.addView(form);
    text("Screen Cover", 30);
    text(
        "Double-tap Volume Down to dim the picture. Double-tap again, Back, or Volume Up to return."
            + " Single presses and holds still lower volume, with a 300 ms initial delay. This is a"
            + " manual cover; ads keep playing.",
        20);
    text(
        "Optional Sonos audio: cover mutes; uncover restores the previous mute state. Other"
            + " speakers are not controlled. Network errors show a message; the picture can still"
            + " be covered.",
        18);
    opacity = number("Cover opacity (%)", "opacity", 95);
    timer = check("Leave a pill-shaped opening for the ad timer", "timer", true);
    text("Timer position (1920 × 1080 reference pixels; scaled for your display)", 20);
    left = number("Left", "left", 1685);
    top = number("Top", "top", 72);
    width = number("Width", "width", 140);
    height = number("Height", "height", 56);
    sonos = check("Mute a Sonos speaker on my local network", "sonos", false);
    text(
        "Enter the home-theater soundbar's private IPv4 address from the Sonos app. Use a router"
            + " address reservation if possible. Only enable this for the speaker playing your TV"
            + " sound.",
        18);
    text("Sonos speaker IPv4 address", 18);
    host = new EditText(this);
    host.setSingleLine(true);
    host.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    host.setText(prefs.getString("host", ""));
    form.addView(host);
    button("Save settings", this::save);
    button(
        "Try cover (Back returns)",
        () -> {
          if (!save()) return false;
          if (CoverService.instance == null) toast("Enable Screen Cover in Accessibility first");
          else CoverService.instance.preview();
          return true;
        });
    button(
        "Open Accessibility settings",
        () -> {
          startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
          return true;
        });
    text(
        "Privacy: Accessibility reads button events, not screen content. No analytics, accounts,"
            + " ads, microphone, or video recording. Network requests are used only for optional"
            + " local Sonos mute control. Enable only one remote-button filtering service at a"
            + " time. Disable Screen Cover in Accessibility to stop it.",
        18);
    setContentView(scroll);
  }

  private void text(String value, int size) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextSize(size);
    view.setPadding(0, 10, 0, 8);
    form.addView(view);
  }

  private EditText number(String label, String key, int fallback) {
    text(label, 18);
    EditText field = new EditText(this);
    field.setSingleLine(true);
    field.setInputType(InputType.TYPE_CLASS_NUMBER);
    field.setText(String.valueOf(prefs.getInt(key, fallback)));
    form.addView(field);
    return field;
  }

  private CheckBox check(String label, String key, boolean fallback) {
    CheckBox field = new CheckBox(this);
    field.setText(label);
    field.setChecked(prefs.getBoolean(key, fallback));
    form.addView(field);
    return field;
  }

  private interface Action {
    boolean run();
  }

  private void button(String label, Action action) {
    Button button = new Button(this);
    button.setText(label);
    button.setOnClickListener(view -> action.run());
    form.addView(button);
  }

  private void toast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private int value(EditText field) {
    return Integer.parseInt(field.getText().toString().trim());
  }

  private boolean save() {
    try {
      int a = value(opacity), l = value(left), t = value(top), w = value(width), h = value(height);
      if (a < 0 || a > 100 || !Config.validGeometry(l, t, w, h)) {
        toast(
            "Use opacity 0–100 and a timer opening that fits inside 1920 × 1080 (minimum 20 ×"
                + " 20).");
        return false;
      }
      String ip = host.getText().toString().trim();
      if (sonos.isChecked() && !Config.validHost(ip)) {
        toast(
            "Enter a private IPv4 address, such as the one shown for your soundbar in the Sonos"
                + " app.");
        return false;
      }
      // Avoid changing audio destinations while the cover is active.
      if (CoverService.instance != null && CoverService.instance.isCovered()) {
        toast("Uncover the screen before changing settings.");
        return false;
      }
      prefs
          .edit()
          .putInt("opacity", a)
          .putInt("left", l)
          .putInt("top", t)
          .putInt("width", w)
          .putInt("height", h)
          .putString("host", ip)
          .putBoolean("timer", timer.isChecked())
          .putBoolean("sonos", sonos.isChecked())
          .apply();
      toast("Settings saved");
      return true;
    } catch (NumberFormatException error) {
      toast("Enter whole numbers for opacity and position.");
      return false;
    }
  }
}
