package juloo.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.Library;
import juloo.keyboard2.R;

/** A list of the user's own words, phrases or snippets, one per item, edited
    like the custom extra keys. The preference key comes from the settings
    layout so that the same class serves the lists of the "Library" screen.
    See [juloo.keyboard2.Library] for what the entries do. */
public class LibraryPreference extends ListGroupPreference<String>
{
  static final ListGroupPreference.Serializer<String> SERIALIZER =
    new ListGroupPreference.StringSerializer();

  public LibraryPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  /** The entries saved under [key], trimmed, without the empty ones. */
  public static String[] get(SharedPreferences prefs, String key)
  {
    List<String> saved = load_from_preferences(key, prefs, null, SERIALIZER);
    if (saved == null)
      return Library.NONE;
    ArrayList<String> out = new ArrayList<String>();
    for (String s : saved)
    {
      String t = s.trim();
      if (t.length() > 0)
        out.add(t);
    }
    return out.toArray(new String[out.size()]);
  }

  String label_of_value(String value, int i) { return value; }

  @Override
  void select(final SelectionCallback<String> callback, String old_value)
  {
    View content = View.inflate(getContext(), R.layout.dialog_edit_text, null);
    ((TextView)content.findViewById(R.id.text)).setText(old_value);
    new AlertDialog.Builder(getContext())
      .setView(content)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int which)
        {
          EditText input = (EditText)((AlertDialog)dialog).findViewById(R.id.text);
          final String k = input.getText().toString().trim();
          if (!k.equals(""))
            callback.select(k);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }

  @Override
  Serializer<String> get_serializer() { return SERIALIZER; }
}
