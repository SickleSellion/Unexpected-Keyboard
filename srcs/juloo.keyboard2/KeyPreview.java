package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

/** Shows the symbol that is about to be typed in a bubble above the key being
    pressed, including the corner symbol selected by a swipe, so that a wrong
    key or an accidental swipe is noticed before the finger is released.

    The bubble is drawn by a view covering the whole window of the input
    method, installed next to the input view in the window's content view.
    Keys of the top row can thus show their bubble above the keyboard. The
    view does not receive touches. See [Keyboard2View.preview_refresh]. */
public final class KeyPreview extends View
{
  /** The bubble stays visible at least this long, so that a quick tap still
      flashes it. */
  static final long MIN_VISIBLE_MS = 60;
  /** Border of the bubble, in dp. */
  static final float BORDER_WIDTH_DP = 2.5f;
  /** Widest bubble, relative to the width of the key. */
  static final float MAX_WIDTH = 2.5f;

  final Theme _theme;
  final Paint _bg_paint;
  final Paint _border_paint;
  final Paint _text_paint;
  final float _border_width;

  /** Geometry of the bubble in this view's coordinates. Valid when
      [_visible]. */
  final RectF _bubble = new RectF();
  /** Union of the previous and current bubbles: the area to redraw. */
  final RectF _dirty = new RectF();
  final RectF _tmp = new RectF();
  final int[] _loc = new int[2];
  boolean _visible = false;
  String _label = "";
  boolean _is_swipe = false;
  float _corner_radius = 0f;
  long _shown_at = 0;

  KeyPreview(Context ctx, Theme theme)
  {
    super(ctx);
    _theme = theme;
    _bg_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    _bg_paint.setColor(theme.colorKeyActivated | 0xFF000000);
    _border_width = BORDER_WIDTH_DP * ctx.getResources().getDisplayMetrics().density;
    _border_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    _border_paint.setStyle(Paint.Style.STROKE);
    _border_paint.setStrokeWidth(_border_width);
    _text_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    _text_paint.setTextAlign(Paint.Align.CENTER);
    setClickable(false);
    setFocusable(false);
    setWillNotDraw(false);
  }

  /** Install a preview view in the window containing [keyboard_view], or
      reuse the one already installed if it was made for the same theme.
      Returns [null] if the window has no content view. */
  static KeyPreview install(View keyboard_view, Theme theme)
  {
    View root = keyboard_view.getRootView();
    if (root == null)
      return null;
    View content = root.findViewById(android.R.id.content);
    if (!(content instanceof ViewGroup))
      return null;
    ViewGroup vg = (ViewGroup)content;
    for (int i = vg.getChildCount() - 1; i >= 0; i--)
    {
      View c = vg.getChildAt(i);
      if (c instanceof KeyPreview)
      {
        if (((KeyPreview)c)._theme == theme)
          return (KeyPreview)c;
        vg.removeView(c); // Made for a previous theme
        break;
      }
    }
    KeyPreview p = new KeyPreview(keyboard_view.getContext(), theme);
    vg.addView(p, new ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT));
    return p;
  }

  /** Only keys that type text are previewed. Modifiers, editing keys and
      other special keys would be noise. */
  static boolean should_preview(KeyValue kv)
  {
    switch (kv.getKind())
    {
      case Char:
      case String:
      case Macro:
      case Hangul_initial:
      case Hangul_medial:
      case Stateful: // Suggestions on keys
        return kv.getString().length() > 0;
      default:
        return false;
    }
  }

  /** Show [kv] above the key at ([key_x], [key_y]) of size ([key_w], [key_h])
      in the coordinates of [keyboard_view]. [is_swipe] tells whether the
      symbol was selected by a swipe. [text_size] is the preferred text size,
      reduced if the label is too long. */
  void show(View keyboard_view, KeyValue kv, boolean is_swipe, float key_x,
      float key_y, float key_w, float key_h, float text_size,
      float corner_radius)
  {
    // Offset of the keyboard view relative to this view.
    keyboard_view.getLocationInWindow(_loc);
    float off_x = _loc[0], off_y = _loc[1];
    getLocationInWindow(_loc);
    off_x -= _loc[0];
    off_y -= _loc[1];
    String label = kv.getString();
    _text_paint.setTypeface(kv.hasFlagsAny(KeyValue.FLAG_KEY_FONT)
        ? Theme.getKeyFont(getContext()) : Typeface.DEFAULT);
    _text_paint.setTextSize(text_size);
    float pad = key_w * 0.15f;
    float text_w = _text_paint.measureText(label);
    float max_text_w = key_w * MAX_WIDTH - 2 * pad;
    if (text_w > max_text_w)
    {
      _text_paint.setTextSize(text_size * max_text_w / text_w);
      text_w = _text_paint.measureText(label);
    }
    float w = Math.max(key_w, text_w + 2 * pad);
    float h = key_h;
    float left = off_x + key_x + (key_w - w) / 2f;
    int view_w = getWidth();
    if (view_w > 0) // Not layed out yet otherwise
      left = Math.max(0f, Math.min(left, view_w - w));
    float bottom = off_y + key_y - key_h * 0.12f;
    float top = Math.max(0f, bottom - h);
    boolean same = _visible && is_swipe == _is_swipe && label.equals(_label)
      && _bubble.left == left && _bubble.top == top
      && _bubble.right == left + w && _bubble.bottom == top + h;
    removeCallbacks(_hide);
    _shown_at = SystemClock.uptimeMillis();
    if (same)
      return;
    if (_visible)
      _dirty.set(_bubble);
    else
      _dirty.setEmpty();
    _bubble.set(left, top, left + w, top + h);
    _dirty.union(_bubble);
    _label = label;
    _is_swipe = is_swipe;
    _corner_radius = corner_radius;
    // The text is readable on the activated key color by construction. The
    // border tells a swipe from a tap.
    _text_paint.setColor(_theme.activatedColor | 0xFF000000);
    _border_paint.setColor(
        (is_swipe ? _theme.lockedColor : _theme.activatedColor) | 0xFF000000);
    _visible = true;
    invalidate_dirty();
  }

  /** Hide the bubble, after a short delay if it was just shown. */
  void hide()
  {
    if (!_visible)
      return;
    long elapsed = SystemClock.uptimeMillis() - _shown_at;
    if (elapsed < MIN_VISIBLE_MS)
      postDelayed(_hide, MIN_VISIBLE_MS - elapsed);
    else
      hide_now();
  }

  void hide_now()
  {
    if (!_visible)
      return;
    _visible = false;
    _dirty.set(_bubble);
    invalidate_dirty();
  }

  final Runnable _hide = new Runnable()
  {
    public void run() { hide_now(); }
  };

  /** Redraw only the area of the bubble. Redrawing the whole view would also
      redraw the keyboard underneath. */
  void invalidate_dirty()
  {
    int m = (int)Math.ceil(_border_width) + 1;
    invalidate((int)_dirty.left - m, (int)_dirty.top - m,
        (int)Math.ceil(_dirty.right) + m, (int)Math.ceil(_dirty.bottom) + m);
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    if (!_visible)
      return;
    float r = _corner_radius;
    canvas.drawRoundRect(_bubble, r, r, _bg_paint);
    float in = _border_width / 2f;
    _tmp.set(_bubble.left + in, _bubble.top + in, _bubble.right - in,
        _bubble.bottom - in);
    canvas.drawRoundRect(_tmp, r, r, _border_paint);
    float y = _bubble.centerY()
      - (_text_paint.ascent() + _text_paint.descent()) / 2f;
    canvas.drawText(_label, _bubble.centerX(), y, _text_paint);
  }
}
