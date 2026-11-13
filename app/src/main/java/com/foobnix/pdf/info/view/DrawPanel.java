package com.foobnix.pdf.info.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.core.graphics.ColorUtils;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import org.ebookdroid.core.codec.Annotation;

/**
 * The drawing panel: what the pen draws, how thick and in which colour, and the eraser for an
 * annotation picked out on the page. It stands in a dialog, or - pinned - docked at the foot
 * of the book above its bottom bar, where it stays while the bars come and go.
 */
public class DrawPanel {

    /** No tool picked: the panel is open, the pen lifted. */
    public static final int NO_TOOL = -1;

    /** The pen draws from now on, with the tool, colour and width it had last. */
    public static void startPen() {
        if (!AppState.DRAW_COLORS.contains(AppState.get().drawColor)) {
            AppState.get().drawColor = AppState.DRAW_COLORS.get(0);
        }
        AppState.get().editLineWidth = nearestDrawWidth(AppState.get().editLineWidth);
        AppState.get().annotationDrawColor = AppState.get().drawColor;
        // The eraser taken up stays up: the panel shown again after each drawing it erases
        if (AppState.get().editWith != AppState.EDIT_DELETE) {
            AppState.get().editWith = AppState.EDIT_PEN;
        }
    }

    /** The pen is put down: the page takes touches again and nothing stays picked out. */
    public static void stopPen(DocumentController controller, DrawView drawView, Runnable onClose) {
        AppState.get().editWith = AppState.EDIT_NONE;
        AppState.get().annotationDrawColor = "";
        drawView.setVisibility(View.GONE);
        drawView.setOnTap(null);
        drawView.setOnDrag(null);
        drawView.clear();
        controller.clearAnnotationSelection();
        controller.handler.postDelayed(onClose, 500);
    }

