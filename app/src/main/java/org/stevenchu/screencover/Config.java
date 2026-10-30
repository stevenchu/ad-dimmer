package org.stevenchu.screencover;

/** Pure validation shared by the TV form and the Sonos connection. */
public final class Config {
  private Config() {}

  public static boolean validHost(String host) {
    if (host == null) return false;
    String[] parts = host.split("\\.", -1);
    if (parts.length != 4) return false;
    int[] octets = new int[4];
    for (int i = 0; i < 4; i++) {
      if (!parts[i].matches("0|[1-9][0-9]{0,2}")) return false;
      octets[i] = Integer.parseInt(parts[i]);
      if (octets[i] > 255) return false;
    }
    return octets[0] == 10
        || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
        || (octets[0] == 192 && octets[1] == 168);
  }

  public static boolean validGeometry(int left, int top, int width, int height) {
    return left >= 0
        && top >= 0
        && width >= 20
        && height >= 20
        && width <= 1920
        && height <= 1080
        && left <= 1920 - width
        && top <= 1080 - height;
  }
}
