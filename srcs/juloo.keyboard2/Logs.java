package juloo.keyboard2;

import android.content.res.Configuration;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import org.json.JSONException;

public final class Logs
{
  static final String TAG = "juloo.keyboard2";

  static LogPrinter _debug_logs = null;

  public static void set_debug_logs(boolean d)
  {
    _debug_logs = d ? new LogPrinter(Log.DEBUG, TAG) : null;
  }

  public static void debug_startup_input_view(EditorInfo info, Config conf)
  {
    if (_debug_logs == null)
      return;
    info.dump(_debug_logs, "");
    if (info.extras != null)
      _debug_logs.println("extras: "+info.extras.toString());
    _debug_logs.println("class: "+(info.inputType & InputType.TYPE_MASK_CLASS));
    _debug_logs.println("flags: "+(info.inputType & InputType.TYPE_MASK_FLAGS));
    _debug_logs.println("variation: "+(info.inputType & InputType.TYPE_MASK_VARIATION));
  }

  public static void debug_config_migration(int from_version, int to_version)
  {
    debug("Migrating config version from " + from_version + " to " + to_version);
  }

  public static void debug(String s)
  {
    if (_debug_logs != null)
      _debug_logs.println(s);
  }

  /** Geometry diagnostics. Everything [Config.refresh] derives from the
      display and the preferences, on a single line. [keyboard_height_percent]
      is the value read from the height preference selected by the current
      orientation and fold state. */
  public static void debug_config_refresh(Config conf, DisplayMetrics dm,
      Configuration c, int keyboard_height_percent)
  {
    if (_debug_logs == null)
      return;
    String height_pref = conf.orientation_landscape
      ? (conf.foldable_unfolded ? "keyboard_height_landscape_unfolded" : "keyboard_height_landscape")
      : (conf.foldable_unfolded ? "keyboard_height_unfolded" : "keyboard_height");
    _debug_logs.println("Config.refresh"
        + " dm=" + dm.widthPixels + "x" + dm.heightPixels + "px"
        + " density=" + dm.density
        + " dp=" + (int)(dm.widthPixels / dm.density) + "x" + (int)(dm.heightPixels / dm.density)
        + " conf.orientation=" + c.orientation
        + " screenWidthDp=" + c.screenWidthDp
        + " screenHeightDp=" + c.screenHeightDp
        + " smallestScreenWidthDp=" + c.smallestScreenWidthDp
        + " densityDpi=" + c.densityDpi
        + " uiMode=0x" + Integer.toHexString(c.uiMode)
        + " landscape=" + conf.orientation_landscape
        + " unfolded=" + conf.foldable_unfolded
        + " wide_screen=" + conf.wide_screen
        + " split_layout=" + conf.split_layout
        + " height_pref=" + height_pref + "=" + keyboard_height_percent
        + " rows_height_px=" + conf.keyboard_rows_height_pixels
        + " screenHeightPixels=" + conf.screenHeightPixels
        + " horizontal_margin_px=" + conf.horizontal_margin
        + " margin_bottom_px=" + conf.margin_bottom
        + " characterSize=" + conf.characterSize
        + " theme=0x" + Integer.toHexString(conf.theme)
        + " layout_index=" + conf.get_current_layout()
        + " thread=" + Thread.currentThread().getName());
  }

  /** Diagnostics for [Keyboard2View.onMeasure]. [config_margin_bottom] is
      the preference value without insets, as used for the gradient height. */
  public static void debug_measure(int wSpec, int hSpec, int width, int height,
      float key_width, Theme.Computed tc, KeyboardData kw,
      int insets_left, int insets_right, int insets_bottom,
      float config_margin_bottom)
  {
    if (_debug_logs == null)
      return;
    String gradient_height = (tc.keyboard_background_paint == null) ? "none"
      : String.valueOf(tc.margin_top + tc.row_height * kw.keysHeight + config_margin_bottom);
    _debug_logs.println("Keyboard2View.onMeasure"
        + " wSpec=" + View.MeasureSpec.toString(wSpec)
        + " hSpec=" + View.MeasureSpec.toString(hSpec)
        + " measured=" + width + "x" + height
        + " keysWidth=" + kw.keysWidth
        + " keysHeight=" + kw.keysHeight
        + " key_width_px=" + key_width
        + " row_height_px=" + tc.row_height
        + " insets_l/r/b=" + insets_left + "/" + insets_right + "/" + insets_bottom
        + " gradient_height=" + gradient_height
        + " thread=" + Thread.currentThread().getName());
  }

  /** Diagnostics for [Keyboard2View.onApplyWindowInsets] (API >= 35). */
  public static void debug_insets(int old_left, int old_right, int old_bottom,
      int left, int right, int bottom)
  {
    if (_debug_logs == null)
      return;
    _debug_logs.println("Keyboard2View.onApplyWindowInsets"
        + " l/r/b " + old_left + "/" + old_right + "/" + old_bottom
        + " -> " + left + "/" + right + "/" + bottom
        + (old_left != left || old_right != right || old_bottom != bottom
          ? " CHANGED" : " unchanged"));
  }

  /** Diagnostics for [Keyboard2.onConfigurationChanged]. */
  public static void debug_configuration_changed(Configuration c)
  {
    if (_debug_logs == null)
      return;
    _debug_logs.println("Keyboard2.onConfigurationChanged"
        + " orientation=" + c.orientation
        + " screenWidthDp=" + c.screenWidthDp
        + " screenHeightDp=" + c.screenHeightDp
        + " smallestScreenWidthDp=" + c.smallestScreenWidthDp
        + " densityDpi=" + c.densityDpi
        + " screenLayout=0x" + Integer.toHexString(c.screenLayout)
        + " uiMode=0x" + Integer.toHexString(c.uiMode)
        + " hardKeyboardHidden=" + c.hardKeyboardHidden
        + " thread=" + Thread.currentThread().getName());
  }

  /** Diagnostics for [Keyboard2.refresh_config]. */
  public static void debug_refresh_config(int prev_theme, int theme,
      boolean candidates_visible, boolean split_layout)
  {
    if (_debug_logs == null)
      return;
    _debug_logs.println("Keyboard2.refresh_config"
        + " theme=0x" + Integer.toHexString(prev_theme)
        + " -> 0x" + Integer.toHexString(theme)
        + (prev_theme != theme ? " VIEWS_RECREATED" : "")
        + " candidates_visible=" + candidates_visible
        + " split_layout=" + split_layout);
  }

  public static void exn(String msg, Exception e)
  {
    Log.e(TAG, msg, e);
  }

  public static void trace()
  {
    if (_debug_logs != null)
      _debug_logs.println(Log.getStackTraceString(new Exception()));
  }
}
