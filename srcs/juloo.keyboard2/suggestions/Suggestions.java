package juloo.keyboard2.suggestions;

import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.Config;
import juloo.keyboard2.Library;
import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;
  boolean _enabled;

  /** Current suggestions. The best suggestion is at index [0]. */
  public String[] suggestions = new String[MAX_COUNT];
  /** Number of suggestions at the beginning of the [suggestions] array that
      are not [null]. */
  public int count = 0;
  public String emoji_suggestion = null;
  /** Number of suggestions in [suggestions]. */
  public static final int MAX_COUNT = 3;

  public Suggestions(Callback c, Config conf)
  {
    _callback = c;
    _config = conf;
  }

  public void started()
  {
    _enabled = _config.editor_config.should_show_candidates_view
      || (_config.editor_config.email_field
          && _config.library_snippets.length > 0);
    clear();
    offer_snippets();
  }

  public void currently_typed_word(String word)
  {
    if (!_enabled)
      return;
    if (word.length() == 0 && offer_snippets())
      return;
    if (word.length() < 2
        || (_config.current_dictionary == null
          && _config.library_entries.length == 0))
      clear();
    else
      query_suggestions(word);
    _callback.set_suggestions(this);
  }

  /** Longest selection to offer replacements for. */
  static final int MAX_SELECTION_LEN = 48;

  /** Text is selected in the editor: offer replacements for it, choosing
      one replaces the whole selection (see
      [KeyEventHandler.replace_selection]). Whitespace around the selection
      is ignored and the selected text itself is not offered. */
  public void selected_text(String text)
  {
    if (!_enabled)
      return;
    text = text.trim();
    if (text.length() < 2 || text.length() > MAX_SELECTION_LEN
        || text.indexOf('\n') >= 0
        || (_config.current_dictionary == null
          && _config.library_entries.length == 0))
      clear();
    else
    {
      query_suggestions(text);
      remove_suggestion(text);
    }
    _callback.set_suggestions(this);
  }

  /** In an editor asking for an email address, offer the library's email
      addresses before anything is typed. Returns whether it did. */
  boolean offer_snippets()
  {
    if (!_config.editor_config.email_field)
      return false;
    String[] emails = Library.emails(_config.library_snippets);
    if (emails.length == 0)
      return false;
    clear();
    for (int i = 0; i < emails.length && i < MAX_COUNT; i++)
      suggestions[count++] = emails[i];
    _callback.set_suggestions(this);
    return true;
  }

  void clear()
  {
    count = 0;
    for (int i = 0; i < MAX_COUNT; i++)
      suggestions[i] = null;
    emoji_suggestion = null;
  }

  int query_suggestions(String word)
  {
    boolean first_char_upper = Character.isUpperCase(word.charAt(0));
    // The user's own entries come first, as they are written: those that
    // start with the word, then those one typo away from it.
    int i = Library.matches(_config.library_entries, word, suggestions, 0,
        MAX_COUNT);
    if (word.length() >= 3)
      i = Library.close_matches(_config.library_entries, word, 1, suggestions,
          i, MAX_COUNT);
    int library_count = i;
    word = apply_substitutions(word);
    Cdict dict = _config.current_dictionary;
    if (dict != null && i < MAX_COUNT)
    {
      Cdict.Result r = dict.find(word);
      if (r.found)
        i = add_suggestion(i, dict.word(r.index));
      int[] suffixes = dict.suffixes(r, MAX_COUNT);
      // Disable distance search for small words
      int[] dist = (word.length() < 3 || i + 1 >= MAX_COUNT) ? NO_RESULTS :
        dict.distance(word, 1, MAX_COUNT);
      for (int j = 0; j < MAX_COUNT && i < MAX_COUNT; j++)
      {
        if (suffixes.length > j)
          i = add_suggestion(i, dict.word(suffixes[j]));
        if (dist.length > j && i < MAX_COUNT)
          i = add_suggestion(i, dict.word(dist[j]));
      }
    }
    count = i;
    if (first_char_upper)
      capitalize_results(library_count);
    emoji_suggestion = query_emoji(word); // word with substitutions applied
    return i;
  }

  /** Add [w] at index [i] unless it is already listed; returns the next
      index. */
  int add_suggestion(int i, String w)
  {
    if (i >= MAX_COUNT)
      return i;
    for (int j = 0; j < i; j++)
      if (suggestions[j].equalsIgnoreCase(w))
        return i;
    suggestions[i] = w;
    return i + 1;
  }

  /** Remove [w] from the suggestions, ignoring case. */
  void remove_suggestion(String w)
  {
    int j = 0;
    for (int i = 0; i < count; i++)
      if (!suggestions[i].equalsIgnoreCase(w))
        suggestions[j++] = suggestions[i];
    for (int i = j; i < count; i++)
      suggestions[i] = null;
    count = j;
  }

  /** Capitalise the dictionary results, from index [from]: the library
      entries keep the case they were written with. */
  void capitalize_results(int from)
  {
    for (int i = from; i < count; i++)
      suggestions[i] = suggestions[i].substring(0, 1).toUpperCase()
        + suggestions[i].substring(1);
  }

  String query_emoji(String word)
  {
    Cdict dict = _config.emoji_dictionary;
    // Disable emoji suggestion for short words
    if (dict == null || word.length() < 3)
      return null;
    Cdict.Result r = dict.find(word);
    if (r.found)
      return dict.word(r.index);
    int[] s = dict.suffixes(r, 1);
    if (s.length > 0)
      return dict.word(s[0]);
    return null;
  }

  /** Apply the same substitutions that were used when building the
      dictionaries to find word aliases. This catches missing diacritics for
      example. */
  String apply_substitutions(String w)
  {
    StringBuilder b = new StringBuilder(w);
    int len = w.length();
    for (int i = 0; i < len; i++)
    {
      char r =
        ComposeKey.transform_char(ComposeKeyData.substitutions, b.charAt(i));
      if (r != 0) b.setCharAt(i, r);
    }
    return b.toString();
  }

  static final int[] NO_RESULTS = new int[0];

  public static interface Callback
  {
    public void set_suggestions(Suggestions suggestions);
  }
}
