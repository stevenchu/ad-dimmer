package org.stevenchu.screencover;

/** Pure gesture timing. Double/hold observes native keys; it never delays or replays them. */
public final class ShortcutGesture {
  public final String mode;
  private final long doubleMs, holdMs;
  private long downAt = -1, lastTap = -1;
  private boolean repeated, held;

  public ShortcutGesture(String mode, long doubleMs, long holdMs) {
    this.mode = mode;
    this.doubleMs = doubleMs;
    this.holdMs = holdMs;
  }

  public void reset() {
    downAt = -1;
    lastTap = -1;
    repeated = false;
    held = false;
  }

  public boolean down(long now, int repeats) {
    if (repeats == 0) {
      downAt = now;
      repeated = false;
      held = false;
    } else {
      repeated = true;
      lastTap = -1;
    }
    return false;
  }

  public boolean tick(long now) {
    if (mode.equals("hold") && downAt >= 0 && !held && now - downAt >= holdMs) {
      held = true;
      lastTap = -1;
      return true;
    }
    return false;
  }

  public boolean up(long now) {
    if (downAt < 0) return false;
    long duration = now - downAt;
    downAt = -1;
    if (mode.equals("single")) return !repeated;
    if (mode.equals("hold")) return false;
    if (repeated || duration >= holdMs) {
      lastTap = -1;
      return false;
    }
    if (lastTap >= 0 && now - lastTap <= doubleMs) {
      lastTap = -1;
      return true;
    }
    lastTap = now;
    return false;
  }
}
