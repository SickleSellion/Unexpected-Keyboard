package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class LibraryTest
{
  static final String[] ENTRIES = {
    "wats doin n that?", "doin", "Doing it", "james@example.com", "jc_1980"
  };

  @Test
  public void matches_prefix_ignoring_case_shortest_first()
  {
    String[] out = new String[3];
    int n = Library.matches(ENTRIES, "DO", out, 0, 3);
    assertEquals(2, n);
    assertEquals("doin", out[0]);
    assertEquals("Doing it", out[1]);
    n = Library.matches(ENTRIES, "wats", out, 0, 3);
    assertEquals(1, n);
    assertEquals("wats doin n that?", out[0]);
    n = Library.matches(ENTRIES, "j", out, 0, 3);
    assertEquals(2, n);
    assertEquals("jc_1980", out[0]);
    assertEquals("james@example.com", out[1]);
  }

  @Test
  public void matches_respects_start_and_limit()
  {
    String[] out = new String[3];
    out[0] = "kept";
    int n = Library.matches(ENTRIES, "d", out, 1, 2);
    assertEquals(2, n);
    assertEquals("kept", out[0]);
    assertEquals("doin", out[1]);
    assertEquals(0, Library.matches(ENTRIES, "", out, 0, 3));
    assertEquals(0, Library.matches(ENTRIES, "zzz", out, 0, 3));
  }

  @Test
  public void contains_ignores_case()
  {
    assertTrue(Library.contains(ENTRIES, "DOIN"));
    assertFalse(Library.contains(ENTRIES, "doi"));
  }

  @Test
  public void single_words_are_letters_only_lower_case()
  {
    String[] w = Library.single_words(ENTRIES);
    assertEquals(1, w.length);
    assertEquals("doin", w[0]);
  }

  @Test
  public void emails_have_an_at_sign_and_no_space()
  {
    String[] e = Library.emails(ENTRIES);
    assertEquals(1, e.length);
    assertEquals("james@example.com", e[0]);
  }

  @Test
  public void concat_keeps_order()
  {
    String[] r = Library.concat(new String[]{"a"}, new String[]{"b", "c"});
    assertArrayEquals(new String[]{"a", "b", "c"}, r);
  }
}
