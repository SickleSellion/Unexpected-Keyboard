package juloo.keyboard2;

import android.content.SharedPreferences;
import java.util.Collections;
import juloo.keyboard2.prefs.LayoutsPreference;

/** Preferences seeded on a fresh install of this fork, so that a new install
    starts with the layout, theme and sizes tuned for the Galaxy Z Fold.

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
    e.putString("theme", "gradientpurplepink");
    e.putString("number_row", "symbols");
    e.putString("change_method_key_replacement", "picker");
    e.putString("slider_sensitivity", "15");
    e.putInt("keyboard_height_unfolded", 17);
    e.putInt("margin_bottom_portrait_unfolded", 0);
    e.putInt("margin_bottom_landscape", 9);
    e.putInt("key_activated_opacity", 73);
    e.putInt("longpress_timeout", 597);
    e.putBoolean("vibrate_custom", true);
  }
}
