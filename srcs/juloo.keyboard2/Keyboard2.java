package juloo.keyboard2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.util.LogPrinter;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.provider.Settings;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.dict.DictionariesActivity;
import juloo.keyboard2.dict.DictionarySwitcher;
import juloo.keyboard2.dict.SupportedDictionaries;
import juloo.keyboard2.prefs.LayoutsPreference;
import juloo.keyboard2.suggestions.CandidatesView;
import juloo.keyboard2.suggestions.Suggestions;

public class Keyboard2 extends InputMethodService
  implements SharedPreferences.OnSharedPreferenceChangeListener
{
  /** The view containing the keyboard and candidates view. */
  private ViewGroup _keyboard_container_view;
  private Keyboard2View _keyboard_layout_view;
  private CandidatesView _candidates_view;
  private Suggestions _suggestions;
  private KeyEventHandler _keyeventhandler;
  /** If not 'null', the layout to use instead of [_config.current_layout]. */
  private KeyboardData _currentSpecialLayout;
  /** Layout associated with the currently selected locale. Not 'null'. */
  private KeyboardData _localeTextLayout;
  /** Installed and current locales. */
  private Dictionaries _dictionaries;
  private ViewGroup _emojiPane = null;
  private ViewGroup _clipboard_pane = null;
  private Handler _handler;

  private Config _config;

  private FoldStateTracker _foldStateTracker;

  /** Layout currently visible before it has been modified. */
  KeyboardData current_layout_unmodified()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    KeyboardData layout = null;
    int layout_i = _config.get_current_layout();
    if (layout_i >= _config.layouts.size())
      layout_i = 0;
    if (layout_i < _config.layouts.size())
      layout = _config.layouts.get(layout_i);
    if (layout == null)
      layout = _localeTextLayout;
    return layout;
  }

  /** Layout currently visible. */
  KeyboardData current_layout()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    return LayoutModifier.modify_layout(current_layout_unmodified());
  }

  void setTextLayout(int l)
  {
    _config.set_current_layout(l);
    _currentSpecialLayout = null;
    // The active dictionary depends on the current layout.
    refresh_current_dictionary();
    refresh_candidates_view();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  void incrTextLayout(int delta)
  {
    int s = _config.layouts.size();
    setTextLayout((_config.get_current_layout() + delta + s) % s);
  }

  void setSpecialLayout(KeyboardData l)
  {
    _currentSpecialLayout = l;
    _keyboard_layout_view.setKeyboard(l);
  }

  KeyboardData loadLayout(int layout_id)
  {
    return KeyboardData.load(getResources(), layout_id);
  }

  /** Load a layout that contains a numpad. */
  KeyboardData loadNumpad(int layout_id)
  {
    return LayoutModifier.modify_numpad(KeyboardData.load(getResources(), layout_id),
        current_layout_unmodified());
  }

  KeyboardData loadNumericLayout()
  {
    return loadNumpad(_config.orientation_landscape ?
        R.xml.numeric_landscape : R.xml.numeric);
  }

  KeyboardData loadPinentry(int layout_id)
  {
    return LayoutModifier.modify_pinentry(KeyboardData.load(getResources(), layout_id),
        current_layout_unmodified());
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    // Enabled early so that the first [Config.refresh] is logged.
    Logs.set_debug_logs(getResources().getBoolean(R.bool.debug_logs));
    SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(this);
    _handler = new Handler(getMainLooper());
    Logs.debug("Keyboard2.onCreate isFoldableDevice=" + FoldStateTracker.isFoldableDevice(this));
    _foldStateTracker = new FoldStateTracker(this);
    _dictionaries = Dictionaries.instance(this);
    Config.initGlobalConfig(prefs, getResources(),
        _foldStateTracker.isUnfolded(), _dictionaries);
    _config = Config.globalConfig();
    Receiver recvr = this.new Receiver();
    _suggestions = new Suggestions(recvr, _config);
    _keyeventhandler = new KeyEventHandler(recvr, _suggestions);
    KeyValue.Stateful._handler = recvr;
    _config.handler = _keyeventhandler;
    prefs.registerOnSharedPreferenceChangeListener(this);
    refreshSubtypeImm();
    create_keyboard_view();
    ClipboardHistoryService.on_startup(this, _keyeventhandler);
    _foldStateTracker.setChangedCallback(() -> { refresh_config(); });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    _foldStateTracker.close();
  }

  private void create_keyboard_view()
  {
    _keyboard_container_view = (ViewGroup)inflate_view(R.layout.keyboard);
    _keyboard_layout_view = (Keyboard2View)_keyboard_container_view.findViewById(R.id.keyboard_view);
    _candidates_view = (CandidatesView)_keyboard_container_view.findViewById(R.id.candidates_view);
    _keyboard_container_view.findViewById(R.id.bar_hide_keyboard)
      .setOnClickListener((v) -> requestHideSelf(0));
  }

  InputMethodManager get_imm()
  {
    return (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
  }

  private void refreshSubtypeImm()
  {
    _config.shouldOfferVoiceTyping = true;
    KeyboardData default_layout = null;
    _config.device_locales = DeviceLocales.load(this);
    if (_config.device_locales.default_ != null)
    {
      String layout_name = _config.device_locales.default_.default_layout;
      if (layout_name != null)
        default_layout = LayoutsPreference.layout_of_string(getResources(), layout_name);
    }
    _config.extra_keys_subtype = _config.device_locales.extra_keys();
    if (default_layout == null)
      default_layout = loadLayout(R.xml.latn_qwerty_us);
    _localeTextLayout = default_layout;
  }

  private void refresh_current_dictionary()
  {
    // The language name and the dictionary button are only useful when there
    // is something to switch to; with a single dictionary they would just
    // occupy the idle bar.
    _config.should_show_dictionary_switch =
      (_config.device_locales.installed.size() > 1);
    String dict_name = _dictionaries.get_selected(_config);
    if (dict_name == null)
      dict_name = (_config.device_locales.default_ != null) ?
        _config.device_locales.default_.dictionary : null;
    _dictionaries.set_current_dictionary(_config, dict_name);
    _config.current_dictionary_name =
      SupportedDictionaries.get(getResources()).get_display_name(dict_name);
  }

  /** Remember and apply the dictionary chosen by the user for the current
      context. */
  private void select_dictionary(String dict_name)
  {
    _dictionaries.set_selected(_config, dict_name);
    refresh_current_dictionary();
    refresh_candidates_view();
  }

  /** Whether the candidates view may be shown for the current editor and
      layout. Its actual visibility also depends on
      [Config.hide_empty_suggestion_bar], see
      [update_candidates_view_visibility()]. */
  private boolean _candidates_view_allowed = false;

  private void refresh_candidates_view()
  {
    _candidates_view_allowed =
      _config.suggestions_enabled
      && _config.editor_config.should_show_candidates_view
      && !_config.split_layout;
    if (_candidates_view_allowed)
    {
      _candidates_view.refresh_config(_config);
      _keyeventhandler.dictionary_changed();
    }
    update_candidates_view_visibility(_suggestions);
  }

  /** Show or hide the candidates bar. When [hide_empty_suggestion_bar] is
      set, the bar (which otherwise shows the language name or the hint to
      install a dictionary) takes space only while there is something to
      suggest. */
  private void update_candidates_view_visibility(Suggestions s)
  {
    boolean visible = _candidates_view_allowed
      && (!_config.hide_empty_suggestion_bar
          || s.count > 0 || s.emoji_suggestion != null);
    _candidates_view.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  /** Might re-create the keyboard view. [_keyboard_layout_view.setKeyboard()] and
      [setInputView()] must be called soon after. */
  private void refresh_config()
  {
    int prev_theme = _config.theme;
    _config.refresh(getResources(), _foldStateTracker.isUnfolded(), _dictionaries);
    refresh_current_dictionary();
    // Refreshing the theme config requires re-creating the views
    if (prev_theme != _config.theme)
    {
      create_keyboard_view();
      _emojiPane = null;
      _clipboard_pane = null;
      setInputView(_keyboard_container_view);
    }
    // Set keyboard background opacity
    Drawable bg = _keyboard_container_view.getBackground().mutate();
    bg.setAlpha(_config.keyboardOpacity);
    _keyboard_container_view.setBackground(bg);
    _keyboard_layout_view.reset();
    refresh_candidates_view();
    Logs.debug_refresh_config(prev_theme, _config.theme,
        _candidates_view.getVisibility() == View.VISIBLE, _config.split_layout);
  }

  private KeyboardData refresh_special_layout()
  {
    if (_config.editor_config.numeric_layout)
    {
      switch (_config.selected_number_layout)
      {
        case PIN:
          return loadPinentry(_config.orientation_landscape ?
              R.xml.pin_landscape : R.xml.pin);
        case NUMBER:
          return loadNumericLayout();
      }
    }
    return null;
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting)
  {
    _config.editor_config.refresh(info, getResources());
    refresh_config();
    _currentSpecialLayout = refresh_special_layout();
    _keyboard_layout_view.setKeyboard(current_layout());
    _keyeventhandler.started(_config);
    setInputView(_keyboard_container_view);
    Logs.debug_startup_input_view(info, _config);
  }

  @Override
  public void setInputView(View v)
  {
    apply_navigation_bar_visibility();
    ViewParent parent = v.getParent();
    if (parent != null && parent instanceof ViewGroup)
      ((ViewGroup)parent).removeView(v);
    super.setInputView(v);
    updateSoftInputWindowLayoutParams();
    v.requestApplyInsets();
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    updateSoftInputWindowLayoutParams();
  }

  private void updateSoftInputWindowLayoutParams() {
    final Window window = getWindow().getWindow();
    // On API >= 35, Keyboard2View behaves as edge-to-edge
    // APIs 30 to 34 have visual artifact when edge-to-edge is enabled
    if (VERSION.SDK_INT >= 35)
    {
      WindowManager.LayoutParams wattrs = window.getAttributes();
      wattrs.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
      // Allow to draw behind system bars
      wattrs.setFitInsetsTypes(0);
      window.setDecorFitsSystemWindows(false);
    }
    updateLayoutHeightOf(window, ViewGroup.LayoutParams.MATCH_PARENT);
    final View inputArea = window.findViewById(android.R.id.inputArea);

    updateLayoutHeightOf(
            (View) inputArea.getParent(),
            isFullscreenMode()
                    ? ViewGroup.LayoutParams.MATCH_PARENT
                    : ViewGroup.LayoutParams.WRAP_CONTENT);
    updateLayoutGravityOf((View) inputArea.getParent(), Gravity.BOTTOM);

  }

  private static void updateLayoutHeightOf(final Window window, final int layoutHeight) {
    final WindowManager.LayoutParams params = window.getAttributes();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      window.setAttributes(params);
    }
  }

  private static void updateLayoutHeightOf(final View view, final int layoutHeight) {
    final ViewGroup.LayoutParams params = view.getLayoutParams();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      view.setLayoutParams(params);
    }
  }

  private static void updateLayoutGravityOf(final View view, final int layoutGravity) {
    final ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof LinearLayout.LayoutParams) {
      final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    } else if (lp instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    }
  }

  @Override
  public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype)
  {
    refreshSubtypeImm();
    refresh_current_dictionary();
    refresh_candidates_view();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
  {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    _keyeventhandler.selection_updated(oldSelStart, newSelStart, newSelEnd);
    if ((oldSelStart == oldSelEnd) != (newSelStart == newSelEnd))
      _keyboard_layout_view.set_selection_state(newSelStart != newSelEnd);
  }

  @Override
  public void onFinishInputView(boolean finishingInput)
  {
    super.onFinishInputView(finishingInput);
    _keyboard_layout_view.reset();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences _prefs, String _key)
  {
    refresh_config();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  /** Name of the view the framework adds at the bottom of the IME window on
      devices where the IME draws the navigation bar (gesture navigation,
      Android 12 and later). It hosts the system's keyboard switcher button
      and, on some devices, a button that hides the keyboard. */
  static final String NAVIGATION_BAR_FRAME_CLASS =
    "android.inputmethodservice.navigationbar.NavigationBarFrame";

  /** Hide or restore the navigation bar frame according to
      [Config.hide_navigation_bar]. Only done with gesture navigation, where
      the frame holds no navigation buttons; with three-button navigation the
      frame is the user's back, home and recents buttons and is left alone.
      [Keyboard2View] reserves only the gesture area when the frame is
      hidden. */
  private View _navigation_bar_frame = null;
  private View _navigation_bar_decor = null;
  private boolean _navigation_bar_hidden = false;

  /** The framework shows the frame again during its own layout passes. Hide
      it before anything is drawn and cancel that draw, so the buttons never
      flash. */
  private final ViewTreeObserver.OnPreDrawListener _navigation_bar_pre_draw =
    () -> {
      View frame = _navigation_bar_frame;
      if (_navigation_bar_hidden && frame != null
          && frame.getVisibility() != View.GONE)
      {
        frame.setVisibility(View.GONE);
        return false;
      }
      return true;
    };

  void apply_navigation_bar_visibility()
  {
    Window w = getWindow().getWindow();
    if (w == null)
      return;
    View decor = w.getDecorView();
    boolean hide = _config.hide_navigation_bar && is_gesture_navigation(this);
    View frame = find_navigation_bar_frame(decor);
    _navigation_bar_frame = frame;
    _navigation_bar_hidden = hide;
    if (frame == null)
    {
      Logs.debug("NavigationBar: frame not found, hide=" + hide
          + " decor=" + describe_view_tree(decor, 0));
      return;
    }
    if (_navigation_bar_decor != decor)
    {
      decor.getViewTreeObserver().addOnPreDrawListener(_navigation_bar_pre_draw);
      _navigation_bar_decor = decor;
    }
    int vis = hide ? View.GONE : View.VISIBLE;
    if (frame.getVisibility() != vis)
    {
      Logs.debug("NavigationBar: frame " + frame.getVisibility() + " -> " + vis);
      frame.setVisibility(vis);
    }
  }

  static View find_navigation_bar_frame(View v)
  {
    if (NAVIGATION_BAR_FRAME_CLASS.equals(v.getClass().getName()))
      return v;
    if (v instanceof ViewGroup)
    {
      ViewGroup g = (ViewGroup)v;
      for (int i = 0; i < g.getChildCount(); i++)
      {
        View r = find_navigation_bar_frame(g.getChildAt(i));
        if (r != null)
          return r;
      }
    }
    return null;
  }

  /** Class names and visibility of the first levels of a view tree, for the
      debug log. */
  static String describe_view_tree(View v, int depth)
  {
    StringBuilder b = new StringBuilder();
    b.append(v.getClass().getSimpleName()).append('/').append(v.getVisibility());
    if (v instanceof ViewGroup && depth < 3)
    {
      ViewGroup g = (ViewGroup)v;
      b.append('[');
      for (int i = 0; i < g.getChildCount(); i++)
      {
        if (i > 0)
          b.append(' ');
        b.append(describe_view_tree(g.getChildAt(i), depth + 1));
      }
      b.append(']');
    }
    return b.toString();
  }

  /** Whether the system navigation uses gestures (NAV_BAR_MODE_GESTURAL); the
      constant is not part of the public API. */
  static boolean is_gesture_navigation(Context ctx)
  {
    return Settings.Secure.getInt(ctx.getContentResolver(), "navigation_mode", 0) == 2;
  }

  @Override
  public void onWindowShown()
  {
    super.onWindowShown();
    apply_navigation_bar_visibility();
  }

  @Override
  public boolean onEvaluateFullscreenMode()
  {
    /* Entirely disable fullscreen mode. */
    return false;
  }

  /** Diagnostics only. The platform re-creates its views and calls
      [onStartInputView] again after this returns. */
  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    Logs.debug_configuration_changed(newConfig);
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onEvaluateInputViewShown()
  {
    // Since Android 16, this method returns [false] for unknown reasons.
    if (super.onEvaluateInputViewShown())
      return true;
    if (getResources().getConfiguration().hardKeyboardHidden
        == Configuration.HARDKEYBOARDHIDDEN_NO
        && _config.physical_keyboard_hide)
    {
      Logs.debug("Physical keyboard is present");
      return false;
    }
    return true;
  }

  public void launch_dictionaries_activity()
  {
    start_activity(DictionariesActivity.class);
  }

  /** Called from [onClick] attributes. */
  public void launch_dictionaries_activity(View v)
  {
    launch_dictionaries_activity();
  }

  void start_activity(Class cls)
  {
    Intent intent = new Intent(this, cls);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  /** Not static */
  public class Receiver implements KeyEventHandler.IReceiver,
         KeyValue.Stateful.Symbol_provider, DictionarySwitcher.Callback
  {
    public void handle_event_key(KeyValue.Event ev)
    {
      switch (ev)
      {
        case CONFIG:
          start_activity(SettingsActivity.class);
          break;

        case SWITCH_TEXT:
          _currentSpecialLayout = null;
          _keyboard_layout_view.setKeyboard(current_layout());
          break;

        case SWITCH_NUMERIC:
          setSpecialLayout(loadNumericLayout());
          break;

        case SWITCH_EMOJI:
          if (_emojiPane == null)
            _emojiPane = (ViewGroup)inflate_view(R.layout.emoji_pane);
          setInputView(_emojiPane);
          break;

        case SWITCH_CLIPBOARD:
          if (_clipboard_pane == null)
            _clipboard_pane = (ViewGroup)inflate_view(R.layout.clipboard_pane);
          setInputView(_clipboard_pane);
          break;

        case SWITCH_BACK_EMOJI:
        case SWITCH_BACK_CLIPBOARD:
          setInputView(_keyboard_container_view);
          break;

        case CHANGE_METHOD_PICKER:
          get_imm().showInputMethodPicker();
          break;

        case CHANGE_METHOD_PREV:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToLastInputMethod(getConnectionToken());
          else
            switchToPreviousInputMethod();
          break;

        case CHANGE_METHOD_NEXT:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToNextInputMethod(getConnectionToken(), false);
          else
            switchToNextInputMethod(false);
          break;

        case ACTION:
          InputConnection conn = getCurrentInputConnection();
          if (conn != null)
            conn.performEditorAction(_config.editor_config.actionId);
          break;

        case SWITCH_FORWARD:
          incrTextLayout(1);
          break;

        case SWITCH_BACKWARD:
          incrTextLayout(-1);
          break;

        case SWITCH_GREEKMATH:
          setSpecialLayout(loadNumpad(R.xml.greekmath));
          break;

        case CAPS_LOCK:
          set_shift_state(true, true);
          break;

        case SWITCH_VOICE_TYPING:
          if (!VoiceImeSwitcher.switch_to_voice_ime(Keyboard2.this, get_imm(),
                Config.globalPrefs()))
            _config.shouldOfferVoiceTyping = false;
          break;

        case SWITCH_VOICE_TYPING_CHOOSER:
          VoiceImeSwitcher.choose_voice_ime(Keyboard2.this, get_imm(),
              Config.globalPrefs());
          break;

        case HIDE_SELF:
          Keyboard2.this.requestHideSelf(0);
          break;

        case CHANGE_DICTIONARY:
          new DictionarySwitcher(Keyboard2.this, _dictionaries, this).choose();
          break;
      }
    }

    public void launch_app(KeyValue.App app)
    {
      AppLauncher.launch(Keyboard2.this, app.package_name);
    }

    public void set_shift_state(boolean state, boolean lock)
    {
      _keyboard_layout_view.set_shift_state(state, lock);
    }

    public void set_compose_pending(boolean pending)
    {
      _keyboard_layout_view.set_compose_pending(pending);
    }

    public void selection_state_changed(boolean selection_is_ongoing)
    {
      _keyboard_layout_view.set_selection_state(selection_is_ongoing);
    }

    public void on_autocorrection()
    {
      _keyboard_layout_view.feedback(VibratorCompat.Feedback.CORRECTION);
    }

    public InputConnection getCurrentInputConnection()
    {
      return Keyboard2.this.getCurrentInputConnection();
    }

    public Handler getHandler()
    {
      return _handler;
    }

    public void set_suggestions(Suggestions suggestions)
    {
      _candidates_view.set_candidates(suggestions);
      update_candidates_view_visibility(suggestions);
    }

    public String provide_stateful_key_symbol(KeyValue.Stateful q)
    {
      switch (q)
      {
        case Complete_first:
        case Complete_first_space: return _suggestions.suggestions[0];
        case Complete_second:
        case Complete_second_space: return _suggestions.suggestions[1];
        case Complete_third:
        case Complete_third_space: return _suggestions.suggestions[2];
        case Complete_emoji: return _suggestions.emoji_suggestion;
      }
      return "";
    }

    public void on_change_dictionary(String dict_name)
    {
      select_dictionary(dict_name);
    }

    public void launch_dictionaries_activity()
    {
      Keyboard2.this.launch_dictionaries_activity();
    }
  }

  private IBinder getConnectionToken()
  {
    return getWindow().getWindow().getAttributes().token;
  }

  private View inflate_view(int layout)
  {
    return View.inflate(new ContextThemeWrapper(this, _config.theme), layout, null);
  }
}
