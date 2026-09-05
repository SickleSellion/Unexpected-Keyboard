package juloo.keyboard2;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.Toast;

/** Open another application from a key, see [KeyValue.App].

    On devices that support freeform windows (for example the "pop-up view"
    of Samsung devices), the application is opened in a small window in the
    top-right corner of the screen, so that the current text field and the
    keyboard stay visible. Typical use: a password manager. On other devices,
    the application is opened normally. */
public final class AppLauncher
{
  /** Size and position of the window, in dp. The window is never wider than
      the screen nor higher than half of it. */
  static final int WINDOW_WIDTH_DP = 420;
  static final int WINDOW_HEIGHT_DP = 640;
  static final int WINDOW_MARGIN_DP = 16;
  static final int WINDOW_TOP_DP = 48;

  public static void launch(Context ctx, String package_name)
  {
    PackageManager pm = ctx.getPackageManager();
    Intent intent = pm.getLaunchIntentForPackage(package_name);
    if (intent == null)
    {
      Toast.makeText(ctx, "App not found: " + package_name, Toast.LENGTH_SHORT).show();
      return;
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try
    {
      if (VERSION.SDK_INT >= 24
          && pm.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT))
      {
        ActivityOptions opts = ActivityOptions.makeBasic();
        opts.setLaunchBounds(window_bounds(ctx));
        Logs.debug("AppLauncher: " + package_name + " in window " + opts.getLaunchBounds());
        ctx.startActivity(intent, opts.toBundle());
      }
      else
      {
        Logs.debug("AppLauncher: " + package_name + " (no freeform support)");
        ctx.startActivity(intent);
      }
    }
    catch (Exception e)
    {
      Logs.exn("Failed to launch " + package_name, e);
      Toast.makeText(ctx, "Cannot open " + package_name, Toast.LENGTH_SHORT).show();
    }
  }

  /** Window in the top-right corner of the screen, above the keyboard. */
  static Rect window_bounds(Context ctx)
  {
    DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
    int margin = dp(dm, WINDOW_MARGIN_DP);
    int top = dp(dm, WINDOW_TOP_DP);
    int width = Math.min(dp(dm, WINDOW_WIDTH_DP), dm.widthPixels - 2 * margin);
    int height = Math.min(dp(dm, WINDOW_HEIGHT_DP), dm.heightPixels / 2);
    int right = dm.widthPixels - margin;
    return new Rect(right - width, top, right, top + height);
  }

  static int dp(DisplayMetrics dm, int value)
  {
    return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm);
  }
}
