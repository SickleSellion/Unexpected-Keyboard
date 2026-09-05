package juloo.keyboard2;

import android.content.SharedPreferences;
import java.util.Collections;
import juloo.keyboard2.prefs.LayoutsPreference;

/** Preferences seeded on a fresh install of this fork, so that a new install
    starts with the layout, theme and sizes tuned on the Galaxy Z Fold.

    Only values that differ from upstream's defaults are listed. They are
    written once, from [Config.migrate], when no preference has been saved yet;
    the user can change any of them afterwards in the settings. Upstream's
    defaults in [res/xml/settings.xml] and [Config] are left untouched, which
    keeps the fork easy to merge. */
public final class ForkDefaults
{
  /** Layout from [srcs/layouts/latn_qwerty_us_fold.xml]. */
  static final String DEFAULT_LAYOUT = "latn_qwerty_us_fold";

  /** Whether [prefs] belong to a fresh install that should be seeded. */
  public static boolean should_apply(SharedPreferences prefs)
  {
    return !prefs.contains("theme") && !prefs.contains("layouts");
  }

  /** Does not call [e.apply()]. */
  public static void apply(SharedPreferences.Editor e)
  {
    LayoutsPreference.save_to_preferences(e, Collections.singletonList(
          (LayoutsPreference.Layout)new LayoutsPreference.NamedLayout(DEFAULT_LAYOUT)));
    // Look
    e.putString("theme", "pine");
    e.putBoolean("border_config", true);
    e.putFloat("custom_border_line_width", 2.8f);
    e.putInt("custom_border_radius", 7);
    e.putFloat("character_size", 1.05f);
    e.putInt("key_activated_opacity", 73);
    // Corner symbols
    e.putFloat("corner_label_size", 0.24f);
    e.putString("corner_label_color", "#F7FF9B");
    e.putInt("corner_label_inset", 12);
    // Layout and sizes
    e.putString("number_row", "symbols");
    e.putInt("keyboard_height_unfolded", 17);
    e.putInt("margin_bottom_portrait_unfolded", 0);
    e.putInt("margin_bottom_landscape", 9);
    e.putInt("horizontal_margin_portrait_unfolded", 0);
    e.putInt("horizontal_margin_landscape_unfolded", 0);
    e.putFloat("key_horizontal_margin", 0f);
    e.putFloat("key_vertical_margin", 1.25f);
    // Typing
    e.putString("swipe_dist", "7.5");
    e.putString("slider_sensitivity", "15");
    e.putInt("longpress_timeout", 597);
    e.putBoolean("lock_double_tap", true);
    e.putBoolean("vibrate_custom", true);
    e.putString("change_method_key_replacement", "picker");
  }
}
