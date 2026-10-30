package org.stevenchu.screencover;

/** Plain-language remote names; raw Android constants stay out of the setup UI. */
public final class RemoteLabels {
  private RemoteLabels() {}

  public static String button(int key) {
    switch (key) {
      case 82:
        return "Menu";
      case 164:
        return "Mute";
      case 178:
        return "Input";
      case 25:
        return "Volume Down";
      default:
        return key < 0 ? "No shortcut" : "Button " + key;
    }
  }

  public static String mode(String mode) {
    return "single".equals(mode) ? "Single-press" : "hold".equals(mode) ? "Hold" : "Double-press";
  }

  public static String shortcut(int key, String mode) {
    return key < 0 ? "Choose a shortcut" : mode(mode) + " " + button(key);
  }
}
