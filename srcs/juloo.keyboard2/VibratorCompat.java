package juloo.keyboard2;

import android.content.Context;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class VibratorCompat
{
  /** What the vibration is for. Swipes and corrections feel different from
      taps when [Config.vibrate_distinct] is enabled, so that an accidental
      corner symbol or a word changed by autocorrection is noticed without
      looking at the screen. */
  public enum Feedback
  {
    /** A key is pressed. */
    TAP,
    /** The pointer moved onto a corner symbol: two short pulses. */
    SWIPE,
    /** A word was replaced by the space bar: one longer pulse. */
    CORRECTION,
  }

  /** Pause between the two pulses of a swipe, in milliseconds. */
  static final long SWIPE_PAUSE_MS = 70;

  public static void vibrate(View v, Config config)
  {
    vibrate(v, config, Feedback.TAP);
  }

  public static void vibrate(final View v, Config config, Feedback f)
  {
    if (!config.vibrate_distinct)
      f = Feedback.TAP;
    if (config.vibrate_custom)
    {
      long d = config.vibrate_duration;
      if (d <= 0)
        return;
      switch (f)
      {
        case TAP:
          vibrator_vibrate(v, d);
          break;
        case SWIPE:
          vibrator_vibrate(v, new long[]{ 0, d, SWIPE_PAUSE_MS, d });
          break;
        case CORRECTION:
          vibrator_vibrate(v, Math.max(d * 3, 40));
          break;
      }
    }
    else
    {
      switch (f)
      {
        case TAP:
          haptic_feedback(v, HapticFeedbackConstants.KEYBOARD_TAP);
          break;
        case SWIPE:
          haptic_feedback(v, HapticFeedbackConstants.KEYBOARD_TAP);
          v.postDelayed(new Runnable() {
            public void run()
            {
              haptic_feedback(v, HapticFeedbackConstants.KEYBOARD_TAP);
            }
          }, SWIPE_PAUSE_MS);
          break;
        case CORRECTION:
          haptic_feedback(v, HapticFeedbackConstants.LONG_PRESS);
          break;
      }
    }
  }

  static void haptic_feedback(View v, int constant)
  {
    v.performHapticFeedback(constant,
        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
  }

  /** Use the older [Vibrator] when the newer API is not available or the user
      wants more control. */
  static void vibrator_vibrate(View v, long duration)
  {
    try
    {
      get_vibrator(v).vibrate(duration);
    }
    catch (Exception e) {}
  }

  /** [pattern] alternates pauses and pulses, starting with a pause. */
  static void vibrator_vibrate(View v, long[] pattern)
  {
    try
    {
      get_vibrator(v).vibrate(pattern, -1);
    }
    catch (Exception e) {}
  }

  static Vibrator vibrator_service = null;

  static Vibrator get_vibrator(View v)
  {
    if (vibrator_service == null)
    {
      vibrator_service =
        (Vibrator)v.getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }
    return vibrator_service;
  }
}
