package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** The user's own words, phrases and snippets, entered in the settings
    (see [prefs.LibraryPreference]). They are suggested as soon as their
    beginning is typed, they are never autocorrected because they become
    their own best suggestion, and the single words among them are known to
    swipe typing. Pure functions, so they can be tested on the JVM. */
public final class Library
{
  public static final String[] NONE = new String[0];

  /** Write into [out], starting at index [from] and never beyond [max]
      entries, the entries that start with [typed] ignoring case, the shortest
      first. Returns the new number of entries in [out]. */
  public static int matches(String[] entries, String typed, String[] out,
      int from, int max)
  {
    if (typed.length() == 0 || from >= max)
      return from;
    String t = typed.toLowerCase();
    List<String> found = new ArrayList<String>();
    for (String e : entries)
      if (e.toLowerCase().startsWith(t))
        found.add(e);
    Collections.sort(found, new Comparator<String>() {
      public int compare(String a, String b) { return a.length() - b.length(); }
    });
    int i = from;
    for (String e : found)
    {
      if (i >= max)
        break;
      out[i++] = e;
    }
    return i;
  }

  /** Whether [word] is an entry, ignoring case. */
  public static boolean contains(String[] entries, String word)
  {
    for (String e : entries)
      if (e.equalsIgnoreCase(word))
        return true;
    return false;
  }

  /** The entries made of letters only, in lower case: the words swipe
      typing can spell. */
  public static String[] single_words(String[] entries)
  {
    List<String> out = new ArrayList<String>();
    for (String e : entries)
    {
      boolean letters = e.length() > 1;
      for (int i = 0; i < e.length() && letters; i++)
        letters = Character.isLetter(e.charAt(i));
      if (letters)
        out.add(e.toLowerCase());
    }
    return out.toArray(new String[out.size()]);
  }

  /** The entries that look like email addresses. */
  public static String[] emails(String[] entries)
  {
    List<String> out = new ArrayList<String>();
    for (String e : entries)
      if (e.indexOf('@') > 0 && e.indexOf(' ') < 0)
        out.add(e);
    return out.toArray(new String[out.size()]);
  }

  public static String[] concat(String[] a, String[] b)
  {
    String[] r = new String[a.length + b.length];
    System.arraycopy(a, 0, r, 0, a.length);
    System.arraycopy(b, 0, r, a.length, b.length);
    return r;
  }
}
