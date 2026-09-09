package com.foobnix.pdf.info;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.graphics.Outline;
import android.view.ViewOutlineProvider;
import android.view.ViewGroup;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.ImageViewCompat;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TintUtil {
    public static final int RADIUS = Dips.dpToPx(2);
    public static final int STROKE = Dips.dpToPx(1);
    public static final int SWATCH_RADIUS = Dips.dpToPx(6);
    public static final int SECTION_RADIUS = Dips.dpToPx(8);
    public static final int BADGE_RADIUS = Dips.dpToPx(20);
    private static final int SWATCH_BORDER = Color.parseColor("#55888888");
    public static int itAlpha = 245;
    public static int colorSecondTab = Color.parseColor("#ddffffff");// Color.parseColor("#9fd8bc");
    public static int cloudSyncColor = Color.parseColor("#66bb6a");// Color.parseColor("#9fd8bc");
    public static int color = Color.parseColor(AppState.STYLE_COLORS.get(0));
    private static List<Drawable> drawables = new ArrayList<Drawable>();
    private static List<GradientDrawable> drawableFill = new ArrayList<GradientDrawable>();
    private static List<View> drawables1 = new ArrayList<View>();

    public static int COLOR_TINT_GRAY = Color.parseColor("#009688");
    public static int COLOR_ORANGE = Color.parseColor("#FF8C00");

    public static int getColorInDayNighth() {
        if(AppState.get().appTheme == AppState.THEME_INK){
            return Color.BLACK;
        }
        return AppState.get().appTheme == AppState.THEME_LIGHT ? TintUtil.color : Color.LTGRAY;
    }

    public static int getColorInDayNighthBook() {
        if(AppState.get().appTheme == AppState.THEME_INK){
            return Color.BLACK;
        }
        return AppState.get().isDayNotInvert ? TintUtil.color : Color.LTGRAY;
    }

    static Random random = new Random();

    public static int randomColor() {
        return Color.HSVToColor(new float[]{random.nextInt(360), random.nextFloat(), (3f + random.nextInt(4)) / 10f});
    }

    public static int randomColor(int hash) {
        try {
            LOG.d("randomColor", hash);
            hash = Math.abs(hash);
            String num = "" + hash;
            float hue = 360f * Float.parseFloat(num.substring(0, 2)) / 100f;
            float sat = Float.parseFloat(num.substring(1, 3)) / 100f;
            float value = Float.parseFloat(num.substring(2, 4)) / 100f;

            LOG.d("randomColor", hash, hue, sat, value);
            return Color.HSVToColor(new float[]{hue, sat, Math.max(Math.min(0.1f, value), 0.5f)});
        } catch (Exception e) {
            return Color.HSVToColor(new float[]{new Random().nextInt(360), new Random().nextFloat(), (3f + new Random().nextInt(4)) / 10f});
        }
    }

    public static int getStatusBarColor() {
        return AppState.get().isDayNotInvert ? AppState.get().statusBarColorDay : AppState.get().statusBarColorNight;
    }

    public static int tintRandomColor() {
        AppState.get().tintColor = Color.HSVToColor(new float[]{new Random().nextInt(360), new Random().nextFloat(), (3f + new Random().nextInt(4)) / 10f});
        TintUtil.color = AppState.get().tintColor;
        return AppState.get().tintColor;
    }

    public static void init() {
        color = AppState.get().tintColor;
    }

    public static void clean() {
        drawables.clear();
    }

    public static void addGradiendDrawableFill(GradientDrawable drawable) {
        drawable = (GradientDrawable) drawable.getCurrent();
        if (!drawableFill.contains(drawable)) {
            drawableFill.add(drawable);
        }

        drawable.setColor(TintUtil.color);
        drawable.setCornerRadius(TintUtil.RADIUS);

    }

    public static void setBackgroundFillColor(View textView, int color) {
        try {
            GradientDrawable drawable = (GradientDrawable) textView.getBackground().getCurrent();
            drawable.setColor(color);
            drawable.setCornerRadius(RADIUS);
        }catch (Exception e){
            LOG.e(e);
        }

    }

    /**
     * Cuts a view to a round without wrapping it in a card - a cover is sized by the adapter
     * that shows it, and a card around it would take that sizing away from the image.
     */
    public static void roundCorners(View view, final float radius) {
        if (view == null) {
            return;
        }
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        view.setClipToOutline(true);
    }

    /**
     * A link drawn as a button: a ring cut to the round the cards are, in the colour of the
     * word inside it, with air enough round it that two side by side do not touch.
     */
    public static void asLinkButton(TextView button) {
        asLinkButton(button, 110);
    }

    /**
     * The same button, with the floor under its width given rather than assumed. Two of them
     * side by side in half a dialog have no room for the width a column of them wants, and
     * are asked for none.
     */
    public static void asLinkButton(TextView button, int minWidthDp) {
        if (button == null) {
            return;
        }
        // Any underline the text was carrying goes with it - the ring says it can be pressed.
        button.setText(button.getText()
                             .toString());
        button.setBackgroundResource(R.drawable.bg_button_outline);
        button.setPadding(Dips.DP_10, Dips.DP_4, Dips.DP_10, Dips.DP_4);
        // A floor under the width, so a column of short values - Top, Dark, Auto - comes out
        // one width instead of a ragged edge. A longer word still makes its button wider.
        button.setMinWidth(Dips.dpToPx(minWidthDp));
        button.setGravity(Gravity.CENTER);
        if (button.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) button.getLayoutParams();
            lp.leftMargin = lp.rightMargin = Dips.DP_8;
            lp.topMargin = lp.bottomMargin = Dips.DP_4;
            button.setLayoutParams(lp);
        }
        setRingColor(button, button.getCurrentTextColor());
    }

    /**
     * Draws every link under a view as a button. A link is what the textLink style tags, so a
     * dialog takes on the settings panel's look by handing this its inflated root.
     */
    public static void asLinkButtons(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                asLinkButtons(group.getChildAt(i));
            }
        } else if (root instanceof TextView && "textLink".equals(root.getTag())) {
            asLinkButton((TextView) root);
            alignInRow((TextView) root);
        }
    }

    /**
     * A value button set against a word in its own row is given half the row, and the word the
     * other half, so a column of them comes out one edge instead of stepping in and out with
     * the length of each word. Rows carrying anything else - a mark, a box to tick - are left
     * as they were laid out.
     */
    private static void alignInRow(TextView button) {
        if (!(button.getParent() instanceof LinearLayout)) {
            return;
        }
        LinearLayout row = (LinearLayout) button.getParent();
        if (row.getOrientation() != LinearLayout.HORIZONTAL || row.getChildCount() != 2) {
            return;
        }
        View label = row.getChildAt(0) == button ? row.getChildAt(1) : row.getChildAt(0);
        if (!(label instanceof TextView) || "textLink".equals(label.getTag())) {
            return;
        }
        halve(label);
        halve(button);
    }

    private static void halve(View view) {
        if (!(view.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return;
        }
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
        lp.width = 0;
        lp.weight = 1;
        view.setLayoutParams(lp);
    }

    /**
     * The count carried in the corner of an icon, drawn as a round in the theme colour rather
     * than as a coloured square. The round is larger than the badge can ever be, so a single
     * digit comes out a circle and a longer count a capsule.
     */
    public static void setBadge(View badge, int color) {
        if (badge == null) {
            return;
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(BADGE_RADIUS);
        shape.setColor(color);
        badge.setBackground(shape);
    }

    /**
     * A settings section head. Same fill as any other tinted bar, but cut to a round of its
     * own, so a head reads as a band laid over the page rather than a block butted against it.
     */
    public static void setSectionFillColor(View textView, int color) {
        try {
            GradientDrawable drawable = (GradientDrawable) textView.getBackground().getCurrent();
            drawable.setColor(color);
            drawable.setCornerRadius(SECTION_RADIUS);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void setBackgroundFillColorBottomRight(View textView, int color) {
        GradientDrawable drawable = (GradientDrawable) textView.getBackground().getCurrent();
        drawable.setColor(color);
        drawable.setCornerRadii(new float[]{0, 0, 0, 0, RADIUS * 2, RADIUS * 2, 0, 0});
    }

    /**
     * The reading progress line: one view carrying both the track and the part read, so the
     * line can be given the width of the row and still be filled by a fraction rather than by
     * a second view measured in pixels. Ends are rounded to half the line's own thickness.
     */
    public static void setProgressLine(View line, int trackColor, int fillColor, double progress) {
        if (line == null) {
            return;
        }
        int height = line.getLayoutParams() == null ? 0 : line.getLayoutParams().height;
        if (height <= 0) {
            height = line.getHeight();
        }
        float radius = height / 2f;

        GradientDrawable track = new GradientDrawable();
        track.setCornerRadius(radius);
        track.setColor(trackColor);

        GradientDrawable read = new GradientDrawable();
        read.setCornerRadius(radius);
        read.setColor(fillColor);

        ClipDrawable clip = new ClipDrawable(read, Gravity.START, ClipDrawable.HORIZONTAL);
        clip.setLevel((int) Math.round(Math.max(0, Math.min(1, progress)) * 10000));

        line.setBackground(new LayerDrawable(new Drawable[]{track, clip}));
    }

    /**
     * A colour to pick from, drawn as a chip with a small round and a hairline round it, so a
     * swatch close to the colour of the sheet behind it still reads as a swatch.
     */
    public static void setColorSwatch(View swatch, int color) {
        if (swatch == null) {
            return;
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(SWATCH_RADIUS);
        shape.setColor(color);
        shape.setStroke(STROKE, SWATCH_BORDER);
        swatch.setBackground(shape);
    }

    /**
     * The chip that opens the colour picker: the same round, drawn as a ring in the colour
     * already chosen rather than filled with it, so the plus on it reads as a button and not
     * as one more colour in the row.
     */
    public static void setColorSwatchOutline(View swatch, int color) {
        if (swatch == null) {
            return;
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(SWATCH_RADIUS);
        shape.setColor(Color.TRANSPARENT);
        shape.setStroke(Dips.DP_2, color);
        swatch.setBackground(shape);
    }

    /**
     * Draws the ring of a round outline button in the given colour. The background is a ripple
     * wrapped around the shape, so the shape has to be dug out of the layers rather than cast
     * to from the background itself.
     */
    /**
     * The states a ring is looked for in. A selector hands out one drawable at a time, so it
     * is asked for each of these in turn - otherwise only the state that happened to be on
     * show is recoloured, and the ring goes back to the colour it was inflated with the moment
     * the view settles into another state.
     */
    private static final int[][] RING_STATES = {
            new int[]{},
            new int[]{android.R.attr.state_pressed},
            new int[]{android.R.attr.state_selected},
            new int[]{android.R.attr.state_focused},
            new int[]{android.R.attr.state_enabled},
    };

    public static void setRingColor(View view, int color) {
        if (view == null || view.getBackground() == null) {
            return;
        }
        strokeRings(view.getBackground().mutate(), color, 0);
        // The states were stepped through to reach every ring; the view is asked to draw
        // itself again so the one on show is the freshly coloured one.
        view.invalidate();
    }

    /**
     * Draws every ring found under a background in the given colour, whichever way it was put
     * together - a shape, a shape inside a ripple or a layer list, a shape held in a selector,
     * or one wrapped in an inset. Anything that is not a ring is passed over.
     */
    private static void strokeRings(Drawable drawable, int color, int depth) {
        if (drawable == null || depth > 4) {
            return;
        }
        if (drawable instanceof GradientDrawable) {
            ((GradientDrawable) drawable).setStroke(STROKE, color);
        } else if (drawable instanceof LayerDrawable) {
            // A ripple is a layer list too, and its mask is not drawn.
            LayerDrawable layers = (LayerDrawable) drawable;
            for (int i = 0; i < layers.getNumberOfLayers(); i++) {
                if (layers.getId(i) == android.R.id.mask) {
                    continue;
                }
                strokeRings(layers.getDrawable(i), color, depth + 1);
            }
        } else if (drawable instanceof StateListDrawable) {
            StateListDrawable states = (StateListDrawable) drawable;
            int[] wasShowing = states.getState();
            for (int[] state : RING_STATES) {
                states.setState(state);
                Drawable child = states.getCurrent();
                if (child != null && child != drawable) {
                    strokeRings(child, color, depth + 1);
                }
            }
            states.setState(wasShowing);
        } else if (drawable instanceof InsetDrawable) {
            strokeRings(((InsetDrawable) drawable).getDrawable(), color, depth + 1);
        }
    }

    public static void setStrokeColor(View textView, int color) {
        GradientDrawable drawable = (GradientDrawable) textView.getBackground().getCurrent();
        drawable.setStroke(STROKE, color);
        drawable.setCornerRadius(RADIUS);
    }

    public static GradientDrawable setStrokeColorWithDash(View textView, int color) {
        GradientDrawable drawable = (GradientDrawable) textView.getBackground().getCurrent();
        drawable.setStroke(Dips.DP_2, color,Dips.DP_6,Dips.DP_6);
        drawable.setCornerRadius(RADIUS);
        return drawable;

    }



    public static void setUITextColor(TextView textView, int color) {
        if (textView == null) {
            return;
        }
        textView.setTextColor(color);
        textView.setHintTextColor(color);
    }

    public static void addDrawable(Drawable drawable) {
        drawable = drawable.getCurrent();
        if (!drawables.contains(drawable)) {
            drawables.add(drawable);
        }
        if (drawable instanceof GradientDrawable) {
            // ((GradientDrawable) drawable).setColor(Color.WHITE);
            ((GradientDrawable) drawable).setStroke(STROKE, color);
            ((GradientDrawable) drawable).setCornerRadius(RADIUS);
        } else {
            drawable.setColorFilter(color, Mode.SRC_ATOP);
        }
    }

    public static Drawable setDrawableTint(Drawable drawable, int color, int alpha) {
        try {
            drawable.setColorFilter(color, Mode.SRC_ATOP);
            drawable.setAlpha(alpha);
        } catch (Exception e) {
            LOG.e(e);
        }
        return drawable;
    }
    public static Drawable setDrawableTint(Drawable drawable, int color) {
        try {
            drawable.setColorFilter(color, Mode.SRC_ATOP);
        } catch (Exception e) {
            LOG.e(e);
        }
        return drawable;
    }


    public static void addTingBg(View textView) {
        if (!drawables1.contains(textView)) {
            drawables1.add(textView);
        }
        setTintBg(textView);
    }

    public static void updateAll() {
        for (Drawable drawable : drawables) {
            if (drawable instanceof GradientDrawable) {
                ((GradientDrawable) drawable).setStroke(STROKE, color);
                ((GradientDrawable) drawable).setCornerRadius(RADIUS);
            } else {
                drawable.setColorFilter(color, Mode.SRC_ATOP);
            }
        }
        for (GradientDrawable drawable : drawableFill) {
            drawable.setColor(TintUtil.color);
            drawable.setCornerRadius(TintUtil.RADIUS);
        }

        for (View textView : drawables1) {
            setTintBg(textView);
        }
    }

    public static ImageView setTintImageWithAlpha(ImageView img) {
        if (img == null) {
            return null;
        }
        //img.setColorFilter(color, Mode.SRC_ATOP);
        ImageViewCompat.setImageTintList(img, ColorStateList.valueOf(color));
        img.setImageAlpha(230);
        return img;
    }

    public static ImageView setNoTintImage(ImageView img) {
        img.setColorFilter(null);
        return img;
    }

    public static ImageView setTintImageWithAlpha(ImageView img, int color) {
        img.setColorFilter(color, Mode.SRC_ATOP);
        //ImageViewCompat.setImageTintList(img, ColorStateList.valueOf(color));
        img.setImageAlpha(230);
        return img;
    }

    public static ImageView setTintImageWithAlpha(ImageView img, int color, int alpha) {
        img.setColorFilter(color, Mode.SRC_ATOP);
        //ImageViewCompat.setImageTintList(img, ColorStateList.valueOf(color));
        if (alpha > 0) {
            img.setImageAlpha(alpha);
        }
        return img;
    }

    public static ImageView setTintImageNoAlpha(ImageView img, int color) {
        //img.setColorFilter(color, Mode.SRC_ATOP);
        ImageViewCompat.setImageTintList(img, ColorStateList.valueOf(color));
        return img;
    }

    public static void setTintText(TextView img) {
        img.setTextColor(color);
    }

    public static void setTintText(TextView img, int color) {
        img.setTextColor(color);
    }

    @SuppressLint("NewApi")
    public static void setTintBgSimple(View txtView, int alpha) {
        ColorDrawable colorDrawable = new ColorDrawable(color);
        colorDrawable.setAlpha(alpha);
        if (Build.VERSION.SDK_INT >= 16) {
            txtView.setBackground(colorDrawable.getCurrent());
        } else {
            txtView.setBackgroundDrawable(colorDrawable);
        }
    }

    @SuppressLint("NewApi")
    public static void setTintBgSimple(View txtView, int alpha, int color) {
        ColorDrawable colorDrawable = new ColorDrawable(color);
        colorDrawable.setAlpha(alpha);
        if (Build.VERSION.SDK_INT >= 16) {
            txtView.setBackground(colorDrawable.getCurrent());
        } else {
            txtView.setBackgroundDrawable(colorDrawable);
        }
    }

    @SuppressLint("NewApi")
    public static void setTintBg(View txtView) {
        if (txtView == null || txtView.getBackground() == null) {
            return;
        }
        txtView.getBackground().setColorFilter(color, Mode.SRC_ATOP);

    }

    @SuppressLint("NewApi")
    public static void setTintBgOld(View txtView) {
        if (txtView == null || txtView.getBackground() == null) {
            return;
        }
        txtView.getBackground().setColorFilter(color, Mode.SRC_ATOP);

        StateListDrawable states = new StateListDrawable();

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(color);
        normal.setAlpha(itAlpha);
        normal.setCornerRadius(RADIUS);
        normal.setStroke(STROKE, Color.parseColor("#eeffffff"));

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(color);
        pressed.setAlpha(200);
        pressed.setCornerRadius(RADIUS);
        pressed.setStroke(STROKE, Color.parseColor("#eeffffff"));

        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, normal);
        if (Build.VERSION.SDK_INT >= 16) {
            txtView.setBackground(states);
        } else {
            txtView.setBackgroundDrawable(states);
        }
    }

    @SuppressLint("NewApi")
    public static void setStatusBarColor(Activity activity) {
        setStatusBarColor(activity, TintUtil.color);
    }

    @SuppressLint("NewApi")
    public static void setStatusBarColor(Activity activity, int color) {

    }

    public static void grayScaleImageView(ImageView v) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0); // 0 means grayscale
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
        v.setColorFilter(cf);
    }

    public static int starColorEmpty = Color.parseColor("#eeFFFFFF");
    public static int starColorFull = Color.parseColor("#eeFFFF00");

    public static void drawStar(final ImageView imageView, boolean isStar) {
        if (isStar) {
            imageView.setImageResource(R.drawable.glyphicons_13_heart);
            TintUtil.setTintImageWithAlpha(imageView, TintUtil.color);
        } else {
            imageView.setImageResource(R.drawable.glyphicons_9_heart_empty);
            TintUtil.setTintImageWithAlpha(imageView, TintUtil.color);
        }
    }

}
