package juloo.keyboard2;

import juloo.keyboard2.suggestions.Autocorrection;
import org.junit.Test;
import static org.junit.Assert.*;

/** Space bar autocorrection decisions, see [Autocorrection.replacement]. */
public class AutocorrectionTest
{
  static String replacement(String typed, String best, String rejected)
  {
    return Autocorrection.replacement(typed, best, rejected);
  }

  @Test
  public void corrects_unknown_words()
  {
    assertEquals("world", replacement("wrld", "world", null));
    assertEquals("World", replacement("Wrld", "World", null));
    // Completion of a partial word
    assertEquals("hello", replacement("hel", "hello", null));
    // Missing diacritics, found through the dictionary aliases
    assertEquals("café", replacement("cafe", "café", null));
  }

  @Test
  public void nothing_to_suggest()
  {
    assertNull(replacement("wrld", null, null));
    assertNull(replacement(null, "world", null));
  }

  @Test
  public void keeps_known_words_and_their_capitalisation()
  {
    assertNull(replacement("hello", "hello", null));
    assertNull(replacement("HELLO", "Hello", null));
    assertNull(replacement("Us", "us", null));
  }

  @Test
  public void never_corrects_a_word_restored_with_backspace()
  {
    assertNull(replacement("brb", "bra", "brb"));
    // Only the restored word is protected
    assertEquals("bra", replacement("brb", "bra", "lol"));
    assertEquals("world", replacement("wrld", "world", "brb"));
  }

  @Test
  public void leaves_names_acronyms_and_codes_alone()
  {
    assertNull(replacement("iPhone", "phone", null));
    assertNull(replacement("NASA", "nasal", null));
    assertNull(replacement("b2b", "bob", null));
    assertNull(replacement("McDonald", "Donald", null));
  }

  @Test
  public void short_words_are_left_alone()
  {
    assertNull(replacement("a", "at", null));
    assertEquals("at", replacement("ay", "at", null));
  }
}
