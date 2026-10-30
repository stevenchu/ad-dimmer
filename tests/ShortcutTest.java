import org.stevenchu.screencover.ShortcutGesture;
import org.stevenchu.screencover.ShortcutPolicy;

public class ShortcutTest {
  static void check(boolean value) {
    if (!value) throw new AssertionError();
  }

  public static void main(String[] args) {
    for (int k :
        new int[] {3, 4, 19, 20, 21, 22, 23, 24, 26, 66, 111, 187, 219, 223, 224, 231, 276})
      for (String m : new String[] {"single", "double", "hold"})
        check(!ShortcutPolicy.allowed(k, m));
    check(!ShortcutPolicy.allowed(25, "single"));
    check(ShortcutPolicy.allowed(82, "double"));
    check(ShortcutPolicy.allowed(164, "single"));
    check(ShortcutPolicy.needsReplacementWarning("single"));
    ShortcutGesture d = new ShortcutGesture("double", 650, 800);
    d.down(0, 0);
    check(!d.up(40));
    d.down(480, 0);
    check(d.up(520));
    d.down(700, 0);
    check(!d.up(740));
    d.down(1500, 0);
    check(!d.up(1540));
    ShortcutGesture held = new ShortcutGesture("double", 650, 800);
    held.down(0, 0);
    held.down(500, 1);
    check(!held.up(1000));
    held.down(1100, 0);
    check(!held.up(1140));
    ShortcutGesture h = new ShortcutGesture("hold", 650, 800);
    h.down(0, 0);
    check(!h.tick(799));
    check(h.tick(800));
    check(!h.tick(900));
    check(!h.up(950));
    ShortcutGesture single = new ShortcutGesture("single", 650, 800);
    single.down(0, 0);
    check(single.up(50));
    check(!single.up(60));
    ShortcutGesture interrupted = new ShortcutGesture("double", 650, 800);
    interrupted.down(0, 0);
    check(!interrupted.up(10));
    interrupted.reset();
    interrupted.down(100, 0);
    check(!interrupted.up(110));
    System.out.println("Shortcut timing and protected-key checks passed");
  }
}
