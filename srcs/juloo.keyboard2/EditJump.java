package juloo.keyboard2;

/** Distances for the « and » keys of the edit panel: each jump crosses one
    run of characters of the same class, so the cursor stops at the end of a
    word, then at the start of the next one, then at its end, and so on. */
public final class EditJump
{
  static final int SPACE = 0;
  static final int WORD = 1;
  static final int OTHER = 2;

  static int char_class(char c)
  {
    if (Character.isWhitespace(c))
      return SPACE;
    if (Character.isLetterOrDigit(c) || c == '_' || c == '\''
        || Character.isSurrogate(c) || Character.getType(c) == Character.NON_SPACING_MARK)
      return WORD;
    return OTHER;
  }

  /** Number of characters to move forward, [after] being the text after the
      cursor. 0 at the end of the text. */
  public static int forward(CharSequence after)
  {
    int n = after.length();
    if (n == 0)
      return 0;
    int cls = char_class(after.charAt(0));
    int i = 1;
    while (i < n && char_class(after.charAt(i)) == cls)
      i++;
    return i;
  }

  /** Number of characters to move backward, [before] being the text before
      the cursor. 0 at the start of the text. */
  public static int backward(CharSequence before)
  {
    int n = before.length();
    if (n == 0)
      return 0;
    int cls = char_class(before.charAt(n - 1));
    int i = n - 1;
    while (i > 0 && char_class(before.charAt(i - 1)) == cls)
      i--;
    return n - i;
  }
}
