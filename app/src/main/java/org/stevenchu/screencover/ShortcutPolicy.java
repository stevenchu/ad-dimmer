package org.stevenchu.screencover;

/** Android key-code constants, kept pure so protected navigation has JVM tests. */
public final class ShortcutPolicy {
  public static boolean allowed(int key, String mode) {
    if (key <= 0 || key > 304) return false;
    for (int protectedKey :
        new int[] {3, 4, 19, 20, 21, 22, 23, 24, 26, 66, 111, 187, 219, 223, 224, 231, 276})
      if (key == protectedKey) return false;
    if (!mode.equals("single") && !mode.equals("double") && !mode.equals("hold")) return false;
    return !(key == 25 && mode.equals("single"));
  }

  public static boolean needsReplacementWarning(String mode) {
    return mode.equals("single");
  }
}
