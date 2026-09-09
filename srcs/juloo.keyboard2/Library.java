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

  /** Like [matches] for the entries at most [max_dist] typos away from
      [typed] (a typo is a missing, added, wrong or swapped character),
      ignoring case, the closest first. Entries already in [out] are skipped.
      Returns the new number of entries in [out]. */
  public static int close_matches(String[] entries, String typed, int max_dist,
      String[] out, int from, int max)
  {
    if (typed.length() == 0 || from >= max)
      return from;
    final String t = typed.toLowerCase();
    List<String> found = new ArrayList<String>();
    final List<Integer> dists = new ArrayList<Integer>();
    for (String e : entries)
    {
      int d = distance(e.toLowerCase(), t, max_dist);
      if (d > max_dist)
        continue;
      boolean listed = false;
      for (int i = 0; i < from && !listed; i++)
        listed = out[i].equalsIgnoreCase(e);
      if (!listed)
      {
        found.add(e);
        dists.add(d);
      }
    }
    Integer[] order = new Integer[found.size()];
    for (int i = 0; i < order.length; i++)
      order[i] = i;
    final List<String> found_ = found;
    java.util.Arrays.sort(order, new Comparator<Integer>() {
      public int compare(Integer a, Integer b)
      {
        int c = dists.get(a) - dists.get(b);
        return (c != 0) ? c : found_.get(a).length() - found_.get(b).length();
      }
    });
    int i = from;
    for (Integer o : order)
    {
      if (i >= max)
        break;
      out[i++] = found.get(o);
    }
    return i;
  }

  /** Number of missing, added, wrong or swapped characters between [a] and
      [b] (optimal string alignment distance), or any number above [max] when
      the strings are further apart than that. */
  static int distance(String a, String b, int max)
  {
    int n = a.length(), m = b.length();
    if (Math.abs(n - m) > max)
      return max + 1;
    int[] prev2 = new int[m + 1], prev = new int[m + 1], cur = new int[m + 1];
    for (int j = 0; j <= m; j++)
      prev[j] = j;
    for (int i = 1; i <= n; i++)
    {
      cur[0] = i;
      int row_min = cur[0];
      for (int j = 1; j <= m; j++)
      {
        int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
        int d = Math.min(Math.min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost);
        if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2)
            && a.charAt(i - 2) == b.charAt(j - 1))
          d = Math.min(d, prev2[j - 2] + 1);
        cur[j] = d;
        row_min = Math.min(row_min, d);
      }
      if (row_min > max)
        return max + 1;
      int[] t = prev2; prev2 = prev; prev = cur; cur = t;
    }
    return prev[m];
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
