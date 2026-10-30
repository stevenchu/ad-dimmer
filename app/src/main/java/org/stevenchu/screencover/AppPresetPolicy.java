package org.stevenchu.screencover;

/** App identity only: never reads accessibility text or the video. No viewing history is saved. */
public final class AppPresetPolicy {
  public static final String DEFAULT = "default";
  public static final String PRIME = "com.amazon.amazonvideo.livingroom";
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

  public String select(boolean automatic, String manual, java.util.Set<String> savedApps) {
    String candidate = automatic ? currentApp : manual;
    return savedApps.contains(candidate) ? candidate : DEFAULT;
  }
}
