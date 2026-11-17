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
    Set<String> bundled = new HashSet<>(Arrays.asList(
        AppPresetPolicy.PRIME, AppPresetPolicy.HBO_MAX, AppPresetPolicy.FULL_DIM));
    check(new AppPresetPolicy().select(true, "", bundled).equals(AppPresetPolicy.FULL_DIM));
    check(p.observe(AppPresetPolicy.HBO_MAX, true, own));
    check(p.select(true, "", bundled).equals(AppPresetPolicy.HBO_MAX));
    check(p.observe("com.hbo.hbonow", true, own));
    check(p.select(true, "", bundled).equals(AppPresetPolicy.HBO_MAX));
    check(p.select(false, AppPresetPolicy.PRIME, bundled).equals(AppPresetPolicy.PRIME));
    check(AppPresetPolicy.cycle(AppPresetPolicy.PRIME, true, bundled).equals(AppPresetPolicy.HBO_MAX));
    check(AppPresetPolicy.cycle(AppPresetPolicy.HBO_MAX, false, bundled).equals(AppPresetPolicy.PRIME));
    check(AppPresetPolicy.cycle(AppPresetPolicy.FULL_DIM, true, bundled).equals(AppPresetPolicy.DEFAULT));
    check(AppPresetPolicy.cycle(AppPresetPolicy.DEFAULT, false, bundled).equals(AppPresetPolicy.FULL_DIM));
    System.out.println("App preset policy checks passed");
  }
}
