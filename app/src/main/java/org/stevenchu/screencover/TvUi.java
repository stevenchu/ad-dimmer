package org.stevenchu.screencover;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Small native-TV style layer. Focus remains visible; no custom gesture navigation or web view. */
public final class TvUi {
  public static final int BG = Color.rgb(15, 22, 26),
      PANEL = Color.rgb(24, 34, 39),
      TEXT = Color.rgb(239, 245, 243),
      MUTED = Color.rgb(155, 174, 178),
      ACCENT = Color.rgb(121, 220, 197);
  final Activity a;

  public TvUi(Activity a) {
    this.a = a;
  }

  public int dp(float n) {
    return Math.round(n * a.getResources().getDisplayMetrics().density);
  }

  public LinearLayout column() {
    LinearLayout l = new LinearLayout(a);
    l.setOrientation(1);
    return l;
  }

  public LinearLayout row() {
    LinearLayout l = new LinearLayout(a);
    l.setOrientation(0);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  public TextView text(String value, int size, boolean strong) {
    TextView t = new TextView(a);
    t.setText(value);
    t.setTextColor(strong ? TEXT : MUTED);
    t.setTextSize(size);
    t.setFontFeatureSettings("kern");
    if (strong) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    t.setPadding(0, dp(4), 0, dp(4));
    return t;
  }

  public void label(LinearLayout parent, String value) {
    TextView t = text(value, 15, false);
    t.setPadding(0, dp(14), 0, dp(5));
    parent.addView(t);
  }

  public void note(LinearLayout parent, String value) {
    parent.addView(text(value, 15, false));
  }

  public GradientDrawable box(int fill, int stroke) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(fill);
    d.setCornerRadius(dp(8));
    if (stroke != 0) d.setStroke(dp(2), stroke);
    return d;
  }

  private StateListDrawable states(boolean primary) {
    StateListDrawable s = new StateListDrawable();
    s.addState(new int[] {-android.R.attr.state_enabled}, box(PANEL, 0));
    s.addState(
        new int[] {android.R.attr.state_focused},
        box(primary ? ACCENT : Color.rgb(31, 58, 57), ACCENT));
    s.addState(new int[] {android.R.attr.state_pressed}, box(Color.rgb(42, 78, 73), ACCENT));
    s.addState(new int[] {}, box(primary ? ACCENT : PANEL, 0));
    return s;
  }

  public Button button(String title, boolean primary, Runnable action) {
    Button b = new Button(a);
    b.setAllCaps(false);
    b.setText(title);
    b.setTextSize(17);
    b.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
    b.setPadding(dp(16), 0, dp(16), 0);
    b.setTextColor(
        new ColorStateList(
            new int[][] {
              new int[] {-android.R.attr.state_enabled},
              new int[] {android.R.attr.state_focused},
              new int[] {android.R.attr.state_pressed},
              new int[] {}
            },
            new int[] {MUTED, BG, BG, TEXT}));
    StateListDrawable background = new StateListDrawable();
    background.addState(new int[] {-android.R.attr.state_enabled}, box(PANEL, 0));
    background.addState(new int[] {android.R.attr.state_focused}, box(ACCENT, ACCENT));
    background.addState(new int[] {android.R.attr.state_pressed}, box(ACCENT, ACCENT));
    background.addState(new int[] {}, box(primary ? Color.rgb(29, 53, 49) : PANEL, 0));
    b.setBackground(background);
    b.setMinHeight(dp(50));
    b.setMinimumHeight(dp(50));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  public void addButton(LinearLayout parent, Button b) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
    p.topMargin = dp(8);
    parent.addView(b, p);
  }

  public EditText input(String value, boolean numeric) {
    EditText e = new EditText(a);
    e.setSingleLine(true);
    e.setText(value);
    e.setTextSize(18);
    e.setTextColor(TEXT);
    e.setHintTextColor(MUTED);
    e.setInputType(
        numeric
            ? android.text.InputType.TYPE_CLASS_NUMBER
            : android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    e.setPadding(dp(12), dp(8), dp(12), dp(8));
    e.setBackground(states(false));
    e.setSelectAllOnFocus(true);
    return e;
  }

  public void focusOutline(View v) {
    StateListDrawable bg = new StateListDrawable();
    bg.addState(new int[] {android.R.attr.state_focused}, box(Color.rgb(31, 58, 57), ACCENT));
    bg.addState(new int[] {}, box(Color.TRANSPARENT, 0));
    v.setBackground(bg);
  }

  public CheckBox check(String title, boolean checked) {
    CheckBox c = new CheckBox(a);
    c.setText(title);
    c.setTextSize(18);
    c.setTextColor(TEXT);
    c.setChecked(checked);
    c.setMinHeight(dp(48));
    c.setButtonTintList(ColorStateList.valueOf(ACCENT));
    c.setPadding(dp(6), dp(4), dp(6), dp(4));
    StateListDrawable bg = new StateListDrawable();
    bg.addState(new int[] {android.R.attr.state_focused}, box(Color.rgb(31, 58, 57), ACCENT));
    bg.addState(new int[] {}, box(Color.TRANSPARENT, 0));
    c.setBackground(bg);
    return c;
  }

  public Spinner spinner(java.util.List<String> labels) {
    Spinner s = new Spinner(a, Spinner.MODE_DROPDOWN);
    s.setPopupBackgroundDrawable(box(PANEL, 0));
    s.setBackground(states(false));
    s.setPadding(dp(8), 0, dp(8), 0);
    s.setAdapter(
        new ArrayAdapter<String>(a, android.R.layout.simple_spinner_item, labels) {
          public View getView(int pos, View old, ViewGroup parent) {
            TextView t = text(getItem(pos), 18, true);
            t.setPadding(dp(8), dp(12), dp(8), dp(12));
            return t;
          }

          public View getDropDownView(int pos, View old, ViewGroup parent) {
            TextView t = text(getItem(pos), 18, true);
            t.setPadding(dp(16), dp(12), dp(16), dp(12));
            t.setBackground(states(false));
            return t;
          }
        });
    return s;
  }

  public Spinner spinner(String... labels) {
    return spinner(java.util.Arrays.asList(labels));
  }

  public void gap(LinearLayout parent, int height) {
    Space s = new Space(a);
    parent.addView(s, new LinearLayout.LayoutParams(1, dp(height)));
  }

  public LinearLayout page(String title, String subtitle) {
    LinearLayout root = column();
    root.setBackgroundColor(BG);
    root.setPadding(dp(28), dp(18), dp(28), dp(18));
    root.addView(text(title, 28, true));
    if (subtitle != null) root.addView(text(subtitle, 16, false));
    return root;
  }

  public ScrollView scroll(LinearLayout content) {
    ScrollView s = new ScrollView(a);
    s.setFillViewport(false);
    s.setClipToPadding(false);
    s.addView(content);
    return s;
  }
}
