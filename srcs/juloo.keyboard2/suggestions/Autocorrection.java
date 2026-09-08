package juloo.keyboard2.suggestions;

/** Decides whether the space bar should replace the word being typed with the
    best suggestion (the "Autocomplete with the space bar" option). Kept free
    of Android dependencies so that it can be unit tested. */
public final class Autocorrection
{
  /** Returns the word that should replace [typed], or [null] to type the word
      as it is. [best] is the best suggestion, or [null] if there is none.
      [rejected] is the word the user restored with backspace after a previous
      correction, or [null]. */
  public static String replacement(String typed, String best, String rejected)
  {
    if (best == null || typed == null || typed.length() < 2)
      return null;
    // Restored once with backspace: the user wants this word.
    if (typed.equals(rejected))
      return null;
    // The word is known. Keep the user's capitalisation ("HELLO", "us").
    if (best.equalsIgnoreCase(typed))
      return null;
    // Names, acronyms and codes are typed on purpose.
    if (looks_deliberate(typed))
      return null;
    return best;
  }

  /** Words containing a digit or a capital after the first letter. */
  static boolean looks_deliberate(String w)
  {
    for (int i = 0; i < w.length(); i++)
    {
      char c = w.charAt(i);
      if (Character.isDigit(c) || (i > 0 && Character.isUpperCase(c)))
        return true;
    }
    return false;
  }
}
