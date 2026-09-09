package juloo.keyboard2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class GlideDecoderTest
{
  /** A QWERTY layout with 100px keys: rows shifted by half a key and one
      and a half key, like the usual layouts. */
  static GlideDecoder.KeyMap qwerty()
  {
    String[] rows = { "qwertyuiop", "asdfghjkl", "zxcvbnm" };
    float[] shifts = { 0f, 0.5f, 1.5f };
    int n = 26;
    char[] letters = new char[n];
    float[] xs = new float[n];
    float[] ys = new float[n];
    int i = 0;
    for (int r = 0; r < rows.length; r++)
      for (int c = 0; c < rows[r].length(); c++)
      {
        letters[i] = rows[r].charAt(c);
        xs[i] = (shifts[r] + c + 0.5f) * 100f;
        ys[i] = (r + 0.5f) * 100f;
        i++;
      }
    return new GlideDecoder.KeyMap(letters, xs, ys, 100f, 100f);
  }

  static final class Words implements GlideDecoder.Dictionary
  {
    final Map<String, Integer> words = new HashMap<String, Integer>();

    Words(String... entries)
    {
      for (String e : entries)
      {
        String[] p = e.split(":");
        words.put(p[0], Integer.parseInt(p[1]));
      }
    }

    public boolean has_prefix(String prefix)
    {
      for (String w : words.keySet())
        if (w.startsWith(prefix))
          return true;
      return false;
    }

    public int word_freq(String word)
    {
      Integer f = words.get(word);
      return (f == null) ? -1 : f;
    }
  }

  static final Words DICT = new Words(
      "world:12", "word:12", "wed:6", "weld:5", "wild:8", "would:14",
      "hello:12", "hell:9", "help:11", "helo:2",
      "to:15", "too:10", "top:9", "the:15", "this:14", "tip:7",
      "quick:8", "a:15", "i:15");

  /** A stroke going straight from key centre to key centre. */
  static float[] stroke(GlideDecoder.KeyMap keys, String letters)
  {
    float[] path = new float[letters.length() * 2];
    for (int i = 0; i < letters.length(); i++)
    {
      int k = keys.nearest_letter(letters.charAt(i), 0f, 0f);
      path[2 * i] = keys.xs[k];
      path[2 * i + 1] = keys.ys[k];
    }
    return path;
  }

  static List<String> decode(String letters)
  {
    GlideDecoder.KeyMap keys = qwerty();
    float[] path = stroke(keys, letters);
    return GlideDecoder.decode(DICT, keys, path, letters.length());
  }

  @Test
  public void straight_words()
  {
    assertEquals("world", decode("world").get(0));
    assertEquals("quick", decode("quick").get(0));
    assertEquals("this", decode("this").get(0));
  }

  @Test
  public void double_letter()
  {
    // The stroke passes over l once; "hello" needs it twice.
    assertEquals("hello", decode("helo").get(0));
  }

  @Test
  public void frequency_breaks_ties()
  {
    // "to" and "too" draw the same stroke; the frequent word wins.
    List<String> r = decode("to");
    assertEquals("to", r.get(0));
    assertTrue(r.contains("too"));
  }

  @Test
  public void shape_distinguishes_word_from_world()
  {
    // Without the detour through l, the stroke spells "word" but cannot
    // spell "world"; with it, "world" wins over "word".
    List<String> r = decode("word");
    assertEquals("word", r.get(0));
    assertFalse(r.contains("world"));
    r = decode("world");
    assertEquals("world", r.get(0));
    assertTrue(r.contains("word"));
  }

  @Test
  public void nothing_for_short_or_unknown_strokes()
  {
    GlideDecoder.KeyMap keys = qwerty();
    assertTrue(GlideDecoder.decode(DICT, keys, new float[]{50f, 50f}, 1).isEmpty());
    assertTrue(decode("zx").isEmpty());
  }

  @Test
  public void resample_keeps_ends()
  {
    float[] pts = { 0f, 0f, 100f, 0f, 100f, 100f };
    float[] r = GlideDecoder.resample(pts, 3, 5);
    assertEquals(10, r.length);
    assertEquals(0f, r[0], 1e-4);
    assertEquals(0f, r[1], 1e-4);
    assertEquals(100f, r[8], 1e-4);
    assertEquals(100f, r[9], 1e-4);
    // Halfway along a 200px path is the corner.
    assertEquals(100f, r[4], 1e-4);
    assertEquals(0f, r[5], 1e-4);
  }
}
