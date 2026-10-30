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
  private CheckBox timer, sonos, automatic;
  private PresetStore presets;
  private Spinner presetChoice;
  private java.util.List<String> presetIds;
  private String editing = AppPresetPolicy.DEFAULT;
  private EditText appId, appName;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    prefs = getSharedPreferences("settings", 0);
    presets = new PresetStore(this);
    ScrollView scroll = new ScrollView(this);
    form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(48, 32, 48, 48);
    scroll.addView(form);
    text("Ad Dimmer", 30);
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
    String detected = CoverService.instance == null ? "" : CoverService.instance.detectedApp();
    text(
        "Detected app: "
            + (detected.isEmpty()
                ? "not yet identified — return to your video app"
                : presets.name(detected)),
        18);
    text("Preset to edit", 20);
    presetChoice = new Spinner(this);
    form.addView(presetChoice);
    automatic =
        check(
            "Automatic app selection (otherwise use the preset selected above)", "automatic", true);
    automatic.setChecked(presets.automatic());
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
    refreshChoices();
    presetChoice.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onItemSelected(
              android.widget.AdapterView<?> parent, android.view.View view, int pos, long id) {
            editing = presetIds.get(pos);
            loadVisual();
          }

          public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    text("Add an app preset (advanced)", 20);
    text(
        "Enter its Android package ID and a friendly name. This copies Default; it is not a tested"
            + " layout. Save edits before switching presets.",
        18);
    appId = new EditText(this);
    appId.setSingleLine(true);
    appId.setHint("Android package ID");
    form.addView(appId);
    appName = new EditText(this);
    appName.setSingleLine(true);
    appName.setHint("Preset name");
    form.addView(appName);
    button(
        "Add app preset",
        () -> {
          String id = appId.getText().toString().trim();
          if (!id.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) {
            toast("Enter a valid Android package ID");
            return false;
          }
          if (presets.apps().contains(id)) {
            toast("That preset already exists");
            return false;
          }
          presets.add(id, appName.getText().toString().trim());
          refreshChoices();
          presetChoice.setSelection(presetIds.indexOf(id));
          return true;
        });
    button("Save settings", this::save);
    button(
        "Try cover (Back returns)",
        () -> {
          if (!save()) return false;
          if (CoverService.instance == null) toast("Enable Ad Dimmer in Accessibility first");
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
            + " time. Disable Ad Dimmer in Accessibility to stop it.",
        18);
    setContentView(scroll);
  }

  private void refreshChoices() {
    presetIds = new java.util.ArrayList<>();
    presetIds.add(AppPresetPolicy.DEFAULT);
    java.util.List<String> ids = new java.util.ArrayList<>(presets.apps());
    java.util.Collections.sort(ids);
    presetIds.addAll(ids);
    java.util.List<String> labels = new java.util.ArrayList<>();
    for (String id : presetIds) labels.add(presets.name(id));
    presetChoice.setAdapter(
        new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, labels));
    int pos = presetIds.indexOf(presets.manual());
    presetChoice.setSelection(Math.max(0, pos));
    editing = presetIds.get(Math.max(0, pos));
    loadVisual();
  }

  private void loadVisual() {
    PresetStore.Visual v = presets.read(editing);
    opacity.setText("" + v.opacity);
    left.setText("" + v.left);
    top.setText("" + v.top);
    width.setText("" + v.width);
    height.setText("" + v.height);
    timer.setChecked(v.timer);
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
      presets.save(
          editing, new PresetStore.Visual(a, l, t, w, h, timer.isChecked()), automatic.isChecked());
      prefs.edit().putString("host", ip).putBoolean("sonos", sonos.isChecked()).apply();
      toast("Settings saved");
      return true;
    } catch (NumberFormatException error) {
      toast("Enter whole numbers for opacity and position.");
      return false;
    }
  }
}
