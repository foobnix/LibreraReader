package com.foobnix.pdf.info.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.ColorUtils;
import androidx.annotation.Nullable;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.ui2.MainTabs2;

import java.util.ArrayList;
import java.util.List;

public class MyPopupMenu {
    Context c;
    List<Menu> list = new ArrayList<Menu>();
    private View anchor;
    private boolean isTabsActivity;
    private boolean isLightSurface;
    private OnDismissListener onDismissListener;

    public MyPopupMenu(Context c, View anchor) {
        this.c = c;
        this.anchor = anchor;
        isTabsActivity = c instanceof MainTabs2;
        isLightSurface = TintUtil.isLightSurface(c, android.R.attr.colorBackgroundFloating);


    }

    public MyPopupMenu(View anchor) {
        this.c = anchor.getContext();
        this.anchor = anchor;
        isTabsActivity = c instanceof MainTabs2;
        isLightSurface = TintUtil.isLightSurface(c, android.R.attr.colorBackgroundFloating);


    }

    public void show() {
        show(-1, false);
    }

    /** The colour every icon in the menu is drawn in, on the surface the menu stands on. */
    private void tintMenuIcon(ImageView imageView) {
        TintUtil.setTintImageWithAlpha(imageView, menuIconColor());
    }

    private int menuIconColor() {
        if (isTabsActivity) {
            if (AppState.get().appTheme == AppState.THEME_INK ||
                    AppState.get().appTheme == AppState.THEME_LIGHT) {
                return TintUtil.color;
            }
            return Color.WHITE;
        }
        if (isLightSurface) {
            // A reader's menu drawn white would swallow the white its bars' icons wear, so on
            // a light menu an icon takes the theme colour.
            return TintUtil.color;
        }
        return MagicHelper.getTextOrIconColor();
    }

    public void show(int pos, boolean isLong) {
        try {
            if (c instanceof MainTabs2) {
                ADS.hideAdsTemp((Activity) c);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        final ListPopupWindow p1 = new ListPopupWindow(c);
        shownPopup = p1;


        if (AppState.get().isEnableAccessibility) {
            getMenu(0).add(R.string.close).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    p1.dismiss();
                    return true;
                }
            });
        }


