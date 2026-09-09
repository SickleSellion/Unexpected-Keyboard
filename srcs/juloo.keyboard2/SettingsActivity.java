package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.ListView;
import android.view.ViewGroup;
import android.view.View;
import android.preference.PreferenceScreen;
import android.app.Dialog;
import android.preference.PreferenceGroup;
import android.preference.Preference;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    // Diagnostics: the keyboard service normally enables this, but the
    // activity might run before the service in this process.
    Logs.set_debug_logs(getResources().getBoolean(R.bool.debug_logs));
    // The preferences can't be read when in direct-boot mode. Avoid crashing
    // and don't allow changing the settings.
    // Run the config migration on this prefs as it might be different from the
    // one used by the keyboard, which have been migrated.
    try
    {
      Config.migrate(getPreferenceManager().getSharedPreferences());
    }
    catch (Exception _e) { fallbackEncrypted(); return; }
    addPreferencesFromResource(R.xml.settings);
    align_rows(getPreferenceScreen());
    pad_list(getListView());

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    findPreference("margin_bottom_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("margin_bottom_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_landscape_unfolded").setEnabled(foldableDevice);
  }

  /** Distance between the rows and the edges of the screen. Some devices
      give the rows no padding at all. */
  static final int SCREEN_PADDING_DP = 24;

  /** Some rows reserve room for an icon and some do not, which gives them
      different left edges. None has an icon: reserve the room nowhere. Nested
      screens open in their own dialog, whose list is padded when it opens. */
  void align_rows(Preference p)
  {
    if (Build.VERSION.SDK_INT >= 26)
      p.setIconSpaceReserved(false);
    if (p instanceof PreferenceScreen && p != getPreferenceScreen())
    {
      final PreferenceScreen screen = (PreferenceScreen)p;
      screen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference _p)
        {
          pad_dialog(screen);
          getListView().post(new Runnable() { public void run() { pad_dialog(screen); } });
          return false;
        }
      });
    }
    if (p instanceof PreferenceGroup)
    {
      PreferenceGroup g = (PreferenceGroup)p;
      for (int i = 0; i < g.getPreferenceCount(); i++)
        align_rows(g.getPreference(i));
    }
  }

  void pad_list(ListView lv)
  {
    if (lv == null)
      return;
    int pad = (int)(SCREEN_PADDING_DP * getResources().getDisplayMetrics().density);
    lv.setPadding(pad, lv.getPaddingTop(), pad, lv.getPaddingBottom());
    lv.setClipToPadding(false);
    lv.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
  }

  void pad_dialog(PreferenceScreen screen)
  {
    Dialog d = screen.getDialog();
    if (d == null || d.getWindow() == null)
      return;
    pad_list(find_list_view(d.getWindow().getDecorView()));
  }

  static ListView find_list_view(View v)
  {
    if (v instanceof ListView)
      return (ListView)v;
    if (v instanceof ViewGroup)
    {
      ViewGroup g = (ViewGroup)v;
      for (int i = 0; i < g.getChildCount(); i++)
      {
        ListView r = find_list_view(g.getChildAt(i));
        if (r != null)
          return r;
      }
    }
    return null;
  }

  void fallbackEncrypted()
  {
    // Can't communicate with the user here.
    finish();
  }

  protected void onStop()
  {
    Logs.debug("SettingsActivity.onStop: copying preferences to protected storage");
    DirectBootAwarePreferences
      .copy_preferences_to_protected_storage(this,
          getPreferenceManager().getSharedPreferences());
    super.onStop();
  }
}
