package org.stevenchu.screencover;

import android.app.*;
import android.os.*;
import android.view.KeyEvent;
import android.widget.*;

/**
 * Recovery-first setup: a trial is temporary, and only a gesture actually observed can be saved.
 */
public class ShortcutActivity extends Activity {
  public static boolean visible;
  private Spinner keyChoice, modeChoice;
  private EditText gap;
  private TextView status;
  private CheckBox alternateAccess;
  private Button confirm;
  private int captured = -1;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable refresh =
      new Runnable() {
        public void run() {
          update();
          handler.postDelayed(this, 500);
        }
      };

  private CoverService service() {
    return CoverService.instance;
  }

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(1);
    content.setPadding(48, 24, 48, 48);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(content);
    label(content, "Ad Dimmer shortcut", 28);
    label(
        content,
        "Home, Back, navigation, Power, and Volume Up are protected. Back always uncovers. Open Ad"
            + " Dimmer from Apps to change or disable your shortcut.",
        18);
    status = new TextView(this);
    status.setTextSize(20);
    content.addView(status);
    Button reset = new Button(this);
    reset.setText("Restore normal remote buttons");
    content.addView(reset);
    reset.setOnClickListener(
        v -> {
          new ShortcutStore(this).disable();
          if (service() != null) service().restoreNormalButtons();
          update();
        });
    label(content, "Choose a button", 20);
    keyChoice = new Spinner(this);
    keyChoice.setAdapter(
        new ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {
              "Menu (☰)", "Mute", "Input", "Volume Down (advanced)", "Recorded button"
            }));
    content.addView(keyChoice);
    Button record = new Button(this);
    record.setText("Record another remote button");
    content.addView(record);
    record.setOnClickListener(
        v -> {
          if (service() == null) {
            message("Enable the Ad Dimmer Accessibility service first");
            return;
          }
          captured = -1;
          keyChoice.setSelection(4);
          service().startRecording();
        });
    label(content, "Trigger", 20);
    modeChoice = new Spinner(this);
    modeChoice.setAdapter(
        new ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {
              "Double press (native actions also run)",
              "Hold (native action also runs)",
              "Single press (replaces normal action)"
            }));
    content.addView(modeChoice);
    label(content, "Double-press window in milliseconds (250–1200)", 18);
    gap = new EditText(this);
    gap.setSingleLine(true);
    gap.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    gap.setText("650");
    content.addView(gap);
    label(
        content,
        "For double press and hold, normal button actions are immediate. A menu may open, or Mute"
            + " may change sound, before dimming. Choose another button if that interferes. Hold is"
            + " 800 ms. Volume Down is never delayed; avoid assigning it if accidental dimming is a"
            + " concern.",
        18);
    alternateAccess = new CheckBox(this);
    alternateAccess.setText(
        "For single press: I know another way to open the original function, using Home / Apps or"
            + " another button.");
    content.addView(alternateAccess);
    Button test = new Button(this);
    test.setText("Start a 90-second temporary test");
    content.addView(test);
    test.setOnClickListener(v -> startTest());
    label(
        content,
        "Try the shortcut, uncover with Back, then return here using Home → Apps → Ad Dimmer →"
            + " Choose / test remote shortcut. Confirm before the test expires. If you do nothing,"
            + " it reverts. No shortcut is saved until confirmation.",
        18);
    Button prime = new Button(this);
    prime.setText("Open Prime Video to test");
    content.addView(prime);
    prime.setOnClickListener(
        v -> {
          android.content.Intent intent =
              getPackageManager().getLeanbackLaunchIntentForPackage(AppPresetPolicy.PRIME);
          if (intent == null)
            intent = getPackageManager().getLaunchIntentForPackage(AppPresetPolicy.PRIME);
          if (intent != null) startActivity(intent);
          else message("Prime Video is not available; use Home to open your video app.");
        });
    confirm = new Button(this);
    confirm.setText("It worked — save this shortcut");
    content.addView(confirm);
    confirm.setOnClickListener(
        v -> {
          if (service() != null && service().confirmTrial()) message("Shortcut saved");
          else message("Trigger the shortcut, uncover, then confirm before the test expires");
          update();
        });
    Button cancel = new Button(this);
    cancel.setText("Cancel test / keep previous shortcut");
    content.addView(cancel);
    cancel.setOnClickListener(
        v -> {
          if (service() != null) service().cancelTrial();
          update();
        });
    setContentView(scroll);
  }

  private void label(LinearLayout parent, String text, int size) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setTextSize(size);
    t.setPadding(0, 10, 0, 10);
    parent.addView(t);
  }

  private void message(String text) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
  }

  private void startTest() {
    if (service() == null) {
      message("Enable Ad Dimmer in Accessibility first");
      return;
    }
    int key = new int[] {82, 164, 178, 25, captured}[keyChoice.getSelectedItemPosition()];
    String mode = new String[] {"double", "hold", "single"}[modeChoice.getSelectedItemPosition()];
    int window;
    try {
      window = Integer.parseInt(gap.getText().toString());
    } catch (Exception e) {
      message("Enter a timing value from 250 to 1200");
      return;
    }
    if (window < 250 || window > 1200) {
      message("Choose a timing value from 250 to 1200");
      return;
    }
    if (!ShortcutPolicy.allowed(key, mode)) {
      message("That button/trigger is protected or no button has been recorded");
      return;
    }
    Runnable apply =
        () -> {
          service().startTrial(new ShortcutStore.Mapping(key, mode, window, 800));
          update();
          message("Temporary test started; expires in 90 seconds unless confirmed");
        };
    if (mode.equals("single")) {
      if (!alternateAccess.isChecked()) {
        message("Confirm another way to reach the button's original function first");
        return;
      }
      new AlertDialog.Builder(this)
          .setTitle("Replace this button's normal action?")
          .setMessage(
              "Single press of "
                  + KeyEvent.keyCodeToString(key)
                  + " will dim instead of its original action. This may replace Settings, Input, or"
                  + " ordinary Mute. Home and Back remain protected. Test expires after 90"
                  + " seconds.")
          .setNegativeButton("Cancel", null)
          .setPositiveButton("Test temporarily", (d, w) -> apply.run())
          .show();
    } else apply.run();
  }

  private void update() {
    if (status == null) return;
    CoverService s = service();
    if (s != null && s.recordedKey() > 0) captured = s.recordedKey();
    ShortcutStore.Mapping m = new ShortcutStore(this).read();
    String value =
        "Saved: "
            + (m.key < 0
                ? "none — native buttons unchanged"
                : m.mode + " " + KeyEvent.keyCodeToString(m.key));
    if (captured > 0) value += "\nRecorded: " + KeyEvent.keyCodeToString(captured);
    if (s != null && s.trialActive())
      value +=
          "\nTemporary test: "
              + s.trialSeconds()
              + " seconds left. "
              + (s.trialWorked()
                  ? "Gesture detected; uncover and confirm."
                  : "Try your chosen gesture.");
    status.setText(value);
    confirm.setEnabled(s != null && s.trialWorked() && !s.isCovered());
  }

  @Override
  protected void onResume() {
    super.onResume();
    visible = true;
    handler.post(refresh);
    confirm.postDelayed(
        () -> {
          if (service() != null && service().trialWorked() && !service().isCovered())
            confirm.requestFocus();
        },
        250);
  }

  @Override
  protected void onPause() {
    visible = false;
    handler.removeCallbacks(refresh);
    super.onPause();
  }
}
