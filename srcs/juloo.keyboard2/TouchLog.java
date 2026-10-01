package juloo.keyboard2;

import android.util.Log;

/** Calibration log: where each touch lands on the keys and what it typed,
    written to logcat with the tag [TAG] while the 'touch_log' option is on,
    never in password fields. Read with `adb logcat -s UKTouch`. Lines:
    G width height unfolded    view size, then one K line per key
    K row col x y w h label    touch area of a key
    D id x y row col time      finger down
    U id x y time              finger up
    V value time               key typed when a finger goes up
    H value time               key repeated by holding */
public final class TouchLog
{
  static final String TAG = "UKTouch";

  public static boolean active()
  {
    Config c = Config.globalConfig();
    return c != null && c.touch_log && !c.editor_config.password_field;
  }

  public static void line(String s)
  {
    Log.i(TAG, s);
  }

  public static String value(KeyValue k)
  {
    if (k == null)
      return "null";
    return k.getKind() + ":" + k.getString().replace(' ', '_');
  }
}
