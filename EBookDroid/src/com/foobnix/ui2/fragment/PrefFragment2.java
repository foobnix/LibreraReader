package com.foobnix.ui2.fragment;

import com.buzzingandroid.ui.HSVColorPickerDialog;
import com.buzzingandroid.ui.HSVColorPickerDialog.OnColorSelectedListener;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.AndroidWhatsNew;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.fragment.KeyCodeDialog;
import com.foobnix.pdf.info.fragment.SearchFragment;
import com.foobnix.pdf.info.view.CustomSeek;
import com.foobnix.pdf.info.view.MultyDocSearchDialog;
import com.foobnix.pdf.info.widget.ColorsDialog;
import com.foobnix.pdf.info.widget.ColorsDialog.ColorsDialogResult;
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.MainTabs2;
import com.jmedeisis.draglinearlayout.DragLinearLayout;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class PrefFragment2 extends UIFragment {

    private static final String WWW_SITE = "http://lirbi.com";
    private static final String WWW_BETA_SITE = "http://beta.lirbi.com";
    private static final String WWW_ARCHIVE_SITE = "http://archive.lirbi.com";
    private SearchFragment searchFragmet;
    private TextView curBrightness;
    private KeyCodeDialog keyCodeDialog;
    private CheckBox isRememberDictionary;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return new Pair<Integer, Integer>(R.string.preferences, R.drawable.glyphicons_281_settings);
    }

    @Override
    public boolean isBackPressed() {
        return false;
    }

    @Override
    public void onSelectFragment() {
    }

    @Override
    public void notifyFragment() {
    }

    @Override
    public void resetFragment() {
    }

    @Override
    public void onTintChanged() {
        TintUtil.setStatusBarColor(getActivity(), TintUtil.color);
        TintUtil.setBackgroundFillColor(section1, TintUtil.color);
        TintUtil.setBackgroundFillColor(section2, TintUtil.color);
        TintUtil.setBackgroundFillColor(section3, TintUtil.color);
        TintUtil.setBackgroundFillColor(section4, TintUtil.color);
        TintUtil.setBackgroundFillColor(section5, TintUtil.color);
        TintUtil.setBackgroundFillColor(section6, TintUtil.color);
        TintUtil.setBackgroundFillColor(section7, TintUtil.color);

    }

    View section1, section2, section3, section4, section5, section6, section7;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View inflate = inflater.inflate(R.layout.preferences, container, false);

        // tabs position
        final DragLinearLayout dragLinearLayout = (DragLinearLayout) inflate.findViewById(R.id.dragLinearLayout);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2));

        final Runnable dragLinear = new Runnable() {

            @Override
            public void run() {
                dragLinearLayout.removeAllViews();
                for (UITab tab : UITab.getOrdered(AppState.get().tabsOrder)) {
                    View library = LayoutInflater.from(getActivity()).inflate(R.layout.item_tab_line, null, false);
                    ((TextView) library.findViewById(R.id.text1)).setText(tab.getName());
                    ((CheckBox) library.findViewById(R.id.isVisible)).setChecked(tab.isVisible());
                    ((ImageView) library.findViewById(R.id.image1)).setImageResource(tab.getIcon());
                    TintUtil.setTintImage(((ImageView) library.findViewById(R.id.image1)), TintUtil.COLOR_TINT_GRAY);
                    library.setTag(tab.getIndex());
                    dragLinearLayout.addView(library, layoutParams);
                }

                for (int i = 0; i < dragLinearLayout.getChildCount(); i++) {
                    View child = dragLinearLayout.getChildAt(i);
                    View handle = child.findViewById(R.id.imageDrag);
                    dragLinearLayout.setViewDraggable(child, handle);
                }
            }
        };
        dragLinear.run();
        TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.tabsApply)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().tabsOrder = "";
                for (int i = 0; i < dragLinearLayout.getChildCount(); i++) {
                    View child = dragLinearLayout.getChildAt(i);
                    boolean isVisible = ((CheckBox) child.findViewById(R.id.isVisible)).isChecked();
                    AppState.get().tabsOrder += child.getTag() + "#" + (isVisible ? "1" : "0") + ",";
                }
                AppState.get().tabsOrder = TxtUtils.replaceLast(AppState.get().tabsOrder, ",", "");
                LOG.d("tabsApply", AppState.get().tabsOrder);
                AppState.get().save(getActivity());
                onTheme();

            }
        });

        final CheckBox isshowPrefAsMenu = (CheckBox) inflate.findViewById(R.id.isshowPrefAsMenu);
        isshowPrefAsMenu.setChecked(AppState.get().tabsOrder.contains("4#0"));
        isshowPrefAsMenu.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppState.get().tabsOrder = AppState.get().tabsOrder.replace("4#1", "4#0");
                } else {
                    AppState.get().tabsOrder = AppState.get().tabsOrder.replace("4#0", "4#1");
                }

                dragLinear.run();

            }
        });

        TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.tabsDefaul)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().tabsOrder = AppState.DEFAULTS_TABS_ORDER;
                isshowPrefAsMenu.setChecked(false);
                dragLinear.run();
            }
        });

        // tabs position

        section1 = inflate.findViewById(R.id.section1);
        section2 = inflate.findViewById(R.id.section2);
        section3 = inflate.findViewById(R.id.section3);
        section4 = inflate.findViewById(R.id.section4);
        section5 = inflate.findViewById(R.id.section5);
        section6 = inflate.findViewById(R.id.section6);
        section7 = inflate.findViewById(R.id.section7);

        onTintChanged();

        final int max = Dips.pxToDp(Dips.screenMinWH() / 2) - 2 * 4;

        CustomSeek coverSmallSize = (CustomSeek) inflate.findViewById(R.id.coverSmallSize);
        coverSmallSize.init(40, max, AppState.get().coverSmallSize);

        coverSmallSize.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                TempHolder.listHash++;
                AppState.get().coverSmallSize = result;
                return false;
            }
        });

        final CustomSeek coverBigSize = (CustomSeek) inflate.findViewById(R.id.coverBigSize);
        coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize), AppState.get().coverBigSize);
        coverBigSize.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                TempHolder.listHash++;
                AppState.get().coverBigSize = result;
                return false;
            }
        });

        final TextView columsCount = (TextView) inflate.findViewById(R.id.columsCount);
        columsCount.setText("" + Dips.screenWidthDP() / AppState.get().coverBigSize);
        TxtUtils.underlineTextView(columsCount);
        columsCount.setOnClickListener(new OnClickListener() {

            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                PopupMenu p = new PopupMenu(getContext(), columsCount);
                for (int i = 1; i <= 8; i++) {
                    final int k = i;
                    p.getMenu().add("" + k).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int result = Dips.screenWidthDP() / k - 8;

                            TempHolder.listHash++;
                            AppState.get().coverBigSize = result;

                            columsCount.setText("" + k);
                            TxtUtils.underlineTextView(columsCount);

                            coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize), AppState.get().coverBigSize);
                            return false;
                        }
                    });
                }

                p.show();
            }
        });

        final ScrollView scrollView = (ScrollView) inflate.findViewById(R.id.scroll);
        scrollView.setVerticalScrollBarEnabled(false);

        ((TextView) inflate.findViewById(R.id.section6)).setText(String.format("%s: %s", getString(R.string.product), AppsConfig.APP_NAME));
        // ((TextView) findViewById(R.id.appName)).setText(AppsConfig.APP_NAME);

        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            final String version = packageInfo.versionName;
            final int code = packageInfo.versionCode;
            ((TextView) inflate.findViewById(R.id.pVersion)).setText(String.format("%s: %s-%s", getString(R.string.version), version, code));
        } catch (final NameNotFoundException e) {
        }

        TextView onCloseApp = (TextView) inflate.findViewById(R.id.onCloseApp);
        onCloseApp.setText(Html.fromHtml("<u>" + getString(R.string.close) + "</u"));
        onCloseApp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ((MainTabs2) getActivity()).closeActivity();
            }
        });
        inflate.findViewById(R.id.onImageClose).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ((MainTabs2) getActivity()).closeActivity();
            }
        });

        inflate.findViewById(R.id.onFullScreen).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                AppState.getInstance().setFullScrean(!AppState.getInstance().isFullScrean());
                DocumentController.chooseFullScreen(getActivity(), AppState.getInstance().isFullScrean());
                fullScreenText();

            }
        });

        final View onScreenMode = inflate.findViewById(R.id.onScreenMode);
        onScreenMode.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                PopupMenu p = new PopupMenu(getContext(), onScreenMode);
                p.getMenu().add(R.string.automatic).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                        rotationText();
                        DocumentController.doRotation(getActivity());
                        return false;
                    }
                });
                p.getMenu().add(R.string.landscape).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                        rotationText();
                        DocumentController.doRotation(getActivity());
                        return false;
                    }
                });

                p.getMenu().add(R.string.portrait).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                        rotationText();
                        DocumentController.doRotation(getActivity());
                        return false;
                    }
                });
                p.show();

            }
        });

        // inflate.findViewById(R.id.onHelpTranslate).setOnClickListener(new
        // OnClickListener() {
        //
        // @Override
        // public void onClick(final View v) {
        // Urls.open(getActivity(),
        // "https://www.dropbox.com/sh/8el7kon2sbx46w8/xm3qoHYT7n");
        // }
        // });

        inflate.findViewById(R.id.onKeyCode).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                keyCodeDialog = new KeyCodeDialog(getActivity(), onCloseDialog);
            }
        });
        final View theme = inflate.findViewById(R.id.themeColor);
        theme.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {

                PopupMenu p = new PopupMenu(getContext(), theme);
                p.getMenu().add(R.string.light).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().isWhiteTheme = true;
                        onTheme();
                        return false;
                    }
                });
                p.getMenu().add(R.string.black).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().isWhiteTheme = false;
                        onTheme();
                        return false;
                    }
                });
                p.show();
            }
        });

        final TextView onMail = (TextView) inflate.findViewById(R.id.onMailSupport);
        onMail.setText(TxtUtils.underline(getString(R.string.my_email)));

        onMail.setOnClickListener(new OnClickListener() {
            @Override

            public void onClick(final View v) {
                if (AppsConfig.checkIsProInstalled(getActivity())) {
                    onEmail();
                } else {
                    Toast.makeText(getContext(), R.string.please_buy_pro_version_to_use_this_setting, Toast.LENGTH_LONG).show();
                }

            }
        });

        CheckBox isFirstSurname = (CheckBox) inflate.findViewById(R.id.isFirstSurname);
        isFirstSurname.setChecked(AppState.get().isFirstSurname);
        isFirstSurname.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isFirstSurname = isChecked;

            }
        });

        autoSettings = (CheckBox) inflate.findViewById(R.id.autoSettings);
        autoSettings.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                onDefaultBr(isChecked);
                saveChanges();
            }
        });

        rememberMode = (CheckBox) inflate.findViewById(R.id.isRememberMode);
        rememberMode.setChecked(AppState.getInstance().isRememberMode);
        rememberMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isRememberMode = isChecked;
            }
        });

        final TextView widgetForRecent = (TextView) inflate.findViewById(R.id.widgetForRecent);
        widgetForRecent.setText(AppState.get().isStarsInWidget ? R.string.starred : R.string.recent);
        TxtUtils.underlineTextView(widgetForRecent);

        widgetForRecent.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(widgetForRecent.getContext(), widgetForRecent);

                final MenuItem recent = popupMenu.getMenu().add(R.string.recent);
                recent.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.get().isStarsInWidget = false;
                        widgetForRecent.setText(AppState.get().isStarsInWidget ? R.string.starred : R.string.recent);
                        TxtUtils.underlineTextView(widgetForRecent);
                        return false;
                    }
                });

                final MenuItem starred = popupMenu.getMenu().add(R.string.starred);
                starred.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.get().isStarsInWidget = true;
                        widgetForRecent.setText(AppState.get().isStarsInWidget ? R.string.starred : R.string.recent);
                        TxtUtils.underlineTextView(widgetForRecent);
                        return false;
                    }
                });

                popupMenu.show();

            }

        });

        selectedOpenMode = (TextView) inflate.findViewById(R.id.selectedOpenMode);
        selectedOpenMode.setOnClickListener(new OnClickListener() {

            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT <= 10) {
                    Toast.makeText(selectedOpenMode.getContext(), R.string.this_function_will_works_in_modern_android, Toast.LENGTH_SHORT).show();
                    return;
                }
                final PopupMenu popupMenu = new PopupMenu(selectedOpenMode.getContext(), selectedOpenMode);

                final MenuItem advanced_mode = popupMenu.getMenu().add(R.string.advanced_mode);
                advanced_mode.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.getInstance().isAlwaysOpenAsMagazine = false;
                        AppState.getInstance().isMusicianMode = false;
                        checkOpenWithSpinner();
                        return false;
                    }
                });

                final MenuItem easy_mode = popupMenu.getMenu().add(R.string.easy_mode);
                easy_mode.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.getInstance().isAlwaysOpenAsMagazine = true;
                        AppState.getInstance().isMusicianMode = false;
                        checkOpenWithSpinner();
                        return false;
                    }
                });
                final MenuItem music_mode = popupMenu.getMenu().add(R.string.music_mode);
                music_mode.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.getInstance().isAlwaysOpenAsMagazine = false;
                        AppState.getInstance().isMusicianMode = true;
                        checkOpenWithSpinner();
                        return false;
                    }
                });
                popupMenu.show();

            }
        });

        checkOpenWithSpinner();

        final CheckBox isCropBookCovers = (CheckBox) inflate.findViewById(R.id.isCropBookCovers);
        isCropBookCovers.setOnCheckedChangeListener(null);
        isCropBookCovers.setChecked(AppState.getInstance().isCropBookCovers);
        isCropBookCovers.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isCropBookCovers = isChecked;
                TempHolder.listHash++;

            }
        });

        CheckBox isBookCoverEffect = (CheckBox) inflate.findViewById(R.id.isBookCoverEffect);
        isBookCoverEffect.setOnCheckedChangeListener(null);
        isBookCoverEffect.setChecked(AppState.getInstance().isBookCoverEffect);
        isBookCoverEffect.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isBookCoverEffect = isChecked;
                ImageLoader.getInstance().clearMemoryCache();
                ImageLoader.getInstance().clearDiskCache();
                TempHolder.listHash++;
                if (isChecked) {
                    isCropBookCovers.setEnabled(false);
                    isCropBookCovers.setChecked(true);
                } else {
                    isCropBookCovers.setEnabled(true);
                }
            }
        });

        final CheckBox isBorderAndShadow = (CheckBox) inflate.findViewById(R.id.isBorderAndShadow);
        isBorderAndShadow.setOnCheckedChangeListener(null);
        isBorderAndShadow.setChecked(AppState.getInstance().isBorderAndShadow);
        isBorderAndShadow.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isBorderAndShadow = isChecked;
                TempHolder.listHash++;

            }
        });

        CheckBox isLoopAutoplay = (CheckBox) inflate.findViewById(R.id.isLoopAutoplay);
        isLoopAutoplay.setChecked(AppState.getInstance().isLoopAutoplay);
        isLoopAutoplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isLoopAutoplay = isChecked;
            }
        });

        CheckBox isOpenLastBook = (CheckBox) inflate.findViewById(R.id.isOpenLastBook);
        isOpenLastBook.setChecked(AppState.getInstance().isOpenLastBook);
        isOpenLastBook.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isOpenLastBook = isChecked;
            }
        });

        CheckBox isShowCloseAppDialog = (CheckBox) inflate.findViewById(R.id.isShowCloseAppDialog);
        isShowCloseAppDialog.setChecked(AppState.getInstance().isShowCloseAppDialog);
        isShowCloseAppDialog.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isShowCloseAppDialog = isChecked;
            }
        });

        ////
        ((CheckBox) inflate.findViewById(R.id.supportPDF)).setChecked(AppState.getInstance().supportPDF);
        ((CheckBox) inflate.findViewById(R.id.supportPDF)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportPDF = isChecked;
                ExtUtils.updateSearchExts();
            }
        });
        ((CheckBox) inflate.findViewById(R.id.supportDJVU)).setChecked(AppState.getInstance().supportDJVU);
        ((CheckBox) inflate.findViewById(R.id.supportDJVU)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportDJVU = isChecked;
                ExtUtils.updateSearchExts();
            }
        });
        ((CheckBox) inflate.findViewById(R.id.supportEPUB)).setChecked(AppState.getInstance().supportEPUB);
        ((CheckBox) inflate.findViewById(R.id.supportEPUB)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportEPUB = isChecked;
                ExtUtils.updateSearchExts();
            }
        });
        ((CheckBox) inflate.findViewById(R.id.supportFB2)).setChecked(AppState.getInstance().supportFB2);
        ((CheckBox) inflate.findViewById(R.id.supportFB2)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportFB2 = isChecked;
                ExtUtils.updateSearchExts();
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportTXT)).setChecked(AppState.getInstance().supportTXT);
        ((CheckBox) inflate.findViewById(R.id.supportTXT)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportTXT = isChecked;
                ExtUtils.updateSearchExts();
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportMOBI)).setChecked(AppState.getInstance().supportMOBI);
        ((CheckBox) inflate.findViewById(R.id.supportMOBI)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportMOBI = isChecked;
                ExtUtils.updateSearchExts();
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportRTF)).setChecked(AppState.getInstance().supportRTF);
        ((CheckBox) inflate.findViewById(R.id.supportRTF)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportRTF = isChecked;
                ExtUtils.updateSearchExts();
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportCBZ)).setChecked(AppState.getInstance().supportCBZ);
        ((CheckBox) inflate.findViewById(R.id.supportCBZ)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportCBZ = isChecked;
                ExtUtils.updateSearchExts();
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportZIP)).setChecked(AppState.getInstance().supportZIP);
        ((CheckBox) inflate.findViewById(R.id.supportZIP)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportZIP = isChecked;
                ExtUtils.updateSearchExts();
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportOther)).setChecked(AppState.getInstance().supportOther);
        ((CheckBox) inflate.findViewById(R.id.supportOther)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().supportOther = isChecked;
                ExtUtils.updateSearchExts();
            }
        });
        // What is new
        ((CheckBox) inflate.findViewById(R.id.isShowWhatIsNewDialog)).setChecked(AppState.getInstance().isShowWhatIsNewDialog);
        ((CheckBox) inflate.findViewById(R.id.isShowWhatIsNewDialog)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isShowWhatIsNewDialog = isChecked;
            }
        });
        final TextView whatIsNew = (TextView) inflate.findViewById(R.id.whatIsNew);
        TxtUtils.underlineTextView(whatIsNew);
        whatIsNew.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AndroidWhatsNew.show(getActivity());

            }
        });

        ///

        final SeekBar mouseSpeed = (SeekBar) inflate.findViewById(R.id.seekWheelSpeed);
        mouseSpeed.setMax(200);
        mouseSpeed.setProgress(AppState.getInstance().mouseWheelSpeed);
        mouseSpeed.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                AppState.getInstance().mouseWheelSpeed = progress;
                LOG.d("TEST", "SET speed" + progress);
            }
        });

        curBrightness = (TextView) inflate.findViewById(R.id.curBrigtness);
        bar = (SeekBar) inflate.findViewById(R.id.seekBrightness);

        initBrigtness();

        nextKeys = (TextView) inflate.findViewById(R.id.textNextKeys);
        prevKeys = (TextView) inflate.findViewById(R.id.textPrevKeys);

        ch = (CheckBox) inflate.findViewById(R.id.onReverse);
        ch.setOnCheckedChangeListener(null);
        ch.setChecked(AppState.getInstance().isReverseKeys);
        ch.setOnCheckedChangeListener(reverseListener);

        inflate.findViewById(R.id.onColorChoser).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
            }
        });
        initKeys();

        searchPaths = (TextView) inflate.findViewById(R.id.searchPaths);
        searchPaths.setText(TxtUtils.underline(AppState.getInstance().searchPaths.replace(",", "<br>")));
        searchPaths.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onFolderConfigDialog();
            }
        });

        inflate.findViewById(R.id.onConfigPath).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                onFolderConfigDialog();
            }
        });

        TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.importButton)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                PrefDialogs.importDialog(getActivity());
            }
        });

        TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.exportButton)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                PrefDialogs.exportDialog(getActivity());
            }
        });

        // Widget Configuration

        final RadioButton widgetList = (RadioButton) inflate.findViewById(R.id.checkBoxWigetList);
        final RadioButton widgetGrid = (RadioButton) inflate.findViewById(R.id.checkBoxWidgetGrid);

        if (Build.VERSION.SDK_INT <= 15) {
            widgetGrid.setVisibility(View.GONE);
        }
        widgetList.setChecked(AppState.getInstance().widgetType == AppState.WIDGET_LIST);
        widgetList.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    AppState.getInstance().widgetType = AppState.WIDGET_LIST;
                }
            }
        });

        widgetGrid.setChecked(AppState.getInstance().widgetType == AppState.WIDGET_GRID);
        widgetGrid.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    AppState.getInstance().widgetType = AppState.WIDGET_GRID;
                }
            }
        });

        final TextView widgetItemsCount = (TextView) inflate.findViewById(R.id.widgetItemsCount);
        widgetItemsCount.setText("" + AppState.getInstance().widgetItemsCount);
        TxtUtils.underlineTextView(widgetItemsCount);
        widgetItemsCount.setOnClickListener(new OnClickListener() {

            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                PopupMenu p = new PopupMenu(getContext(), columsCount);
                for (int i = 1; i <= 50; i++) {
                    final int k = i;
                    p.getMenu().add("" + k).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            AppState.getInstance().widgetItemsCount = k;
                            widgetItemsCount.setText("" + k);
                            TxtUtils.underlineTextView(widgetItemsCount);
                            return false;
                        }
                    });
                }

                p.show();
            }
        });

        // dictionary
        widgetGrid.setChecked(AppState.getInstance().widgetType == AppState.WIDGET_GRID);
        widgetGrid.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    AppState.getInstance().widgetType = AppState.WIDGET_GRID;
                }
            }
        });

        isRememberDictionary = (CheckBox) inflate.findViewById(R.id.isRememberDictionary);
        isRememberDictionary.setChecked(AppState.getInstance().isRememberDictionary);
        isRememberDictionary.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.getInstance().isRememberDictionary = isChecked;
            }
        });

        selectedDictionaly = (TextView) inflate.findViewById(R.id.selectedDictionaly);
        selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
        selectedDictionaly.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DialogTranslateFromTo.show(getActivity(), new Runnable() {

                    @Override
                    public void run() {
                        selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
                    }
                });
            }
        });

        textDayColor = (TextView) inflate.findViewById(R.id.onDayColor);
        textDayColor.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new ColorsDialog(getActivity(), true, AppState.get().colorDayText, AppState.get().colorDayBg, false, new ColorsDialogResult() {

                    @Override
                    public void onChooseColor(int colorText, int colorBg) {
                        textDayColor.setTextColor(colorText);
                        textDayColor.setBackgroundColor(colorBg);

                        AppState.get().colorDayText = colorText;
                        AppState.get().colorDayBg = colorBg;

                        ImageLoader.getInstance().clearDiskCache();
                        ImageLoader.getInstance().clearMemoryCache();
                    }
                });
            }
        });

        textNigthColor = (TextView) inflate.findViewById(R.id.onNigthColor);
        textNigthColor.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new ColorsDialog(getActivity(), false, AppState.get().colorNigthText, AppState.get().colorNigthBg, false, new ColorsDialogResult() {

                    @Override
                    public void onChooseColor(int colorText, int colorBg) {
                        textNigthColor.setTextColor(colorText);
                        textNigthColor.setBackgroundColor(colorBg);

                        AppState.get().colorNigthText = colorText;
                        AppState.get().colorNigthBg = colorBg;

                    }
                });
            }
        });

        TextView onDefalt = TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.onDefaultColor));
        onDefalt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().colorDayText = AppState.COLOR_BLACK;
                AppState.get().colorDayBg = AppState.COLOR_WHITE;

                textDayColor.setTextColor(AppState.COLOR_BLACK);
                textDayColor.setBackgroundColor(AppState.COLOR_WHITE);

                AppState.get().colorNigthText = AppState.COLOR_WHITE;
                AppState.get().colorNigthBg = AppState.COLOR_BLACK;

                textNigthColor.setTextColor(AppState.COLOR_WHITE);
                textNigthColor.setBackgroundColor(AppState.COLOR_BLACK);
            }
        });

        inflate.findViewById(R.id.iconRandom).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                TintUtil.tintRandomColor();
                onTintChanged();
                sendNotifyTintChanged();

                AppState.get().save(getActivity());

                TempHolder.listHash++;
            }
        });

        LinearLayout colorsLine = (LinearLayout) inflate.findViewById(R.id.colorsLine);
        colorsLine.removeAllViews();

        for (String color : AppState.STYLE_COLORS) {
            View view = inflater.inflate(R.layout.item_color, (ViewGroup) inflate, false);
            view.setBackgroundColor(Color.TRANSPARENT);
            final int intColor = Color.parseColor(color);
            final View img = view.findViewById(R.id.itColor);
            img.setBackgroundColor(intColor);

            colorsLine.addView(view, new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

            view.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    TintUtil.color = intColor;
                    AppState.get().tintColor = intColor;
                    onTintChanged();
                    sendNotifyTintChanged();

                    AppState.get().save(getActivity());

                    TempHolder.listHash++;

                }
            });

        }

        View view = inflater.inflate(R.layout.item_color, (ViewGroup) inflate, false);
        view.setBackgroundColor(Color.TRANSPARENT);
        final ImageView img = (ImageView) view.findViewById(R.id.itColor);
        img.setColorFilter(getResources().getColor(R.color.tint_gray));
        img.setImageResource(R.drawable.glyphicons_433_plus);
        img.setBackgroundColor(AppState.get().userColor);
        colorsLine.addView(view, new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new HSVColorPickerDialog(getContext(), AppState.get().userColor, new OnColorSelectedListener() {

                    @Override
                    public void colorSelected(Integer color) {
                        AppState.get().userColor = color;
                        AppState.get().tintColor = color;
                        TintUtil.color = color;
                        img.setBackgroundColor(color);

                        onTintChanged();
                        sendNotifyTintChanged();

                        AppState.get().save(getActivity());

                        TempHolder.listHash++;

                    }
                }).show();

            }
        });

        underline(inflate.findViewById(R.id.onRateIt)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                rateIT();
            }
        });
        underline(inflate.findViewById(R.id.openWeb)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                Urls.open(getActivity(), WWW_SITE);
            }
        });
        underline(inflate.findViewById(R.id.openBeta)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                Urls.open(getActivity(), WWW_BETA_SITE);
            }
        });
        underline(inflate.findViewById(R.id.openArchive)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                Urls.open(getActivity(), WWW_ARCHIVE_SITE);
            }
        });

        TextView proText = (TextView) inflate.findViewById(R.id.downloadPRO);
        TxtUtils.underlineTextView(proText);
        proText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                Urls.openPdfPro(getActivity());
            }
        });

        if (AppsConfig.checkIsProInstalled(getActivity())) {
            proText.setVisibility(View.GONE);
        }

        inflate.findViewById(R.id.cleanRecent).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                builder.setMessage(getString(R.string.clear_all_recent) + "?");
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppSharedPreferences.get().cleanRecent();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub

                    }
                });
                builder.show();
            }
        });

        inflate.findViewById(R.id.cleanBookmarks).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                builder.setMessage(getString(R.string.clear_all_bookmars) + "?");
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppSharedPreferences.get().cleanBookmarks();

                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub

                    }
                });
                builder.show();

            }
        });

        // tutorials

        final String displayLanguage = Urls.getLangCode();
        boolean containsKey = AppState.getUserGuides().containsKey(displayLanguage);

        TextView tutorialLink = (TextView) inflate.findViewById(R.id.tutorialLink);
        LinearLayout tutorialLayout = (LinearLayout) inflate.findViewById(R.id.tutorialLayout);
        tutorialLayout.setVisibility(containsKey ? View.VISIBLE : View.GONE);
        section5.setVisibility(containsKey ? View.VISIBLE : View.GONE);

        final String web = "http://" + displayLanguage + ".lirbi.com";
        tutorialLink.setText(web);

        tutorialLink.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                Urls.open(getActivity(), web);
            }
        });

        // licences link
        underline(inflate.findViewById(R.id.libraryLicenses)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.licenses_for_libraries);

                WebView wv = new WebView(getActivity());
                wv.loadUrl("file:///android_asset/licenses.html");
                wv.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });

                alert.setView(wv);
                alert.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                alert.show();
            }
        });

        TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.docSearch)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                MultyDocSearchDialog.show(getActivity());
            }
        });
        // convert
        final TextView docConverter = (TextView) inflate.findViewById(R.id.docConverter);
        TxtUtils.underlineTextView(docConverter);
        docConverter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PopupMenu p = new PopupMenu(getContext(), v);
                for (final String id : AppState.CONVERTERS.keySet()) {
                    p.getMenu().add("" + getActivity().getString(R.string.convert_to) + " " + id).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            ShareDialog.showsItemsDialog(getActivity(), getActivity().getString(R.string.convert_to) + " " + id, AppState.CONVERTERS.get(id));

                            return false;
                        }
                    });

                }
                p.show();
            }
        });

        return inflate;

    }

    OnCheckedChangeListener reverseListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            AppState.getInstance().isReverseKeys = isChecked;
            initKeys();
            saveChanges();
            LOG.d("Save Changes", 3);
        }
    };

    public void initBrigtness() {
        float curBr = AppState.getInstance().brightness;
        if (curBr == 0.0) {
            curBr = DocumentController.getSystemBrigtness(getActivity());
            bar.setEnabled(false);
            autoSettings.setChecked(true);
        } else {
            autoSettings.setChecked(false);
            bar.setOnSeekBarChangeListener(onSeek);
            bar.setEnabled(true);
        }

        bar.setMax(100);
        final int intBrightness = (int) (curBr * 100);
        bar.setProgress(intBrightness);
        curBrightness.setText("" + intBrightness);
    }

    public View underline(View text) {
        CharSequence myText = ((TextView) text).getText();
        ((TextView) text).setText(Html.fromHtml("<u>" + myText + "</u>"));
        return text;
    }

    private void checkOpenWithSpinner() {
        int modId = R.string.advanced_mode;
        if (AppState.getInstance().isMusicianMode) {
            modId = R.string.music_mode;
        } else if (AppState.getInstance().isAlwaysOpenAsMagazine) {
            modId = R.string.easy_mode;
        } else {
            modId = R.string.advanced_mode;
        }

        selectedOpenMode.setText(TxtUtils.underline(getString(modId)));
    }

    public void onFolderConfigDialog() {
        AppState.getInstance().searchPaths = AppState.getInstance().searchPaths.replace("//", "/");
        PrefDialogs.chooseFolderDialog(getActivity(), new Runnable() {

            @Override
            public void run() {
                AppState.getInstance().searchPaths = AppState.getInstance().searchPaths.replace("//", "/");
                searchPaths.setText(TxtUtils.underline(AppState.getInstance().searchPaths.replace(",", "<br>")));
                saveChanges();
                LOG.d("Save Changes", 2);
            }
        }, new Runnable() {

            @Override
            public void run() {
                onScan();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        rotationText();

        fullScreenText();

        ch.setOnCheckedChangeListener(null);
        ch.setChecked(AppState.getInstance().isReverseKeys);
        ch.setOnCheckedChangeListener(reverseListener);

        rememberMode.setChecked(AppState.getInstance().isRememberMode);
        checkOpenWithSpinner();

        textNigthColor.setTextColor(AppState.get().colorNigthText);
        textNigthColor.setBackgroundColor(AppState.get().colorNigthBg);

        textDayColor.setTextColor(AppState.get().colorDayText);
        textDayColor.setBackgroundColor(AppState.get().colorDayBg);

        isRememberDictionary.setChecked(AppState.getInstance().isRememberDictionary);
        selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());

        initBrigtness();

    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            RecentUpates.updateAll(getActivity());
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void onColorChoose() {

    }

    public void onDefaultBr(final boolean isAuto) {
        if (isAuto) {
            final float val = DocumentController.getSystemBrigtness(getActivity());
            bar.setProgress((int) (val * 100));
            bar.setOnSeekBarChangeListener(null);
            bar.setEnabled(false);
            AppState.getInstance().brightness = 0;
            DocumentController.applyBrigtness(getActivity());
        } else {
            bar.setEnabled(true);
            bar.setOnSeekBarChangeListener(onSeek);
        }
    }

    public void initKeys() {
        nextKeys.setText(String.format("%s: %s", getActivity().getString(R.string.next_keys), AppState.keyToString(AppState.getInstance().nextKeys)));
        prevKeys.setText(String.format("%s: %s", getActivity().getString(R.string.prev_keys), AppState.keyToString(AppState.getInstance().prevKeys)));
    }

    Runnable onCloseDialog = new Runnable() {

        @Override
        public void run() {
            initKeys();
        }
    };

    OnSeekBarChangeListener onSeek = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
            final float f = (float) progress / 100;
            if (f <= 0.01) {
                return;
            }
            AppState.getInstance().brightness = f;
            DocumentController.applyBrigtness(getActivity());
            curBrightness.setText("" + progress);

        }
    };
    private TextView nextKeys;
    private TextView prevKeys;
    private SeekBar bar;
    private CheckBox autoSettings;
    private TextView searchPaths;
    private CheckBox ch;
    private CheckBox rememberMode;
    private TextView selectedOpenMode;

    private TextView textNigthColor;
    private TextView textDayColor;
    private TextView selectedDictionaly;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onEmail() {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        String string = getResources().getString(R.string.my_email).replace("<u>", "").replace("</u>", "");
        final String aEmailList[] = { string };
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, AppsConfig.APP_NAME + " " + Apps.getVersionName(getContext()) + "-" + System.getProperty("os.arch"));
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hi Support, ");

        try {
            startActivity(Intent.createChooser(emailIntent, getActivity().getString(R.string.send_mail)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), R.string.there_are_no_email_applications_installed_, Toast.LENGTH_SHORT).show();
        }
    }

    public void onTheme() {
        getActivity().finish();
        startActivity(getActivity().getIntent());
    }

    public void onScan() {
        getActivity().startService(new Intent(getActivity(), BooksService.class).setAction(BooksService.ACTION_SEARCH_ALL));

        Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, 0);//

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    public void rateIT() {
        try {
            Urls.open(getActivity(), "market://details?id=" + getActivity().getPackageName());
        } catch (Exception e) {
            Urls.open(getActivity(), "https://play.google.com/store/apps/details?id=" + getActivity().getPackageName());
            LOG.e(e);
        }
    }

    public void rotationText() {
        final int type = AppState.getInstance().orientation;
        TextView screenType = (TextView) getActivity().findViewById(R.id.screenModeType);
        if (type == ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
            screenType.setText(TxtUtils.underline(getString(R.string.automatic)));
        }
        if (type == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            screenType.setText(TxtUtils.underline(getString(R.string.landscape)));
        }
        if (type == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            screenType.setText(TxtUtils.underline(getString(R.string.portrait)));
        }
        DocumentController.doRotation(getActivity());
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fullScreenText();
        rotationText();
        TextView theme = (TextView) getActivity().findViewById(R.id.themeColor);
        if (AppState.getInstance().isWhiteTheme) {
            theme.setText(TxtUtils.underline(getString(R.string.light)));
        } else {
            theme.setText(TxtUtils.underline(getString(R.string.black)));
        }
    }

    public void fullScreenText() {
        final boolean full = AppState.getInstance().isFullScrean();
        int textID = full ? R.string.on : R.string.off;
        ((TextView) getActivity().findViewById(R.id.fullscreenOnOff)).setText(TxtUtils.underline(getString(textID)));

    }

    public SearchFragment getSearchFragmet() {
        return searchFragmet;
    }

    public void setSearchFragmet(final SearchFragment searchFragmet) {
        this.searchFragmet = searchFragmet;
    }

    private void saveChanges() {
        if (getActivity() != null) {
            AppState.get().save(getActivity());
        }
    }

}
