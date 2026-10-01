package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class TouchCorrectionTest
{
  static final float[] LEFTS = { 0, 100, 200 };
  static final float[] WIDTHS = { 100, 100, 100 };

  @Test
  public void no_shift_keeps_the_borders()
  {
    float[] s = { 0, 0, 0 };
    assertEquals(0, TouchCorrection.pick(LEFTS, WIDTHS, s, 99));
    assertEquals(1, TouchCorrection.pick(LEFTS, WIDTHS, s, 100));
    assertEquals(2, TouchCorrection.pick(LEFTS, WIDTHS, s, 299));
    assertEquals(-1, TouchCorrection.pick(LEFTS, WIDTHS, s, 300));
    assertEquals(-1, TouchCorrection.pick(LEFTS, WIDTHS, s, -1));
  }

  @Test
  public void a_key_pressed_left_of_centre_moves_left()
  {
    // The middle key moves 0.2 key widths to the left: the border with the
    // left key moves 10 px left, the border with the right key too.
    float[] s = { 0, -0.2f, 0 };
    assertEquals(1, TouchCorrection.pick(LEFTS, WIDTHS, s, 92));
    assertEquals(0, TouchCorrection.pick(LEFTS, WIDTHS, s, 89));
    assertEquals(2, TouchCorrection.pick(LEFTS, WIDTHS, s, 191));
    // The last key leaning left does not lose its right edge.
    float[] e = { 0, 0, -0.2f };
    assertEquals(2, TouchCorrection.pick(LEFTS, WIDTHS, e, 299));
    assertEquals(2, TouchCorrection.pick(LEFTS, WIDTHS, e, 191));
  }

  @Test
  public void parse_reads_the_option()
  {
    TouchCorrection tc = TouchCorrection.parse(
        "screen=cover (folded);date=2026-10-01;dy=0.09;o=-0.192;A=-0.267");
    assertEquals("cover (folded)", tc.screen);
    assertEquals(0.09f, tc.dy, 1e-6);
    assertEquals(-0.192f, tc.dx.get('o'), 1e-6);
    assertEquals(-0.267f, tc.dx.get('a'), 1e-6);
    assertEquals(0.09f, tc.top_of(1), 1e-6);
    TouchCorrection t2 = TouchCorrection.parse("dy=0.09;top1=-0.15");
    assertEquals(-0.15f, t2.top_of(1), 1e-6);
    assertEquals(0.09f, t2.top_of(2), 1e-6);
    assertNull(TouchCorrection.parse(""));
    assertNull(TouchCorrection.parse("screen=x"));
  }
}