    /** The panel's controls, wired to the pen; the dialog and the dock both show these. */
    public static View create(LayoutInflater inflater, final DocumentController controller, final DrawView drawView,
                              final Runnable onClose) {
        final View panel = inflater.inflate(R.layout.draw_panel, null, false);
        final ImageView delete = panel.findViewById(R.id.drawDelete);
        final View widthLine = panel.findViewById(R.id.drawWidthLine);
        final LinearLayout colors = panel.findViewById(R.id.drawColors);
        final int[] tools = {R.id.drawFree, R.id.drawLine, R.id.drawArrow, R.id.drawRect, R.id.drawStar};
        for (int tool : tools) {
            pressable(panel.findViewById(tool), TintUtil.SWATCH_RADIUS);
        }
        pressable(panel.findViewById(R.id.drawWidth), TintUtil.SWATCH_RADIUS);
        pressable(delete, TintUtil.SWATCH_RADIUS);

        final Runnable updatePen = () -> {
            final int color = Color.parseColor(AppState.get().drawColor);
            // With no tool picked the pen is lifted: the page turns, scrolls and is tapped as
            // it is without the panel, which stays open to pick a tool again - unless a drawing
            // is picked out, which the layer above the page stays for, to move it
            final boolean drawing = AppState.get().editShape != NO_TOOL;
            drawView.setShape(AppState.get().editShape);
            if (!drawing) {
                drawView.clear();
            }
            drawView.setColor(IMG.alphaColor(AppState.get().editAlphaColor, AppState.get().drawColor),
                              AppState.get().editLineWidth);
            drawView.setVisibility(drawing || controller.selectedAnnotation != null ? View.VISIBLE : View.GONE);
            updateDelete(controller, delete);

            // The tool in use is ringed, in the colour the panel draws its marks in
            for (int i = 0; i < tools.length; i++) {
                final ImageView tool = panel.findViewById(tools[i]);
                if (i == AppState.get().editShape) {
                    final ColorStateList tint = tool.getImageTintList();
                    TintUtil.setColorSwatchOutline(tool, tint != null ? tint.getDefaultColor() : TintUtil.color);
                } else {
                    tool.setBackground(null);
                }
            }

            // The width as a line that thick, in the pen's colour
            final GradientDrawable line = new GradientDrawable();
            line.setColor(color);
            line.setCornerRadius(Dips.DP_10);
            widthLine.setBackground(line);
            widthLine.getLayoutParams().height = Math.min(Dips.dpToPx(12), drawWidthPx(AppState.get().editLineWidth));
            widthLine.requestLayout();

            // The colour in use carries a tick
            for (int i = 0; i < AppState.DRAW_COLORS.size(); i++) {
                final ImageView swatch = (ImageView) colors.getChildAt(i);
                final String name = AppState.DRAW_COLORS.get(i);
                if (name.equals(AppState.get().drawColor)) {
                    swatch.setImageResource(R.drawable.glyphicons_239_check);
                    swatch.setColorFilter(MagicHelper.isColorDark(Color.parseColor(name)) ? Color.WHITE : Color.BLACK);
                } else {
                    swatch.setImageDrawable(null);
                }
            }
        };

        for (int i = 0; i < tools.length; i++) {
            final int shape = i;
            panel.findViewById(tools[i]).setOnClickListener(v -> {
                // The tool in use, tapped again, is put down
                AppState.get().editShape = AppState.get().editShape == shape ? NO_TOOL : shape;
                AppState.get().editWith = AppState.EDIT_PEN;
                updatePen.run();
            });
        }

        for (int i = 0; i < AppState.DRAW_COLORS.size(); i++) {
            final String name = AppState.DRAW_COLORS.get(i);
            final ImageView swatch = new ImageView(panel.getContext());
            // Round, with no frame: kept out of the pass that rings the dialog's shapes
            TintUtil.setColorSwatch(swatch, Color.parseColor(name));
            ((GradientDrawable) swatch.getBackground()).setStroke(0, Color.TRANSPARENT);
            swatch.setTag("no_tint");
            swatch.setPadding(Dips.DP_8, Dips.DP_8, Dips.DP_8, Dips.DP_8);
            swatch.setContentDescription(name);
            pressable(swatch, TintUtil.SWATCH_RADIUS);
            // Eight of them fit in a phone's width beside the dock's buttons
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Dips.dpToPx(32), Dips.dpToPx(32));
            params.rightMargin = Dips.DP_4;
            colors.addView(swatch, i, params);
            swatch.setOnClickListener(v -> {
                AppState.get().drawColor = name;
                AppState.get().annotationDrawColor = name;
                updatePen.run();
                // The drawing picked out takes the colour too
                if (controller.selectedAnnotation != null) {
                    controller.styleSelectedAnnotation(Color.parseColor(name), 0);
                }
            });
        }

        panel.findViewById(R.id.drawWidth).setOnClickListener(v -> showDrawWidths(v, () -> {
            updatePen.run();
            // The drawing picked out takes the width too, in the units a new one is drawn with
            if (controller.selectedAnnotation != null) {
                controller.styleSelectedAnnotation(null, AppState.get().editLineWidth * 3);
            }
        }));

        // A tap on the page picks out the annotation under it, which can then be erased
        updateDelete(controller, delete);
        drawView.setOnTap((x, y) -> {
            final boolean hadPicked = controller.selectedAnnotation != null;
            final Annotation picked = controller.selectAnnotationAt(x, y);
            // Docked, the panel stays while the book's panels come and go: a tap on the page
            // with nothing on it to pick, or to let go of, shows them or puts them away
            if (picked == null && !hadPicked && isDocked(controller)) {
                controller.togglePanels();
            }
            updatePen.run();
        });
        // The drawing picked out is taken with a finger and put elsewhere on its page
        drawView.setOnDrag(new DrawView.OnDrag() {
            @Override public RectF draggable() {
                return controller.selectedAnnotationOnScreen();
            }

            @Override public void onDrop(float dx, float dy) {
                controller.moveSelectedAnnotation(dx, dy);
                updatePen.run();
            }
        });
        delete.setOnClickListener(v -> {
            if (controller.selectedAnnotation != null) {
                controller.deleteSelectedAnnotation();
            } else {
                // Nothing picked out: the eraser is taken up, or put down, and a tap on a
                // drawing on the page then erases it; the pen is put down for it
                final boolean erasing = AppState.get().editWith == AppState.EDIT_DELETE;
                AppState.get().editWith = erasing ? AppState.EDIT_PEN : AppState.EDIT_DELETE;
                if (!erasing) {
                    AppState.get().editShape = NO_TOOL;
                }
            }
            updatePen.run();
        });

