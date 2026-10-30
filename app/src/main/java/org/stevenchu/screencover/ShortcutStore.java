package org.stevenchu.screencover;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Only confirmed mappings persist. Trials live in the service and cannot survive a crash/reboot.
 */
public final class ShortcutStore {
  public static final class Mapping {
    public final int key, doubleMs, holdMs;
    public final String mode;

    public Mapping(int key, String mode, int doubleMs, int holdMs) {
      this.key = key;
      this.mode = mode;
      this.doubleMs = doubleMs;
      this.holdMs = holdMs;
    }
  }

  private final SharedPreferences prefs;

  public ShortcutStore(Context c) {
    prefs = c.getSharedPreferences("shortcut", 0);
  }

  public Mapping read() {
    return new Mapping(
        prefs.getInt("key", -1),
        prefs.getString("mode", "double"),
        prefs.getInt("doubleMs", 650),
        prefs.getInt("holdMs", 800));
  }

  public void save(Mapping m) {
    prefs
        .edit()
        .putInt("key", m.key)
        .putString("mode", m.mode)
        .putInt("doubleMs", m.doubleMs)
        .putInt("holdMs", m.holdMs)
        .commit();
  }

  public void disable() {
    prefs.edit().clear().commit();
  }
}
