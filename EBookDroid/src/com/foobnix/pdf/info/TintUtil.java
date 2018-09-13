package com.foobnix.pdf.info;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class TintUtil {
    public static final int RADIUS = Dips.dpToPx(2);
    public static final int STROKE = Dips.dpToPx(1);
    public static int itAlpha = 245;
    public static int colorSecondTab = Color.parseColor("#eeffffff");// Color.parseColor("#9fd8bc");
    public static int color = Color.parseColor(AppState.STYLE_COLORS.get(0));
    private static List<Drawable> drawables = new ArrayList<Drawable>();
    private static List<GradientDrawable> drawableFill = new ArrayList<GradientDrawable>();
    private static List<View> drawables1 = new ArrayList<View>();

    public static int COLOR_TINT_GRAY = Color.parseColor("#009688");
    public static int COLOR_ORANGE = Color.parseColor("#FF8C00");

    public static int getColorInDayNighth() {
        return AppState.get().isWhiteTheme ? TintUtil.color : Color.LTGRAY;
    }

    public static int getColorInDayNighthBook() {
        return AppState.get().isDayNotInvert ? TintUtil.color : Color.LTGRAY;
    }

    static Random random = new Random();

    public static int randomColor() {
        return Color.HSVToColor(new float[] { random.nextInt(360), random.nextFloat(), (3f + random.nextInt(4)) / 10f });
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
            return Color.HSVToColor(new float[] { hue, sat, Math.max(Math.min(0.1f, value), 0.5f) });
        } catch (Exception e) {
            return Color.HSVToColor(new float[] { new Random().nextInt(360), new Random().nextFloat(), (3f + new Random().nextInt(4)) / 10f });
        }
    }

    public static int getStatusBarColor() {
        return AppState.get().isDayNotInvert ? AppState.get().statusBarColorDay : AppState.get().statusBarColorNight;
    }

    public static int tintRandomColor() {
        AppState.get().tintColor = Color.HSVToColor(new float[] { new Random().nextInt(360), new Random().nextFloat(), (3f + new Random().nextInt(4)) / 10f });
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
        GradientDrawable drawable = (GradientDrawable) textView.getBackground().getCurrent();
        drawable.setColor(color);
        drawable.setCornerRadius(RADIUS);

    }

    public static void setBackgroundFillColorBottomRight(View textView, int color) {
        GradientDrawable drawable = (GradientDrawable) textView.getBackground().getCurrent();
        drawable.setColor(color);
        drawable.setCornerRadii(new float[] { 0, 0, 0, 0, RADIUS * 2, RADIUS * 2, 0, 0 });
    }

    public static void setStrokeColor(View textView, int color) {
        GradientDrawable drawable = (GradientDrawable) textView.getBackground().getCurrent();
        drawable.setStroke(STROKE, color);
        drawable.setCornerRadius(RADIUS);
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

    public static void setDrawableTint(Drawable drawable, int color) {
        drawable.setColorFilter(color, Mode.SRC_ATOP);
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
        img.setColorFilter(color, Mode.SRC_ATOP);
        img.setAlpha(230);
        return img;
    }

    public static ImageView setNoTintImage(ImageView img) {
        img.setColorFilter(null);
        return img;
    }

    public static ImageView setTintImageWithAlpha(ImageView img, int color) {
        img.setColorFilter(color, Mode.SRC_ATOP);
        img.setAlpha(230);
        return img;
    }

    public static ImageView setTintImageNoAlpha(ImageView img, int color) {
        img.setColorFilter(color, Mode.SRC_ATOP);
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

        states.addState(new int[] { android.R.attr.state_pressed }, pressed);
        states.addState(new int[] {}, normal);
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
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(MagicHelper.darkerColor(color));
        }
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
            imageView.setImageResource(R.drawable.star_1);
            TintUtil.setTintImageWithAlpha(imageView, TintUtil.color);
        } else {
            imageView.setImageResource(R.drawable.star_2);
            TintUtil.setTintImageWithAlpha(imageView, TintUtil.color);
        }
    }

}
