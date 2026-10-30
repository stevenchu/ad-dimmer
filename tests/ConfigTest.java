import org.stevenchu.screencover.Config;

public class ConfigTest {
  private static void check(boolean result) {
    if (!result) throw new AssertionError();
  }

  public static void main(String[] args) {
    for (String ip : new String[] {"10.1.2.3", "172.16.0.1", "172.31.255.254", "192.168.2.8"})
      check(Config.validHost(ip));
    for (String ip :
        new String[] {
          "",
          "localhost",
          "8.8.8.8",
          "127.0.0.1",
          "172.32.0.1",
          "10.0.0.256",
          "10.0.0.-1",
          "010.0.0.1",
          "10.0.0.1:1400",
          "10.0.0.1/path"
        }) check(!Config.validHost(ip));
    check(Config.validGeometry(1685, 72, 140, 56));
    check(Config.validGeometry(0, 0, 1920, 1080));
    check(!Config.validGeometry(-1, 0, 100, 50));
    check(!Config.validGeometry(1800, 0, 140, 50));
    check(!Config.validGeometry(0, 1050, 140, 56));
    check(!Config.validGeometry(0, 0, 10, 50));
    check(!Config.validGeometry(Integer.MAX_VALUE, 0, 140, 50));
    System.out.println("Configuration checks passed");
  }
}
