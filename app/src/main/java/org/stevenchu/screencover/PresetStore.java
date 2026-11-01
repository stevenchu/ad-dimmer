package org.stevenchu.screencover;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/** Versioned local storage. Geometry uses 1920 x 1080 reference pixels; audio stays global. */
public final class PresetStore {
  private final SharedPreferences data;

  public static final class Visual {
    public final int opacity, left, top, width, height;
    public final boolean timer;

    public Visual(int opacity, int left, int top, int width, int height, boolean timer) {
      this.opacity = opacity;
      this.left = left;
      this.top = top;
      this.width = width;
      this.height = height;
      this.timer = timer;
    }
  }

  public PresetStore(Context context) {
    data = context.getSharedPreferences("presets", 0);
    if (data.getInt("schema", 0) == 0) {
      SharedPreferences old = context.getSharedPreferences("settings", 0);
      Visual inherited =
          new Visual(
              old.getInt("opacity", 95),
              old.getInt("left", 1685),
              old.getInt("top", 72),
              old.getInt("width", 140),
              old.getInt("height", 56),
              old.getBoolean("timer", true));
      SharedPreferences.Editor edit = data.edit();
      write(edit, AppPresetPolicy.DEFAULT, inherited);
      // Preserve layouts from the legacy app; fresh installs use the calibrated Prime cutout.
      boolean legacyLayout = old.contains("left") || old.contains("top")
          || old.contains("width") || old.contains("height");
      write(edit, AppPresetPolicy.PRIME, legacyLayout ? inherited : primeDefault());
      edit.putStringSet("apps", new HashSet<>(Arrays.asList(AppPresetPolicy.PRIME)))
          .putString("name." + AppPresetPolicy.PRIME, "Prime Video")
          .putBoolean("automatic", true)
          .putString("manual", AppPresetPolicy.DEFAULT)
          .putInt("schema", 1)
          .commit();
    }
  }

  /** Calibrated on Prime Video at 1080p; coordinates scale with the overlay. */
  public static Visual primeDefault() {
    return new Visual(95, 1685, 72, 138, 54, true);
  }

  public Set<String> apps() {
    return new HashSet<>(data.getStringSet("apps", Collections.emptySet()));
  }

  public String name(String id) {
    return id.equals(AppPresetPolicy.DEFAULT)
        ? "Default (other apps)"
        : data.getString("name." + id, id);
  }

  public boolean automatic() {
    return data.getBoolean("automatic", true);
  }

  public String manual() {
    return data.getString("manual", AppPresetPolicy.DEFAULT);
  }

  public Visual read(String id) {
    String p = id + ".";
    if (!data.contains(p + "opacity") && !id.equals(AppPresetPolicy.DEFAULT))
      return read(AppPresetPolicy.DEFAULT);
    return new Visual(
        data.getInt(p + "opacity", 95),
        data.getInt(p + "left", 1685),
        data.getInt(p + "top", 72),
        data.getInt(p + "width", 140),
        data.getInt(p + "height", 56),
        data.getBoolean(p + "timer", true));
  }

  private void write(SharedPreferences.Editor e, String id, Visual v) {
    String p = id + ".";
    e.putInt(p + "opacity", v.opacity)
        .putInt(p + "left", v.left)
        .putInt(p + "top", v.top)
        .putInt(p + "width", v.width)
        .putInt(p + "height", v.height)
        .putBoolean(p + "timer", v.timer);
  }

  public void save(String id, Visual visual, boolean automatic) {
    SharedPreferences.Editor e = data.edit();
    write(e, id, visual);
    e.putBoolean("automatic", automatic).putString("manual", id).commit();
  }

  public void add(String id, String name) {
    Set<String> ids = apps();
    ids.add(id);
    SharedPreferences.Editor e = data.edit();
    write(e, id, read(AppPresetPolicy.DEFAULT));
    e.putStringSet("apps", ids).putString("name." + id, name.isEmpty() ? id : name).commit();
  }
}
