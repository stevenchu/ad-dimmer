package org.stevenchu.screencover;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/**
 * Compact setup; replacement cautions and rollback remain part of the actual test, not fine print.
 */
public class ShortcutActivity extends Activity {
  public static boolean visible;
  private TvUi ui;
  private Spinner keyChoice, modeChoice;
  private EditText gap;
  private TextView status, behavior;
  private CheckBox alternateAccess;
  private Button confirm, cancel, test, record;
  private LinearLayout timing, trialHelp;
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
    ui = new TvUi(this);
    LinearLayout root = ui.page("Shortcut", "Home and Back always remain available.");
    LinearLayout content = ui.column();
    root.addView(ui.scroll(content), new LinearLayout.LayoutParams(-1, 0, 1));
    status = ui.text("", 20, true);
    content.addView(status);
    ui.gap(content, 8);
    LinearLayout selectors = ui.row();
    LinearLayout first = ui.column(), second = ui.column();
    LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1);
    half.rightMargin = ui.dp(16);
    selectors.addView(first, half);
    selectors.addView(second, new LinearLayout.LayoutParams(0, -2, 1));
    content.addView(selectors);
    ui.label(first, "Button");
    keyChoice = ui.spinner("Menu (☰)", "Mute", "Input", "Volume Down", "Another button");
    first.addView(keyChoice);
    ui.label(second, "Press type");
    modeChoice = ui.spinner("Double press", "Hold", "Single press — replaces original");
    second.addView(modeChoice);
    ShortcutStore.Mapping savedMapping = new ShortcutStore(this).read();
    if (savedMapping.key >= 0) {
      int pos = -1;
      int[] keys = {82, 164, 178, 25};
      for (int i = 0; i < keys.length; i++) if (keys[i] == savedMapping.key) pos = i;
      if (pos < 0) {
        captured = savedMapping.key;
        pos = 4;
      }
      keyChoice.setSelection(pos);
      modeChoice.setSelection(
          savedMapping.mode.equals("hold") ? 1 : savedMapping.mode.equals("single") ? 2 : 0);
    }
    record =
        ui.button(
            "Record button",
            false,
            () -> {
              if (service() == null) {
                message("Enable remote controls first");
                return;
              }
              captured = -1;
              keyChoice.setSelection(4);
              service().startRecording();
            });
    ui.addButton(content, record);
    timing = ui.row();
    TextView timingLabel = ui.text("Time between presses", 17, false);
    timingLabel.setPadding(0, 0, ui.dp(20), 0);
    timing.addView(timingLabel, new LinearLayout.LayoutParams(-2, -2));
    gap = ui.input("" + savedMapping.doubleMs, true);
    timing.addView(gap, new LinearLayout.LayoutParams(ui.dp(110), -2));
    TextView ms = ui.text("  ms", 16, false);
    timing.addView(ms);
    content.addView(timing);
    behavior = ui.text("", 16, false);
    content.addView(behavior);
    alternateAccess = ui.check("I can reach the original function another way", false);
    content.addView(alternateAccess);
    android.widget.AdapterView.OnItemSelectedListener changed =
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            updateBehavior();
          }

          public void onNothingSelected(AdapterView<?> p) {}
        };
    keyChoice.setOnItemSelectedListener(changed);
    modeChoice.setOnItemSelectedListener(changed);
    LinearLayout actions = ui.row();
    test = ui.button("Test shortcut", true, this::startTest);
    Button video = ui.button("Open Prime Video", false, this::openPrime);
    LinearLayout.LayoutParams actionHalf = new LinearLayout.LayoutParams(0, ui.dp(52), 1);
    actionHalf.rightMargin = ui.dp(12);
    actions.addView(test, actionHalf);
    actions.addView(video, new LinearLayout.LayoutParams(0, ui.dp(52), 1));
    ui.gap(content, 8);
    content.addView(actions);
    trialHelp = ui.column();
    ui.note(
        trialHelp,
        "Try the shortcut in your video app. Uncover, then return here to save it. Unconfirmed"
            + " tests revert after 90 seconds.");
    content.addView(trialHelp);
    confirm =
        ui.button(
            "Keep this shortcut",
            true,
            () -> {
              if (service() != null && service().confirmTrial()) message("Shortcut saved");
              else message("Try the shortcut, uncover, then confirm before time runs out");
              update();
            });
    ui.addButton(content, confirm);
    cancel =
        ui.button(
            "Cancel test",
            false,
            () -> {
              if (service() != null) service().cancelTrial();
              update();
            });
    ui.addButton(content, cancel);
    ui.addButton(
        content,
        ui.button(
            "How testing works",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("A safe way to try a button")
                    .setMessage(
                        "Start a temporary test, open your video app, and try the shortcut. Back"
                            + " uncovers. Return through Home → Apps → Ad Dimmer → Change shortcut,"
                            + " then keep it before the 90-second timer ends. Otherwise the"
                            + " previous shortcut returns.\n\n"
                            + "Double press and hold also run the button's ordinary actions. A menu"
                            + " may appear or Mute may change sound first. Try another button if"
                            + " that interferes.\n\n"
                            + "Hold is 800 ms. Double-press timing can be 250–1200 ms; 650 ms gives"
                            + " you more time than a rapid double-click.")
                    .setPositiveButton("Got it", null)
                    .show()));
    ui.addButton(
        content,
        ui.button(
            "Disable shortcut",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Disable the dimmer shortcut?")
                    .setNegativeButton("Keep shortcut", null)
                    .setPositiveButton(
                        "Disable shortcut",
                        (d, w) -> {
                          new ShortcutStore(this).disable();
                          if (service() != null) service().restoreNormalButtons();
                          update();
                        })
                    .show()));
    setContentView(root);
    updateBehavior();
    update();
    test.requestFocus();
  }

  private void updateBehavior() {
    if (behavior == null) return;
    boolean single = modeChoice.getSelectedItemPosition() == 2;
    record.setVisibility(keyChoice.getSelectedItemPosition() == 4 ? View.VISIBLE : View.GONE);
    timing.setVisibility(modeChoice.getSelectedItemPosition() == 0 ? View.VISIBLE : View.GONE);
    alternateAccess.setVisibility(single ? View.VISIBLE : View.GONE);
    String button = keyChoice.getSelectedItem().toString();
    behavior.setText(
        single
            ? "Replaces ordinary " + button + ". Keep another way to access it."
            : "The button’s usual action may also run. Test before saving.");
  }

  private void message(String value) {
    Toast.makeText(this, value, Toast.LENGTH_LONG).show();
  }

  private void startTest() {
    if (service() == null) {
      message("Enable remote controls in Android Accessibility first");
      return;
    }
    int key = new int[] {82, 164, 178, 25, captured}[keyChoice.getSelectedItemPosition()];
    String mode = new String[] {"double", "hold", "single"}[modeChoice.getSelectedItemPosition()];
    int window;
    try {
      window = Integer.parseInt(gap.getText().toString());
    } catch (Exception e) {
      message("Use a timing value from 250 to 1200 ms");
      return;
    }
    if (window < 250 || window > 1200) {
      message("Choose 250–1200 ms");
      return;
    }
    if (!ShortcutPolicy.allowed(key, mode)) {
      message("Choose or record a supported button");
      return;
    }
    Runnable apply =
        () -> {
          service().startTrial(new ShortcutStore.Mapping(key, mode, window, 800));
          update();
          message("Test started — 90 seconds to try and keep it");
        };
    if (mode.equals("single")) {
      if (!alternateAccess.isChecked()) {
        message("Confirm another route to the original function first");
        return;
      }
      new AlertDialog.Builder(this)
          .setTitle("Replace ordinary " + RemoteLabels.button(key) + "?")
          .setMessage(
              "This button will dim instead of its normal action. Home and Back remain available."
                  + " The test reverts after 90 seconds unless you keep it.")
          .setNegativeButton("Cancel", null)
          .setPositiveButton("Test temporarily", (d, w) -> apply.run())
          .show();
    } else apply.run();
  }

  private void openPrime() {
    android.content.Intent intent =
        getPackageManager().getLeanbackLaunchIntentForPackage(AppPresetPolicy.PRIME);
    if (intent == null)
      intent = getPackageManager().getLaunchIntentForPackage(AppPresetPolicy.PRIME);
    if (intent != null) startActivity(intent);
    else message("Use Home to open your video app");
  }

  private void update() {
    if (status == null) return;
    CoverService s = service();
    if (s != null && s.recordedKey() > 0) captured = s.recordedKey();
    ShortcutStore.Mapping m = new ShortcutStore(this).read();
    boolean trial = s != null && s.trialActive();
    String label = RemoteLabels.shortcut(m.key, m.mode);
    if (trial)
      label =
          "Test · "
              + s.trialSeconds()
              + "s left · "
              + (s.trialWorked() ? "Shortcut detected" : "Try your gesture");
    else if (captured > 0 && keyChoice.getSelectedItemPosition() == 4)
      label = "Recorded: " + RemoteLabels.button(captured);
    status.setText(label);
    confirm.setVisibility(trial ? View.VISIBLE : View.GONE);
    confirm.setEnabled(trial && s.trialWorked() && !s.isCovered());
    cancel.setVisibility(trial ? View.VISIBLE : View.GONE);
    trialHelp.setVisibility(trial ? View.VISIBLE : View.GONE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    visible = true;
    handler.post(refresh);
    if (confirm != null)
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
