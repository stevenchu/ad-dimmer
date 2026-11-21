package org.stevenchu.screencover;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/** Versioned local storage. Geometry uses 1920 x 1080 reference pixels; audio stays global. */
public final class PresetStore {
  private static final Visual HBO_MAX_DEFAULT = new Visual(95, 106, 74, 226, 43, true);
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
    int schema = data.getInt("schema", 0);
    if (schema == 0) {
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
    if (schema < 2) {
      Set<String> ids = apps();
      SharedPreferences.Editor edit = data.edit();
      if (ids.add(AppPresetPolicy.HBO_MAX)) {
        // Estimated from the full-screen reference photo, including the Ad badge.
        write(edit, AppPresetPolicy.HBO_MAX, HBO_MAX_DEFAULT);
        edit.putString("name." + AppPresetPolicy.HBO_MAX, "HBO Max");
      }
      ids.add(AppPresetPolicy.FULL_DIM);
      write(edit, AppPresetPolicy.FULL_DIM, new Visual(95, 0, 0, 20, 20, false));
      edit.putString("name." + AppPresetPolicy.FULL_DIM, "Full dimming")
          .putStringSet("apps", ids)
          .putInt("schema", 6)
          .commit();
    }
    if (schema >= 2 && schema < 6) {
      Visual hbo = read(AppPresetPolicy.HBO_MAX);
      SharedPreferences.Editor edit = data.edit();
      // Replace the original preview geometry while preserving custom positions and darkness.
      boolean originalPreview =
          schema == 2 && hbo.left == 116 && hbo.top == 76 && hbo.width == 276 && hbo.height == 66;
      boolean shortRectangle =
          schema <= 3 && hbo.left == 106 && hbo.top == 66 && hbo.width == 226 && hbo.height == 26;
      boolean tallRectangle =
          schema <= 4 && hbo.left == 106 && hbo.top == 71 && hbo.width == 226 && hbo.height == 50;
      boolean previousRectangle =
          hbo.left == 106 && hbo.top == 73 && hbo.width == 226 && hbo.height == 46;
      if (originalPreview || shortRectangle || tallRectangle || previousRectangle) {
        write(
            edit,
            AppPresetPolicy.HBO_MAX,
            new Visual(
                hbo.opacity, HBO_MAX_DEFAULT.left, HBO_MAX_DEFAULT.top, HBO_MAX_DEFAULT.width,
                HBO_MAX_DEFAULT.height, hbo.timer));
      }
      edit.putInt("schema", 6).commit();
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
        ? "Default"
        : data.getString("name." + id, id);
  }

  public boolean automatic() {
    return data.getBoolean("automatic", true);
  }

  public String manual() {
    return data.getString("manual", AppPresetPolicy.DEFAULT);
  }

  public void saveManualSelection(String selected) {
    data.edit().putString("manual", selected).apply();
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
