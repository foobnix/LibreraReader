package com.foobnix.ui2.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.buzzingandroid.ui.HSVColorPickerDialog;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AndroidWhatsNew;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.BuildConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PasswordDialog;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.databinding.DialogCustomReadingModesBinding;
import com.foobnix.pdf.info.databinding.ItemColorBinding;
import com.foobnix.pdf.info.databinding.ItemTabLineBinding;
import com.foobnix.pdf.info.databinding.PreferencesBinding;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.KeyCodeDialog;
import com.foobnix.pdf.info.view.MultiDocSearchDialog;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.widget.ColorsDialog;
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.PasswordState;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.msg.GDriveSycnEvent;
import com.foobnix.pdf.search.activity.msg.MessageSync;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.MyContextWrapper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

import org.ebookdroid.LibreraApp;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PrefFragment2 extends UIFragment {
    public static final Pair<Integer, Integer> PAIR = new Pair<>(R.string.preferences, R.drawable.glyphicons_281_settings);

    private static final String WWW_SITE = "http://librera.mobi";
    private static final String WWW_BETA_SITE = "http://beta.librera.mobi";
    private static final String WWW_WIKI_SITE = "http://wiki.librera.mobi/faq";
    private PreferencesBinding preferencesBinding;
    View overlay;
    OnCheckedChangeListener reverseListener = (buttonView, isChecked) -> {
        AppState.get().isReverseKeys = isChecked;
        initKeys();
        saveChanges();
        LOG.d("Save Changes", 3);
    };
    Runnable onCloseDialog = this::initKeys;

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
        TintUtil.setBackgroundFillColor(preferencesBinding.section1, TintUtil.color);
        TintUtil.setBackgroundFillColor(preferencesBinding.section2, TintUtil.color);
        TintUtil.setBackgroundFillColor(preferencesBinding.section3, TintUtil.color);
        TintUtil.setBackgroundFillColor(preferencesBinding.section4, TintUtil.color);
        TintUtil.setBackgroundFillColor(preferencesBinding.section5, TintUtil.color);
        TintUtil.setBackgroundFillColor(preferencesBinding.section6, TintUtil.color);
        TintUtil.setBackgroundFillColor(preferencesBinding.section7, TintUtil.color);
        TintUtil.setBackgroundFillColor(preferencesBinding.section8, TintUtil.color);
        TintUtil.setBackgroundFillColor(preferencesBinding.section9, TintUtil.color);

        if (getActivity() != null) {
            final String p = AppProfile.getCurrent(getActivity());
            preferencesBinding.profileLetter.setText(TxtUtils.getFirstLetter(p));
            preferencesBinding.profileLetter.setBackgroundDrawable(AppProfile.getProfileColorDrawable(getActivity(), TintUtil.color));
            preferencesBinding.profileLetter.setContentDescription(p + " " + getString(R.string.profile));
        }

        if (AppState.get().appTheme == AppState.THEME_INK) {
            TxtUtils.setInkTextView(preferencesBinding.getRoot().getRootView());
        }
    }

    @Subscribe
    public void updateSyncInfo(GDriveSycnEvent event) {
        String gdriveInfo = GFile.getDisplayInfo(getActivity());

        if (TxtUtils.isEmpty(gdriveInfo)) {
            AppSP.get().isEnableSync = false;
            preferencesBinding.syncInfo.setVisibility(View.GONE);
            preferencesBinding.signIn.setText(R.string.sign_in);
            TxtUtils.underlineTextView(preferencesBinding.signIn);
            preferencesBinding.signIn.setOnClickListener(v -> {
                GFile.init(getActivity());
                updateSyncInfo(null);
            });
        } else {
            preferencesBinding.syncInfo.setVisibility(View.VISIBLE);
            preferencesBinding.syncInfo.setText(gdriveInfo);
            preferencesBinding.signIn.setText(R.string.sign_out);
            TxtUtils.underlineTextView(preferencesBinding.signIn);

            preferencesBinding.signIn.setOnClickListener(v -> {
                AppSP.get().isEnableSync = false;
                AppSP.get().syncRootID = "";
                AppSP.get().syncTime = 0;
                GFile.logout(getActivity());
                updateSyncInfo(null);
            });
        }

        preferencesBinding.isEnableSync.setChecked(AppSP.get().isEnableSync);
        onSync(null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSync(MessageSync msg) {
        if (AppSP.get().syncTime > 0) {
            final Date date = new Date(AppSP.get().syncTime);
            String format = "";
            if (DateUtils.isToday(AppSP.get().syncTime)) {
                format = getString(R.string.today) + " " + DateFormat.getTimeInstance().format(date);
            } else {
                format = DateFormat.getDateTimeInstance().format(date);
            }

            String status = AppSP.get().syncTimeStatus == MessageSync.STATE_SUCCESS ? getString(R.string.success) : getString(R.string.fail);
            if (AppSP.get().syncTimeStatus == MessageSync.STATE_VISIBLE) {
                status = "...";
            }

            preferencesBinding.syncInfo2.setText(format + " - " + status);
            preferencesBinding.syncInfo2.setVisibility(View.VISIBLE);
        } else {
            preferencesBinding.syncInfo2.setText("");
            preferencesBinding.syncInfo2.setVisibility(View.GONE);
            preferencesBinding.syncHeader.setText(R.string.sync_google_drive);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        preferencesBinding = PreferencesBinding.inflate(inflater, container, false);

        onSync(null);
        preferencesBinding.syncHeader.setOnClickListener((in) -> Dialogs.showSyncLOGDialog(getActivity()));

        preferencesBinding.isEnableSync.setChecked(AppSP.get().isEnableSync);
        preferencesBinding.isEnableSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSP.get().isEnableSync = isChecked;
            if (isChecked && getActivity() != null) {
                if (GoogleSignIn.getLastSignedInAccount(getActivity()) == null) {
                    GFile.init(getActivity());
                } else {
                    GFile.runSyncService(getActivity());
                }
            }
        });

        preferencesBinding.isEnableSyncSettings.setOnClickListener(v -> {
            final CheckBox isSyncPullToRefresh = new CheckBox(getActivity());
            isSyncPullToRefresh.setText(R.string.pull_to_start_sync);
            isSyncPullToRefresh.setChecked(BookCSS.get().isSyncPullToRefresh);
            isSyncPullToRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> BookCSS.get().isSyncPullToRefresh = isChecked);

            final CheckBox isSyncWifiOnly = new CheckBox(getActivity());
            isSyncWifiOnly.setText(R.string.wifi_sync_only);
            isSyncWifiOnly.setChecked(BookCSS.get().isSyncWifiOnly);
            isSyncWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> BookCSS.get().isSyncWifiOnly = isChecked);

            final CheckBox isShowSyncWheel = new CheckBox(getActivity());
            isShowSyncWheel.setText(getString(R.string.animate_sync_progress));
            isShowSyncWheel.setChecked(BookCSS.get().isSyncAnimation);
            isShowSyncWheel.setOnCheckedChangeListener((buttonView, isChecked) -> BookCSS.get().isSyncAnimation = isChecked);

            AlertDialogs.showViewDialog(getActivity(), null, isSyncPullToRefresh, isSyncWifiOnly, isShowSyncWheel);
        });

        updateSyncInfo(null);

        preferencesBinding.sectionSync.setVisibility(AppsConfig.IS_FDROID ? View.GONE : View.VISIBLE);

        // tabs position
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2));

        final Handler handler = new Handler();
        final Runnable ask2 = () -> {
            if (getActivity() == null) {
                return;
            }
            AlertDialogs.showDialog(getActivity(), getActivity().getString(R.string.you_neet_to_apply_the_new_settings),
                    getString(R.string.ok), () -> preferencesBinding.tabsApply.performClick(), null);
        };

        final int timeout = 1500;
        preferencesBinding.isShowPrefAsMenu.setSaveEnabled(false);

        final Runnable dragLinear = () -> {
            preferencesBinding.dragLinearLayout.removeAllViews();
            for (UITab tab : UITab.getOrdered()) {
                if (AppsConfig.IS_FDROID && tab == UITab.CloudsFragment) {
                    continue;
                }

                if (AppsConfig.IS_FDROID && tab == UITab.OpdsFragment) {
                    continue;
                }

                final ItemTabLineBinding lineBinding =
                        ItemTabLineBinding.inflate(LayoutInflater.from(getActivity()), null, false);
                if (AppState.get().appTheme == AppState.THEME_DARK_OLED || AppState.get().appTheme == AppState.THEME_DARK) {
                    lineBinding.getRoot().setBackgroundColor(Color.BLACK);
                }

                lineBinding.text1.setText(tab.getName());
                lineBinding.isVisible.setSaveEnabled(false);
                lineBinding.isVisible.setChecked(tab.isVisible());
                lineBinding.isVisible.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    handler.removeCallbacks(ask2);
                    handler.postDelayed(ask2, timeout);

                    if (tab == UITab.PrefFragment) {
                        preferencesBinding.isShowPrefAsMenu.setChecked(!isChecked);
                    }
                });
                lineBinding.image1.setImageResource(tab.getIcon());
                TintUtil.setTintImageWithAlpha(lineBinding.image1, TintUtil.COLOR_TINT_GRAY);
                lineBinding.getRoot().setTag(tab.getIndex());
                preferencesBinding.dragLinearLayout.addView(lineBinding.getRoot(), layoutParams);
            }

            for (int i = 0; i < preferencesBinding.dragLinearLayout.getChildCount(); i++) {
                View child = preferencesBinding.dragLinearLayout.getChildAt(i);
                View handle = child.findViewById(R.id.imageDrag);
                preferencesBinding.dragLinearLayout.setViewDraggable(child, handle);
            }
        };
        dragLinear.run();
        TxtUtils.underlineTextView(preferencesBinding.tabsApply).setOnClickListener(v -> {
            handler.removeCallbacks(ask2);
            synchronized (AppState.get().tabsOrder7) {
                AppState.get().tabsOrder7 = "";
                for (int i = 0; i < preferencesBinding.dragLinearLayout.getChildCount(); i++) {
                    View child = preferencesBinding.dragLinearLayout.getChildAt(i);
                    boolean isVisible = ((CheckBox) child.findViewById(R.id.isVisible)).isChecked();
                    AppState.get().tabsOrder7 += child.getTag() + "#" + (isVisible ? "1" : "0") + ",";
                }
                AppState.get().tabsOrder7 = TxtUtils.replaceLast(AppState.get().tabsOrder7, ",", "");
                LOG.d("tabsApply", AppState.get().tabsOrder7);
            }

            if (UITab.isShowCloudsPreferences()) {
                Clouds.get().init(getActivity());
            }
            onTheme();
        });

        preferencesBinding.isShowPrefAsMenu.setChecked(AppState.get().tabsOrder7.contains(UITab.PrefFragment.index + "#0"));
        preferencesBinding.isShowPrefAsMenu.setOnCheckedChangeListener((buttonView, isChecked) -> {
            handler.removeCallbacks(ask2);
            handler.postDelayed(ask2, timeout);
            synchronized (AppState.get().tabsOrder7) {
                if (isChecked) {
                    AppState.get().tabsOrder7 = AppState.get().tabsOrder7.replace(UITab.PrefFragment.index + "#1", UITab.PrefFragment.index + "#0");
                } else {
                    AppState.get().tabsOrder7 = AppState.get().tabsOrder7.replace(UITab.PrefFragment.index + "#0", UITab.PrefFragment.index + "#1");
                }
            }
            dragLinear.run();
        });

        TxtUtils.underlineTextView(preferencesBinding.tabsDefault).setOnClickListener(v -> {
            handler.removeCallbacks(ask2);

            AlertDialogs.showOkDialog(getActivity(), getString(R.string.restore_defaults_full), () -> {
                synchronized (AppState.get().tabsOrder7) {
                    AppState.get().tabsOrder7 = AppState.DEFAULTS_TABS_ORDER;
                }
                onTheme();
            });
        });

        onTintChanged();

        final int max = Dips.pxToDp(Dips.screenMinWH() / 2) - 2 * 4;

        preferencesBinding.coverSmallSize.init(40, max, AppState.get().coverSmallSize);
        preferencesBinding.coverSmallSize.setOnSeekChanged(result -> {
            TempHolder.listHash++;
            AppState.get().coverSmallSize = result;
            return false;
        });

        preferencesBinding.coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize), AppState.get().coverBigSize);
        preferencesBinding.coverBigSize.setOnSeekChanged(result -> {
            TempHolder.listHash++;
            AppState.get().coverBigSize = result;
            return false;
        });

        preferencesBinding.columnsCount.setText("" + Dips.screenWidthDP() / AppState.get().coverBigSize);
        TxtUtils.underlineTextView(preferencesBinding.columnsCount);
        preferencesBinding.columnsCount.setOnClickListener(v -> {
            PopupMenu p = new PopupMenu(getContext(), preferencesBinding.columnsCount);
            for (int i = 1; i <= 8; i++) {
                final int k = i;
                p.getMenu().add("" + k).setOnMenuItemClickListener(item -> {
                    int result = Dips.screenWidthDP() / k - 8;

                    TempHolder.listHash++;
                    AppState.get().coverBigSize = result;

                    preferencesBinding.columnsCount.setText("" + k);
                    TxtUtils.underlineTextView(preferencesBinding.columnsCount);

                    preferencesBinding.coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize), AppState.get().coverBigSize);
                    return false;
                });
            }

            p.show();
        });

        TxtUtils.underlineTextView(preferencesBinding.columnsDefault);
        preferencesBinding.columnsDefault.setOnClickListener(v -> {
            if (getActivity() == null) {
                return;
            }

            AlertDialogs.showOkDialog(getActivity(), getActivity().getString(R.string.restore_defaults_full), () -> {
                IMG.clearDiscCache();
                IMG.clearMemoryCache();
                AppState.get().coverBigSize = (int) (((Dips.screenWidthDP() / (Dips.screenWidthDP() / 120)) - 8) * (Dips.isXLargeScreen() ? 1.5f : 1));
                AppState.get().coverSmallSize = 80;
                TempHolder.listHash++;

                preferencesBinding.columnsCount.setText("" + Dips.screenWidthDP() / AppState.get().coverBigSize);
                TxtUtils.underlineTextView(preferencesBinding.columnsCount);

                preferencesBinding.coverSmallSize.init(40, max, AppState.get().coverSmallSize);
                preferencesBinding.coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize), AppState.get().coverBigSize);
            });
        });

        preferencesBinding.scroll.setVerticalScrollBarEnabled(false);

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
            preferencesBinding.scroll.setBackgroundColor(Color.BLACK);
        }

        preferencesBinding.section6.setText(String.format("%s: %s", getString(R.string.product), Apps.getApplicationName(getActivity())));
        // ((TextView) findViewById(R.id.appName)).setText(AppsConfig.APP_NAME);

        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = packageInfo.versionName + " (" + LibreraApp.MUPDF_VERSION + "-" + BuildConfig.FLAVOR + ")";
            if (Dips.isEInk()) {
                version += " INK";
            }
            if (AppsConfig.IS_BETA) {
                version += "\n MODEL: " + Build.MODEL;
                version += "\n BRAND: " + Build.BRAND;
                version += "\n PRODUCT: " + Build.PRODUCT;
                version += "\n MANUFACTURER: " + Build.MANUFACTURER;
                version += "\n DEVICE: " + Build.DEVICE;
                version += "\n REFRESH: " + Dips.getRefreshRate();
                version += "\n W x H: " + Dips.screenWidthDP() + " x " + Dips.screenHeightDP();
                version += "\n Night: " + Apps.isNight(getActivity());
            }

            // ((TextView) inflate.findViewById(R.id.pVersion)).setText(String.format("%s:
            // %s (%s)", getString(R.string.version), version, AppsConfig.MUPDF_VERSION));
            preferencesBinding.pVersion.setText(String.format("%s: %s", getString(R.string.version), version));
        } catch (final NameNotFoundException e) {
        }

        TxtUtils.underlineTextView(preferencesBinding.onCloseApp);
        preferencesBinding.onCloseApp.setOnClickListener(v -> getActivity().finish());

        preferencesBinding.fullscreen.setText(DocumentController.getFullScreenName(getActivity(), AppState.get().fullScreenMainMode));

        TxtUtils.underlineTextView(preferencesBinding.fullscreen);

        preferencesBinding.fullscreen.setOnClickListener(v -> DocumentController.showFullScreenPopup(getActivity(), v, id -> {
            AppState.get().fullScreenMainMode = id;
            preferencesBinding.fullscreen.setText(DocumentController.getFullScreenName(getActivity(), AppState.get().fullScreenMainMode));
            TxtUtils.underlineTextView(preferencesBinding.fullscreen);
            DocumentController.chooseFullScreen(getActivity(), AppState.get().fullScreenMainMode);
            return true;
        }, AppState.get().fullScreenMainMode));

        String tabText = AppState.get().tapPositionTop ? getString(R.string.top) : getString(R.string.bottom);
        tabText += AppState.get().tabWithNames ? "" : " - " + getString(R.string.icons_only);
        preferencesBinding.tapPositionTop.setText(tabText);

        TxtUtils.underlineTextView(preferencesBinding.tapPositionTop);

        preferencesBinding.tapPositionTop.setOnClickListener(v -> {
            MyPopupMenu popup = new MyPopupMenu(getActivity(), v);
            popup.getMenu().add(R.string.top).setOnMenuItemClickListener(item -> {
                AppState.get().tapPositionTop = true;
                AppState.get().tabWithNames = true;
                onTheme();
                return false;
            });

            popup.getMenu().add(R.string.bottom).setOnMenuItemClickListener(item -> {
                AppState.get().tapPositionTop = false;
                AppState.get().tabWithNames = true;
                onTheme();
                return false;
            });

            popup.getMenu().add(getString(R.string.top) + " - " + getString(R.string.icons_only)).setOnMenuItemClickListener(item -> {
                AppState.get().tapPositionTop = true;
                AppState.get().tabWithNames = false;
                onTheme();
                return false;
            });

            popup.getMenu().add(getString(R.string.bottom) + " - " + getString(R.string.icons_only)).setOnMenuItemClickListener(item -> {
                AppState.get().tapPositionTop = false;
                AppState.get().tabWithNames = false;
                onTheme();
                return false;
            });

            popup.show();

        });

        preferencesBinding.screenOrientation.setText(DocumentController.getRotationText());
        TxtUtils.underlineTextView(preferencesBinding.screenOrientation);

        preferencesBinding.screenOrientation.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            for (int i = 0; i < DocumentController.orientationIds.size(); i++) {
                final int j = i;
                final int name = DocumentController.orientationTexts.get(i);
                menu.getMenu().add(name).setOnMenuItemClickListener(item -> {
                    AppState.get().orientation = DocumentController.orientationIds.get(j);
                    preferencesBinding.screenOrientation.setText(DocumentController.orientationTexts.get(j));
                    TxtUtils.underlineTextView(preferencesBinding.screenOrientation);
                    DocumentController.doRotation(getActivity());
                    return false;
                });
            }
            menu.show();
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

        preferencesBinding.closeMenu.setOnClickListener(v -> closeLeftMenu());
        preferencesBinding.closeMenu.setVisibility(TxtUtils.visibleIf(AppState.get().isEnableAccessibility));

        preferencesBinding.onKeyCode.setOnClickListener(v -> new KeyCodeDialog(getActivity(), onCloseDialog));

        preferencesBinding.isEnableAccessibility.setChecked(AppState.get().isEnableAccessibility);
        preferencesBinding.isEnableAccessibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isEnableAccessibility = isChecked;

            if (isChecked) {
                AppState.get().tabWithNames = false;
                AppState.get().tapPositionTop = true;
                BookCSS.get().appFontScale = 1.3f;
                AppState.get().isScrollAnimation = false;
                AppSP.get().isFirstTimeVertical = false;
                AppSP.get().isFirstTimeHorizontal = false;
            } else {
                BookCSS.get().appFontScale = 1.0f;
            }

            onTheme();
        });

        preferencesBinding.themeColor.setOnClickListener(v -> {
            PopupMenu p = new PopupMenu(getContext(), preferencesBinding.themeColor);
            p.getMenu().add(R.string.light).setOnMenuItemClickListener(item -> {
                AppState.get().appTheme = AppState.THEME_LIGHT;

                AppState.get().contrastImage = 0;
                AppState.get().brigtnessImage = 0;
                AppState.get().bolderTextOnImage = false;
                AppState.get().isEnableBC = false;

                IMG.clearDiscCache();
                IMG.clearMemoryCache();
                onTheme();

                return false;
            });
            p.getMenu().add(R.string.black).setOnMenuItemClickListener(item -> {
                AppState.get().appTheme = AppState.THEME_DARK;

                AppState.get().contrastImage = 0;
                AppState.get().brigtnessImage = 0;
                AppState.get().bolderTextOnImage = false;
                AppState.get().isEnableBC = false;

                IMG.clearDiscCache();
                IMG.clearMemoryCache();

                onTheme();
                return false;
            });
            p.getMenu().add(R.string.dark_oled).setOnMenuItemClickListener(item -> {
                AppState.get().appTheme = AppState.THEME_DARK_OLED;

                AppState.get().contrastImage = 0;
                AppState.get().brigtnessImage = 0;
                AppState.get().bolderTextOnImage = false;
                AppState.get().isEnableBC = false;
                AppState.get().tintColor = Color.BLACK;

                IMG.clearDiscCache();
                IMG.clearMemoryCache();

                onTheme();
                return false;
            });
            p.getMenu().add("Ink").setOnMenuItemClickListener(item -> {
                IMG.clearDiscCache();
                IMG.clearMemoryCache();

                onEink();
                return false;
            });
            p.show();
        });

        preferencesBinding.appLang.setText(DialogTranslateFromTo.getLanuageByCode(AppState.get().appLang));
        TxtUtils.underlineTextView(preferencesBinding.appLang);

        preferencesBinding.appLang.setOnClickListener(v -> {
            final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

            List<String> langs = new ArrayList<>();
            for (String code : AppState.langCodes) {
                langs.add(DialogTranslateFromTo.getLanuageByCode(code) + ":" + code);
            }
            Collections.sort(langs);

            popupMenu.getMenu().add(R.string.system_language).setOnMenuItemClickListener(item -> {
                TxtUtils.underlineTextView(preferencesBinding.appLang);
                AppState.get().appLang = AppState.MY_SYSTEM_LANG;
                TempHolder.get().forseAppLang = true;
                MyContextWrapper.wrap(getContext());
                onTheme();
                return false;
            });

            for (int i = 0; i < langs.size(); i++) {
                String[] all = langs.get(i).split(":");
                final String name = all[0];
                final String code = all[1];
                popupMenu.getMenu().add(name).setOnMenuItemClickListener(item -> {
                    AppState.get().appLang = code;
                    TxtUtils.underlineTextView(preferencesBinding.appLang);
                    onTheme();
                    return false;
                });
            }
            popupMenu.show();
        });

        preferencesBinding.appFontScale.setText(getFontName(BookCSS.get().appFontScale));
        TxtUtils.underlineTextView(preferencesBinding.appFontScale);
        preferencesBinding.appFontScale.setOnClickListener(v -> {
            final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            for (float i = 0.7f; i < 2.1f; i += 0.1) {
                final float number = i;
                popupMenu.getMenu().add(getFontName(number)).setOnMenuItemClickListener(item -> {
                    BookCSS.get().appFontScale = number;
                    onTheme();
                    return false;
                });
            }
            popupMenu.show();
        });

        preferencesBinding.onMailSupport.setText(TxtUtils.underline(getString(R.string.my_email)));
        preferencesBinding.onMailSupport.setOnClickListener(v -> onEmail());

        preferencesBinding.isRememberMode.setChecked(AppState.get().isRememberMode);
        preferencesBinding.isRememberMode.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isRememberMode = isChecked);

        preferencesBinding.selectedOpenMode.setOnClickListener(v -> {
            final PopupMenu popupMenu = new PopupMenu(preferencesBinding.selectedOpenMode.getContext(), preferencesBinding.selectedOpenMode);

            final MenuItem advanced_mode = popupMenu.getMenu().add(AppState.get().nameVerticalMode);
            advanced_mode.setOnMenuItemClickListener(item -> {
                AppSP.get().readingMode = AppState.READING_MODE_SCROLL;
                checkOpenWithSpinner();
                return false;
            });

            final MenuItem easyMode = popupMenu.getMenu().add(AppState.get().nameHorizontalMode);
            easyMode.setOnMenuItemClickListener(item -> {
                AppSP.get().readingMode = AppState.READING_MODE_BOOK;
                checkOpenWithSpinner();
                return false;
            });
            final MenuItem music_mode = popupMenu.getMenu().add(AppState.get().nameMusicianMode);
            music_mode.setOnMenuItemClickListener(item -> {
                AppSP.get().readingMode = AppState.READING_MODE_MUSICIAN;
                checkOpenWithSpinner();
                return false;
            });
            final MenuItem tags = popupMenu.getMenu().add(getString(R.string.tag_manager));
            tags.setOnMenuItemClickListener(item -> {
                AppSP.get().readingMode = AppState.READING_MODE_TAG_MANAGER;
                checkOpenWithSpinner();
                return false;
            });
            final MenuItem owith = popupMenu.getMenu().add(getString(R.string.open_with));
            owith.setOnMenuItemClickListener(item -> {
                AppSP.get().readingMode = AppState.READING_MODE_OPEN_WITH;
                checkOpenWithSpinner();
                return false;
            });
            popupMenu.show();
        });

        preferencesBinding.moreModeSettings.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final DialogCustomReadingModesBinding readingModesBinding =
                    DialogCustomReadingModesBinding.inflate(LayoutInflater.from(getActivity()), null, false);
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_custom_reading_modes, null, false);
            builder.setView(readingModesBinding.getRoot());

            readingModesBinding.prefScrollMode.setText(AppState.get().prefScrollMode);
            readingModesBinding.prefBookMode.setText(AppState.get().prefBookMode);
            readingModesBinding.prefMusicianMode.setText(AppState.get().prefMusicianMode);

            readingModesBinding.isPrefFormatMode.setChecked(AppState.get().isPrefFormatMode);

            readingModesBinding.prefRestore.setOnClickListener(v1 -> AlertDialogs.showDialog(getActivity(),
                    getString(R.string.restore_defaults_full), getString(R.string.ok), () -> {
                        AppState.get().isPrefFormatMode = false;
                        AppState.get().prefScrollMode = AppState.PREF_SCROLL_MODE;
                        AppState.get().prefBookMode = AppState.PREF_BOOK_MODE;
                        AppState.get().prefMusicianMode = AppState.PREF_MUSIC_MODE;

                        readingModesBinding.isPrefFormatMode.setChecked(AppState.get().isPrefFormatMode);
                        readingModesBinding.prefScrollMode.setText(AppState.get().prefScrollMode);
                        readingModesBinding.prefBookMode.setText(AppState.get().prefBookMode);
                        readingModesBinding.prefMusicianMode.setText(AppState.get().prefMusicianMode);
                    }, null));

            builder.setPositiveButton(R.string.save, (dialog, id) -> {
                Keyboards.close(readingModesBinding.prefScrollMode);
                AppState.get().isPrefFormatMode = readingModesBinding.isPrefFormatMode.isChecked();
                AppState.get().prefScrollMode = readingModesBinding.prefScrollMode.getText().toString();
                AppState.get().prefBookMode = readingModesBinding.prefBookMode.getText().toString();
                AppState.get().prefMusicianMode = readingModesBinding.prefMusicianMode.getText().toString();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, id) -> { });
            builder.show();
        });

        checkOpenWithSpinner();

        preferencesBinding.isCropBookCovers.setOnCheckedChangeListener(null);
        preferencesBinding.isCropBookCovers.setChecked(AppState.get().isCropBookCovers);
        preferencesBinding.isCropBookCovers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isCropBookCovers = isChecked;
            TempHolder.listHash++;
        });

        preferencesBinding.isBookCoverEffect.setOnCheckedChangeListener(null);
        preferencesBinding.isBookCoverEffect.setChecked(AppState.get().isBookCoverEffect);
        preferencesBinding.isBookCoverEffect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isBookCoverEffect = isChecked;
            IMG.clearMemoryCache();
            IMG.clearDiscCache();

            TempHolder.listHash++;
            if (isChecked) {
                preferencesBinding.isCropBookCovers.setEnabled(false);
                preferencesBinding.isCropBookCovers.setChecked(true);
            } else {
                preferencesBinding.isCropBookCovers.setEnabled(true);
            }
        });

        preferencesBinding.isBorderAndShadow.setOnCheckedChangeListener(null);
        preferencesBinding.isBorderAndShadow.setChecked(AppState.get().isBorderAndShadow);
        preferencesBinding.isBorderAndShadow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isBorderAndShadow = isChecked;
            TempHolder.listHash++;
        });

        preferencesBinding.isShowImages.setOnCheckedChangeListener(null);
        preferencesBinding.isShowImages.setChecked(AppState.get().isShowImages);
        preferencesBinding.isShowImages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isShowImages = isChecked;
            TempHolder.listHash++;
            preferencesBinding.isCropBookCovers.setEnabled(AppState.get().isShowImages);
            preferencesBinding.isBookCoverEffect.setEnabled(AppState.get().isShowImages);
            preferencesBinding.isBorderAndShadow.setEnabled(AppState.get().isShowImages);
        });
        preferencesBinding.isCropBookCovers.setEnabled(AppState.get().isShowImages);
        preferencesBinding.isBookCoverEffect.setEnabled(AppState.get().isShowImages);
        preferencesBinding.isBorderAndShadow.setEnabled(AppState.get().isShowImages);

        preferencesBinding.isLoopAutoplay.setChecked(AppState.get().isLoopAutoplay);
        preferencesBinding.isLoopAutoplay.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isLoopAutoplay = isChecked);

        preferencesBinding.isOpenLastBook.setChecked(AppState.get().isOpenLastBook);
        preferencesBinding.isOpenLastBook.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isOpenLastBook = isChecked);

        preferencesBinding.isShowCloseAppDialog.setChecked(AppState.get().isShowCloseAppDialog);
        preferencesBinding.isShowCloseAppDialog.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isShowCloseAppDialog = isChecked);

        final Runnable ask = () -> {
            LOG.d("timer ask");
            if (getActivity() == null) {
                return;
            }

            AlertDialogs.showDialog(getActivity(), getActivity().getString(R.string.you_need_to_update_the_library),
                    getString(R.string.ok), this::onScan, null);
        };

        TxtUtils.underlineTextView(preferencesBinding.moreLibrarySettings).setOnClickListener(v -> {
            final CheckBox isFirstSurname = new CheckBox(v.getContext());
            isFirstSurname.setText(getString(R.string.in_the_author_s_name_first_the_surname));

            final CheckBox isSkipFolderWithNOMEDIA = new CheckBox(v.getContext());
            isSkipFolderWithNOMEDIA.setText(getString(R.string.ignore_folder_scan_if_nomedia_file_exists));

            final CheckBox isAuthorTitleFromMetaPDF = new CheckBox(v.getContext());
            isAuthorTitleFromMetaPDF.setText(R.string.displaying_the_author_and_title_of_the_pdf_book_from_the_meta_tags);

            final CheckBox isShowOnlyOriginalFileNames = new CheckBox(v.getContext());
            isShowOnlyOriginalFileNames.setText(R.string.display_original_file_names_without_metadata);

            final CheckBox isUseCalibreOpf = new CheckBox(v.getContext());
            isUseCalibreOpf.setText(R.string.use_calibre_metadata);

            final CheckBox isDisplayAnnotation = new CheckBox(v.getContext());
            isDisplayAnnotation.setText(R.string.show_book_description);

            final AlertDialog d = AlertDialogs.showViewDialog(getActivity(), null,
                    isFirstSurname,
                    isSkipFolderWithNOMEDIA,
                    isShowOnlyOriginalFileNames,
                    isAuthorTitleFromMetaPDF,
                    isUseCalibreOpf,
                    isDisplayAnnotation);

            isFirstSurname.setChecked(AppState.get().isFirstSurname);
            isSkipFolderWithNOMEDIA.setChecked(AppState.get().isSkipFolderWithNOMEDIA);
            isAuthorTitleFromMetaPDF.setChecked(AppState.get().isAuthorTitleFromMetaPDF);
            isShowOnlyOriginalFileNames.setChecked(AppState.get().isShowOnlyOriginalFileNames);
            isUseCalibreOpf.setChecked(AppState.get().isUseCalibreOpf);
            isDisplayAnnotation.setChecked(AppState.get().isDisplayAnnotation);

            final OnCheckedChangeListener listener = (buttonView, isChecked) -> {
                AppState.get().isFirstSurname = isFirstSurname.isChecked();
                AppState.get().isSkipFolderWithNOMEDIA = isSkipFolderWithNOMEDIA.isChecked();
                AppState.get().isAuthorTitleFromMetaPDF = isAuthorTitleFromMetaPDF.isChecked();
                AppState.get().isShowOnlyOriginalFileNames = isShowOnlyOriginalFileNames.isChecked();
                AppState.get().isUseCalibreOpf = isUseCalibreOpf.isChecked();
                AppState.get().isDisplayAnnotation = isDisplayAnnotation.isChecked();

                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(ask, timeout);
                handler.postDelayed(d::dismiss, timeout);
            };

            isFirstSurname.setOnCheckedChangeListener(listener);
            isAuthorTitleFromMetaPDF.setOnCheckedChangeListener(listener);
            isSkipFolderWithNOMEDIA.setOnCheckedChangeListener(listener);
            isShowOnlyOriginalFileNames.setOnCheckedChangeListener(listener);
            isUseCalibreOpf.setOnCheckedChangeListener(listener);
            isDisplayAnnotation.setOnCheckedChangeListener(listener);
        });

        ////
        preferencesBinding.supportPDF.setChecked(AppState.get().supportPDF);
        preferencesBinding.supportPDF.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportPDF = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportXPS.setChecked(AppState.get().supportXPS);
        preferencesBinding.supportXPS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportXPS = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportDJVU.setChecked(AppState.get().supportDJVU);
        preferencesBinding.supportDJVU.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportDJVU = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportEPUB.setChecked(AppState.get().supportEPUB);
        preferencesBinding.supportEPUB.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportEPUB = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportFB2.setChecked(AppState.get().supportFB2);
        preferencesBinding.supportFB2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportFB2 = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportTXT.setChecked(AppState.get().supportTXT);
        preferencesBinding.supportTXT.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportTXT = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportMOBI.setChecked(AppState.get().supportMOBI);
        preferencesBinding.supportMOBI.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportMOBI = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportRTF.setChecked(AppState.get().supportRTF);
        preferencesBinding.supportRTF.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportRTF = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportDOCX.setChecked(AppState.get().supportDOCX);
        preferencesBinding.supportDOCX.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportDOCX = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportODT.setChecked(AppState.get().supportODT);
        preferencesBinding.supportDOCX.setText(AppsConfig.isDOCXSupported ? "DOC/DOCX" : "DOC");
        preferencesBinding.supportODT.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportODT = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportCBZ.setChecked(AppState.get().supportCBZ);
        preferencesBinding.supportCBZ.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportCBZ = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportZIP.setChecked(AppState.get().supportZIP);
        preferencesBinding.supportZIP.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportZIP = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportArch.setChecked(AppState.get().supportArch);
        preferencesBinding.supportArch.setText(getString(R.string.archives) + " (RAR/7z/...)");
        preferencesBinding.supportArch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportArch = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.supportOther.setChecked(AppState.get().supportOther);
        preferencesBinding.supportOther.setText(getString(R.string.other) + " (CHM/...)");
        preferencesBinding.supportOther.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportOther = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        preferencesBinding.isDisplayAllFilesInFolder.setChecked(AppState.get().isDisplayAllFilesInFolder);
        preferencesBinding.isDisplayAllFilesInFolder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isDisplayAllFilesInFolder = isChecked;
            TempHolder.listHash++;
        });
        // app password
        preferencesBinding.isAppPassword.setChecked(PasswordState.get().hasPassword() && AppState.get().isAppPassword);
        preferencesBinding.isAppPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && PasswordState.get().hasPassword()) {
                AppState.get().isAppPassword = true;
            } else if (!PasswordState.get().hasPassword()) {
                PasswordDialog.showDialog(getActivity(), true, () ->
                        preferencesBinding.isAppPassword.setChecked(PasswordState.get().hasPassword())
                );
            } else {
                AppState.get().isAppPassword = false;
                preferencesBinding.isAppPassword.setChecked(false);
            }
        });

        TxtUtils.underlineTextView(preferencesBinding.appPassword).setOnClickListener(v ->
                PasswordDialog.showDialog(getActivity(), true, () -> {
                            if (PasswordState.get().hasPassword()) {
                                preferencesBinding.isAppPassword.setChecked(true);
                                AppState.get().isAppPassword = true;
                            }
                        }
                )
        );

        // What is new
        preferencesBinding.isShowWhatIsNewDialog.setChecked(AppState.get().isShowWhatIsNewDialog);
        preferencesBinding.isShowWhatIsNewDialog.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isShowWhatIsNewDialog = isChecked);

        preferencesBinding.whatIsNew.setText(getString(R.string.what_is_new_in) + " " + Apps.getApplicationName(getActivity())
                + " " + Apps.getVersionName(getActivity()));
        TxtUtils.underlineTextView(preferencesBinding.whatIsNew);
        preferencesBinding.whatIsNew.setOnClickListener(v -> AndroidWhatsNew.show2(getActivity()));

        ///

        // BrightnessHelper.controlsWrapper(inflate, getActivity());

        preferencesBinding.onReverse.setOnCheckedChangeListener(null);
        preferencesBinding.onReverse.setChecked(AppState.get().isReverseKeys);
        preferencesBinding.onReverse.setOnCheckedChangeListener(reverseListener);

        preferencesBinding.onColorChoser.setOnClickListener(v -> { });

        initKeys();

        preferencesBinding.searchPaths.setText(JsonDB.fromHtml(BookCSS.get().searchPathsJson));
        preferencesBinding.searchPaths.setOnClickListener(v -> onFolderConfigDialog());

        TxtUtils.underlineTextView(preferencesBinding.onConfigPath);
        preferencesBinding.onConfigPath.setOnClickListener(v -> onFolderConfigDialog());

        TxtUtils.underlineTextView(preferencesBinding.importButton).setOnClickListener(v -> PrefDialogs.importDialog(getActivity()));

        TxtUtils.underlineTextView(preferencesBinding.exportButton).setOnClickListener(v -> PrefDialogs.exportDialog(getActivity()));

        TxtUtils.underlineTextView(preferencesBinding.migrationButton).setOnClickListener(v -> PrefDialogs.migrationDialog(getActivity()));

        // folders

        TxtUtils.underline(preferencesBinding.rootFolder, TxtUtils.smallPathFormat(AppSP.get().rootPath));
        preferencesBinding.rootFolder.setOnClickListener(v -> ChooserDialogFragment.chooseFolder(getActivity(),
                AppSP.get().rootPath).setOnSelectListener((nPath, dialog) -> {
            if (new File(nPath).canWrite()) {
                AppSP.get().rootPath = nPath;
                new File(nPath, "Fonts").mkdirs();
                TxtUtils.underline(preferencesBinding.rootFolder, TxtUtils.smallPathFormat(nPath));
                onTheme();
            } else {
                Toast.makeText(getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
            }
            dialog.dismiss();
            return false;
        }));

        TxtUtils.underline(preferencesBinding.fontFolder, TxtUtils.smallPathFormat(BookCSS.get().fontFolder));
        preferencesBinding.fontFolder.setOnClickListener(v -> ChooserDialogFragment.chooseFolder(getActivity(),
                BookCSS.get().fontFolder).setOnSelectListener((nPath, dialog) -> {
            BookCSS.get().fontFolder = nPath;
            TxtUtils.underline(preferencesBinding.fontFolder, TxtUtils.smallPathFormat(BookCSS.get().fontFolder));
            dialog.dismiss();
            return false;
        }));

        TxtUtils.underline(preferencesBinding.downloadFolder, TxtUtils.smallPathFormat(BookCSS.get().downlodsPath));
        preferencesBinding.downloadFolder.setOnClickListener(v -> ChooserDialogFragment.chooseFolder(getActivity(),
                BookCSS.get().downlodsPath).setOnSelectListener((nPath, dialog) -> {
            BookCSS.get().downlodsPath = nPath;
            TxtUtils.underline(preferencesBinding.downloadFolder, TxtUtils.smallPathFormat(BookCSS.get().downlodsPath));
            dialog.dismiss();
            return false;
        }));

        TxtUtils.underline(preferencesBinding.syncPath, TxtUtils.smallPathFormat(BookCSS.get().syncDropboxPath));
        preferencesBinding.syncPath.setOnClickListener(v -> ChooserDialogFragment.chooseFolder(getActivity(),
                BookCSS.get().syncDropboxPath).setOnSelectListener((nPath, dialog) -> {
            BookCSS.get().syncDropboxPath = nPath;
            TxtUtils.underline(preferencesBinding.downloadFolder, TxtUtils.smallPathFormat(BookCSS.get().syncDropboxPath));
            dialog.dismiss();
            return false;
        }));

        TxtUtils.underline(preferencesBinding.ttsFolder, TxtUtils.smallPathFormat(BookCSS.get().ttsSpeakPath));
        preferencesBinding.ttsFolder.setOnClickListener(v -> ChooserDialogFragment.chooseFolder(getActivity(),
                BookCSS.get().ttsSpeakPath).setOnSelectListener((nPath, dialog) -> {
            BookCSS.get().ttsSpeakPath = nPath;
            TxtUtils.underline(preferencesBinding.ttsFolder, TxtUtils.smallPathFormat(BookCSS.get().ttsSpeakPath));
            dialog.dismiss();
            return false;
        }));

        TxtUtils.underline(preferencesBinding.backupFolder, TxtUtils.smallPathFormat(BookCSS.get().backupPath));
        preferencesBinding.backupFolder.setOnClickListener(v -> ChooserDialogFragment.chooseFolder(getActivity(),
                BookCSS.get().backupPath).setOnSelectListener((nPath, dialog) -> {
            BookCSS.get().backupPath = nPath;
            TxtUtils.underline(preferencesBinding.backupFolder, TxtUtils.smallPathFormat(BookCSS.get().backupPath));
            dialog.dismiss();
            return false;
        }));

        // Widget Configuration
        preferencesBinding.widgetLayout.setText(AppState.get().widgetType == AppState.WIDGET_LIST ? R.string.list : R.string.grid);
        TxtUtils.underlineTextView(preferencesBinding.widgetLayout);

        preferencesBinding.widgetLayout.setOnClickListener(v -> {
            final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

            final MenuItem recent = popupMenu.getMenu().add(R.string.list);
            recent.setOnMenuItemClickListener(item -> {
                AppState.get().widgetType = AppState.WIDGET_LIST;
                preferencesBinding.widgetLayout.setText(R.string.list);
                TxtUtils.underlineTextView(preferencesBinding.widgetLayout);
                RecentUpates.updateAll();
                return false;
            });

            final MenuItem starred = popupMenu.getMenu().add(R.string.grid);
            starred.setOnMenuItemClickListener(item -> {
                AppState.get().widgetType = AppState.WIDGET_GRID;
                preferencesBinding.widgetLayout.setText(R.string.grid);
                TxtUtils.underlineTextView(preferencesBinding.widgetLayout);
                RecentUpates.updateAll();
                return false;
            });

            popupMenu.show();
        });

        preferencesBinding.widgetForRecent.setText(AppState.get().isStarsInWidget ? R.string.starred : R.string.recent);
        TxtUtils.underlineTextView(preferencesBinding.widgetForRecent);

        preferencesBinding.widgetForRecent.setOnClickListener(v -> {
            final PopupMenu popupMenu = new PopupMenu(preferencesBinding.widgetForRecent.getContext(), preferencesBinding.widgetForRecent);

            final MenuItem recent = popupMenu.getMenu().add(R.string.recent);
            recent.setOnMenuItemClickListener(item -> {
                AppState.get().isStarsInWidget = false;
                preferencesBinding.widgetForRecent.setText(AppState.get().isStarsInWidget ? R.string.starred : R.string.recent);
                TxtUtils.underlineTextView(preferencesBinding.widgetForRecent);

                RecentUpates.updateAll();
                return false;
            });

            final MenuItem starred = popupMenu.getMenu().add(R.string.starred);
            starred.setOnMenuItemClickListener(item -> {
                AppState.get().isStarsInWidget = true;
                preferencesBinding.widgetForRecent.setText(AppState.get().isStarsInWidget ? R.string.starred : R.string.recent);
                TxtUtils.underlineTextView(preferencesBinding.widgetForRecent);

                RecentUpates.updateAll();
                return false;
            });

            popupMenu.show();
        });

        preferencesBinding.widgetItemsCount.setText("" + AppState.get().widgetItemsCount);
        TxtUtils.underlineTextView(preferencesBinding.widgetItemsCount);
        preferencesBinding.widgetItemsCount.setOnClickListener(v -> {
            PopupMenu p = new PopupMenu(getContext(), preferencesBinding.columnsCount);
            for (int i = 1; i <= 50; i++) {
                final int k = i;
                p.getMenu().add("" + k).setOnMenuItemClickListener(item -> {
                    AppState.get().widgetItemsCount = k;
                    preferencesBinding.widgetItemsCount.setText("" + k);
                    TxtUtils.underlineTextView(preferencesBinding.widgetItemsCount);
                    RecentUpates.updateAll();
                    return false;
                });
            }

            p.show();
        });

        // dictionary
        preferencesBinding.isRememberDictionary.setChecked(AppState.get().isRememberDictionary);
        preferencesBinding.isRememberDictionary.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isRememberDictionary = isChecked);

        preferencesBinding.selectedDictionary.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
        preferencesBinding.selectedDictionary.setOnClickListener(v -> DialogTranslateFromTo.show(getActivity(), false,
                () -> preferencesBinding.selectedDictionary.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline()), false));

        preferencesBinding.onDayColor.setOnClickListener(v -> new ColorsDialog(getActivity(), true,
                AppState.get().colorDayText, AppState.get().colorDayBg, false, true, (colorText, colorBg) -> {
            preferencesBinding.onDayColor.setTextColor(colorText);
            preferencesBinding.onDayColor.setBackgroundColor(colorBg);

            AppState.get().colorDayText = colorText;
            AppState.get().colorDayBg = colorBg;

            IMG.clearDiscCache();
            IMG.clearMemoryCache();
        }));

        preferencesBinding.onNightColor.setOnClickListener(v -> new ColorsDialog(getActivity(), false, AppState.get().colorNightText,
                AppState.get().colorNightBg, false, true, (colorText, colorBg) -> {
            preferencesBinding.onNightColor.setTextColor(colorText);
            preferencesBinding.onNightColor.setBackgroundColor(colorBg);

            AppState.get().colorNightText = colorText;
            AppState.get().colorNightBg = colorBg;
        }));

        TextView onDefault = TxtUtils.underlineTextView(preferencesBinding.onDefaultColor);
        onDefault.setOnClickListener(v -> {
            AppState.get().colorDayText = AppState.COLOR_BLACK;
            AppState.get().colorDayBg = AppState.COLOR_WHITE;

            preferencesBinding.onDayColor.setTextColor(AppState.COLOR_BLACK);
            preferencesBinding.onDayColor.setBackgroundColor(AppState.COLOR_WHITE);

            AppState.get().colorNightText = AppState.COLOR_WHITE;
            AppState.get().colorNightBg = AppState.COLOR_BLACK;

            preferencesBinding.onNightColor.setTextColor(AppState.COLOR_WHITE);
            preferencesBinding.onNightColor.setBackgroundColor(AppState.COLOR_BLACK);
        });

        preferencesBinding.colorsLine.removeAllViews();

        for (String color : AppState.STYLE_COLORS) {
            final ItemColorBinding itemColorBinding =
                    ItemColorBinding.inflate(inflater, preferencesBinding.getRoot(), false);
            final int intColor = Color.parseColor(color);
            itemColorBinding.itColor.setBackgroundColor(intColor);
            itemColorBinding.itColor.setContentDescription(getString(R.string.color));

            preferencesBinding.colorsLine.addView(itemColorBinding.getRoot(),
                    new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

            itemColorBinding.getRoot().setOnClickListener(v -> {
                TintUtil.color = intColor;
                AppState.get().tintColor = intColor;
                TempHolder.listHash++;

                onTintChanged();
                sendNotifyTintChanged();

                AppProfile.save(getActivity());
            });
        }

        final ItemColorBinding itemColorBinding = ItemColorBinding.inflate(inflater, preferencesBinding.getRoot(), false);
        itemColorBinding.getRoot().setBackgroundColor(Color.TRANSPARENT);
        itemColorBinding.getRoot().setContentDescription(getString(R.string.color));
        itemColorBinding.itColor.setColorFilter(getResources().getColor(R.color.tint_gray));
        itemColorBinding.itColor.setImageResource(R.drawable.glyphicons_433_plus);
        itemColorBinding.itColor.setBackgroundColor(AppState.get().userColor);
        preferencesBinding.colorsLine.addView(itemColorBinding.getRoot(),
                new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));
        itemColorBinding.getRoot().setOnClickListener(v -> new HSVColorPickerDialog(getContext(),
                AppState.get().userColor, color -> {
            AppState.get().userColor = color;
            AppState.get().tintColor = color;
            TintUtil.color = color;
            itemColorBinding.itColor.setBackgroundColor(color);

            onTintChanged();
            sendNotifyTintChanged();

            AppProfile.save(getActivity());

            TempHolder.listHash++;
        }).show());

        underline(preferencesBinding.linksColor).setOnClickListener(v -> {
            closeLeftMenu();
            Dialogs.showLinksColorDialog(getActivity(), () -> {
                TempHolder.listHash++;
                onTintChanged();
                sendNotifyTintChanged();
                ((MainTabs2) getActivity()).updateCurrentFragment();

                TxtUtils.updateAllLinks(preferencesBinding.getRoot().getRootView());
            });
        });

        underline(preferencesBinding.onContrast).setOnClickListener(v -> Dialogs.showContrastDialogByUrl(getActivity(), () -> {
            IMG.clearDiscCache();
            IMG.clearMemoryCache();
            TempHolder.listHash++;
            notifyFragment();
        }));

        underline(preferencesBinding.onRateIt).setOnClickListener(v -> Urls.rateIT(getActivity()));

        underline(preferencesBinding.openWeb).setOnClickListener(v -> Urls.open(getActivity(), WWW_SITE));

        underline(preferencesBinding.openBeta).setOnClickListener(v -> Urls.open(getActivity(), WWW_BETA_SITE));

        underline(preferencesBinding.openWiki).setOnClickListener(v -> Urls.open(getActivity(), WWW_WIKI_SITE));

        underline(preferencesBinding.onTelegram).setOnClickListener(v -> Urls.open(getActivity(), "https://t.me/LibreraReader"));

        TxtUtils.underlineTextView(preferencesBinding.downloadPRO);
        ((View) preferencesBinding.downloadPRO.getParent()).setOnClickListener(v -> Urls.openPdfPro(getActivity()));

        if (AppsConfig.checkIsProInstalled(getActivity())) {
            ((View) preferencesBinding.downloadPRO.getParent()).setVisibility(View.GONE);
        }

        preferencesBinding.cleanRecent.setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            builder.setMessage(getString(R.string.clear_all_recent) + "?");
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                //BookmarksData.get().cleanRecent();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                // TODO Auto-generated method stub
            });
            builder.show();
        });

        preferencesBinding.cleanBookmarks.setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            builder.setMessage(getString(R.string.clear_all_bookmars) + "?");
            builder.setPositiveButton(R.string.yes, (dialog, which) -> BookmarksData.get().cleanBookmarks());
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                // TODO Auto-generated method stub
            });
            builder.show();
        });

        // licences link
        underline(preferencesBinding.libraryLicenses).setOnClickListener(v -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.licenses_for_libraries);

            WebView wv = new WebView(getActivity());
            wv.loadUrl("file:///android_asset/licenses.html");
            wv.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view1, String url) {
                    view1.loadUrl(url);
                    return true;
                }
            });

            alert.setView(wv);
            alert.setNegativeButton(R.string.close, (dialog, id) -> dialog.dismiss());
            AlertDialog create = alert.create();
            create.setOnDismissListener(dialog -> Keyboards.hideNavigation(getActivity()));
            create.show();
        });

        TxtUtils.underlineTextView(preferencesBinding.docSearch).setOnClickListener(v -> MultiDocSearchDialog.show(getActivity()));
        // convert
        TxtUtils.underlineTextView(preferencesBinding.docConverter);
        preferencesBinding.docConverter.setOnClickListener(v -> {
            PopupMenu p = new PopupMenu(getContext(), v);
            for (final String id : AppState.CONVERTERS.keySet()) {
                p.getMenu().add("" + getActivity().getString(R.string.convert_to) + " " + id).setOnMenuItemClickListener(item -> {
                    ShareDialog.showsItemsDialog(getActivity(), getString(R.string.convert_to) + " " + id, AppState.CONVERTERS.get(id));
                    return false;
                });
            }
            p.show();
        });

        TxtUtils.underlineTextView(preferencesBinding.newFile);
        preferencesBinding.newFile.setOnClickListener(v -> AlertDialogs.editFileTxt(getActivity(), null, AppProfile.DOWNLOADS_DIR, string -> {
            ExtUtils.openFile(getActivity(), new FileMeta(string));
            return false;
        }));

        overlay = getActivity().findViewById(R.id.overlay);

        final String p = AppProfile.getCurrent(getActivity());

        preferencesBinding.profileLetter.setText(TxtUtils.getFirstLetter(p));
        preferencesBinding.profileLetter.setBackgroundDrawable(AppProfile.getProfileColorDrawable(getActivity(), p));

        preferencesBinding.onProfile.setText(p);

        preferencesBinding.profileLetter.setContentDescription(p + " " + getString(R.string.profile));
        preferencesBinding.onProfile.setContentDescription(p + " " + getString(R.string.profile));

        TxtUtils.underlineTextView(preferencesBinding.onProfile);
        preferencesBinding.onProfile.setOnClickListener(v -> {
            if (BooksService.isRunning) {
                Toast.makeText(getActivity(), R.string.please_wait_books_are_being_processed_, Toast.LENGTH_SHORT).show();
                return;
            }

            MyPopupMenu popup = new MyPopupMenu(getActivity(), v);

            List<String> all = AppProfile.getAllProfiles();
            for (String profile : all) {
                popup.getMenu().setDrawable(TxtUtils.getFirstLetter(profile),
                        AppProfile.getProfileColorDrawable(getActivity(), profile)).add(profile)
                        .setOnMenuItemClickListener(menu -> {
                            {
                                if (!profile.equals(AppProfile.getCurrent(getActivity()))) {
                                    AlertDialogs.showOkDialog(getActivity(), getActivity().getString(R.string.do_you_want_to_switch_profile_), () -> {
                                        AppProfile.saveCurrent(getActivity(), profile);
                                        RecentUpates.updateAll();
                                        onTheme();
                                    });
                                }

                                return false;
                            }
                        });
            }
            popup.show();
        });
        preferencesBinding.profileLetter.setOnClickListener(v -> preferencesBinding.onProfile.performClick());

        final View.OnLongClickListener onDefaultProfile = v -> {
            if (BooksService.isRunning) {
                Toast.makeText(getActivity(), R.string.please_wait_books_are_being_processed_, Toast.LENGTH_SHORT).show();
                return true;
            }

            AlertDialogs.showOkDialog(getActivity(), getString(R.string.restore_defaults_full), () -> {
                //AppProfile.clear();
                final BookCSS b = new BookCSS();
                b.resetToDefault(getActivity());
                IO.writeObjAsync(AppProfile.syncCSS, b);

                final AppState o = new AppState();
                o.defaults(getActivity());

                IO.writeObjAsync(AppProfile.syncState, o);

                //AppProfile.init(getActivity());
                BooksService.startForeground(getActivity(), BooksService.ACTION_SEARCH_ALL);
                onTheme();
            });

            return true;
        };
        preferencesBinding.onProfile.setOnLongClickListener(onDefaultProfile);
        preferencesBinding.profileLetter.setOnLongClickListener(onDefaultProfile);

        preferencesBinding.onProfileEdit.setOnClickListener(v -> {
            if (BooksService.isRunning) {
                Toast.makeText(getActivity(), R.string.please_wait_books_are_being_processed_, Toast.LENGTH_SHORT).show();
                return;
            }

            AppProfile.showDialog(getActivity(), profile -> {
                if (!profile.equals(AppProfile.getCurrent(getActivity()))) {
                    AlertDialogs.showOkDialog(getActivity(), getActivity().getString(R.string.do_you_want_to_switch_profile_), () -> {
                        AppProfile.saveCurrent(getActivity(), profile);
                        onTheme();
                    });
                }
                return false;
            });
        });

        return preferencesBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        preferencesBinding = null;
    }

    private void onEink() {
        AppState.get().appTheme = AppState.THEME_INK;
        AppState.get().blueLightAlpha = 0;
        AppState.get().tintColor = Color.BLACK;
        TintUtil.color = Color.BLACK;

        onTintChanged();
        sendNotifyTintChanged();

        AppProfile.save(getActivity());

        getActivity().finish();
        MainTabs2.startActivity(getActivity(), TempHolder.get().currentTab);
    }

    public View underline(View text) {
        CharSequence myText = ((TextView) text).getText();
        ((TextView) text).setText(Html.fromHtml("<u>" + myText + "</u>"));
        return text;
    }

    private void checkOpenWithSpinner() {
        String modId = AppState.get().nameVerticalMode;
        if (AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN) {
            modId = AppState.get().nameMusicianMode;
        } else if (AppSP.get().readingMode == AppState.READING_MODE_BOOK) {
            modId = AppState.get().nameHorizontalMode;
        } else if (AppSP.get().readingMode == AppState.READING_MODE_SCROLL) {
            modId = AppState.get().nameVerticalMode;
        } else if (AppSP.get().readingMode == AppState.READING_MODE_TAG_MANAGER) {
            modId = getString(R.string.tag_manager);
        } else if (AppSP.get().readingMode == AppState.READING_MODE_OPEN_WITH) {
            modId = getString(R.string.open_with);
        }

        preferencesBinding.selectedOpenMode.setText(TxtUtils.underline(modId));
    }

    public void onFolderConfigDialog() {
        PrefDialogs.chooseFolderDialog(getActivity(), () -> {
            preferencesBinding.searchPaths.setText(JsonDB.fromHtml(BookCSS.get().searchPathsJson));
            saveChanges();
            LOG.d("Save Changes", 2);
        }, this::onScan);
    }

    @Override
    public void onResume() {
        super.onResume();

        BrightnessHelper.updateOverlay(overlay);
        BrightnessHelper.showBlueLightDialogAndBrightness(getActivity(), preferencesBinding.getRoot(), () -> BrightnessHelper.updateOverlay(overlay));

        rotationText();

        preferencesBinding.onReverse.setOnCheckedChangeListener(null);
        preferencesBinding.onReverse.setChecked(AppState.get().isReverseKeys);
        preferencesBinding.onReverse.setOnCheckedChangeListener(reverseListener);

        preferencesBinding.isRememberMode.setChecked(AppState.get().isRememberMode);
        checkOpenWithSpinner();

        preferencesBinding.onNightColor.setTextColor(AppState.get().colorNightText);
        preferencesBinding.onNightColor.setBackgroundColor(AppState.get().colorNightBg);

        preferencesBinding.onDayColor.setTextColor(AppState.get().colorDayText);
        preferencesBinding.onDayColor.setBackgroundColor(AppState.get().colorDayBg);

        preferencesBinding.isRememberDictionary.setChecked(AppState.get().isRememberDictionary);
        preferencesBinding.selectedDictionary.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
    }

    public void initKeys() {
        preferencesBinding.textNextKeys.setText(String.format("%s: %s", getString(R.string.next_keys), AppState.keyToString(AppState.get().nextKeys)));
        preferencesBinding.textPrevKeys.setText(String.format("%s: %s", getString(R.string.prev_keys), AppState.keyToString(AppState.get().prevKeys)));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onEmail() {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        String string = getResources().getString(R.string.my_email).replace("<u>", "").replace("</u>", "");
        final String[] aEmailList = {string};
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, Apps.getApplicationName(getContext()) + " " + Apps.getVersionName(getContext()));
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hi Support, ");

        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_mail)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), R.string.there_are_no_email_applications_installed_, Toast.LENGTH_SHORT).show();
        }
    }

    public void onTheme() {
        Apps.accessibilityText(getActivity(), R.string.apply);
        IMG.clearMemoryCache();
        AppProfile.save(getActivity());
        AppProfile.clear();
        getActivity().finish();
        MainTabs2.startActivity(getActivity(), TempHolder.get().currentTab);
    }

    public void onScan() {
        if (getActivity() == null) {
            return;
        }
        AppProfile.save(getActivity());
        closeLeftMenu();

        BooksService.startForeground(getActivity(), BooksService.ACTION_SEARCH_ALL);

        Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.SearchFragment));//

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    private void closeLeftMenu() {
        try {
            final DrawerLayout drawerLayout = getActivity().findViewById(R.id.drawer_layout);
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START, !Dips.isEInk());
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void rotationText() {
        preferencesBinding.screenOrientation.setText(DocumentController.getRotationText());
        TxtUtils.underlineTextView(preferencesBinding.screenOrientation);
        DocumentController.doRotation(getActivity());
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rotationText();

        if (AppState.get().appTheme == AppState.THEME_INK) {
            preferencesBinding.themeColor.setText(TxtUtils.underline("Ink"));
        } else if (AppState.get().appTheme == AppState.THEME_LIGHT) {
            preferencesBinding.themeColor.setText(TxtUtils.underline(getString(R.string.light)));
        } else if (AppState.get().appTheme == AppState.THEME_DARK) {
            preferencesBinding.themeColor.setText(TxtUtils.underline(getString(R.string.black)));
        } else if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
            preferencesBinding.themeColor.setText(TxtUtils.underline(getString(R.string.dark_oled)));
        } else {
            preferencesBinding.themeColor.setText("unknown");
        }
    }

    private void saveChanges() {
        if (getActivity() != null) {
            AppProfile.save(getActivity());
        }
    }

    public String getFontName(float number) {
        String prefix = getString(R.string.normal);
        float f1 = (number - 1f) * 10;
        float f2 = (1f - number) * 10 + 0.01f;
        if (number < 1) {
            prefix = getString(R.string.small) + " (-" + (int) f2 + ")";
        } else if (number > 1) {
            prefix = getString(R.string.large) + " (+" + (int) f1 + ")";
        }
        return prefix;
    }
}
