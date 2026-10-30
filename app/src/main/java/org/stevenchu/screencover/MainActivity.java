package org.stevenchu.screencover;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Four short sections instead of a single long form; storage and remote routing are unchanged. */
public class MainActivity extends Activity {
  public static boolean visible;
  private TvUi ui;
  private SharedPreferences prefs;
  private PresetStore presets;
  private LinearLayout panel;
  private TextView summary;
  private EditText soundHost;
  private CheckBox soundToggle;
  private String editing;
  private String section = "Shortcut";
  private PresetStore.Visual draft;
  private boolean automatic;
  private EditText left, top, width, height;
  private final Map<String, Button> navigation = new LinkedHashMap<>();

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    ui = new TvUi(this);
    prefs = getSharedPreferences("settings", 0);
    presets = new PresetStore(this);
    editing = presets.manual();
    if (presets.automatic() && CoverService.instance != null)
      editing = CoverService.instance.selectedPreset();
    draft = presets.read(editing);
    automatic = presets.automatic();
    LinearLayout root = ui.page("Ad Dimmer", null);
    summary = ui.text("", 16, false);
    root.addView(summary);
    ui.gap(root, 14);
    LinearLayout body = ui.row();
    body.setGravity(Gravity.TOP);
    LinearLayout nav = ui.column();
    body.addView(nav, new LinearLayout.LayoutParams(ui.dp(180), -1));
    panel = ui.column();
    panel.setPadding(ui.dp(30), 0, 0, 0);
    body.addView(ui.scroll(panel), new LinearLayout.LayoutParams(0, -1, 1));
    root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
    for (String name : new String[] {"Shortcut", "Picture", "Sound", "Advanced"}) {
      Button b = ui.button(name, false, () -> selectSection(name));
      navigation.put(name, b);
      ui.addButton(nav, b);
    }
    Space spacer = new Space(this);
    nav.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
    ui.addButton(
        nav,
        ui.button(
            "Disable shortcut",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Disable the dimmer shortcut?")
                    .setMessage(
                        "Your remote will work normally. You can choose another shortcut later.")
                    .setNegativeButton("Keep shortcut", null)
                    .setPositiveButton(
                        "Disable shortcut",
                        (d, w) -> {
                          new ShortcutStore(this).disable();
                          if (CoverService.instance != null)
                            CoverService.instance.restoreNormalButtons();
                          refreshSummary();
                          if (section.equals("Shortcut")) showSection();
                        })
                    .show()));
    setContentView(root);
    showSection();
    navigation.get("Shortcut").requestFocus();
  }

  private void refreshSummary() {
    if (summary == null) return;
    ShortcutStore.Mapping m = new ShortcutStore(this).read();
    String active =
        CoverService.instance == null
            ? AppPresetPolicy.DEFAULT
            : CoverService.instance.selectedPreset();
    int darkness = presets.read(active).opacity;
    String presetLabel = active.equals(AppPresetPolicy.DEFAULT) ? "Default" : presets.name(active);
    summary.setText(
        (section.equals("Shortcut") ? "" : RemoteLabels.shortcut(m.key, m.mode) + "   ·   ")
            + darkness
            + "% darkness   ·   Preset: "
            + presetLabel);
  }

  private void selectSection(String name) {
    if (name.equals(section)) return;
    if (section.equals("Sound")
        && soundHost != null
        && (soundToggle.isChecked() != prefs.getBoolean("sonos", false)
            || !soundHost.getText().toString().trim().equals(prefs.getString("host", "")))) {
      new AlertDialog.Builder(this)
          .setTitle("Save sound changes?")
          .setPositiveButton(
              "Save",
              (d, w) -> {
                if (saveSound()) {
                  section = name;
                  showSection();
                }
              })
          .setNegativeButton(
              "Discard",
              (d, w) -> {
                section = name;
                showSection();
              })
          .setNeutralButton("Stay here", null)
          .show();
      return;
    }
    if (!captureGeometry()) return;
    if (section.equals("Picture") || section.equals("Advanced")) {
      PresetStore.Visual saved = presets.read(editing);
      if (changed(saved, draft) || automatic != presets.automatic()) {
        new AlertDialog.Builder(this)
            .setTitle("Save picture changes?")
            .setNegativeButton(
                "Discard",
                (d, w) -> {
                  draft = presets.read(editing);
                  automatic = presets.automatic();
                  section = name;
                  showSection();
                })
            .setPositiveButton(
                "Save",
                (d, w) -> {
                  if (!savePicture()) return;
                  section = name;
                  showSection();
                })
            .setNeutralButton("Stay here", null)
            .show();
        return;
      }
    }
    section = name;
    showSection();
  }

  private boolean changed(PresetStore.Visual a, PresetStore.Visual b) {
    return a.opacity != b.opacity
        || a.left != b.left
        || a.top != b.top
        || a.width != b.width
        || a.height != b.height
        || a.timer != b.timer;
  }

  private void heading(String title, String note) {
    panel.addView(ui.text(title, 24, true));
    if (note != null) ui.note(panel, note);
    ui.gap(panel, 8);
  }

  private void showSection() {
    panel.removeAllViews();
    left = top = width = height = null;
    for (Map.Entry<String, Button> n : navigation.entrySet())
      n.getValue().setText((n.getKey().equals(section) ? "›  " : "") + n.getKey());
    if (section.equals("Shortcut")) shortcut();
    else if (section.equals("Picture")) picture();
    else if (section.equals("Sound")) sound();
    else advanced();
    refreshSummary();
  }

  private void shortcut() {
    heading("Your shortcut", "Back always brings the picture back.");
    ShortcutStore.Mapping m = new ShortcutStore(this).read();
    panel.addView(ui.text(RemoteLabels.shortcut(m.key, m.mode), 22, true));
    ui.addButton(
        panel,
        ui.button(
            m.key < 0 ? "Choose shortcut" : "Change shortcut",
            true,
            () -> startActivity(new Intent(this, ShortcutActivity.class))));
    ui.addButton(
        panel,
        ui.button(
            "Try dimmer",
            false,
            () -> {
              if (CoverService.instance == null) toast("Enable remote controls first");
              else CoverService.instance.preview();
            }));
    if (CoverService.instance == null) {
      ui.note(panel, "Enable remote controls to use Ad Dimmer.");
      ui.addButton(
          panel,
          ui.button(
              "Enable remote controls",
              false,
              () ->
                  startActivity(
                      new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))));
    }
  }

  private void picture() {
    heading("Picture", "Choose how much of the ad you want to see.");
    ui.label(panel, "Preset");
    List<String> ids = new ArrayList<>();
    ids.add(AppPresetPolicy.DEFAULT);
    List<String> apps = new ArrayList<>(presets.apps());
    Collections.sort(apps);
    ids.addAll(apps);
    List<String> labels = new ArrayList<>();
    for (String id : ids) labels.add(presets.name(id));
    Spinner profile = ui.spinner(labels);
    profile.setSelection(Math.max(0, ids.indexOf(editing)));
    panel.addView(profile);
    profile.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            String next = ids.get(pos);
            if (next.equals(editing)) return;
            if (changed(presets.read(editing), draft)) {
              new AlertDialog.Builder(MainActivity.this)
                  .setTitle("Save changes to " + presets.name(editing) + "?")
                  .setPositiveButton(
                      "Save",
                      (d, w) -> {
                        savePicture();
                        editing = next;
                        draft = presets.read(next);
                        showSection();
                      })
                  .setNegativeButton(
                      "Discard",
                      (d, w) -> {
                        editing = next;
                        draft = presets.read(next);
                        showSection();
                      })
                  .setOnCancelListener(d -> profile.setSelection(ids.indexOf(editing)))
                  .show();
            } else {
              editing = next;
              draft = presets.read(next);
              showSection();
            }
          }

          public void onNothingSelected(AdapterView<?> p) {}
        });
    CheckBox auto = ui.check("Choose preset automatically", automatic);
    auto.setOnCheckedChangeListener((b, on) -> automatic = on);
    panel.addView(auto);
    TextView value = ui.text("Darkness   " + draft.opacity + "%", 20, true);
    panel.addView(value);
    SeekBar slider = new SeekBar(this);
    slider.setMax(100);
    slider.setProgress(draft.opacity);
    slider.setProgressTintList(android.content.res.ColorStateList.valueOf(TvUi.ACCENT));
    slider.setThumbTintList(android.content.res.ColorStateList.valueOf(TvUi.ACCENT));
    slider.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
    slider.setMinimumHeight(ui.dp(50));
    ui.focusOutline(slider);
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar s, int n, boolean user) {
            draft =
                new PresetStore.Visual(
                    n, draft.left, draft.top, draft.width, draft.height, draft.timer);
            value.setText("Darkness   " + n + "%");
          }

          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}
        });
    panel.addView(slider);
    CheckBox timer = ui.check("Keep ad timer visible", draft.timer);
    timer.setOnCheckedChangeListener(
        (b, on) ->
            draft =
                new PresetStore.Visual(
                    draft.opacity, draft.left, draft.top, draft.width, draft.height, on));
    panel.addView(timer);
    ui.addButton(panel, ui.button("Save picture", true, this::savePicture));
  }

  private void sound() {
    heading("Sound", "Optional Sonos mute while the picture is dimmed.");
    boolean enabled = prefs.getBoolean("sonos", false);
    CheckBox toggle = ui.check("Mute Sonos", enabled);
    soundToggle = toggle;
    panel.addView(toggle);
    LinearLayout address = ui.column();
    ui.label(address, "Speaker address");
    EditText host = ui.input(prefs.getString("host", ""), false);
    soundHost = host;
    host.setHint("From your Sonos app");
    address.addView(host);
    panel.addView(address);
    address.setVisibility(enabled ? View.VISIBLE : View.GONE);
    toggle.setOnCheckedChangeListener(
        (b, on) -> address.setVisibility(on ? View.VISIBLE : View.GONE));
    ui.addButton(panel, ui.button("Save sound", true, () -> saveSound()));
    ui.addButton(
        panel,
        ui.button(
            "Where do I find the address?",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Sonos speaker address")
                    .setMessage(
                        "Find the soundbar's IPv4 address in the Sonos app's system information, or"
                            + " in your router's device list. Use the speaker playing your TV"
                            + " audio. Both devices need the same home network.")
                    .setPositiveButton("Got it", null)
                    .show()));
  }

  private boolean saveSound() {
    String ip = soundHost.getText().toString().trim();
    if (soundToggle.isChecked() && !Config.validHost(ip)) {
      toast("Enter the speaker's private IPv4 address");
      return false;
    }
    if (CoverService.instance != null && CoverService.instance.isCovered()) {
      toast("Uncover before changing sound settings");
      return false;
    }
    prefs.edit().putBoolean("sonos", soundToggle.isChecked()).putString("host", ip).apply();
    toast("Sound saved");
    return true;
  }

  private void advanced() {
    heading("Timer opening", presets.name(editing) + " · 1920 × 1080 reference pixels");
    LinearLayout row = ui.row();
    left = geometry(row, "Left", draft.left);
    top = geometry(row, "Top", draft.top);
    width = geometry(row, "Width", draft.width);
    height = geometry(row, "Height", draft.height);
    panel.addView(row);
    ui.addButton(panel, ui.button("Save position", true, this::savePicture));
    ui.gap(panel, 10);
    ui.label(panel, "More app presets");
    ui.note(panel, "For contributors: add the app's Android package ID.");
    EditText id = ui.input("", false);
    id.setHint("Package ID");
    panel.addView(id);
    EditText name = ui.input("", false);
    name.setHint("Preset name");
    panel.addView(name);
    ui.addButton(
        panel,
        ui.button(
            "Add preset",
            false,
            () -> {
              String key = id.getText().toString().trim();
              if (!key.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) {
                toast("Enter a valid package ID");
                return;
              }
              if (presets.apps().contains(key)) {
                toast("That preset already exists");
                return;
              }
              if (!savePicture()) return;
              presets.add(key, name.getText().toString().trim());
              editing = key;
              draft = presets.read(key);
              section = "Picture";
              showSection();
            }));
    ui.addButton(
        panel,
        ui.button(
            "Privacy & permissions",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Privacy & permissions")
                    .setMessage(
                        "Ad Dimmer handles remote buttons and app identity. It does not read video"
                            + " or screen text, record your screen, or upload viewing history."
                            + " Optional Sonos commands stay on your home network. Enable only one"
                            + " remote-button remapper at a time.")
                    .setPositiveButton("Got it", null)
                    .show()));
  }

  private EditText geometry(LinearLayout row, String title, int value) {
    LinearLayout cell = ui.column();
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
    p.rightMargin = ui.dp(10);
    row.addView(cell, p);
    ui.label(cell, title);
    EditText e = ui.input("" + value, true);
    cell.addView(e);
    return e;
  }

  private boolean captureGeometry() {
    if (left == null) return true;
    try {
      int l = Integer.parseInt(left.getText().toString()),
          t = Integer.parseInt(top.getText().toString()),
          w = Integer.parseInt(width.getText().toString()),
          h = Integer.parseInt(height.getText().toString());
      if (!Config.validGeometry(l, t, w, h)) throw new IllegalArgumentException();
      draft = new PresetStore.Visual(draft.opacity, l, t, w, h, draft.timer);
      return true;
    } catch (Exception e) {
      toast("Use whole numbers and keep the opening inside 1920 × 1080");
      return false;
    }
  }

  private boolean savePicture() {
    if (!captureGeometry()) return false;
    if (CoverService.instance != null && CoverService.instance.isCovered()) {
      toast("Uncover before saving");
      return false;
    }
    presets.save(editing, draft, automatic);
    refreshSummary();
    toast("Picture saved");
    return true;
  }

  private void toast(String value) {
    Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onResume() {
    super.onResume();
    visible = true;
    if (ui != null) {
      refreshSummary();
      if (section.equals("Shortcut")) showSection();
    }
  }

  @Override
  protected void onPause() {
    visible = false;
    super.onPause();
  }
}
