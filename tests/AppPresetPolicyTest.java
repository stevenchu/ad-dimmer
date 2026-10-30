import java.util.*;
import org.stevenchu.screencover.AppPresetPolicy;

public class AppPresetPolicyTest {
  static void check(boolean ok) {
    if (!ok) throw new AssertionError();
  }

  public static void main(String[] args) {
    AppPresetPolicy p = new AppPresetPolicy();
    String own = "org.stevenchu.screencover";
    Set<String> profiles =
        new HashSet<>(Arrays.asList(AppPresetPolicy.PRIME, "example.test.player"));
    check(p.select(true, "", profiles).equals("default"));
    check(p.observe(AppPresetPolicy.PRIME, true, own));
    check(p.select(true, "", profiles).equals(AppPresetPolicy.PRIME));
    for (String ignored :
        new String[] {
          own, "com.android.systemui", "android", "com.google.android.inputmethod.latin"
        }) check(!p.observe(ignored, true, own));
    check(!p.observe("example.volume.overlay", false, own));
    check(!p.observe(null, true, own));
    check(p.select(true, "", profiles).equals(AppPresetPolicy.PRIME));
    check(!p.observe(AppPresetPolicy.PRIME, true, own));
    check(p.observe("example.unknown.player", true, own));
    check(p.select(true, "", profiles).equals("default"));
    check(p.select(false, AppPresetPolicy.PRIME, profiles).equals(AppPresetPolicy.PRIME));
    check(p.select(false, "missing.profile", profiles).equals("default"));
    check(p.observe("example.test.player", true, own));
    check(p.select(true, "", profiles).equals("example.test.player"));
    System.out.println("App preset policy checks passed");
  }
}
