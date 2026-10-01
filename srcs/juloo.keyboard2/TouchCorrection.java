package juloo.keyboard2;

import java.util.HashMap;

/** Personal touch correction measured with [TouchLog]: the touch areas of
    the keys move to where the owner actually presses. Stored in the option
    'touch_correction_folded' as "key=value;" pairs: 'screen' and 'date'
    describe the calibration, 'dy' is the vertical shift of every row (in row
    heights, + = down), 'topN' moves only the top edge of row N (counted from
    0 at the top, in heights of that row, - = up) and a single letter is the
    horizontal shift of that key (in key widths, + = right). */
public final class TouchCorrection
{
  public float dy = 0.f;
  public final HashMap<Character, Float> dx = new HashMap<Character, Float>();
  public final HashMap<Integer, Float> top = new HashMap<Integer, Float>();
  public String screen = "";
  public String date = "";

  /** Returns [null] if [s] is empty or has no shift. */
  public static TouchCorrection parse(String s)
  {
    if (s == null || s.isEmpty())
      return null;
    TouchCorrection tc = new TouchCorrection();
    for (String pair : s.split(";"))
    {
      int eq = pair.indexOf('=');
      if (eq <= 0)
        continue;
      String k = pair.substring(0, eq).trim();
      String v = pair.substring(eq + 1).trim();
      try
      {
        if (k.equals("screen")) tc.screen = v;
        else if (k.equals("date")) tc.date = v;
        else if (k.equals("dy")) tc.dy = Float.parseFloat(v);
        else if (k.startsWith("top"))
          tc.top.put(Integer.parseInt(k.substring(3)), Float.parseFloat(v));
        else if (k.length() == 1)
          tc.dx.put(Character.toLowerCase(k.charAt(0)), Float.parseFloat(v));
      }
      catch (NumberFormatException e) {}
    }
    if (tc.dy == 0.f && tc.dx.isEmpty() && tc.top.isEmpty())
      return null;
    return tc;
  }

  /** Shift of the top edge of row [r], in heights of that row. */
  public float top_of(int r)
  {
    Float f = top.get(r);
    return (f == null) ? dy : f;
  }

  /** Horizontal shift for the key typing [c], 0 for other keys. */
  public float dx_of(KeyValue k)
  {
    if (k == null || k.getKind() != KeyValue.Kind.Char)
      return 0.f;
    Float f = dx.get(Character.toLowerCase(k.getChar()));
    return (f == null) ? 0.f : f;
  }

  /** Index of the key under [x] in a row once each key [i], spanning
      [lefts[i], lefts[i] + widths[i]], has moved by [shifts[i]] key widths.
      The border between two keys moves by the average of their shifts.
      Returns -1 outside of the row. */
  public static int pick(float[] lefts, float[] widths, float[] shifts, float x)
  {
    int n = lefts.length;
    if (n == 0)
      return -1;
    float lo = lefts[0] + Math.min(0.f, shifts[0] * widths[0]);
    if (x < lo)
      return -1;
    for (int i = 0; i < n; i++)
    {
      float hi;
      if (i + 1 < n)
        hi = (lefts[i] + widths[i] + lefts[i + 1]) / 2.f
          + (shifts[i] * widths[i] + shifts[i + 1] * widths[i + 1]) / 2.f;
      else
        hi = lefts[i] + widths[i] + Math.max(0.f, shifts[i] * widths[i]);
      if (x < hi)
        return i;
    }
    return -1;
  }
}
