package juloo.keyboard2;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

/** Hit testing within a row, see [KeyboardData.Row.get_key_at_x]. Rows are
    built programmatically so that no Android resources are needed. */
public class KeyboardDataTest
{
  static final float TOL = 0.5f;

  /** Three keys of width 1: [a][b] then a gap of 1 unit then [c]. */
  static KeyboardData.Row row()
  {
    return row_of(key(1f, 0f), key(1f, 0f), key(1f, 1f));
  }

  @Test
  public void inside_keys()
  {
    KeyboardData.Row r = row();
    assertSame(r.keys.get(0), r.get_key_at_x(0f, TOL));
    assertSame(r.keys.get(0), r.get_key_at_x(0.99f, TOL));
    assertSame(r.keys.get(1), r.get_key_at_x(1f, TOL));
    assertSame(r.keys.get(1), r.get_key_at_x(1.99f, TOL));
    assertSame(r.keys.get(2), r.get_key_at_x(3f, TOL));
    assertSame(r.keys.get(2), r.get_key_at_x(3.99f, TOL));
  }

  @Test
  public void gap_is_attributed_to_the_nearest_key()
  {
    KeyboardData.Row r = row();
    assertSame(r.keys.get(1), r.get_key_at_x(2.2f, TOL));
    assertSame(r.keys.get(2), r.get_key_at_x(2.8f, TOL));
    // Ties go to the key on the left.
    assertSame(r.keys.get(1), r.get_key_at_x(2.5f, TOL));
  }

  @Test
  public void gap_beyond_tolerance_is_dead()
  {
    KeyboardData.Row r = row();
    assertNull(r.get_key_at_x(2.5f, 0.2f));
    assertSame(r.keys.get(1), r.get_key_at_x(2.15f, 0.2f));
    assertSame(r.keys.get(2), r.get_key_at_x(2.85f, 0.2f));
  }

  @Test
  public void before_the_first_key()
  {
    KeyboardData.Row r = row();
    assertSame(r.keys.get(0), r.get_key_at_x(-0.3f, TOL));
    assertNull(r.get_key_at_x(-0.6f, TOL));
  }

  @Test
  public void after_the_last_key()
  {
    KeyboardData.Row r = row();
    assertSame(r.keys.get(2), r.get_key_at_x(4.3f, TOL));
    assertNull(r.get_key_at_x(4.6f, TOL));
  }

  @Test
  public void leading_shift_like_a_staggered_row()
  {
    // Second row of the QWERTY (US) Fold layout: the first key is shifted by
    // half a key. A tap in that half key hits the first key.
    KeyboardData.Row r = row_of(key(1f, 0.5f), key(1f, 0f));
    assertSame(r.keys.get(0), r.get_key_at_x(0.1f, TOL));
    assertSame(r.keys.get(0), r.get_key_at_x(0.5f, TOL));
    assertSame(r.keys.get(0), r.get_key_at_x(1.4f, TOL));
    assertSame(r.keys.get(1), r.get_key_at_x(1.5f, TOL));
    assertNull(r.get_key_at_x(-0.1f, 0f));
  }

  @Test
  public void empty_row()
  {
    assertNull(row_of().get_key_at_x(0f, TOL));
  }

  static KeyboardData.Key key(float width, float shift)
  {
    return new KeyboardData.Key(new KeyValue[9], null, 0, width, shift, null,
        KeyboardData.Key.Role.Normal);
  }

  static KeyboardData.Row row_of(KeyboardData.Key... keys)
  {
    ArrayList<KeyboardData.Key> l = new ArrayList<KeyboardData.Key>();
    for (KeyboardData.Key k : keys)
      l.add(k);
    return new KeyboardData.Row(l, 1f, 0f);
  }
}