        drawView.setOnFinishDraw(() -> {
            controller.clearAnnotationSelection();
            controller.saveChanges(drawView.getPoints(), Color.parseColor(AppState.get().drawColor));
            drawView.clear();
            controller.saveAnnotationsToFile();
            controller.handler.postDelayed(onClose, 500);
            // A shape is drawn once and the tool put down; the free line is the pen's own
            if (AppState.get().editShape != DrawView.SHAPE_FREE) {
                AppState.get().editShape = NO_TOOL;
                updatePen.run();
            }
        });

        updatePen.run();
        return panel;
    }

    /** The dock at the foot of the book, as each reading screen lays it out, or null. */
    private static ViewGroup dock(DocumentController controller) {
        final View dock = controller.getActivity().findViewById(R.id.drawDock);
        return dock instanceof ViewGroup ? (ViewGroup) dock : null;
    }

    public static boolean isDocked(DocumentController controller) {
        final ViewGroup dock = dock(controller);
        return dock != null && dock.getVisibility() == View.VISIBLE;
    }

    /**
     * Shows the panel docked above the book's bottom bar, with a button that puts it back in a
     * window and one that puts the pen down.
     */
    public static void showDocked(final DocumentController controller, final DrawView drawView, final Runnable onClose,
                                  final Runnable unpin) {
        final ViewGroup dock = dock(controller);
        if (dock == null) {
            return;
        }
        final Context context = dock.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        dock.removeAllViews();

        final LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final GradientDrawable sheet = new GradientDrawable();
        // The colour the book's bars are, and as see-through: docked on them, it is one of them
        sheet.setColor(ColorUtils.setAlphaComponent(TintUtil.color, AppState.get().transparencyUI));
        // Rounded on top only: its foot stands on the bottom bar, and round corners there would
        // show the page through between the two
        final float radius = Dips.dpToPx(16);
        sheet.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        row.setBackground(sheet);
        row.setElevation(Dips.DP_8);
        // Touches on the sheet are the panel's, not the page's under it
        row.setClickable(true);

        row.addView(create(inflater, controller, drawView, onClose),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        final LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.addView(dockButton(context, R.drawable.glyphicons_415_push_pin, R.string.draw_unpin, v -> {
            hideDock(controller);
            unpin.run();
        }));
        buttons.addView(dockButton(context, R.drawable.glyphicons_599_menu_close, R.string.close, v -> {
            AppState.get().isDrawPanelPinned = false;
            hideDock(controller);
            stopPen(controller, drawView, onClose);
        }));
        row.addView(buttons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dock.addView(row);
        dock.setVisibility(View.VISIBLE);
        // The panel's marks take the colours the bars give theirs
        TxtUtils.updateAllLinks(row, false);
        // The pin stands ringed: the panel is pinned, and a tap lets it go
        final ImageView pin = (ImageView) buttons.getChildAt(0);
        final ColorStateList tint = pin.getImageTintList();
        TintUtil.setColorSwatchOutline(pin, tint != null ? tint.getDefaultColor() : TintUtil.color);
    }

    public static void hideDock(DocumentController controller) {
        final ViewGroup dock = dock(controller);
        if (dock != null) {
            dock.removeAllViews();
            dock.setVisibility(View.GONE);
        }
    }

    private static ImageView dockButton(Context context, int icon, int description, View.OnClickListener onClick) {
        final ImageView button = new ImageView(context);
        button.setImageResource(icon);
        button.setContentDescription(context.getString(description));
        button.setPadding(Dips.DP_8, Dips.DP_8, Dips.DP_8, Dips.DP_8);
        button.setOnClickListener(onClick);
        pressable(button, TintUtil.SWATCH_RADIUS);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Dips.dpToPx(36), Dips.dpToPx(36));
        params.setMargins(Dips.DP_4, Dips.DP_4, Dips.DP_6, Dips.DP_4);
        button.setLayoutParams(params);
        return button;
    }

    /**
     * The eraser: there while a drawing is picked out, to erase it - and in musician's mode
     * always, as a tap on the page turns it there rather than picking a drawing out, so the
     * eraser is taken up instead and the drawing tapped. Ringed while it has one to erase.
     */
    private static void updateDelete(DocumentController controller, ImageView delete) {
        final boolean picked = controller.selectedAnnotation != null;
        delete.setVisibility(picked || controller.isMusicianMode() ? View.VISIBLE : View.GONE);
        if (picked || AppState.get().editWith == AppState.EDIT_DELETE) {
            final ColorStateList tint = delete.getImageTintList();
            TintUtil.setColorSwatchOutline(delete, tint != null ? tint.getDefaultColor() : TintUtil.color);
        } else {
            delete.setBackground(null);
        }
    }

    /**
     * A press shows on the button: a grey wash over it in its own round, drawn over whatever
     * the button carries - a swatch's colour, a tool's ring - and read on a bar or a sheet alike.
     */
    private static void pressable(View view, float radius) {
        final GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(radius);
        view.setForeground(new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#66888888")), null, mask));
    }

    /** The pen's widths, each as a line that thick, under the button that shows the one in use. */
    private static void showDrawWidths(final View button, final Runnable updatePen) {
        final Context context = button.getContext();
        final boolean light = TintUtil.isLightSurface(context, android.R.attr.colorBackground);
        final LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, Dips.DP_6, 0, Dips.DP_6);
        final GradientDrawable sheet = new GradientDrawable();
        sheet.setColor(light ? Color.WHITE : Color.parseColor("#263238"));
        sheet.setCornerRadius(Dips.dpToPx(16));
        list.setBackground(sheet);
        // The row of the width in use is shaded to the sheet's round, not past it
        list.setClipToOutline(true);

        final int sheetWidth = Dips.dpToPx(150);
        final PopupWindow popup = new PopupWindow(list, sheetWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setElevation(Dips.DP_8);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        for (final float width : AppState.DRAW_WIDTHS) {
            final FrameLayout row = new FrameLayout(context);
            row.setPadding(Dips.dpToPx(20), 0, Dips.dpToPx(20), 0);
            if (width == AppState.get().editLineWidth) {
                row.setBackgroundColor(light ? Color.parseColor("#1F000000") : Color.parseColor("#26FFFFFF"));
            }
            final View line = new View(context);
            final GradientDrawable shape = new GradientDrawable();
            shape.setColor(Color.parseColor(AppState.get().drawColor));
            shape.setCornerRadius(Dips.DP_10);
            line.setBackground(shape);
            row.addView(line, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, drawWidthPx(width), Gravity.CENTER));
            row.setContentDescription(String.valueOf(width));
            pressable(row, 0);
            row.setOnClickListener(v -> {
                AppState.get().editLineWidth = width;
                updatePen.run();
                popup.dismiss();
            });
            list.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Dips.dpToPx(44)));
        }
        // Its right edge on the button's, as the button stands at the panel's end; under the
        // button, or over it where the button stands low - the docked panel is at the foot
        list.measure(View.MeasureSpec.makeMeasureSpec(sheetWidth, View.MeasureSpec.EXACTLY),
                     View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int[] at = new int[2];
        button.getLocationOnScreen(at);
        final int screenHeight = button.getRootView().getHeight();
        final boolean above = at[1] + button.getHeight() + list.getMeasuredHeight() > screenHeight;
        popup.showAsDropDown(button, button.getWidth() - sheetWidth,
                             above ? -(button.getHeight() + list.getMeasuredHeight()) : 0);
    }

    /** A width as the pen draws it on screen. */
    private static int drawWidthPx(float width) {
        return Math.max(Dips.dpToPx(2), Dips.dpToPx((int) width * 2));
    }

    /** The width of the panel nearest a width chosen before the panel had only these. */
    private static float nearestDrawWidth(float width) {
        float nearest = AppState.DRAW_WIDTHS.get(0);
        for (float w : AppState.DRAW_WIDTHS) {
            if (Math.abs(w - width) < Math.abs(nearest - width)) {
                nearest = w;
            }
        }
        return nearest;
    }
}
