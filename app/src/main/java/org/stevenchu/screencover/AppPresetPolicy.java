package org.stevenchu.screencover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** App identity only: never reads accessibility text or the video. No viewing history is saved. */
public final class AppPresetPolicy {
  public static final String DEFAULT = "default";
  public static final String PRIME = "com.amazon.amazonvideo.livingroom";
  public static final String HBO_MAX = "com.wbd.stream";
  public static final String FULL_DIM = "full-dim";
  private static final String HBO_MAX_LEGACY = "com.hbo.hbonow";
  private String currentApp = "";

  /**
   * A window event alone is not proof of foreground activity. The caller also checks launchability.
   */
  public boolean observe(String pkg, boolean confirmedApp, String ownPackage) {
    if (!confirmedApp
        || pkg == null
        || pkg.isEmpty()
        || pkg.equals(ownPackage)
        || pkg.equals("android")
        || pkg.equals("com.android.systemui")
        || pkg.contains("inputmethod")
        || pkg.contains("keyboard")
        || pkg.equals(currentApp)) return false;
    currentApp = pkg;
    return true;
  }

  public String currentApp() {
    return currentApp;
  }

  public String select(boolean automatic, String manual, Set<String> savedApps) {
    String candidate = automatic ? currentApp : manual;
    if (automatic && candidate.equals(HBO_MAX_LEGACY) && !savedApps.contains(candidate)) {
      candidate = HBO_MAX;
    }
    if (savedApps.contains(candidate)) return candidate;
    return automatic && savedApps.contains(FULL_DIM) ? FULL_DIM : DEFAULT;
  }

  public static List<String> orderedProfiles(Set<String> savedApps) {
    List<String> profiles = new ArrayList<>(savedApps);
    profiles.remove(DEFAULT);
    Collections.sort(profiles);
    profiles.add(0, DEFAULT);
    return profiles;
  }

  public static String cycle(String selected, boolean right, Set<String> savedApps) {
    List<String> profiles = orderedProfiles(savedApps);
    int index = Math.max(0, profiles.indexOf(selected));
    return profiles.get(Math.floorMod(index + (right ? 1 : -1), profiles.size()));
  }
}
