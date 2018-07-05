package com.foobnix.ui2.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.buzzingandroid.ui.HSVColorPickerDialog;
import com.buzzingandroid.ui.HSVColorPickerDialog.OnColorSelectedListener;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.AndroidWhatsNew;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PasswordDialog;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.view.CustomSeek;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.KeyCodeDialog;
import com.foobnix.pdf.info.view.MultyDocSearchDialog;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.widget.ColorsDialog;
import com.foobnix.pdf.info.widget.ColorsDialog.ColorsDialogResult;
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.PasswordState;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.MyContextWrapper;
import com.jmedeisis.draglinearlayout.DragLinearLayout;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PrefFragment2 extends UIFragment {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.preferences, R.drawable.glyphicons_281_settings);

    private static final String WWW_SITE = "http://librera.mobi";
    private static final String WWW_BETA_SITE = "http://beta.librera.mobi";
    private static final String WWW_ARCHIVE_SITE = "http://archive.librera.mobi";
    private TextView curBrightness, themeColor;
    private CheckBox isRememberDictionary;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public boolean isBackPressed() {
        return false;
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

    View section1, section2, section3, section4, section5, section6, section7, overlay;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        inflate = inflater.inflate(R.layout.preferences, container, false);

        // tabs position
        final DragLinearLayout dragLinearLayout = (DragLinearLayout) inflate.findViewById(R.id.dragLinearLayout);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2));

        final Runnable dragLinear = new Runnable() {

            @Override
            public void run() {
                dragLinearLayout.removeAllViews();
                for (UITab tab : UITab.getOrdered(AppState.get().tabsOrder7)) {
                    View library = LayoutInflater.from(getActivity()).inflate(R.layout.item_tab_line, null, false);
                    ((TextView) library.findViewById(R.id.text1)).setText(tab.getName());
                    ((CheckBox) library.findViewById(R.id.isVisible)).setChecked(tab.isVisible());
                    ((ImageView) library.findViewById(R.id.image1)).setImageResource(tab.getIcon());
                    TintUtil.setTintImageWithAlpha(((ImageView) library.findViewById(R.id.image1)), TintUtil.COLOR_TINT_GRAY);
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
                AppState.get().tabsOrder7 = "";
                for (int i = 0; i < dragLinearLayout.getChildCount(); i++) {
                    View child = dragLinearLayout.getChildAt(i);
                    boolean isVisible = ((CheckBox) child.findViewById(R.id.isVisible)).isChecked();
                    AppState.get().tabsOrder7 += child.getTag() + "#" + (isVisible ? "1" : "0") + ",";
                }
                AppState.get().tabsOrder7 = TxtUtils.replaceLast(AppState.get().tabsOrder7, ",", "");
                LOG.d("tabsApply", AppState.get().tabsOrder7);
                AppState.get().save(getActivity());
                onTheme();
            }
        });

        final CheckBox isshowPrefAsMenu = (CheckBox) inflate.findViewById(R.id.isshowPrefAsMenu);
        isshowPrefAsMenu.setChecked(AppState.get().tabsOrder7.contains(UITab.PrefFragment.index + "#0"));
        isshowPrefAsMenu.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppState.get().tabsOrder7 = AppState.get().tabsOrder7.replace(UITab.PrefFragment.index + "#1", UITab.PrefFragment.index + "#0");
                } else {
                    AppState.get().tabsOrder7 = AppState.get().tabsOrder7.replace(UITab.PrefFragment.index + "#0", UITab.PrefFragment.index + "#1");
                }

                dragLinear.run();

            }
        });

        TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.tabsDefaul)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AlertDialogs.showOkDialog(getActivity(), getActivity().getString(R.string.restore_defaults_full), new Runnable() {

                    @Override
                    public void run() {
                        AppState.get().tabsOrder7 = AppState.DEFAULTS_TABS_ORDER;
                        isshowPrefAsMenu.setChecked(false);
                        dragLinear.run();
                    }
                });

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

        final CustomSeek coverSmallSize = (CustomSeek) inflate.findViewById(R.id.coverSmallSize);
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
        final TextView columsDefaul = (TextView) inflate.findViewById(R.id.columsDefaul);
        TxtUtils.underlineTextView(columsDefaul);
        columsDefaul.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                AlertDialogs.showOkDialog(getActivity(), getActivity().getString(R.string.restore_defaults_full), new Runnable() {

                    @Override
                    public void run() {
                        IMG.clearDiscCache();
                        IMG.clearMemoryCache();
                        AppState.get().coverBigSize = (int) (((Dips.screenWidthDP() / (Dips.screenWidthDP() / 120)) - 8) * (Dips.isXLargeScreen() ? 1.5f : 1));
                        AppState.get().coverSmallSize = 80;
                        TempHolder.listHash++;

                        columsCount.setText("" + Dips.screenWidthDP() / AppState.get().coverBigSize);
                        TxtUtils.underlineTextView(columsCount);

                        coverSmallSize.init(40, max, AppState.get().coverSmallSize);
                        coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize), AppState.get().coverBigSize);
                    }
                });

            }
        });

        final ScrollView scrollView = (ScrollView) inflate.findViewById(R.id.scroll);
        scrollView.setVerticalScrollBarEnabled(false);

        ((TextView) inflate.findViewById(R.id.section6)).setText(String.format("%s: %s", getString(R.string.product), AppsConfig.TXT_APP_NAME));
        // ((TextView) findViewById(R.id.appName)).setText(AppsConfig.APP_NAME);

        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            final String version = packageInfo.versionName;
            // ((TextView) inflate.findViewById(R.id.pVersion)).setText(String.format("%s:
            // %s (%s)", getString(R.string.version), version, AppsConfig.MUPDF_VERSION));
            ((TextView) inflate.findViewById(R.id.pVersion)).setText(String.format("%s: %s", getString(R.string.version), version));
        } catch (final NameNotFoundException e) {
        }

        TextView onCloseApp = (TextView) inflate.findViewById(R.id.onCloseApp);
        TxtUtils.underlineTextView(onCloseApp);
        onCloseApp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ((MainTabs2) getActivity()).showInterstial();
            }
        });

        final TextView onFullScreen = (TextView) inflate.findViewById(R.id.fullscreen);
        onFullScreen.setText(AppState.get().isFullScreenMain ? R.string.yes : R.string.no);
        TxtUtils.underlineTextView(onFullScreen);

        onFullScreen.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {

                MyPopupMenu popup = new MyPopupMenu(getActivity(), v);
                popup.getMenu().add(R.string.yes).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.get().isFullScreenMain = true;

                        onFullScreen.setText(R.string.yes);
                        TxtUtils.underlineTextView(onFullScreen);
                        DocumentController.chooseFullScreen(getActivity(), AppState.get().isFullScreenMain);
                        return false;
                    }
                });

                popup.getMenu().add(R.string.no).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.get().isFullScreenMain = false;

                        onFullScreen.setText(R.string.no);
                        TxtUtils.underlineTextView(onFullScreen);
                        DocumentController.chooseFullScreen(getActivity(), AppState.get().isFullScreenMain);
                        return false;
                    }
                });

                popup.show();

            }
        });

        screenOrientation = (TextView) inflate.findViewById(R.id.screenOrientation);
        screenOrientation.setText(DocumentController.getRotationText());
        TxtUtils.underlineTextView(screenOrientation);

        screenOrientation.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                for (int i = 0; i < DocumentController.orientationIds.size(); i++) {
                    final int j = i;
                    final int name = DocumentController.orientationTexts.get(i);
                    menu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            AppState.get().orientation = DocumentController.orientationIds.get(j);
                            screenOrientation.setText(DocumentController.orientationTexts.get(j));
                            TxtUtils.underlineTextView(screenOrientation);
                            DocumentController.doRotation(getActivity());
                            return false;
                        }
                    });
                }
                menu.show();
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
                new KeyCodeDialog(getActivity(), onCloseDialog);
            }
        });

        themeColor = (TextView) inflate.findViewById(R.id.themeColor);
        themeColor.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {

                PopupMenu p = new PopupMenu(getContext(), themeColor);
                p.getMenu().add(R.string.light).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.get().isWhiteTheme = true;
                        AppState.get().isInkMode = false;

                        AppState.get().contrastImage = 0;
                        AppState.get().brigtnessImage = 0;
                        AppState.get().bolderTextOnImage = false;
                        AppState.get().isEnableBC = false;

                        AppState.get().save(getActivity());

                        IMG.clearDiscCache();
                        IMG.clearMemoryCache();
                        onTheme();

                        return false;
                    }
                });
                p.getMenu().add(R.string.black).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.get().isWhiteTheme = false;
                        AppState.get().isInkMode = false;

                        AppState.get().contrastImage = 0;
                        AppState.get().brigtnessImage = 0;
                        AppState.get().bolderTextOnImage = false;
                        AppState.get().isEnableBC = false;

                        AppState.get().save(getActivity());

                        IMG.clearDiscCache();
                        IMG.clearMemoryCache();

                        onTheme();
                        return false;
                    }
                });
                p.getMenu().add("Ink").setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        IMG.clearDiscCache();
                        IMG.clearMemoryCache();

                        onEink();
                        return false;
                    }
                });
                p.show();
            }
        });

        final TextView hypenLang = (TextView) inflate.findViewById(R.id.appLang);
        hypenLang.setText(DialogTranslateFromTo.getLanuageByCode(AppState.get().appLang));
        TxtUtils.underlineTextView(hypenLang);

        hypenLang.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                final List<String> codes = Arrays.asList(//
                        "en", "ar", "de", "es", "fa", "fi", "fr", "he", //
                        "hi", "hu", "id", "it", "ja", "ko", "la", "lt", //
                        "nl", "no", "pl", "pt", "ro", "ru", "sk", "sv", //
                        "sw", "th", "tr", "uk", "vi", "zh");
                List<String> langs = new ArrayList<String>();
                for (String code : codes) {
                    langs.add(DialogTranslateFromTo.getLanuageByCode(code) + ":" + code);
                }
                Collections.sort(langs);

                popupMenu.getMenu().add(R.string.system_language).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        TxtUtils.underlineTextView(hypenLang);
                        AppState.get().appLang = AppState.MY_SYSTEM_LANG;
                        TempHolder.get().forseAppLang = true;
                        MyContextWrapper.wrap(getContext());
                        AppState.get().save(getActivity());
                        onTheme();
                        return false;
                    }
                });

                for (int i = 0; i < langs.size(); i++) {
                    String all[] = langs.get(i).split(":");
                    final String name = all[0];
                    final String code = all[1];
                    popupMenu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            AppState.get().appLang = code;
                            TxtUtils.underlineTextView(hypenLang);
                            AppState.get().save(getActivity());
                            onTheme();
                            return false;
                        }
                    });
                }
                popupMenu.show();

            }
        });

        final TextView appFontScale = (TextView) inflate.findViewById(R.id.appFontScale);
        appFontScale.setText(getFontName(AppState.get().appFontScale));
        TxtUtils.underlineTextView(appFontScale);
        appFontScale.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                for (float i = 0.7f; i < 2.1f; i += 0.1) {
                    final float number = i;
                    popupMenu.getMenu().add(getFontName(number)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            AppState.get().appFontScale = number;
                            AppState.get().save(getActivity());
                            onTheme();
                            return false;
                        }
                    });
                }
                popupMenu.show();
            }
        });

        final TextView onMail = (TextView) inflate.findViewById(R.id.onMailSupport);
        onMail.setText(TxtUtils.underline(getString(R.string.my_email)));

        onMail.setOnClickListener(new OnClickListener() {
            @Override

            public void onClick(final View v) {
                onEmail();
            }
        });

        rememberMode = (CheckBox) inflate.findViewById(R.id.isRememberMode);
        rememberMode.setChecked(AppState.get().isRememberMode);
        rememberMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isRememberMode = isChecked;
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

                final MenuItem advanced_mode = popupMenu.getMenu().add(AppState.get().nameVerticalMode);
                advanced_mode.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.get().isAlwaysOpenAsMagazine = false;
                        AppState.get().isMusicianMode = false;
                        checkOpenWithSpinner();
                        return false;
                    }
                });

                final MenuItem easy_mode = popupMenu.getMenu().add(AppState.get().nameHorizontalMode);
                easy_mode.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.get().isAlwaysOpenAsMagazine = true;
                        AppState.get().isMusicianMode = false;
                        checkOpenWithSpinner();
                        return false;
                    }
                });
                final MenuItem music_mode = popupMenu.getMenu().add(AppState.get().nameMusicianMode);
                music_mode.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.get().isAlwaysOpenAsMagazine = false;
                        AppState.get().isMusicianMode = true;
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
        isCropBookCovers.setChecked(AppState.get().isCropBookCovers);
        isCropBookCovers.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isCropBookCovers = isChecked;
                TempHolder.listHash++;

            }
        });

        final CheckBox isBookCoverEffect = (CheckBox) inflate.findViewById(R.id.isBookCoverEffect);
        isBookCoverEffect.setOnCheckedChangeListener(null);
        isBookCoverEffect.setChecked(AppState.get().isBookCoverEffect);
        isBookCoverEffect.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isBookCoverEffect = isChecked;
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
        isBorderAndShadow.setChecked(AppState.get().isBorderAndShadow);
        isBorderAndShadow.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isBorderAndShadow = isChecked;
                TempHolder.listHash++;

            }
        });

        final CheckBox isShowImages = (CheckBox) inflate.findViewById(R.id.isShowImages);
        isShowImages.setOnCheckedChangeListener(null);
        isShowImages.setChecked(AppState.get().isShowImages);
        isShowImages.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isShowImages = isChecked;
                TempHolder.listHash++;
                isCropBookCovers.setEnabled(AppState.get().isShowImages);
                isBookCoverEffect.setEnabled(AppState.get().isShowImages);
                isBorderAndShadow.setEnabled(AppState.get().isShowImages);

            }
        });
        isCropBookCovers.setEnabled(AppState.get().isShowImages);
        isBookCoverEffect.setEnabled(AppState.get().isShowImages);
        isBorderAndShadow.setEnabled(AppState.get().isShowImages);

        CheckBox isLoopAutoplay = (CheckBox) inflate.findViewById(R.id.isLoopAutoplay);
        isLoopAutoplay.setChecked(AppState.get().isLoopAutoplay);
        isLoopAutoplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isLoopAutoplay = isChecked;
            }
        });

        CheckBox isOpenLastBook = (CheckBox) inflate.findViewById(R.id.isOpenLastBook);
        isOpenLastBook.setChecked(AppState.get().isOpenLastBook);
        isOpenLastBook.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isOpenLastBook = isChecked;
            }
        });

        CheckBox isShowCloseAppDialog = (CheckBox) inflate.findViewById(R.id.isShowCloseAppDialog);
        isShowCloseAppDialog.setChecked(AppState.get().isShowCloseAppDialog);
        isShowCloseAppDialog.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isShowCloseAppDialog = isChecked;
            }
        });

        final Handler handler = new Handler();
        final Runnable ask = new Runnable() {

            @Override
            public void run() {
                AlertDialogs.showDialog(getActivity(), getActivity().getString(R.string.you_need_to_update_the_library), getString(R.string.ok), new Runnable() {

                    @Override
                    public void run() {
                        onScan();
                    }
                }, null);
            }
        };

        final int timeout = 1500;

        inflate.findViewById(R.id.moreLybraryettings).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final CheckBox check = new CheckBox(v.getContext());
                check.setText(R.string.displaying_the_author_and_title_of_the_pdf_book_from_the_meta_tags);

                final AlertDialog d = AlertDialogs.showViewDialog(getActivity(), check);

                check.setChecked(AppState.get().isAuthorTitleFromMetaPDF);

                check.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isAuthorTitleFromMetaPDF = isChecked;
                        handler.removeCallbacksAndMessages(null);
                        handler.postDelayed(ask, timeout);
                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                d.dismiss();
                            }
                        }, timeout);
                    }
                });

            }
        });

        final CheckBox isFirstSurname = (CheckBox) inflate.findViewById(R.id.isFirstSurname);
        isFirstSurname.setChecked(AppState.get().isFirstSurname);
        isFirstSurname.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isFirstSurname = isChecked;
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        ////
        ((CheckBox) inflate.findViewById(R.id.supportPDF)).setChecked(AppState.get().supportPDF);
        ((CheckBox) inflate.findViewById(R.id.supportPDF)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportPDF = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportXPS)).setChecked(AppState.get().supportXPS);
        ((CheckBox) inflate.findViewById(R.id.supportXPS)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportXPS = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportDJVU)).setChecked(AppState.get().supportDJVU);
        ((CheckBox) inflate.findViewById(R.id.supportDJVU)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportDJVU = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });
        ((CheckBox) inflate.findViewById(R.id.supportEPUB)).setChecked(AppState.get().supportEPUB);
        ((CheckBox) inflate.findViewById(R.id.supportEPUB)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportEPUB = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });
        ((CheckBox) inflate.findViewById(R.id.supportFB2)).setChecked(AppState.get().supportFB2);
        ((CheckBox) inflate.findViewById(R.id.supportFB2)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportFB2 = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportTXT)).setChecked(AppState.get().supportTXT);
        ((CheckBox) inflate.findViewById(R.id.supportTXT)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportTXT = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportMOBI)).setChecked(AppState.get().supportMOBI);
        ((CheckBox) inflate.findViewById(R.id.supportMOBI)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportMOBI = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportRTF)).setChecked(AppState.get().supportRTF);
        ((CheckBox) inflate.findViewById(R.id.supportRTF)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportRTF = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        ((CheckBox) inflate.findViewById(R.id.supportCBZ)).setChecked(AppState.get().supportCBZ);
        ((CheckBox) inflate.findViewById(R.id.supportCBZ)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportCBZ = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        CheckBox supportZIP = (CheckBox) inflate.findViewById(R.id.supportZIP);
        supportZIP.setChecked(AppState.get().supportZIP);
        supportZIP.setText(getString(R.string.archives) + " (ZIP/RAR/...)");
        supportZIP.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportZIP = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        CheckBox supportOther = (CheckBox) inflate.findViewById(R.id.supportOther);
        supportOther.setChecked(AppState.get().supportOther);
        supportOther.setText(getString(R.string.other) + " (DOC/CHM/...)");
        supportOther.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().supportOther = isChecked;
                ExtUtils.updateSearchExts();
                handler.removeCallbacks(ask);
                handler.postDelayed(ask, timeout);
            }
        });

        CheckBox isDisplayAllFilesInFolder = (CheckBox) inflate.findViewById(R.id.isDisplayAllFilesInFolder);
        isDisplayAllFilesInFolder.setChecked(AppState.get().isDisplayAllFilesInFolder);
        isDisplayAllFilesInFolder.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isDisplayAllFilesInFolder = isChecked;
                TempHolder.listHash++;
            }
        });
        // app password
        final CheckBox isAppPassword = (CheckBox) inflate.findViewById(R.id.isAppPassword);
        isAppPassword.setChecked(PasswordState.get().isAppPassword);
        isAppPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                PasswordState.get().isAppPassword = isChecked;
                if (isChecked && TxtUtils.isEmpty(PasswordState.get().appPassword)) {
                    PasswordDialog.showDialog(getActivity(), true, new Runnable() {

                        @Override
                        public void run() {
                            if (TxtUtils.isEmpty(PasswordState.get().appPassword)) {
                                isAppPassword.setChecked(false);
                            }
                        }
                    });
                }
            }
        });

        TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.appPassword)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PasswordDialog.showDialog(getActivity(), true, new Runnable() {

                    @Override
                    public void run() {
                        if (TxtUtils.isEmpty(PasswordState.get().appPassword)) {
                            isAppPassword.setChecked(false);
                        }
                    }
                });
            }
        });

        // What is new
        CheckBox showWhatIsNew = (CheckBox) inflate.findViewById(R.id.isShowWhatIsNewDialog);
        showWhatIsNew.setChecked(AppState.get().isShowWhatIsNewDialog);
        showWhatIsNew.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isShowWhatIsNewDialog = isChecked;
            }
        });

        final TextView whatIsNew = (TextView) inflate.findViewById(R.id.whatIsNew);
        whatIsNew.setText(getActivity().getString(R.string.what_is_new_in) + " " + Apps.getApplicationName(getActivity()) + " " + Apps.getVersionName(getActivity()));
        TxtUtils.underlineTextView(whatIsNew);
        whatIsNew.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AndroidWhatsNew.show(getActivity());

            }
        });

        ///

        // BrightnessHelper.controlsWrapper(inflate, getActivity());

        nextKeys = (TextView) inflate.findViewById(R.id.textNextKeys);
        prevKeys = (TextView) inflate.findViewById(R.id.textPrevKeys);

        ch = (CheckBox) inflate.findViewById(R.id.onReverse);
        ch.setOnCheckedChangeListener(null);
        ch.setChecked(AppState.get().isReverseKeys);
        ch.setOnCheckedChangeListener(reverseListener);

        inflate.findViewById(R.id.onColorChoser).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
            }
        });
        initKeys();

        searchPaths = (TextView) inflate.findViewById(R.id.searchPaths);
        searchPaths.setText(TxtUtils.underline(AppState.get().searchPaths.replace(",", "<br>")));
        searchPaths.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onFolderConfigDialog();
            }
        });

        TextView addFolder = (TextView) inflate.findViewById(R.id.onConfigPath);
        TxtUtils.underlineTextView(addFolder);
        addFolder.setOnClickListener(new View.OnClickListener() {

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

        final CheckBox isAutomaticExport = (CheckBox) inflate.findViewById(R.id.isAutomaticExport);
        isAutomaticExport.setChecked(AppState.get().isAutomaticExport);
        isAutomaticExport.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppState.get().isAutomaticExport = isChecked;
            }
        });
        // folders

        final TextView fontFolder = (TextView) inflate.findViewById(R.id.fontFolder);
        TxtUtils.underline(fontFolder, TxtUtils.lastTwoPath(BookCSS.get().fontFolder));
        fontFolder.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ChooserDialogFragment.chooseFolder(getActivity(), BookCSS.get().fontFolder).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                    @Override
                    public boolean onResultRecive(String nPath, Dialog dialog) {
                        BookCSS.get().fontFolder = nPath;
                        TxtUtils.underline(fontFolder, TxtUtils.lastTwoPath(BookCSS.get().fontFolder));
                        dialog.dismiss();
                        return false;
                    }
                });
            }
        });

        final TextView downloadFolder = (TextView) inflate.findViewById(R.id.downloadFolder);
        TxtUtils.underline(downloadFolder, TxtUtils.lastTwoPath(AppState.get().downlodsPath));
        downloadFolder.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ChooserDialogFragment.chooseFolder(getActivity(), AppState.get().downlodsPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                    @Override
                    public boolean onResultRecive(String nPath, Dialog dialog) {
                        AppState.get().downlodsPath = nPath;
                        TxtUtils.underline(downloadFolder, TxtUtils.lastTwoPath(AppState.get().downlodsPath));
                        dialog.dismiss();
                        return false;
                    }
                });
            }
        });

        final TextView syncPath = (TextView) inflate.findViewById(R.id.syncPath);
        TxtUtils.underline(syncPath, TxtUtils.lastTwoPath(AppState.get().syncDropboxPath));
        syncPath.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ChooserDialogFragment.chooseFolder(getActivity(), AppState.get().syncDropboxPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                    @Override
                    public boolean onResultRecive(String nPath, Dialog dialog) {
                        AppState.get().syncDropboxPath = nPath;
                        TxtUtils.underline(downloadFolder, TxtUtils.lastTwoPath(AppState.get().syncDropboxPath));
                        dialog.dismiss();
                        return false;
                    }
                });
            }
        });

        final TextView ttsFolder = (TextView) inflate.findViewById(R.id.ttsFolder);
        TxtUtils.underline(ttsFolder, TxtUtils.lastTwoPath(AppState.get().ttsSpeakPath));
        ttsFolder.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ChooserDialogFragment.chooseFolder(getActivity(), AppState.get().ttsSpeakPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                    @Override
                    public boolean onResultRecive(String nPath, Dialog dialog) {
                        AppState.get().ttsSpeakPath = nPath;
                        TxtUtils.underline(ttsFolder, TxtUtils.lastTwoPath(AppState.get().ttsSpeakPath));
                        dialog.dismiss();
                        return false;
                    }
                });
            }
        });

        final TextView backupPath = (TextView) inflate.findViewById(R.id.backupFolder);
        TxtUtils.underline(backupPath, TxtUtils.lastTwoPath(AppState.get().backupPath));
        backupPath.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ChooserDialogFragment.chooseFolder(getActivity(), AppState.get().backupPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                    @Override
                    public boolean onResultRecive(String nPath, Dialog dialog) {
                        AppState.get().backupPath = nPath;
                        TxtUtils.underline(backupPath, TxtUtils.lastTwoPath(AppState.get().backupPath));
                        dialog.dismiss();
                        return false;
                    }
                });
            }
        });

        // Widget Configuration

        final TextView widgetLayout = (TextView) inflate.findViewById(R.id.widgetLayout);
        widgetLayout.setText(AppState.get().widgetType == AppState.WIDGET_LIST ? R.string.list : R.string.grid);
        TxtUtils.underlineTextView(widgetLayout);

        widgetLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                final MenuItem recent = popupMenu.getMenu().add(R.string.list);
                recent.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.get().widgetType = AppState.WIDGET_LIST;
                        widgetLayout.setText(R.string.list);
                        TxtUtils.underlineTextView(widgetLayout);
                        return false;
                    }
                });

                final MenuItem starred = popupMenu.getMenu().add(R.string.grid);
                starred.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        AppState.get().widgetType = AppState.WIDGET_GRID;
                        widgetLayout.setText(R.string.grid);
                        TxtUtils.underlineTextView(widgetLayout);
                        return false;
                    }
                });

                popupMenu.show();

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

        final TextView widgetItemsCount = (TextView) inflate.findViewById(R.id.widgetItemsCount);
        widgetItemsCount.setText("" + AppState.get().widgetItemsCount);
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
                            AppState.get().widgetItemsCount = k;
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
        isRememberDictionary = (CheckBox) inflate.findViewById(R.id.isRememberDictionary);
        isRememberDictionary.setChecked(AppState.get().isRememberDictionary);
        isRememberDictionary.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isRememberDictionary = isChecked;
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
                new ColorsDialog(getActivity(), true, AppState.get().colorDayText, AppState.get().colorDayBg, false, true, new ColorsDialogResult() {

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
                new ColorsDialog(getActivity(), false, AppState.get().colorNigthText, AppState.get().colorNigthBg, false, true, new ColorsDialogResult() {

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
                    TempHolder.listHash++;

                    onTintChanged();
                    sendNotifyTintChanged();

                    AppState.get().save(getActivity());

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

        underline(inflate.findViewById(R.id.linksColor)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                closeLeftMenu();
                Dialogs.showLinksColorDialog(getActivity(), new Runnable() {

                    @Override
                    public void run() {
                        TempHolder.listHash++;
                        onTintChanged();
                        sendNotifyTintChanged();
                        ((MainTabs2) getActivity()).updateCurrentFragment();

                        TxtUtils.updateAllLinks((ViewGroup) inflate.getRootView());

                    }
                });
            }
        });

        underline(inflate.findViewById(R.id.onContrast)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                Dialogs.showContrastDialogByUrl(getActivity(), new Runnable() {

                    @Override
                    public void run() {
                        ImageLoader.getInstance().clearDiskCache();
                        ImageLoader.getInstance().clearMemoryCache();
                        TempHolder.listHash++;
                        notifyFragment();

                    }
                });
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

        underline(inflate.findViewById(R.id.onTelegram)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                Urls.open(getActivity(), "https://t.me/LibreraReader");
            }
        });

        TextView proText = (TextView) inflate.findViewById(R.id.downloadPRO);
        TxtUtils.underlineTextView(proText);
        ((View) proText.getParent()).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                Urls.openPdfPro(getActivity());
            }
        });

        if (AppsConfig.checkIsProInstalled(getActivity())) {
            ((View) proText.getParent()).setVisibility(View.GONE);
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

        TextView tutorialLink = (TextView) inflate.findViewById(R.id.tutorialLink);
        TxtUtils.underlineTextView(tutorialLink);

        tutorialLink.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Urls.open(getActivity(), AndroidWhatsNew.DETAIL_URL_RU);
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
                AlertDialog create = alert.create();
                create.setOnDismissListener(new OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Keyboards.hideNavigation(getActivity());
                    }
                });
                create.show();
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

        overlay = getActivity().findViewById(R.id.overlay);

        return inflate;

    }

    private void onEink() {
        AppState.get().isInkMode = true;
        AppState.get().isWhiteTheme = true;
        AppState.get().defaults(getActivity());
        AppState.get().blueLightAlpha = 0;

        onTintChanged();
        sendNotifyTintChanged();

        AppState.get().save(getActivity());

        getActivity().finish();
        MainTabs2.startActivity(getActivity(), TempHolder.get().currentTab);

    }

    OnCheckedChangeListener reverseListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            AppState.get().isReverseKeys = isChecked;
            initKeys();
            saveChanges();
            LOG.d("Save Changes", 3);
        }
    };

    public View underline(View text) {
        CharSequence myText = ((TextView) text).getText();
        ((TextView) text).setText(Html.fromHtml("<u>" + myText + "</u>"));
        return text;
    }

    private void checkOpenWithSpinner() {
        String modId = AppState.get().nameVerticalMode;
        if (AppState.get().isMusicianMode) {
            modId = AppState.get().nameMusicianMode;
        } else if (AppState.get().isAlwaysOpenAsMagazine) {
            modId = AppState.get().nameHorizontalMode;
        } else {
            modId = AppState.get().nameVerticalMode;
        }

        selectedOpenMode.setText(TxtUtils.underline(modId));
    }

    public void onFolderConfigDialog() {
        AppState.get().searchPaths = AppState.get().searchPaths.replace("//", "/");
        PrefDialogs.chooseFolderDialog(getActivity(), new Runnable() {

            @Override
            public void run() {
                AppState.get().searchPaths = AppState.get().searchPaths.replace("//", "/");
                searchPaths.setText(TxtUtils.underline(AppState.get().searchPaths.replace(",", "<br>")));
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

        BrightnessHelper.updateOverlay(overlay);
        BrightnessHelper.showBlueLigthDialogAndBrightness(getActivity(), inflate, new Runnable() {

            @Override
            public void run() {
                BrightnessHelper.updateOverlay(overlay);
            }
        });

        rotationText();

        ch.setOnCheckedChangeListener(null);
        ch.setChecked(AppState.get().isReverseKeys);
        ch.setOnCheckedChangeListener(reverseListener);

        rememberMode.setChecked(AppState.get().isRememberMode);
        checkOpenWithSpinner();

        textNigthColor.setTextColor(AppState.get().colorNigthText);
        textNigthColor.setBackgroundColor(AppState.get().colorNigthBg);

        textDayColor.setTextColor(AppState.get().colorDayText);
        textDayColor.setBackgroundColor(AppState.get().colorDayBg);

        isRememberDictionary.setChecked(AppState.get().isRememberDictionary);
        selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());

    }

    public void onColorChoose() {

    }

    public void initKeys() {
        nextKeys.setText(String.format("%s: %s", getActivity().getString(R.string.next_keys), AppState.keyToString(AppState.get().nextKeys)));
        prevKeys.setText(String.format("%s: %s", getActivity().getString(R.string.prev_keys), AppState.keyToString(AppState.get().prevKeys)));
    }

    Runnable onCloseDialog = new Runnable() {

        @Override
        public void run() {
            initKeys();
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

    private TextView screenOrientation;

    private View inflate;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onEmail() {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        String string = getResources().getString(R.string.my_email).replace("<u>", "").replace("</u>", "");
        final String aEmailList[] = { string };
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, AppsConfig.TXT_APP_NAME + " " + Apps.getVersionName(getContext()));
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hi Support, ");

        try {
            startActivity(Intent.createChooser(emailIntent, getActivity().getString(R.string.send_mail)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), R.string.there_are_no_email_applications_installed_, Toast.LENGTH_SHORT).show();
        }
    }

    public String getFullDeviceInfo() {
        return "(" + Build.BRAND + ", " + Build.MODEL + ", " + android.os.Build.VERSION.RELEASE + ", " + Dips.screenWidthDP() + "dp" + ")";
    }

    public void onTheme() {
        getActivity().finish();
        MainTabs2.startActivity(getActivity(), TempHolder.get().currentTab);
    }

    public void onScan() {
        closeLeftMenu();

        getActivity().startService(new Intent(getActivity(), BooksService.class).setAction(BooksService.ACTION_SEARCH_ALL));

        Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.SearchFragment));//

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    private void closeLeftMenu() {
        try {
            final DrawerLayout drawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
            if (drawerLayout.isDrawerOpen(Gravity.START)) {
                drawerLayout.closeDrawer(Gravity.START, !Dips.isEInk(getActivity()));
            }
        } catch (Exception e) {
            LOG.e(e);
        }
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
        screenOrientation.setText(DocumentController.getRotationText());
        TxtUtils.underlineTextView(screenOrientation);
        DocumentController.doRotation(getActivity());
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rotationText();

        if (AppState.get().isInkMode) {
            themeColor.setText(TxtUtils.underline("Ink"));
        } else if (AppState.get().isWhiteTheme) {
            themeColor.setText(TxtUtils.underline(getString(R.string.light)));
        } else {
            themeColor.setText(TxtUtils.underline(getString(R.string.black)));
        }
    }

    private void saveChanges() {
        if (getActivity() != null) {
            AppState.get().save(getActivity());
        }
    }

    public String getFontName(float number) {
        String prefix = getActivity().getString(R.string.normal);
        float f1 = (number - 1f) * 10;
        float f2 = (1f - number) * 10 + 0.01f;
        if (number < 1) {
            prefix = getActivity().getString(R.string.small) + " (-" + (int) f2 + ")";
        } else if (number > 1) {
            prefix = getActivity().getString(R.string.large) + " (+" + (int) f1 + ")";
        }
        return prefix;
    }

}