        BaseItemLayoutAdapter<Menu> a = new BaseItemLayoutAdapter<Menu>(c, R.layout.item_dict_line, list) {
            @Override
            public void populateView(View layout, int position, final Menu item) {
                TextView textView = (TextView) layout.findViewById(R.id.text1);
                TextView profileLetter = (TextView) layout.findViewById(R.id.profileLetter);
                profileLetter.setVisibility(View.GONE);

                final String stringRes = item.stringRes;
                textView.setVisibility(TxtUtils.visibleIf(TxtUtils.isNotEmpty(stringRes)));
                textView.setText(stringRes);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.isLabel ? 14 : 20);
                textView.setAlpha(item.isLabel ? 0.7f : 1f);

                // A head names what the rows under it choose between: set in caps, underlined,
                // and out of reach of a tap. Rows come back recycled, so the plain ones undo it.
                textView.setAllCaps(item.isTitle);
                textView.setTypeface(null, item.isTitle ? Typeface.BOLD : Typeface.NORMAL);
                if (item.isTitle) {
                    textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                } else {
                    textView.setPaintFlags(textView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
                }

                if (TxtUtils.isNotEmpty(item.fontPath)) {
                    textView.setTypeface(BookCSS.getTypeFaceForFont(item.fontPath));
                }
                if (AppState.get().appTheme == AppState.THEME_INK) {
                    textView.setTextColor(Color.BLACK);
                }

                CheckBox checkbox1 = (CheckBox) layout.findViewById(R.id.checkbox1);
                checkbox1.setVisibility(View.GONE);

                if (TxtUtils.isNotEmpty(item.checkboxString)) {
                    checkbox1.setVisibility(View.VISIBLE);
                    checkbox1.setText(item.checkboxString);
                    checkbox1.setOnCheckedChangeListener(null);
                    checkbox1.setChecked(item.checkboxState);
                    checkbox1.setOnCheckedChangeListener(item.checkedChangeListener);
                }


                // A mark goes at the end of the row, after the name; an icon stands in the
                // column in front of it. Rows come back recycled, so both are set every time.
                ImageView imageEnd = (ImageView) layout.findViewById(R.id.imageEnd);
                imageEnd.setVisibility(View.GONE);
                if (item.smallIcon && item.iconRes != 0) {
                    imageEnd.setVisibility(View.VISIBLE);
                    imageEnd.setImageResource(item.iconRes);
                    tintMenuIcon(imageEnd);
                }

                ImageView imageView = (ImageView) layout.findViewById(R.id.image1);
                // Where any row in the menu carries an icon of its own, the rows without one
                // keep the room it takes: the names all start at the same place.
                boolean anyIcon = false;
                for (Menu other : list) {
                    if (other.iconRes != 0 && !other.smallIcon) {
                        anyIcon = true;
                        break;
                    }
                }
                imageView.setVisibility(anyIcon ? View.INVISIBLE : View.GONE);
                if (item.iconRes != 0 && !item.smallIcon) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageResource(item.iconRes);
                    if (item.iconRes == R.mipmap.icon_pdf_pro || Boolean.TRUE.equals(item.active)) {
                        TintUtil.setNoTintImage(imageView);
                    } else if (Boolean.FALSE.equals(item.active)) {
                        TintUtil.setTintImageWithAlpha(imageView, Color.LTGRAY);
                    } else {
                        tintMenuIcon(imageView);
                    }
                } else if (item.drawable != null) {
                    profileLetter.setVisibility(View.VISIBLE);
                    profileLetter.setText(item.letter);
                    profileLetter.setBackground(item.drawable);
                    imageView.setVisibility(View.GONE);
                } else {
                    imageView.setVisibility(anyIcon ? View.INVISIBLE : View.GONE);
                }
                // Choices made on the row itself, under the label that names them. The menu stays
                // up when one is taken, so the rest can be set in the same go. Such rows and labels
                // are only as tall as they need; rows come back recycled, so the others get theirs back.
                // A slider row: shown only while what it sets is in use, else no height at all
                final FrameLayout extra = layout.findViewById(R.id.rowExtra);
                if (item.seek != null) {
                    final boolean shown = item.seek.shown();
                    textView.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    layout.findViewById(R.id.choices).setVisibility(View.GONE);
                    extra.setVisibility(shown ? View.VISIBLE : View.GONE);
                    if (shown) {
                        extra.removeAllViews();
                        final CustomSeek seek = new CustomSeek(c);
                        seek.init(item.seekMin, item.seekMax, item.seek.value(), "%");
                        seek.tint(menuIconColor());
                        seek.setOnSeekChanged(value -> {
                            item.seek.onValue(value);
                            return false;
                        });
                        extra.addView(seek, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                    ViewGroup.LayoutParams rowParams = layout.getLayoutParams();
                    if (rowParams != null) {
                        rowParams.height = shown ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
                        layout.setLayoutParams(rowParams);
                    }
                    layout.setPadding(Dips.DP_8, 0, Dips.DP_4, shown ? Dips.DP_4 : 0);
                    layout.setForeground(null);
                    layout.setOnClickListener(null);
                    layout.setOnLongClickListener(null);
                    layout.setClickable(false);
                    layout.setLongClickable(false);
                    return;
                }
                extra.removeAllViews();
                extra.setVisibility(View.GONE);

                ChoiceFlowLayout choices = (ChoiceFlowLayout) layout.findViewById(R.id.choices);
                choices.removeAllViews();
                final boolean hasChoices = item.choices != null || item.choiceIcons != null;
                choices.setVisibility(hasChoices ? View.VISIBLE : View.GONE);
                if (hasChoices) {
                    textView.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    final int count = item.choiceIcons != null ? item.choiceIcons.length : item.choices.length;
                    for (int i = 0; i < count; i++) {
                        choices.addView(choiceView(item, i, textView.getCurrentTextColor()));
                    }
                }
                if (item.isLabel) {
                    imageView.setVisibility(View.GONE);
                }
                ViewGroup.LayoutParams rowParams = layout.getLayoutParams();
                if (rowParams != null) {
                    rowParams.height = hasChoices || item.isLabel ? ViewGroup.LayoutParams.WRAP_CONTENT : Dips.dpToPx(42);
                    layout.setLayoutParams(rowParams);
                }
                layout.setPadding(Dips.DP_4, item.isLabel ? Dips.DP_8 : Dips.DP_4, Dips.DP_4, Dips.DP_4);

                // A line under the row parts it from the rows that follow. Rows come back recycled,
                // so the plain ones lose it again.
                if (item.lineBelow) {
                    ColorDrawable line = new ColorDrawable(isLightSurface ? 0x33000000 : 0x55FFFFFF);
                    LayerDrawable under = new LayerDrawable(new Drawable[]{line});
                    under.setLayerGravity(0, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL);
                    under.setLayerHeight(0, Dips.DP_1);
                    layout.setForeground(under);
                } else {
                    layout.setForeground(null);
                }

                if (item.isTitle || item.isLabel || hasChoices) {
                    layout.setOnClickListener(null);
                    layout.setOnLongClickListener(null);
                    layout.setClickable(false);
                    layout.setLongClickable(false);
                    return;
                }

                layout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (item.click != null) {
                            item.click.onMenuItemClick(new MyMenuItem(stringRes));
                        }
                        if (item.keepOpen) {
                            // the row may have changed what it says: drawn again, menu still up
                            notifyDataSetChanged();
                            return;
                        }
                        try {
                            p1.dismiss();
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                });

                layout.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        if (item.onLongClick == null) {
                            return false;
                        }
                        item.onLongClick.onMenuItemClick(new MyMenuItem(stringRes));
                        try {
                            p1.dismiss();
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                        return true;
                    }

                });

            }
        };

        if (anchor != null) {

            p1.setModal(true);
            p1.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss() {
                    p1.dismiss();
                    if (isTabsActivity) {
                        if (AppState.get().fullScreenMainMode == AppState.FULL_SCREEN_FULLSCREEN) {
                            Keyboards.hideNavigation((Activity) c);
                        }
                    } else {
                        if (AppState.get().fullScreenMode == AppState.FULL_SCREEN_FULLSCREEN) {
                            Keyboards.hideNavigation((Activity) c);
                        }
                    }
                    if (onDismissListener != null) {
                        onDismissListener.onDismiss();
                    }

                }

            });


            p1.setAnchorView(anchor);


            p1.setAdapter(a);

            try {
                // Never wider than the screen
                p1.setWidth(Math.min(measureContentWidth(a, c) + Dips.dpToPx(20), Dips.screenWidth() - Dips.dpToPx(16)));
            } catch (Exception e) {
                LOG.e(e);
                p1.setWidth(200);
            }
            if(isLong) {
                p1.setHeight(Dips.screenHeight() / 2 + Dips.dpToPx(80));
            }
            //p1.setWidth(ListPopupWindow.MATCH_PARENT);
            //p1.setDropDownGravity(Gravity.CENTER);

            try {
                p1.show();
            }catch (Exception e) {
                LOG.e(e);
            }

            if (pos != -1) {
                p1.setSelection(pos - 2);
            }
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(c);
            ListView list = new ListView(c);
            list.setDivider(null);
            list.setAdapter(a);
            builder.setView(list);
            builder.show();
        }

    }

    /**
     * One choice of a row, a word or an icon: plain, or on a light wash of the theme colour when
     * it is the one taken.
     */
    private View choiceView(final Menu item, final int index, int textColor) {
        final boolean taken = index == item.choiceSelected;
        final View chip;
        if (item.choiceIcons != null) {
            ImageView icon = new ImageView(c);
            icon.setImageResource(item.choiceIcons[index]);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setPadding(Dips.dpToPx(10), Dips.DP_4, Dips.dpToPx(10), Dips.DP_4);
            icon.setLayoutParams(new ViewGroup.LayoutParams(Dips.dpToPx(52), Dips.dpToPx(36)));
            tintMenuIcon(icon);
            chip = icon;
        } else {
            TextView text = new TextView(c);
            text.setText(item.choices[index]);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            text.setSingleLine(true);
            text.setGravity(Gravity.CENTER);
            text.setMinWidth(Dips.dpToPx(44));
            text.setMinHeight(Dips.dpToPx(36));
            text.setPadding(Dips.dpToPx(10), 0, Dips.dpToPx(10), 0);
            text.setTextColor(textColor);
            chip = text;
        }
        if (item.choiceDescriptions != null) {
            // Read out by a screen reader, and shown on a long press to whoever cannot tell
            // what an icon is from its picture
            chip.setContentDescription(item.choiceDescriptions[index]);
            TooltipCompat.setTooltipText(chip, item.choiceDescriptions[index]);
        }

        // No ring: the one taken stands on a see-through wash of the theme colour, in the round of
        // the chrome, its icon or word in the colour of the others
        final int accent = AppState.get().appTheme == AppState.THEME_INK ? Color.BLACK : TintUtil.color;
        final int radius = c.getResources().getDimensionPixelSize(R.dimen.radius);
        final GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        shape.setColor(taken ? ColorUtils.setAlphaComponent(accent, 0x40) : Color.TRANSPARENT);
        final GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(radius);
        mask.setColor(Color.WHITE);
        chip.setBackground(new RippleDrawable(ColorStateList.valueOf(0x66808080), shape, mask));
        chip.setSelected(taken);

        chip.setOnClickListener(v -> {
            item.choiceSelected = index;
            if (item.onChoice != null) {
                item.onChoice.onChoice(index);
            }
            notifyRows(chip);
            // A row may have come or gone with the choice (a slider): the menu takes its new height
            final ListPopupWindow popup = shownPopup;
            if (popup != null && popup.isShowing()) {
                chip.post(() -> {
                    if (popup.isShowing()) {
                        popup.show();
                    }
                });
            }
        });
        return chip;
    }

    /** Draws the rows of the list the view stands in again, its choice now taken. */
    private static void notifyRows(View inRow) {
        View v = inRow;
        while (v.getParent() instanceof View) {
            v = (View) v.getParent();
            if (v instanceof ListView) {
                ListAdapter adapter = ((ListView) v).getAdapter();
                if (adapter instanceof android.widget.BaseAdapter) {
                    ((android.widget.BaseAdapter) adapter).notifyDataSetChanged();
                }
                return;
            }
        }
    }

    private int measureContentWidth(ListAdapter listAdapter, Context mContext) {
        ViewGroup mMeasureParent = null;
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final ListAdapter adapter = listAdapter;
        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(mContext);
            }

            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();

            if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }

    public Context getContext() {
        return c;
    }

    public Menu getMenu() {
        Menu m = new Menu();
        list.add(m);
        return m;
    }
    public Menu getMenu(int pos) {
        Menu m = new Menu();
        list.add(pos, m);
        return m;
    }

    public Menu getMenu(int icon, int text, Runnable run) {
        Menu m = new Menu();
        m.setIcon(icon);
        m.add(text);
        m.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                run.run();
                return true;
            }
        });

        list.add(m);

        return m;
    }

    public void setAnchor(View anchor) {
        this.anchor = anchor;
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public interface OnChoice {
        void onChoice(int index);
    }

    /** A value set on a slider in its own row, shown only while it is in use. */
    public interface SeekRow {
        boolean shown();

        int value();

        void onValue(int value);
    }

    private ListPopupWindow shownPopup;

    public static class MyMenuItem implements MenuItem {
        String title;

        public MyMenuItem(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public MenuItem setIconTintList(@Nullable ColorStateList tint) {
            return null;
        }

        @Nullable
        @Override
        public ColorStateList getIconTintList() {
            return null;
        }

        @Override
        public MenuItem setIconTintMode(@Nullable PorterDuff.Mode tintMode) {
            return null;
        }

        @Nullable
        @Override
        public PorterDuff.Mode getIconTintMode() {
            return null;
        }

        @Override
        public MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers, int alphaModifiers) {
            return null;
        }

        @Override
        public MenuItem setNumericShortcut(char numericChar, int numericModifiers) {
            return null;
        }

        @Override
        public int getNumericModifiers() {
            return 0;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers) {
            return null;
        }

        @Override
        public int getAlphabeticModifiers() {
            return 0;
        }

        @Override
        public MenuItem setContentDescription(CharSequence contentDescription) {
            return null;
        }

        @Override
        public CharSequence getContentDescription() {
            return null;
        }

        @Override
        public MenuItem setTooltipText(CharSequence tooltipText) {
            return null;
        }

        @Override
        public CharSequence getTooltipText() {
            return null;
        }

        @Override
        public int getItemId() {
            return 0;
        }

        @Override
        public int getGroupId() {
            return 0;
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public MenuItem setTitle(CharSequence title) {
            return null;
        }

        @Override
        public MenuItem setTitle(int title) {
            return null;
        }

        @Override
        public MenuItem setTitleCondensed(CharSequence title) {
            return null;
        }

        @Override
        public CharSequence getTitleCondensed() {
            return null;
        }

        @Override
        public MenuItem setIcon(Drawable icon) {
            return null;
        }

        @Override
        public MenuItem setIcon(int iconRes) {
            return null;
        }

        @Override
        public Drawable getIcon() {
            return null;
        }

        @Override
        public MenuItem setIntent(Intent intent) {
            return null;
        }

        @Override
        public Intent getIntent() {
            return null;
        }

        @Override
        public MenuItem setShortcut(char numericChar, char alphaChar) {
            return null;
        }

        @Override
        public MenuItem setNumericShortcut(char numericChar) {
            return null;
        }

        @Override
        public char getNumericShortcut() {
            return 0;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char alphaChar) {
            return null;
        }

        @Override
        public char getAlphabeticShortcut() {
            return 0;
        }

        @Override
        public MenuItem setCheckable(boolean checkable) {
            return null;
        }

        @Override
        public boolean isCheckable() {
            return false;
        }

        @Override
        public MenuItem setChecked(boolean checked) {
            return null;
        }

        @Override
        public boolean isChecked() {
            return false;
        }

        @Override
        public MenuItem setVisible(boolean visible) {
            return null;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public MenuItem setEnabled(boolean enabled) {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public boolean hasSubMenu() {
            return false;
        }

        @Override
        public SubMenu getSubMenu() {
            return null;
        }

        @Override
        public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
            return null;
        }

        @Override
        public ContextMenu.ContextMenuInfo getMenuInfo() {
            return null;
        }

        @Override
        public void setShowAsAction(int actionEnum) {

        }

        @Override
        public MenuItem setShowAsActionFlags(int actionEnum) {
            return null;
        }

        @Override
        public MenuItem setActionView(View view) {
            return null;
        }

        @Override
        public MenuItem setActionView(int resId) {
            return null;
        }

        @Override
        public View getActionView() {
            return null;
        }

        @Override
        public MenuItem setActionProvider(ActionProvider actionProvider) {
            return null;
        }

        @Override
        public ActionProvider getActionProvider() {
            return null;
        }

        @Override
        public boolean expandActionView() {
            return false;
        }

        @Override
        public boolean collapseActionView() {
            return false;
        }

        @Override
        public boolean isActionViewExpanded() {
            return false;
        }

        @Override
        public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
            return null;
        }
    }

    public class Menu {
        String stringRes;
        int iconRes;
        Drawable drawable;
        String letter;
        OnMenuItemClickListener click;
        OnMenuItemClickListener onLongClick;
        Boolean active;
        boolean smallIcon;
        String checkboxString;
        boolean checkboxState;
        CompoundButton.OnCheckedChangeListener checkedChangeListener;
        boolean isTitle;
        boolean lineBelow;
        boolean keepOpen;
        boolean isLabel;
        String[] choices;
        int[] choiceIcons;
        String[] choiceDescriptions;
        int choiceSelected = -1;
        OnChoice onChoice;
        private String fontPath;

        /** Makes the row a small name over the choices under it, not there to be tapped. */
        public Menu asLabel() {
            this.isLabel = true;
            return this;
        }

        SeekRow seek;
        int seekMin, seekMax;

        /** Makes the row a slider from {@code min} to {@code max} percent. */
        public Menu setSeek(int min, int max, SeekRow seek) {
            this.seekMin = min;
            this.seekMax = max;
            this.seek = seek;
            return this;
        }

        /**
         * Makes the row a set of choices, {@code selected} the one taken (-1: none). A tap takes
         * one and leaves the menu up, drawn again.
         */
        public Menu setChoices(int selected, OnChoice onChoice, String... choices) {
            this.choices = choices;
            this.choiceSelected = selected;
            this.onChoice = onChoice;
            return this;
        }

        /** As {@link #setChoices}, each choice an icon: name them with {@link #setChoiceDescriptions}. */
        public Menu setChoiceIcons(int selected, OnChoice onChoice, int... icons) {
            this.choiceIcons = icons;
            this.choiceSelected = selected;
            this.onChoice = onChoice;
            return this;
        }

        /** What each choice is read out as, where it shows no words or only some. */
        public Menu setChoiceDescriptions(String... descriptions) {
            this.choiceDescriptions = descriptions;
            return this;
        }

        /** A tap on the row leaves the menu up, to be tapped again. */
        public Menu keepOpen() {
            this.keepOpen = true;
            return this;
        }

        public Menu setTitle(String name) {
            this.stringRes = name;
            return this;
        }

        /** Draws a line under the row, between it and the rows after it. */
        public Menu lineBelow() {
            this.lineBelow = true;
            return this;
        }

        /** Makes the row the popup's head: named, underlined, and not there to be tapped. */
        public Menu asTitle() {
            this.isTitle = true;
            return this;
        }

        public Menu add(int res) {
            if(res>0) {
                this.stringRes = c.getString(res);
            }
            return this;
        }

        public Menu add(String name) {
            if(TxtUtils.isNotEmpty(name)) {
                this.stringRes = name;
            }
            return this;
        }

        public Menu addCheckbox(String name, boolean state, CompoundButton.OnCheckedChangeListener listener) {
            this.checkboxString = name;
            this.checkboxState = state;
            // The menu outlives this call: shown again, it has to show the state it was left in
            this.checkedChangeListener = (button, checked) -> {
                this.checkboxState = checked;
                listener.onCheckedChanged(button, checked);
            };
            return this;
        }

        public Menu add(String name, String fontPath) {
            this.stringRes = name;
            this.fontPath = fontPath;
            return this;
        }

        public Menu setIcon(int res) {
            this.iconRes = res;
            return this;
        }

        /** An icon that marks a row rather than naming it: drawn small, and kept close to the name. */
        public Menu setIconSmall(int res) {
            this.iconRes = res;
            this.smallIcon = res != 0;
            return this;
        }

        public Menu setDrawable(String letter, Drawable d) {
            this.drawable = d;
            this.letter = letter;
            return this;
        }

        public Menu active(Boolean active) {
            this.active = active;
            return this;
        }

        public Menu setOnMenuItemClickListener(OnMenuItemClickListener onclick) {
            this.click = onclick;
            return this;
        }

        public Menu setOnMenuItemLongClickListener(OnMenuItemClickListener onLongClick) {
            this.onLongClick = onLongClick;
            return this;
        }

    }

}
