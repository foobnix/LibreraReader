package com.foobnix.pdf.info.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.ActionProvider;
import android.view.ContextMenu;
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
import com.foobnix.ui2.MainTabs2;

import java.util.ArrayList;
import java.util.List;

public class MyPopupMenu {
    Context c;
    List<Menu> list = new ArrayList<Menu>();
    private View anchor;
    private boolean isTabsActivity;
    private OnDismissListener onDismissListener;

    public MyPopupMenu(Context c, View anchor) {
        this.c = c;
        this.anchor = anchor;
        isTabsActivity = c instanceof MainTabs2;


    }

    public MyPopupMenu(View anchor) {
        this.c = anchor.getContext();
        this.anchor = anchor;
        isTabsActivity = c instanceof MainTabs2;


    }

    public void show() {
        show(-1, false);
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
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

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


                ImageView imageView = (ImageView) layout.findViewById(R.id.image1);
                imageView.setVisibility(View.GONE);
                if (item.iconRes != 0) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageResource(item.iconRes);
                    if (item.iconRes == R.mipmap.icon_pdf_pro || Boolean.TRUE.equals(item.active)) {
                        TintUtil.setNoTintImage(imageView);
                    } else if (Boolean.FALSE.equals(item.active)) {
                        TintUtil.setTintImageWithAlpha(imageView, Color.LTGRAY);
                    } else {
                        if (isTabsActivity) {
                            if (AppState.get().appTheme == AppState.THEME_INK || AppState.get().appTheme == AppState.THEME_LIGHT) {
                                TintUtil.setTintImageWithAlpha(imageView, TintUtil.color);
                            } else {
                                TintUtil.setTintImageWithAlpha(imageView, Color.LTGRAY);
                            }
                        } else {

                            if (AppState.get().isDayNotInvert) {
                                TintUtil.setTintImageWithAlpha(imageView, TintUtil.color);
                            } else {
                                TintUtil.setTintImageWithAlpha(imageView, Color.LTGRAY);
                            }
                        }
                    }
                } else if (item.drawable != null) {
                    profileLetter.setVisibility(View.VISIBLE);
                    profileLetter.setText(item.letter);
                    profileLetter.setBackgroundDrawable(item.drawable);
                    imageView.setVisibility(View.GONE);
                } else {
                    imageView.setVisibility(View.GONE);
                }
                layout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (item.click != null) {
                            item.click.onMenuItemClick(new MyMenuItem(stringRes));
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
                p1.setWidth(measureContentWidth(a, c) + Dips.dpToPx(20));
            } catch (Exception e) {
                LOG.e(e);
                p1.setWidth(200);
            }
            if(isLong) {
                p1.setHeight(Dips.screenHeight() / 2 + Dips.dpToPx(80));
            }
            //p1.setWidth(ListPopupWindow.MATCH_PARENT);
            //p1.setDropDownGravity(Gravity.CENTER);

            p1.show();

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
        String checkboxString;
        boolean checkboxState;
        CompoundButton.OnCheckedChangeListener checkedChangeListener;
        private String fontPath;

        public Menu add(int res) {
            this.stringRes = c.getString(res);
            return this;
        }

        public Menu add(String name) {
            this.stringRes = name;
            return this;
        }

        public Menu addCheckbox(String name, boolean state, CompoundButton.OnCheckedChangeListener listener) {
            this.checkboxString = name;
            this.checkboxState = state;
            this.checkedChangeListener = listener;
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
