package com.foobnix.ui2.fragment;

import static com.foobnix.pdf.info.view.confline.ConfAction.of;

import android.content.res.ColorStateList;
import android.widget.ProgressBar;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import androidx.cardview.widget.CardView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.buzzingandroid.ui.HSVColorPickerDialog;
import com.buzzingandroid.ui.HSVColorPickerDialog.OnColorSelectedListener;
import com.foobnix.LibreraBuildConfig;
import com.foobnix.StringResponse;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Views;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AndroidWhatsNew;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.Clouds;
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
import com.foobnix.pdf.info.view.DragingPopup;
import com.foobnix.pdf.info.view.KeyCodeDialog;
import com.foobnix.pdf.info.view.MultyDocSearchDialog;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.view.confline.ConfLineView;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.widget.ColorsDialog;
import com.foobnix.pdf.info.widget.ColorsDialog.ColorsDialogResult;
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
import com.foobnix.ui2.AdsFragmentActivity;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.MyContextWrapper;
import com.foobnix.work.SearchAllBooksWorker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.jmedeisis.draglinearlayout.DragLinearLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PrefFragment2 extends UIFragment {
    public static final Pair<Integer, Integer> PAIR =
            new Pair<>(R.string.preferences, R.drawable.glyphicons_5_settings);

    private static final String WWW_SITE = "https://librera.mobi";
    private static final String WWW_BETA_SITE = "http://beta.librera.mobi";
    private static final String WWW_WIKI_SITE = "https://librera.mobi/faq";
    View section1, section2, section3, section4, section5, section6, section7, section8, section9, panelRecent, overlay,
            statusBarHack;
    TextView singIn, syncInfo, syncInfo2, syncHeader, syncNow;
    ProgressBar syncProgress;
    CheckBox isEnableSync;
    private TextView curBrightness, themeColor, profileLetter;
    private CheckBox isRememberDictionary;
    private TextView nextKeys;
    private TextView prevKeys;
    OnCheckedChangeListener reverseListener = new OnCheckedChangeListener() {

        @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            AppState.get().isReverseKeys = isChecked;
            initKeys();
            saveChanges();
            LOG.d("Save Changes", 3);
        }
    };
    Runnable onCloseDialog = new Runnable() {

        @Override public void run() {
            initKeys();
        }
    };
    private SeekBar bar;
    private CheckBox autoSettings;
    private LinearLayout searchPaths;
    private CheckBox ch;
    private TextView selectedOpenMode;
    private TextView textNigthColor;
    private TextView textDayColor;
    private TextView selectedDictionaly;
    private TextView screenOrientation;
    private View inflate;
    ConfLineView configSingleClick;

    @Override public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override public boolean isBackPressed() {
        return false;
    }

    @Override public void notifyFragment() {
    }

    @Override public void resetFragment() {
    }

    @Override public void onTintChanged() {

        TintUtil.setStatusBarColor(getActivity(), TintUtil.color);
        TintUtil.setSectionFillColor(section1, TintUtil.color);
        TintUtil.setSectionFillColor(section2, TintUtil.color);
        TintUtil.setSectionFillColor(section3, TintUtil.color);
        TintUtil.setSectionFillColor(section4, TintUtil.color);
        TintUtil.setSectionFillColor(section5, TintUtil.color);
        TintUtil.setSectionFillColor(section6, TintUtil.color);
        TintUtil.setSectionFillColor(section7, TintUtil.color);
        TintUtil.setSectionFillColor(section8, TintUtil.color);
        TintUtil.setSectionFillColor(section9, TintUtil.color);
        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);
        if (statusBarHack != null) {
            statusBarHack.setBackgroundColor(TintUtil.color);
        }

        if (profileLetter != null && getActivity() != null) {
            final String p = AppProfile.getCurrent();
            profileLetter.setText(TxtUtils.getFirstLetter(p));
            profileLetter.setBackgroundDrawable(AppProfile.getProfileColorDrawable(getActivity(), TintUtil.color));
            profileLetter.setContentDescription(p + " " + getString(R.string.profile));
        }

        if (AppState.get().appTheme == AppState.THEME_INK) {
            TxtUtils.setInkTextView(inflate.getRootView());
        }
        TxtUtils.updateAllLinks(inflate,true);
        paintLinkButtons(inflate);

    }

    @Subscribe public void updateSyncInfo(GDriveSycnEvent event) {
        String gdriveInfo = GFile.getDisplayInfo(getActivity());

        if (TxtUtils.isEmpty(gdriveInfo)) {
            AppSP.get().isEnableSync = false;
            syncInfo.setVisibility(View.GONE);
            singIn.setText(R.string.sign_in);
            asButton(singIn);
            singIn.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    GFile.init(getActivity());
                    updateSyncInfo(null);
                }
            });
        } else {
            syncInfo.setVisibility(View.VISIBLE);
            syncInfo.setText(gdriveInfo);
            singIn.setText(R.string.sign_out);
            asButton(singIn);

            singIn.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    AppSP.get().isEnableSync = false;
                    AppSP.get().syncRootID = "";
                    AppSP.get().syncTime = 0;
                    GFile.logout(getActivity());
                    updateSyncInfo(null);
                }
            });
        }

        isEnableSync.setChecked(AppSP.get().isEnableSync);
        onSync(null);
        updateSyncNow();

    }

    // The sync-now button stands only while an account is signed in.
    private void updateSyncNow() {
        if (syncNow == null || getActivity() == null) {
            return;
        }
        syncNow.setVisibility(TxtUtils.isNotEmpty(GFile.getDisplayInfo(getActivity())) ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onSync(MessageSync msg) {
        if (AppSP.get().syncTime > 0) {

            final Date date = new Date(AppSP.get().syncTime);
            String format = "";
            if (DateUtils.isToday(AppSP.get().syncTime)) {
                format = getString(R.string.today) + " " + DateFormat.getTimeInstance()
                                                                     .format(date);
            } else {
                format = DateFormat.getDateTimeInstance()
                                   .format(date);
            }

            String status = AppSP.get().syncTimeStatus == MessageSync.STATE_SUCCESS ? getString(R.string.success) :
                    getString(R.string.fail);
            if (AppSP.get().syncTimeStatus == MessageSync.STATE_VISIBLE) {
                status = "...";
            }

            syncInfo2.setText(format + " - " + status);
            syncInfo2.setVisibility(View.VISIBLE);
        } else {
            syncInfo2.setText("");
            syncInfo2.setVisibility(View.GONE);
            syncHeader.setText(R.string.sync_google_drive);

        }

        // A running sync is drawn here as a small wheel beside the last-sync line. The message
        // says so as it is sent; before any message has come, the saved status does.
        if (syncProgress != null) {
            boolean running = msg != null ? msg.state == MessageSync.STATE_VISIBLE
                                          : AppSP.get().syncTimeStatus == MessageSync.STATE_VISIBLE;
            syncProgress.setVisibility(running ? View.VISIBLE : View.GONE);
        }
    }

    @Override public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                                       final Bundle savedInstanceState) {
        inflate = inflater.inflate(R.layout.fragment_preferences, container, false);

        singIn = inflate.findViewById(R.id.signIn);
        syncInfo = inflate.findViewById(R.id.syncInfo);
        syncInfo2 = inflate.findViewById(R.id.syncInfo2);
        syncProgress = inflate.findViewById(R.id.syncProgress);
        syncProgress.setIndeterminateTintList(ColorStateList.valueOf(TintUtil.getColorInDayNighth()));
        // The last-sync line opens the sync log, where a running sync shows its progress.
        inflate.findViewById(R.id.syncStatus)
               .setOnClickListener(v -> Dialogs.showSyncLOGDialog(getActivity()));
        syncHeader = inflate.findViewById(R.id.syncHeader);
        syncNow = inflate.findViewById(R.id.syncNow);
        // A sync by hand. With sync switched off the service does nothing, so the button
        // switches it on through its own box, whose listener starts the first run.
        syncNow.setOnClickListener(v -> {
            if (!AppSP.get().isEnableSync) {
                isEnableSync.setChecked(true);
            } else {
                GFile.runSyncService(getActivity(), true);
            }
        });
        updateSyncNow();
        onSync(null);
        syncHeader.setOnClickListener((in) -> Dialogs.showSyncLOGDialog(getActivity()));

        isEnableSync = inflate.findViewById(R.id.isEnableSync);
        isEnableSync.setChecked(AppSP.get().isEnableSync);
        isEnableSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSP.get().isEnableSync = isChecked;
            if (isChecked && getActivity() != null) {
                if (GoogleSignIn.getLastSignedInAccount(getActivity()) == null) {
                    GFile.init(getActivity());
                } else {
                    GFile.runSyncService(getActivity());
                }
            }
        });

        inflate.findViewById(R.id.isEnableSyncSettings)
               .setOnClickListener(v -> {
                   final CheckBox isSyncPullToRefresh = new CheckBox(getActivity());
                   isSyncPullToRefresh.setText(R.string.pull_to_start_sync);
                   isSyncPullToRefresh.setChecked(BookCSS.get().isSyncPullToRefresh);
                   isSyncPullToRefresh.setOnCheckedChangeListener(
                           (buttonView, isChecked) -> BookCSS.get().isSyncPullToRefresh = isChecked);

                   final CheckBox isSyncWifiOnly = new CheckBox(getActivity());
                   isSyncWifiOnly.setText(R.string.wifi_sync_only);
                   isSyncWifiOnly.setChecked(BookCSS.get().isSyncWifiOnly);
                   isSyncWifiOnly.setOnCheckedChangeListener(
                           (buttonView, isChecked) -> BookCSS.get().isSyncWifiOnly = isChecked);

                   AlertDialogs.showViewDialog(getActivity(), null, isSyncPullToRefresh, isSyncWifiOnly);
               });

        updateSyncInfo(null);

        section8 = inflate.findViewById(R.id.section8);

        inflate.findViewById(R.id.sectionSync)
               .setVisibility(AppsConfig.IS_FDROID ? View.GONE : View.VISIBLE);
        //inflate.findViewById(R.id.sectionSync).setVisibility(View.GONE);//TODO GDIVE need to fix

        section9 = inflate.findViewById(R.id.section9);
        panelRecent = inflate.findViewById(R.id.panelRecent);

        // tabs position
        final DragLinearLayout dragLinearLayout = inflate.findViewById(R.id.dragLinearLayout);
        final LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2));

        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable ask2 = new Runnable() {

            @Override public void run() {
                if (getActivity() == null) {
                    return;
                }
                AlertDialogs.showDialog(getActivity(),
                        getActivity().getString(R.string.you_neet_to_apply_the_new_settings), getString(R.string.ok),
                        new Runnable() {

                            @Override public void run() {
                                inflate.findViewById(R.id.tabsApply)
                                       .performClick();
                            }
                        }, null);
            }
        };

        final int timeout = 1500;
        final CheckBox isshowPrefAsMenu = inflate.findViewById(R.id.isshowPrefAsMenu);
        isshowPrefAsMenu.setSaveEnabled(false);

        final Runnable dragLinear = new Runnable() {

            @Override public void run() {
                dragLinearLayout.removeAllViews();
                for (UITab tab : UITab.getOrdered()) {
//                    if (tab == UITab.GoogleDrive2Fragment) {//SKIP for all
//                        continue;
//                    }

                    if (AppsConfig.IS_FDROID && tab == UITab.GoogleDrive2Fragment) {
                        continue;
                    }

                    View library = LayoutInflater.from(getActivity())
                                                 .inflate(R.layout.item_tab_line, null, false);
                    // The row is drawn on the colour the panel behind it is, rather than
                    // blacked out against it. setBackgroundColor would also put a plain colour
                    // where the card's round-rect is and square the corners off with it, so
                    // the card's own fill is what changes.
                    if (library instanceof CardView) {
                        TypedValue panel = new TypedValue();
                        getActivity().getTheme()
                                     .resolveAttribute(android.R.attr.colorBackground, panel, true);
                        ((CardView) library).setCardBackgroundColor(panel.data);
                    }

                    ((TextView) library.findViewById(R.id.text1)).setText(tab.getName());
                    CheckBox isVisible = library.findViewById(R.id.isVisible);
                    isVisible.setSaveEnabled(false);
                    isVisible.setChecked(tab.isVisible());
                    isVisible.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                        @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            handler.removeCallbacks(ask2);
                            handler.postDelayed(ask2, timeout);

                            if (tab == UITab.PrefFragment) {
                                isshowPrefAsMenu.setChecked(!isChecked);
                            }

                        }
                    });
                    ((ImageView) library.findViewById(R.id.image1)).setImageResource(tab.getIcon());
                    //TintUtil.setTintImageWithAlpha(library.findViewById(R.id.image1), TintUtil.COLOR_TINT_GRAY);
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

        asButton(inflate.findViewById(R.id.tabsApply))
                .setOnClickListener(new OnClickListener() {

                    @Override public void onClick(View v) {
                        handler.removeCallbacks(ask2);
                        synchronized (AppState.get().tabsOrder9) {
                            AppState.get().tabsOrder9 = "";
                            for (int i = 0; i < dragLinearLayout.getChildCount(); i++) {
                                View child = dragLinearLayout.getChildAt(i);
                                boolean isVisible = ((CheckBox) child.findViewById(R.id.isVisible)).isChecked();
                                AppState.get().tabsOrder9 += child.getTag() + "#" + (isVisible ? "1" : "0") + ",";
                            }
                            AppState.get().tabsOrder9 = TxtUtils.replaceLast(AppState.get().tabsOrder9, ",", "");
                            LOG.d("tabsApply", AppState.get().tabsOrder9);
                        }

                        if (UITab.isShowCloudsPreferences()) {
                            Clouds.get()
                                  .init(getActivity());
                        }
                        onTheme();
                    }
                });

        isshowPrefAsMenu.setChecked(AppState.get().tabsOrder9.contains(UITab.PrefFragment.index + "#0"));
        isshowPrefAsMenu.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handler.removeCallbacks(ask2);
                handler.postDelayed(ask2, timeout);
                synchronized (AppState.get().tabsOrder9) {
                    if (isChecked) {
                        AppState.get().tabsOrder9 = AppState.get().tabsOrder9.replace(UITab.PrefFragment.index + "#1",
                                UITab.PrefFragment.index + "#0");
                    } else {
                        AppState.get().tabsOrder9 = AppState.get().tabsOrder9.replace(UITab.PrefFragment.index + "#0",
                                UITab.PrefFragment.index + "#1");
                    }
                }
                dragLinear.run();
            }
        });

        asButton(inflate.findViewById(R.id.tabsDefaul))
                .setOnClickListener(new OnClickListener() {

                    @Override public void onClick(View v) {
                        handler.removeCallbacks(ask2);

                        AlertDialogs.showOkDialog(getActivity(),
                                getActivity().getString(R.string.restore_defaults_full), new Runnable() {

                                    @Override public void run() {
                                        synchronized (AppState.get().tabsOrder9) {
                                            AppState.get().tabsOrder9 = AppState.DEFAULTS_TABS_ORDER;
                                        }
                                        onTheme();
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

        final CustomSeek coverSmallSize = inflate.findViewById(R.id.coverSmallSize);
        coverSmallSize.init(40, max, AppState.get().coverSmallSize);

        coverSmallSize.setOnSeekChanged(new IntegerResponse() {

            @Override public boolean onResultRecive(int result) {
                TempHolder.listHash++;
                AppState.get().coverSmallSize = result;
                return false;
            }
        });

        final CustomSeek coverBigSize = inflate.findViewById(R.id.coverBigSize);
        coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize), AppState.get().coverBigSize);
        coverBigSize.setOnSeekChanged(new IntegerResponse() {

            @Override public boolean onResultRecive(int result) {
                TempHolder.listHash++;
                AppState.get().coverBigSize = result;
                return false;
            }
        });

        final TextView columsCount = inflate.findViewById(R.id.columsCount);
        columsCount.setText("" + Dips.screenWidthDP() / AppState.get().coverBigSize);
        asButton(columsCount);
        columsCount.setOnClickListener(new OnClickListener() {

            @SuppressLint("NewApi") @Override public void onClick(View v) {
                PopupMenu p = new PopupMenu(getContext(), columsCount);
                for (int i = 1; i <= 8; i++) {
                    final int k = i;
                    p.getMenu()
                     .add("" + k)
                     .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                         @Override public boolean onMenuItemClick(MenuItem item) {
                             int result = Dips.screenWidthDP() / k - 8;

                             TempHolder.listHash++;
                             AppState.get().coverBigSize = result;

                             columsCount.setText("" + k);
                             asButton(columsCount);

                             coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize),
                                     AppState.get().coverBigSize);
                             return false;
                         }
                     });
                }

                p.show();
            }
        });
        final TextView columsDefaul = inflate.findViewById(R.id.columsDefaul);
        asButton(columsDefaul);
        columsDefaul.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                if (getActivity() == null) {
                    return;
                }

                AlertDialogs.showOkDialog(getActivity(), getActivity().getString(R.string.restore_defaults_full),
                        new Runnable() {

                            @Override public void run() {
                                IMG.clearDiscCache();
                                IMG.clearMemoryCache();
                                AppState.get().coverBigSize =
                                        (int) (((Dips.screenWidthDP() / (Dips.screenWidthDP() / 110)) - 8) * (
                                                Dips.isXLargeScreen() ? 1.5f : 1));
                                AppState.get().coverSmallSize = 80;
                                TempHolder.listHash++;

                                columsCount.setText("" + Dips.screenWidthDP() / AppState.get().coverBigSize);
                                asButton(columsCount);

                                coverSmallSize.init(40, max, AppState.get().coverSmallSize);
                                coverBigSize.init(40, Math.max(max, AppState.get().coverBigSize),
                                        AppState.get().coverBigSize);
                            }
                        });

            }
        });

        final ScrollView scrollView = inflate.findViewById(R.id.scroll);
        scrollView.setVerticalScrollBarEnabled(false);

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
            scrollView.setBackgroundColor(Color.BLACK);
        }

        // ((TextView) findViewById(R.id.appName)).setText(AppsConfig.APP_NAME);

        try {
            PackageInfo packageInfo = getActivity().getPackageManager()
                                                   .getPackageInfo(getActivity().getPackageName(), 0);
            String version =
                    packageInfo.versionName + " (" + AppsConfig.MUPDF_FZ_VERSION + "-" + LibreraBuildConfig.FLAVOR + ")";
            if (Dips.isEInk()) {
                version += " INK";
            }
            if (AppsConfig.IS_LOG) {
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
            ((TextView) inflate.findViewById(R.id.pVersion)).setText(
                    String.format("%s: %s", getString(R.string.version), version));
            ((TextView) inflate.findViewById(R.id.section6)).setText(
                    String.format("%s: %s %s %s", Apps.getApplicationName(getActivity()), version,
                            "SDK: " + Build.VERSION.SDK_INT, Build.MANUFACTURER));
        } catch (final NameNotFoundException e) {
        }

        View onCloseApp = inflate.findViewById(R.id.onCloseApp);
        onCloseApp.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                getActivity().finish();
            }
        });

        final TextView onFullScreen = inflate.findViewById(R.id.fullscreen);

        onFullScreen.setText(DocumentController.getFullScreenName(getActivity(), AppState.get().fullScreenMainMode));

        asButton(onFullScreen);

        onFullScreen.setOnClickListener(v -> {

            DocumentController.showFullScreenPopup(getActivity(), v, id -> {
                AppState.get().fullScreenMainMode = id;
                onFullScreen.setText(
                        DocumentController.getFullScreenName(getActivity(), AppState.get().fullScreenMainMode));
                asButton(onFullScreen);
                DocumentController.chooseFullScreen(getActivity(), AppState.get().fullScreenMainMode);
                return true;
            }, AppState.get().fullScreenMainMode);

        });

        final TextView tapPositionTop = inflate.findViewById(R.id.tapPositionTop);

        String tabText = AppState.get().tapPositionTop ? getString(R.string.top) : getString(R.string.bottom);
        tabText += AppState.get().tabWithNames ? "" : " - " + getString(R.string.icons_only);
        tapPositionTop.setText(tabText);

        asButton(tapPositionTop);

        tapPositionTop.setOnClickListener(v -> {

            MyPopupMenu popup = new MyPopupMenu(getActivity(), v);
            popup.getMenu()
                 .add(R.string.top)
                 .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                     @Override public boolean onMenuItemClick(MenuItem item) {
                         AppState.get().tapPositionTop = true;
                         AppState.get().tabWithNames = true;
                         onTheme();
                         return false;
                     }
                 });

            popup.getMenu()
                 .add(R.string.bottom)
                 .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                     @Override public boolean onMenuItemClick(MenuItem item) {
                         AppState.get().tapPositionTop = false;
                         AppState.get().tabWithNames = true;
                         onTheme();
                         return false;
                     }
                 });

            popup.getMenu()
                 .add(getString(R.string.top) + " - " + getString(R.string.icons_only))
                 .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                     @Override public boolean onMenuItemClick(MenuItem item) {
                         AppState.get().tapPositionTop = true;
                         AppState.get().tabWithNames = false;
                         onTheme();
                         return false;
                     }
                 });

            popup.getMenu()
                 .add(getString(R.string.bottom) + " - " + getString(R.string.icons_only))
                 .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                     @Override public boolean onMenuItemClick(MenuItem item) {
                         AppState.get().tapPositionTop = false;
                         AppState.get().tabWithNames = false;
                         onTheme();
                         return false;
                     }
                 });

            popup.show();

        });

        screenOrientation = inflate.findViewById(R.id.screenOrientation);
        screenOrientation.setText(DocumentController.getRotationText());
        asButton(screenOrientation);

        screenOrientation.setOnClickListener(new

                                                     OnClickListener() {

                                                         @Override public void onClick(View v) {
                                                             PopupMenu menu = new PopupMenu(v.getContext(), v);
                                                             for (int i =
                                                                  0; i < DocumentController.orientationIds.size(); i++) {
                                                                 final int j = i;
                                                                 final int name =
                                                                         DocumentController.orientationTexts.get(i);
                                                                 menu.getMenu()
                                                                     .add(name)
                                                                     .setOnMenuItemClickListener(
                                                                             new OnMenuItemClickListener() {

                                                                                 @Override
                                                                                 public boolean onMenuItemClick(
                                                                                         MenuItem item) {
                                                                                     AppState.get().orientation =
                                                                                             DocumentController.orientationIds.get(
                                                                                                     j);
                                                                                     screenOrientation.setText(
                                                                                             DocumentController.orientationTexts.get(
                                                                                                     j));
                                                                                     asButton(
                                                                                             screenOrientation);
                                                                                     DocumentController.doRotation(
                                                                                             getActivity());
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

        View closeMenu = inflate.findViewById(R.id.closeMenu);
        closeMenu.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                closeLeftMenu();
            }
        });
        closeMenu.setVisibility(TxtUtils.visibleIf(AppState.get().isEnableAccessibility));

        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(getActivity());

        TextView adsSettigns = inflate.findViewById(R.id.adsSettigns);
        adsSettigns.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {

                UserMessagingPlatform.showPrivacyOptionsForm(getActivity(),
                        new ConsentForm.OnConsentFormDismissedListener() {
                            @Override public void onConsentFormDismissed(@Nullable FormError formError) {
                                if (formError != null) {
                                    Toast.makeText(getActivity(), formError.getMessage(), Toast.LENGTH_LONG)
                                         .show();
                                }
                            }
                        });

            }
        });
        boolean isPrivicyOptionRequired =
                consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;

        adsSettigns.setVisibility(TxtUtils.visibleIf(isPrivicyOptionRequired));
        asButton(adsSettigns);

        inflate.findViewById(R.id.onKeyCode)
               .

                       setOnClickListener(new OnClickListener() {

                   @Override public void onClick(final View v) {
                       new KeyCodeDialog(getActivity(), onCloseDialog);
                   }
               });

        CheckBox isEnableAccessibility = inflate.findViewById(R.id.isEnableAccessibility);

        isEnableAccessibility.setChecked(AppState.get().isEnableAccessibility);
        isEnableAccessibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isEnableAccessibility = isChecked;

            if (isChecked) {
                AppState.get()
                        .accessibilityDefaults();
            } else {
                BookCSS.get().appFontScale = 1.0f;
            }

            onTheme();
        });

        themeColor = inflate.findViewById(R.id.themeColor);
        themeColor.setOnClickListener(new

                                              OnClickListener() {

                                                  @Override public void onClick(final View v) {

                                                      PopupMenu p = new PopupMenu(getContext(), themeColor);
                                                      p.getMenu()
                                                       .add(R.string.system)
                                                       .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                                           @Override public boolean onMenuItemClick(MenuItem item) {
                                                               AppState.get().isSystemThemeColor = true;
                                                               AppState.get().appTheme =
                                                                       Dips.isDarkThemeOn() ? AppState.THEME_DARK :
                                                                               AppState.THEME_LIGHT;
                                                               AppState.get().contrastImage = 0;
                                                               AppState.get().brigtnessImage = 0;
                                                               AppState.get().bolderTextOnImage = false;
                                                               AppState.get().isEnableBCOptional1 = false;

                                                               IMG.clearDiscCache();
                                                               IMG.clearMemoryCache();
                                                               onTheme();

                                                               return false;
                                                           }
                                                       });

                                                      p.getMenu()
                                                       .add(R.string.light)
                                                       .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                                           @Override public boolean onMenuItemClick(MenuItem item) {
                                                               AppState.get().isSystemThemeColor = false;
                                                               AppState.get().appTheme = AppState.THEME_LIGHT;

                                                               AppState.get().contrastImage = 0;
                                                               AppState.get().brigtnessImage = 0;
                                                               AppState.get().bolderTextOnImage = false;
                                                               AppState.get().isEnableBCOptional1 = false;

                                                               IMG.clearDiscCache();
                                                               IMG.clearMemoryCache();
                                                               onTheme();

                                                               return false;
                                                           }
                                                       });
                                                      p.getMenu()
                                                       .add(R.string.black)
                                                       .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                                           @Override public boolean onMenuItemClick(MenuItem item) {
                                                               AppState.get().isSystemThemeColor = false;
                                                               AppState.get().appTheme = AppState.THEME_DARK;

                                                               AppState.get().contrastImage = 0;
                                                               AppState.get().brigtnessImage = 0;
                                                               AppState.get().bolderTextOnImage = false;
                                                               AppState.get().isEnableBCOptional1 = false;

                                                               IMG.clearDiscCache();
                                                               IMG.clearMemoryCache();

                                                               onTheme();
                                                               return false;
                                                           }
                                                       });
                                                      p.getMenu()
                                                       .add(R.string.dark_oled)
                                                       .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                                           @Override public boolean onMenuItemClick(MenuItem item) {
                                                               AppState.get().isSystemThemeColor = false;
                                                               AppState.get().appTheme = AppState.THEME_DARK_OLED;

                                                               AppState.get().contrastImage = 0;
                                                               AppState.get().brigtnessImage = 0;
                                                               AppState.get().bolderTextOnImage = false;
                                                               AppState.get().isEnableBCOptional1 = false;
                                                               AppState.get().tintThemeColor = Color.BLACK;
                                                               AppState.get().isUiTextColor = false;

                                                               IMG.clearDiscCache();
                                                               IMG.clearMemoryCache();

                                                               onTheme();
                                                               return false;
                                                           }
                                                       });
                                                      p.getMenu()
                                                       .add("Ink")
                                                       .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                                           @Override public boolean onMenuItemClick(MenuItem item) {
                                                               AppState.get().isSystemThemeColor = false;
                                                               IMG.clearDiscCache();
                                                               IMG.clearMemoryCache();

                                                               onEink();
                                                               return false;
                                                           }
                                                       });
                                                      p.show();
                                                  }
                                              });

        TextView appEngine = inflate.findViewById(R.id.appEngine);
        appEngine.setText("" + AppsConfig.MUPDF_FZ_VERSION);
        asButton(appEngine);
        Views.visible(appEngine, false /**LOG.isEnable || AppsConfig.IS_PRO**/);

//        appEngine.setOnClickListener(v -> {
//            if (BooksService.isRunning) {
//                Toast.makeText(getActivity(), R.string.please_wait_books_are_being_processed_, Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            PopupMenu p = new PopupMenu(getContext(), appEngine);
//
//            p.getMenu().add(AppsConfig.ENGINE_MuPDF_1_11).setOnMenuItemClickListener(item -> {
//                AlertDialogs.showDialog(getActivity(), getString(R.string.restart_manually), getString(R.string.ok), new Runnable() {
//                    @Override
//                    public void run() {
//                        AppsConfig.setEngine(getActivity(), AppsConfig.ENGINE_MuPDF_1_11);
//                        android.os.Process.killProcess(android.os.Process.myPid());
//                    }
//                });
//
//                return false;
//            });
//            p.getMenu().add(AppsConfig.ENGINE_MuPDF_LATEST).setOnMenuItemClickListener(item -> {
//                AlertDialogs.showDialog(getActivity(), getString(R.string.restart_manually), getString(R.string.ok), new Runnable() {
//                    @Override
//                    public void run() {
//                        AppsConfig.setEngine(getActivity(), AppsConfig.ENGINE_MuPDF_LATEST);
//                        android.os.Process.killProcess(android.os.Process.myPid());
//                    }
//                });
//
//                return false;
//            });
//
//            p.show();
//        });

        final TextView hypenLang = inflate.findViewById(R.id.appLang);
        hypenLang.setText(DialogTranslateFromTo.getLanuageByCode(AppState.get().appLang));
        asButton(hypenLang);

        hypenLang.setOnClickListener(new

                                             OnClickListener() {

                                                 @Override public void onClick(View v) {

                                                     final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                                                     List<String> langs = new ArrayList<>();
                                                     for (String code : AppState.langCodes) {
                                                         langs.add(DialogTranslateFromTo.getLanuageByCode(
                                                                 code) + ":" + code);
                                                     }
                                                     Collections.sort(langs);

                                                     popupMenu.getMenu()
                                                              .add(R.string.system_language)
                                                              .setOnMenuItemClickListener(
                                                                      new OnMenuItemClickListener() {

                                                                          @Override public boolean onMenuItemClick(
                                                                                  MenuItem item) {
                                                                              asButton(hypenLang);
                                                                              AppState.get().appLang =
                                                                                      AppState.MY_SYSTEM_LANG;
                                                                              TempHolder.get().forseAppLang = true;
                                                                              MyContextWrapper.wrap(getContext());
                                                                              onTheme();
                                                                              return false;
                                                                          }
                                                                      });

                                                     for (int i = 0; i < langs.size(); i++) {
                                                         String[] all = langs.get(i)
                                                                             .split(":");
                                                         String name = all[0];
                                                         final String code = all[1];

                                                         if (AppsConfig.IS_LOG) {
                                                             name += " [" + code + "]";
                                                         }
                                                         popupMenu.getMenu()
                                                                  .add(name)
                                                                  .setOnMenuItemClickListener(
                                                                          new OnMenuItemClickListener() {

                                                                              @Override public boolean onMenuItemClick(
                                                                                      MenuItem item) {
                                                                                  AppState.get().appLang = code;
                                                                                  asButton(hypenLang);
                                                                                  onTheme();
                                                                                  return false;
                                                                              }
                                                                          });
                                                     }
                                                     popupMenu.show();

                                                 }
                                             });

        final TextView appFontScale = inflate.findViewById(R.id.appFontScale);
        appFontScale.setText(

                getFontName(BookCSS.get().appFontScale));
        asButton(appFontScale);
        appFontScale.setOnClickListener(new

                                                OnClickListener() {

                                                    @Override public void onClick(View v) {
                                                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                                                        for (float i = 0.7f; i < 2.1f; i += 0.1) {
                                                            final float number = i;
                                                            popupMenu.getMenu()
                                                                     .add(getFontName(number))
                                                                     .setOnMenuItemClickListener(
                                                                             new OnMenuItemClickListener() {

                                                                                 @Override
                                                                                 public boolean onMenuItemClick(
                                                                                         MenuItem item) {
                                                                                     BookCSS.get().appFontScale =
                                                                                             number;
                                                                                     onTheme();
                                                                                     return false;
                                                                                 }
                                                                             });
                                                        }
                                                        popupMenu.show();
                                                    }
                                                });

        final TextView onMail = inflate.findViewById(R.id.onMailSupport);
        asButton(onMail, getString(R.string.my_email));

        onMail.setOnClickListener(new

                                          OnClickListener() {
                                              @Override

                                              public void onClick(final View v) {
                                                  onEmail();
                                              }
                                          });

        ((ConfLineView) inflate.findViewById(R.id.configLongClick)).init(//
                () -> AppState.get().defaultLongClick,//
                value -> AppState.get().defaultLongClick = value,//
                of(R.string.file_info, AppState.ACTION_BOOK_INFORMATION),//
                of(R.string.book_menu, AppState.ACTION_BOOK_MENU));

        configSingleClick = (ConfLineView) inflate.findViewById(R.id.configSingeClick);
        configSingleClick.init(//
                () -> AppState.get().isRememberMode ? AppSP.get().readingMode:AppState.READING_MODE_SELECT_MODE,//
                value -> {
                    AppState.get().isRememberMode = value != AppState.READING_MODE_SELECT_MODE;
                    AppSP.get().readingMode = value;
                },//
                of(getString(R.string.select_mode), AppState.READING_MODE_SELECT_MODE),//
                of(AppState.get().nameVerticalMode, AppState.READING_MODE_SCROLL),//
                of(AppState.get().nameHorizontalMode, AppState.READING_MODE_BOOK),//
                of(AppState.get().nameMusicianMode, AppState.READING_MODE_MUSICIAN),//
                of(getString(R.string.tag_manager), AppState.READING_MODE_TAG_MANAGER),//
                of(getString(R.string.open_with), AppState.READING_MODE_OPEN_WITH)//
                              );

        inflate.findViewById(R.id.moreModeSettings)
               .

                       setOnClickListener(new OnClickListener() {
                   @Override public void onClick(View v) {
                       AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                       View view = LayoutInflater.from(getActivity())
                                                 .inflate(R.layout.dialog_custom_reading_modes, null, false);
                       builder.setView(view);

                       EditText prefScrollMode = view.findViewById(R.id.prefScrollMode);
                       EditText prefBookMode = view.findViewById(R.id.prefBookMode);
                       EditText prefMusicianMode = view.findViewById(R.id.prefMusicianMode);

                       prefScrollMode.setText(AppState.get().prefScrollMode);
                       prefBookMode.setText(AppState.get().prefBookMode);
                       prefMusicianMode.setText(AppState.get().prefMusicianMode);

                       CheckBox isPrefFormatMode = view.findViewById(R.id.isPrefFormatMode);
                       isPrefFormatMode.setChecked(AppState.get().isPrefFormatMode);

                       view.findViewById(R.id.prefRestore)
                           .setOnClickListener(new OnClickListener() {
                               @Override public void onClick(View v) {
                                   AlertDialogs.showDialog(getActivity(),
                                           getActivity().getString(R.string.restore_defaults_full),
                                           getString(R.string.ok), new Runnable() {

                                               @Override public void run() {
                                                   AppState.get().isPrefFormatMode = false;
                                                   AppState.get().prefScrollMode = AppState.PREF_SCROLL_MODE;
                                                   AppState.get().prefBookMode = AppState.PREF_BOOK_MODE;
                                                   AppState.get().prefMusicianMode = AppState.PREF_MUSIC_MODE;

                                                   isPrefFormatMode.setChecked(AppState.get().isPrefFormatMode);
                                                   prefScrollMode.setText(AppState.get().prefScrollMode);
                                                   prefBookMode.setText(AppState.get().prefBookMode);
                                                   prefMusicianMode.setText(AppState.get().prefMusicianMode);
                                               }
                                           }, null);

                               }
                           });

                       builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

                           @Override public void onClick(final DialogInterface dialog, final int id) {
                               Keyboards.close(prefScrollMode);
                               AppState.get().isPrefFormatMode = isPrefFormatMode.isChecked();
                               AppState.get().prefScrollMode = prefScrollMode.getText()
                                                                             .toString();
                               AppState.get().prefBookMode = prefBookMode.getText()
                                                                         .toString();
                               AppState.get().prefMusicianMode = prefMusicianMode.getText()
                                                                                 .toString();
                           }
                       });
                       builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                           @Override public void onClick(final DialogInterface dialog, final int id) {

                           }
                       });
                       builder.show();
                   }
               });

        LOG.d("CONF-init", 2);

        final CheckBox isCropBookCovers = inflate.findViewById(R.id.isCropBookCovers);
        isCropBookCovers.setOnCheckedChangeListener(null);
        isCropBookCovers.setChecked(AppState.get().isCropBookCovers);
        isCropBookCovers.setOnCheckedChangeListener(new

                                                            OnCheckedChangeListener() {

                                                                @Override public void onCheckedChanged(
                                                                        final CompoundButton buttonView,
                                                                        final boolean isChecked) {
                                                                    AppState.get().isCropBookCovers = isChecked;
                                                                    TempHolder.listHash++;

                                                                }
                                                            });

        final CheckBox isBookCoverEffect = inflate.findViewById(R.id.isBookCoverEffect);
        isBookCoverEffect.setOnCheckedChangeListener(null);
        isBookCoverEffect.setChecked(AppState.get().isBookCoverEffect);
        isBookCoverEffect.setOnCheckedChangeListener(new

                                                             OnCheckedChangeListener() {

                                                                 @Override public void onCheckedChanged(
                                                                         final CompoundButton buttonView,
                                                                         final boolean isChecked) {
                                                                     AppState.get().isBookCoverEffect = isChecked;
                                                                     IMG.clearMemoryCache();
                                                                     IMG.clearDiscCache();

                                                                     TempHolder.listHash++;
                                                                     if (isChecked) {
                                                                         isCropBookCovers.setEnabled(false);
                                                                         isCropBookCovers.setChecked(true);
                                                                     } else {
                                                                         isCropBookCovers.setEnabled(true);
                                                                     }
                                                                 }
                                                             });

        final CheckBox isBorderAndShadow = inflate.findViewById(R.id.isBorderAndShadow);
        isBorderAndShadow.setOnCheckedChangeListener(null);
        isBorderAndShadow.setChecked(AppState.get().isBorderAndShadow);
        isBorderAndShadow.setOnCheckedChangeListener(new

                                                             OnCheckedChangeListener() {

                                                                 @Override public void onCheckedChanged(
                                                                         final CompoundButton buttonView,
                                                                         final boolean isChecked) {
                                                                     AppState.get().isBorderAndShadow = isChecked;
                                                                     TempHolder.listHash++;

                                                                 }
                                                             });

        final CheckBox isShowImages = inflate.findViewById(R.id.isShowImages);
        isShowImages.setOnCheckedChangeListener(null);
        isShowImages.setChecked(AppState.get().isShowImages);
        isShowImages.setOnCheckedChangeListener(new

                                                        OnCheckedChangeListener() {

                                                            @Override public void onCheckedChanged(
                                                                    final CompoundButton buttonView,
                                                                    final boolean isChecked) {
                                                                AppState.get().isShowImages = isChecked;
                                                                TempHolder.listHash++;
                                                                isCropBookCovers.setEnabled(
                                                                        AppState.get().isShowImages);
                                                                isBookCoverEffect.setEnabled(
                                                                        AppState.get().isShowImages);
                                                                isBorderAndShadow.setEnabled(
                                                                        AppState.get().isShowImages);

                                                            }
                                                        });
        isCropBookCovers.setEnabled(AppState.get().isShowImages);
        isBookCoverEffect.setEnabled(AppState.get().isShowImages);
        isBorderAndShadow.setEnabled(AppState.get().isShowImages);

        CheckBox isLoopAutoplay = inflate.findViewById(R.id.isLoopAutoplay);
        isLoopAutoplay.setChecked(AppState.get().isLoopAutoplay);
        isLoopAutoplay.setOnCheckedChangeListener(new

                                                          OnCheckedChangeListener() {

                                                              @Override public void onCheckedChanged(
                                                                      final CompoundButton buttonView,
                                                                      final boolean isChecked) {
                                                                  AppState.get().isLoopAutoplay = isChecked;
                                                              }
                                                          });

        CheckBox isOpenLastBook = inflate.findViewById(R.id.isOpenLastBook);
        isOpenLastBook.setChecked(AppState.get().isOpenLastBook);
        isOpenLastBook.setOnCheckedChangeListener(new

                                                          OnCheckedChangeListener() {

                                                              @Override public void onCheckedChanged(
                                                                      final CompoundButton buttonView,
                                                                      final boolean isChecked) {
                                                                  AppState.get().isOpenLastBook = isChecked;
                                                              }
                                                          });

        CheckBox isRestoreSearchQuery = inflate.findViewById(R.id.isRestoreSearchQuery);
        isRestoreSearchQuery.setChecked(AppState.get().isRestoreSearchQuery);
        isRestoreSearchQuery.setOnCheckedChangeListener(new

                                                                OnCheckedChangeListener() {

                                                                    @Override public void onCheckedChanged(
                                                                            final CompoundButton buttonView,
                                                                            final boolean isChecked) {
                                                                        AppState.get().isRestoreSearchQuery = isChecked;
                                                                    }
                                                                });

        CheckBox lockBooksByDefault = inflate.findViewById(R.id.lockBooksByDefault);
        lockBooksByDefault.setChecked(AppState.get().lockBooksByDefault);
        lockBooksByDefault.setOnCheckedChangeListener(new

                                                              OnCheckedChangeListener() {

                                                                  @Override public void onCheckedChanged(
                                                                          final CompoundButton buttonView,
                                                                          final boolean isChecked) {
                                                                      AppState.get().lockBooksByDefault = isChecked;
                                                                  }
                                                              });

        CheckBox isShowCloseAppDialog = inflate.findViewById(R.id.isShowCloseAppDialog);
        isShowCloseAppDialog.setChecked(AppState.get().isShowCloseAppDialog);
        isShowCloseAppDialog.setOnCheckedChangeListener(new

                                                                OnCheckedChangeListener() {

                                                                    @Override public void onCheckedChanged(
                                                                            final CompoundButton buttonView,
                                                                            final boolean isChecked) {
                                                                        AppState.get().isShowCloseAppDialog = isChecked;
                                                                    }
                                                                });

        final Runnable ask = new Runnable() {

            @Override public void run() {
                LOG.d("timer ask");
                if (getActivity() == null) {
                    return;
                }

                AlertDialogs.showDialog(getActivity(), getActivity().getString(R.string.you_need_to_update_the_library),
                        getString(R.string.ok), new Runnable() {

                            @Override public void run() {
                                onScan();
                            }
                        }, null);
            }
        };

        View libPrefView = inflate.findViewById(R.id.moreLybraryettings);
        asButton(libPrefView)
                .setOnClickListener(v -> {

                    final CheckBox isScanOnLaunch = new CheckBox(v.getContext());
                    isScanOnLaunch.setText(getString(R.string.scan_for_new_books_at_launch));

                    final CheckBox isFirstSurname = new CheckBox(v.getContext());
                    isFirstSurname.setText(getString(R.string.in_the_author_s_name_first_the_surname));

                    final CheckBox isSkipFolderWithNOMEDIA = new CheckBox(v.getContext());
                    isSkipFolderWithNOMEDIA.setText(getString(R.string.ignore_folder_scan_if_nomedia_file_exists));

                    final CheckBox isAuthorTitleFromMetaPDF = new CheckBox(v.getContext());
                    isAuthorTitleFromMetaPDF.setText(
                            R.string.displaying_the_author_and_title_of_the_pdf_book_from_the_meta_tags);

                    final CheckBox isShowOnlyOriginalFileNames = new CheckBox(v.getContext());
                    isShowOnlyOriginalFileNames.setText(R.string.display_original_file_names_without_metadata);

                    final CheckBox isUseCalibreOpf = new CheckBox(v.getContext());
                    isUseCalibreOpf.setText(R.string.use_calibre_metadata);

                    final CheckBox isDisplayAnnotation = new CheckBox(v.getContext());
                    isDisplayAnnotation.setText(R.string.show_book_description);

                    final CheckBox isHideReadBook = new CheckBox(v.getContext());
                    isHideReadBook.setText(R.string.hide_read_books);

                    final CheckBox isShowSeriesNumberInTitle = new CheckBox(v.getContext());
                    isShowSeriesNumberInTitle.setText(R.string.show_series_number_in_title);

                    final AlertDialog d =
                            AlertDialogs.showViewDialog(getActivity(), null, isScanOnLaunch, isFirstSurname,
                                    isSkipFolderWithNOMEDIA,
                                    isShowOnlyOriginalFileNames, isAuthorTitleFromMetaPDF, isUseCalibreOpf,
                                    isDisplayAnnotation, isHideReadBook, isShowSeriesNumberInTitle);

                    isScanOnLaunch.setChecked(AppState.get().isScanOnLaunch);
                    isFirstSurname.setChecked(AppState.get().isFirstSurname);
                    isSkipFolderWithNOMEDIA.setChecked(AppState.get().isSkipFolderWithNOMEDIA);
                    isAuthorTitleFromMetaPDF.setChecked(AppState.get().isAuthorTitleFromMetaPDF);
                    isShowOnlyOriginalFileNames.setChecked(AppState.get().isShowOnlyOriginalFileNames);
                    isUseCalibreOpf.setChecked(AppState.get().isUseCalibreOpf);
                    isDisplayAnnotation.setChecked(AppState.get().isDisplayAnnotation);
                    isHideReadBook.setChecked(AppState.get().isHideReadBook);
                    isShowSeriesNumberInTitle.setChecked(AppState.get().isShowSeriesNumberInTitle);

                    final OnCheckedChangeListener listener = (buttonView, isChecked) -> {
                        AppState.get().isScanOnLaunch = isScanOnLaunch.isChecked();
                        AppState.get().isFirstSurname = isFirstSurname.isChecked();
                        AppState.get().isSkipFolderWithNOMEDIA = isSkipFolderWithNOMEDIA.isChecked();
                        AppState.get().isAuthorTitleFromMetaPDF = isAuthorTitleFromMetaPDF.isChecked();
                        AppState.get().isShowOnlyOriginalFileNames = isShowOnlyOriginalFileNames.isChecked();
                        AppState.get().isUseCalibreOpf = isUseCalibreOpf.isChecked();
                        AppState.get().isDisplayAnnotation = isDisplayAnnotation.isChecked();
                        AppState.get().isShowSeriesNumberInTitle = isShowSeriesNumberInTitle.isChecked();

                        handler.removeCallbacksAndMessages(null);
                        handler.postDelayed(ask, timeout);
                        handler.postDelayed(new Runnable() {

                            @Override public void run() {
                                d.dismiss();
                            }
                        }, timeout);
                    };

                    isScanOnLaunch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        AppState.get().isScanOnLaunch = isChecked;
                    });
                    isFirstSurname.setOnCheckedChangeListener(listener);
                    isAuthorTitleFromMetaPDF.setOnCheckedChangeListener(listener);
                    isSkipFolderWithNOMEDIA.setOnCheckedChangeListener(listener);
                    isShowOnlyOriginalFileNames.setOnCheckedChangeListener(listener);
                    isUseCalibreOpf.setOnCheckedChangeListener(listener);
                    isDisplayAnnotation.setOnCheckedChangeListener(listener);
                    isHideReadBook.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                        @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            AppState.get().isHideReadBook = isHideReadBook.isChecked();
                            TempHolder.listHash++;
                            notifyFragment();
                        }
                    });
                    isShowSeriesNumberInTitle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                        @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            AppState.get().isShowSeriesNumberInTitle = isShowSeriesNumberInTitle.isChecked();
                            TempHolder.listHash++;
                            notifyFragment();
                        }
                    });

                });

        ////
        ((CheckBox) inflate.findViewById(R.id.supportPDF)).

                                                                  setChecked(AppState.get().supportPDF);
        ((CheckBox) inflate.findViewById(R.id.supportPDF)).

                                                                  setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportPDF = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        ((CheckBox) inflate.findViewById(R.id.supportXPS)).

                                                                  setChecked(AppState.get().supportXPS);
        ((CheckBox) inflate.findViewById(R.id.supportXPS)).

                                                                  setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportXPS = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        ((CheckBox) inflate.findViewById(R.id.supportDJVU)).

                                                                   setChecked(AppState.get().supportDJVU);
        ((CheckBox) inflate.findViewById(R.id.supportDJVU)).

                                                                   setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportDJVU = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });
        ((CheckBox) inflate.findViewById(R.id.supportEPUB)).

                                                                   setChecked(AppState.get().supportEPUB);
        ((CheckBox) inflate.findViewById(R.id.supportEPUB)).

                                                                   setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportEPUB = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });
        ((CheckBox) inflate.findViewById(R.id.supportFB2)).

                                                                  setChecked(AppState.get().supportFB2);
        ((CheckBox) inflate.findViewById(R.id.supportFB2)).

                                                                  setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportFB2 = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        ((CheckBox) inflate.findViewById(R.id.supportTXT)).

                                                                  setChecked(AppState.get().supportTXT);
        ((CheckBox) inflate.findViewById(R.id.supportTXT)).

                                                                  setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportTXT = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        ((CheckBox) inflate.findViewById(R.id.supportMOBI)).

                                                                   setChecked(AppState.get().supportMOBI);
        ((CheckBox) inflate.findViewById(R.id.supportMOBI)).

                                                                   setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportMOBI = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        ((CheckBox) inflate.findViewById(R.id.supportRTF)).

                                                                  setChecked(AppState.get().supportRTF);
        ((CheckBox) inflate.findViewById(R.id.supportRTF)).

                                                                  setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportRTF = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        ((CheckBox) inflate.findViewById(R.id.supportDOCX)).

                                                                   setChecked(AppState.get().supportDOCX);
        ((CheckBox) inflate.findViewById(R.id.supportDOCX)).

                                                                   setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportDOCX = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        ((CheckBox) inflate.findViewById(R.id.supportODT)).

                                                                  setChecked(AppState.get().supportODT);
        ((CheckBox) inflate.findViewById(R.id.supportDOCX)).

                                                                   setText(
                AppsConfig.isDOCXSupported ? "DOC/DOCX" : "DOC");
        ((CheckBox) inflate.findViewById(R.id.supportODT)).

                                                                  setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportODT = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        ((CheckBox) inflate.findViewById(R.id.supportCBZ)).

                                                                  setChecked(AppState.get().supportCBZ);
        ((CheckBox) inflate.findViewById(R.id.supportCBZ)).

                                                                  setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().supportCBZ = isChecked;
                        ExtUtils.updateSearchExts();
                        handler.removeCallbacks(ask);
                        handler.postDelayed(ask, timeout);
                    }
                });

        CheckBox supportZIP = inflate.findViewById(R.id.supportZIP);
        supportZIP.setChecked(AppState.get().supportZIP);
        supportZIP.setOnCheckedChangeListener(new

                                                      OnCheckedChangeListener() {

                                                          @Override
                                                          public void onCheckedChanged(final CompoundButton buttonView,
                                                                                       final boolean isChecked) {
                                                              AppState.get().supportZIP = isChecked;
                                                              ExtUtils.updateSearchExts();
                                                              handler.removeCallbacks(ask);
                                                              handler.postDelayed(ask, timeout);
                                                          }
                                                      });

        CheckBox supportArch = inflate.findViewById(R.id.supportArch);
        supportArch.setChecked(AppState.get().supportArch);
        supportArch.setText(

                getString(R.string.archives) + " (RAR/7z/...)");
        supportArch.setOnCheckedChangeListener(new

                                                       OnCheckedChangeListener() {

                                                           @Override
                                                           public void onCheckedChanged(final CompoundButton buttonView,
                                                                                        final boolean isChecked) {
                                                               AppState.get().supportArch = isChecked;
                                                               ExtUtils.updateSearchExts();
                                                               handler.removeCallbacks(ask);
                                                               handler.postDelayed(ask, timeout);
                                                           }
                                                       });

        CheckBox supportOther = inflate.findViewById(R.id.supportOther);
        supportOther.setChecked(AppState.get().supportOther);
        supportOther.setText(

                getString(R.string.other) + " (CHM/...)");
        supportOther.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().supportOther = isChecked;
            ExtUtils.updateSearchExts();
            handler.removeCallbacks(ask);
            handler.postDelayed(ask, timeout);
        });

        CheckBox isDisplayAllFilesInFolder = inflate.findViewById(R.id.isDisplayAllFilesInFolder);
        isDisplayAllFilesInFolder.setChecked(AppState.get().isDisplayAllFilesInFolder);
        isDisplayAllFilesInFolder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isDisplayAllFilesInFolder = isChecked;
            TempHolder.listHash++;
        });

        CheckBox isAlwaysOpenOnPage1 = inflate.findViewById(R.id.isAlwaysOpenOnPage1);
        isAlwaysOpenOnPage1.setChecked(AppState.get().isAlwaysOpenOnPage1);
        isAlwaysOpenOnPage1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isAlwaysOpenOnPage1 = isChecked;
        });
        // app password
        final CheckBox isAppPassword = inflate.findViewById(R.id.isAppPassword);
        isAppPassword.setChecked(PasswordState.get()
                                              .hasPassword() && AppState.get().isAppPassword);
        isAppPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked && PasswordState.get()
                                          .hasPassword()) {
                AppState.get().isAppPassword = true;
            } else if (!PasswordState.get()
                                     .hasPassword()) {
                PasswordDialog.showDialog(getActivity(), true, () -> isAppPassword.setChecked(PasswordState.get()
                                                                                                           .hasPassword()));
            } else {
                AppState.get().isAppPassword = false;
                isAppPassword.setChecked(false);
            }
        });

        asButton(inflate.findViewById(R.id.appPassword))
                .setOnClickListener(v -> PasswordDialog.showDialog(getActivity(), true, () -> {
                    if (PasswordState.get()
                                     .hasPassword()) {
                        isAppPassword.setChecked(true);
                        AppState.get().isAppPassword = true;
                    }
                }));

        // What is new
        CheckBox showWhatIsNew = inflate.findViewById(R.id.isShowWhatIsNewDialog);
        showWhatIsNew.setChecked(AppState.get().isShowWhatIsNewDialog);
        showWhatIsNew.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isShowWhatIsNewDialog = isChecked;
            }
        });

        CheckBox isMenuIntegration = inflate.findViewById(R.id.isMenuIntegration);
        isMenuIntegration.setVisibility(TxtUtils.visibleIf(Build.VERSION.SDK_INT >= 23));
        isMenuIntegration.setChecked(AppState.get().isMenuIntegration);
        isMenuIntegration.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isMenuIntegration = isChecked;
                DocumentController.doContextMenu(getActivity());
            }
        });

        final TextView whatIsNew = inflate.findViewById(R.id.whatIsNew);
        whatIsNew.setText(

                getActivity().

                                     getString(R.string.what_is_new_in) + " " + Apps.getApplicationName(

                        getActivity()) + " " + Apps.getVersionName(

                        getActivity()));
        asButton(whatIsNew);
        whatIsNew.setOnClickListener(new View.OnClickListener() {

            @Override public void onClick(View v) {
                AndroidWhatsNew.show2(getActivity());

            }
        });

        ///

        // BrightnessHelper.controlsWrapper(inflate, getActivity());

        nextKeys = inflate.findViewById(R.id.textNextKeys);
        prevKeys = inflate.findViewById(R.id.textPrevKeys);

        ch = inflate.findViewById(R.id.onReverse);
        ch.setOnCheckedChangeListener(null);
        ch.setChecked(AppState.get().isReverseKeys);
        ch.setOnCheckedChangeListener(reverseListener);

        inflate.findViewById(R.id.onColorChoser)
               .

                       setOnClickListener(new OnClickListener() {

                   @Override public void onClick(final View v) {
                   }
               });

        initKeys();

        searchPaths = inflate.findViewById(R.id.searchPaths);
        showSearchPaths();

        TextView addFolder = inflate.findViewById(R.id.onConfigPath);
        asButton(addFolder, "+ " + getString(R.string.add_folder));

        // Rebuilds the library from the folders now listed, without going through the dialog
        // to reach the same scan.
        View updateLibrary = asButton(inflate.findViewById(R.id.onUpdateLibrary));
        updateLibrary.setOnClickListener(v -> onScan());
        addFolder.setOnClickListener(v -> onAddFolder());

        asButton(inflate.findViewById(R.id.importButton))
                .setOnClickListener(v -> PrefDialogs.importDialog(getActivity()));

        asButton(inflate.findViewById(R.id.exportButton))
                .setOnClickListener(v -> PrefDialogs.exportDialog(getActivity()));


        // folders

        final TextView rootFolder = inflate.findViewById(R.id.rootFolder);
        asButton(rootFolder, TxtUtils.smallPathFormat(AppSP.get().getRootPath(getActivity())));
        rootFolder.setOnClickListener(v -> ChooserDialogFragment.chooseFolder(getActivity(),
                                                                        AppSP.get().getRootPath(getActivity()))
                                                                .setOnSelectListener(
                                                                        new ResultResponse2<String, Dialog>() {
                                                                            @Override
                                                                            public boolean onResultRecive(String nPath,
                                                                                                          Dialog dialog) {
                                                                                if (new File(nPath).canWrite()) {
                                                                                    AppSP.get().rootPath1 = nPath;
                                                                                    new File(nPath, "Fonts").mkdirs();
                                                                                    asButton(rootFolder,
                                                                                            TxtUtils.smallPathFormat(
                                                                                                    nPath));
                                                                                    onTheme();
                                                                                } else {
                                                                                    Toast.makeText(getActivity(),
                                                                                                 R.string.msg_unexpected_error,
                                                                                                 Toast.LENGTH_LONG)
                                                                                         .show();
                                                                                }
                                                                                dialog.dismiss();
                                                                                return false;
                                                                            }
                                                                        }));

        final TextView fontFolder = inflate.findViewById(R.id.fontFolder);
        asButton(fontFolder, TxtUtils.smallPathFormat(BookCSS.get().fontFolder));
        fontFolder.setOnClickListener(new

                                              OnClickListener() {

                                                  @Override public void onClick(View v) {
                                                      ChooserDialogFragment.chooseFolder(getActivity(),
                                                                                   BookCSS.get().fontFolder)
                                                                           .setOnSelectListener(
                                                                                   new ResultResponse2<String, Dialog>() {
                                                                                       @Override
                                                                                       public boolean onResultRecive(
                                                                                               String nPath,
                                                                                               Dialog dialog) {
                                                                                           BookCSS.get().fontFolder =
                                                                                                   nPath;
                                                                                                   asButton(
                                                                                                   fontFolder,
                                                                                                   TxtUtils.smallPathFormat(
                                                                                                           BookCSS.get().fontFolder));
                                                                                           dialog.dismiss();
                                                                                           return false;
                                                                                       }
                                                                                   });
                                                  }
                                              });

        final TextView downloadFolder = inflate.findViewById(R.id.downloadFolder);
        asButton(downloadFolder, TxtUtils.smallPathFormat(BookCSS.get().downlodsPath));
        downloadFolder.setOnClickListener(new

                                                  OnClickListener() {

                                                      @Override public void onClick(View v) {
                                                          ChooserDialogFragment.chooseFolder(getActivity(),
                                                                                       BookCSS.get().downlodsPath)
                                                                               .setOnSelectListener(
                                                                                       new ResultResponse2<String, Dialog>() {
                                                                                           @Override
                                                                                           public boolean onResultRecive(
                                                                                                   String nPath,
                                                                                                   Dialog dialog) {
                                                                                               BookCSS.get().downlodsPath =
                                                                                                       nPath;
                                                                                                       asButton(
                                                                                                       downloadFolder,
                                                                                                       TxtUtils.smallPathFormat(
                                                                                                               BookCSS.get().downlodsPath));
                                                                                               dialog.dismiss();
                                                                                               return false;
                                                                                           }
                                                                                       });
                                                      }
                                                  });

        final TextView syncPath = inflate.findViewById(R.id.syncPath);
        asButton(syncPath, TxtUtils.smallPathFormat(BookCSS.get().syncDropboxPath));
        syncPath.setOnClickListener(new

                                            OnClickListener() {

                                                @Override public void onClick(View v) {
                                                    ChooserDialogFragment.chooseFolder(getActivity(),
                                                                                 BookCSS.get().syncDropboxPath)
                                                                         .setOnSelectListener(
                                                                                 new ResultResponse2<String, Dialog>() {
                                                                                     @Override
                                                                                     public boolean onResultRecive(
                                                                                             String nPath,
                                                                                             Dialog dialog) {
                                                                                         BookCSS.get().syncDropboxPath =
                                                                                                 nPath;
                                                                                                 asButton(
                                                                                                 downloadFolder,
                                                                                                 TxtUtils.smallPathFormat(
                                                                                                         BookCSS.get().syncDropboxPath));
                                                                                         dialog.dismiss();
                                                                                         return false;
                                                                                     }
                                                                                 });
                                                }
                                            });

        final TextView ttsFolder = inflate.findViewById(R.id.ttsFolder);
        asButton(ttsFolder, TxtUtils.smallPathFormat(BookCSS.get().ttsSpeakPath));
        ttsFolder.setOnClickListener(new

                                             OnClickListener() {

                                                 @Override public void onClick(View v) {
                                                     ChooserDialogFragment.chooseFolder(getActivity(),
                                                                                  BookCSS.get().ttsSpeakPath)
                                                                          .setOnSelectListener(
                                                                                  new ResultResponse2<String, Dialog>() {
                                                                                      @Override
                                                                                      public boolean onResultRecive(
                                                                                              String nPath,
                                                                                              Dialog dialog) {
                                                                                          BookCSS.get().ttsSpeakPath =
                                                                                                  nPath;
                                                                                          asButton(ttsFolder,
                                                                                                  TxtUtils.smallPathFormat(
                                                                                                          BookCSS.get().ttsSpeakPath));
                                                                                          dialog.dismiss();
                                                                                          return false;
                                                                                      }
                                                                                  });
                                                 }
                                             });

        final TextView backupPath = inflate.findViewById(R.id.backupFolder);
        asButton(backupPath, TxtUtils.smallPathFormat(BookCSS.get().backupPath));
        backupPath.setOnClickListener(new

                                              OnClickListener() {

                                                  @Override public void onClick(View v) {
                                                      ChooserDialogFragment.chooseFolder(getActivity(),
                                                                                   BookCSS.get().backupPath)
                                                                           .setOnSelectListener(
                                                                                   new ResultResponse2<String, Dialog>() {
                                                                                       @Override
                                                                                       public boolean onResultRecive(
                                                                                               String nPath,
                                                                                               Dialog dialog) {
                                                                                           BookCSS.get().backupPath =
                                                                                                   nPath;
                                                                                                   asButton(
                                                                                                   backupPath,
                                                                                                   TxtUtils.smallPathFormat(
                                                                                                           BookCSS.get().backupPath));
                                                                                           dialog.dismiss();
                                                                                           return false;
                                                                                       }
                                                                                   });
                                                  }
                                              });

        // Widget Configuration

        final TextView widgetLayout = inflate.findViewById(R.id.widgetLayout);
        widgetLayout.setText(AppState.get().widgetType == AppState.WIDGET_LIST ? R.string.list : R.string.grid);
        asButton(widgetLayout);

        widgetLayout.setOnClickListener(new

                                                OnClickListener() {

                                                    @Override public void onClick(View v) {
                                                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                                                        final MenuItem recent = popupMenu.getMenu()
                                                                                         .add(R.string.list);
                                                        recent.setOnMenuItemClickListener(
                                                                new OnMenuItemClickListener() {

                                                                    @Override public boolean onMenuItemClick(
                                                                            final MenuItem item) {
                                                                        AppState.get().widgetType =
                                                                                AppState.WIDGET_LIST;
                                                                        widgetLayout.setText(R.string.list);
                                                                        asButton(widgetLayout);
                                                                        RecentUpates.updateAll();
                                                                        return false;
                                                                    }
                                                                });

                                                        final MenuItem starred = popupMenu.getMenu()
                                                                                          .add(R.string.grid);
                                                        starred.setOnMenuItemClickListener(
                                                                new OnMenuItemClickListener() {

                                                                    @Override public boolean onMenuItemClick(
                                                                            final MenuItem item) {
                                                                        AppState.get().widgetType =
                                                                                AppState.WIDGET_GRID;
                                                                        widgetLayout.setText(R.string.grid);
                                                                        asButton(widgetLayout);
                                                                        RecentUpates.updateAll();
                                                                        return false;
                                                                    }
                                                                });

                                                        popupMenu.show();

                                                    }

                                                });

        final TextView widgetForRecent = inflate.findViewById(R.id.widgetForRecent);
        widgetForRecent.setText(AppState.get().isStarsInWidget ? R.string.starred : R.string.recent);
        asButton(widgetForRecent);

        widgetForRecent.setOnClickListener(new

                                                   OnClickListener() {

                                                       @Override public void onClick(View v) {
                                                           final PopupMenu popupMenu =
                                                                   new PopupMenu(widgetForRecent.getContext(),
                                                                           widgetForRecent);

                                                           final MenuItem recent = popupMenu.getMenu()
                                                                                            .add(R.string.recent);
                                                           recent.setOnMenuItemClickListener(
                                                                   new OnMenuItemClickListener() {

                                                                       @Override public boolean onMenuItemClick(
                                                                               final MenuItem item) {
                                                                           AppState.get().isStarsInWidget = false;
                                                                           widgetForRecent.setText(
                                                                                   AppState.get().isStarsInWidget ?
                                                                                           R.string.starred :
                                                                                           R.string.recent);
                                                                           asButton(widgetForRecent);

                                                                           RecentUpates.updateAll();
                                                                           return false;
                                                                       }
                                                                   });

                                                           final MenuItem starred = popupMenu.getMenu()
                                                                                             .add(R.string.starred);
                                                           starred.setOnMenuItemClickListener(
                                                                   new OnMenuItemClickListener() {

                                                                       @Override public boolean onMenuItemClick(
                                                                               final MenuItem item) {
                                                                           AppState.get().isStarsInWidget = true;
                                                                           widgetForRecent.setText(
                                                                                   AppState.get().isStarsInWidget ?
                                                                                           R.string.starred :
                                                                                           R.string.recent);
                                                                           asButton(widgetForRecent);

                                                                           RecentUpates.updateAll();
                                                                           return false;
                                                                       }
                                                                   });

                                                           popupMenu.show();

                                                       }

                                                   });

        final TextView widgetItemsCount = inflate.findViewById(R.id.widgetItemsCount);
        widgetItemsCount.setText("" + AppState.get().widgetItemsCount);
        asButton(widgetItemsCount);
        widgetItemsCount.setOnClickListener(new

                                                    OnClickListener() {

                                                        @SuppressLint("NewApi") @Override public void onClick(View v) {
                                                            PopupMenu p = new PopupMenu(getContext(), columsCount);
                                                            for (int i = 1; i <= 50; i++) {
                                                                final int k = i;
                                                                p.getMenu()
                                                                 .add("" + k)
                                                                 .setOnMenuItemClickListener(
                                                                         new OnMenuItemClickListener() {

                                                                             @Override public boolean onMenuItemClick(
                                                                                     MenuItem item) {
                                                                                 AppState.get().widgetItemsCount = k;
                                                                                 widgetItemsCount.setText("" + k);
                                                                                 asButton(
                                                                                         widgetItemsCount);
                                                                                 RecentUpates.updateAll();
                                                                                 return false;
                                                                             }
                                                                         });
                                                            }

                                                            p.show();
                                                        }
                                                    });

        // dictionary
        isRememberDictionary = inflate.findViewById(R.id.isRememberDictionary);
        isRememberDictionary.setChecked(AppState.get().isRememberDictionary);
        isRememberDictionary.setOnCheckedChangeListener(new

                                                                OnCheckedChangeListener() {

                                                                    @Override public void onCheckedChanged(
                                                                            final CompoundButton buttonView,
                                                                            final boolean isChecked) {
                                                                        AppState.get().isRememberDictionary = isChecked;
                                                                    }
                                                                });

        selectedDictionaly = inflate.findViewById(R.id.selectedDictionaly);
        selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
        selectedDictionaly.setOnClickListener(new

                                                      OnClickListener() {

                                                          @Override public void onClick(View v) {
                                                              DialogTranslateFromTo.show(getActivity(), false,
                                                                      new Runnable() {

                                                                          @Override public void run() {
                                                                              selectedDictionaly.setText(
                                                                                      DialogTranslateFromTo.getSelectedDictionaryUnderline());
                                                                          }
                                                                      }, false);
                                                          }
                                                      });

        textDayColor = inflate.findViewById(R.id.onDayColor);
        textDayColor.setOnClickListener(new

                                                OnClickListener() {

                                                    @Override public void onClick(View v) {
                                                        new ColorsDialog(getActivity(), true,
                                                                AppState.get().colorDayText, AppState.get().colorDayBg,
                                                                AppState.get().colorDayForeground, false, true,
                                                                new ColorsDialogResult() {

                                                                    @Override public void onChooseColor(int colorText,
                                                                                                        int colorBg,
                                                                                                        int colorForeground) {
                                                                        textDayColor.setTextColor(colorText);
                                                                        textDayColor.setBackgroundColor(colorBg);

                                                                        AppState.get().colorDayText = colorText;
                                                                        AppState.get().colorDayBg = colorBg;
                                                                        AppState.get().colorDayForeground =
                                                                                colorForeground;

                                                                        IMG.clearDiscCache();
                                                                        IMG.clearMemoryCache();
                                                                    }
                                                                });
                                                    }
                                                });

        textNigthColor = inflate.findViewById(R.id.onNigthColor);
        textNigthColor.setOnClickListener(new

                                                  OnClickListener() {

                                                      @Override public void onClick(View v) {
                                                          new ColorsDialog(getActivity(), false,
                                                                  AppState.get().colorNigthText,
                                                                  AppState.get().colorNigthBg,
                                                                  AppState.get().colorNigthForeground, false, true,
                                                                  new ColorsDialogResult() {

                                                                      @Override public void onChooseColor(int colorText,
                                                                                                          int colorBg,
                                                                                                          int colorForeground) {
                                                                          textNigthColor.setTextColor(colorText);
                                                                          textNigthColor.setBackgroundColor(colorBg);

                                                                          AppState.get().colorNigthText = colorText;
                                                                          AppState.get().colorNigthBg = colorBg;
                                                                          AppState.get().colorNigthForeground =
                                                                                  colorForeground;

                                                                      }
                                                                  });
                                                      }
                                                  });

        View onDefalt = asButton(inflate.findViewById(R.id.onDefaultColor));
        onDefalt.setOnClickListener(new

                                            OnClickListener() {

                                                @Override public void onClick(View v) {
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

        //color
        {
            LinearLayout colorsLine = inflate.findViewById(R.id.colorsLine);
            colorsLine.removeAllViews();

            for (String color : AppState.STYLE_COLORS) {
                View view = inflater.inflate(R.layout.item_color, (ViewGroup) inflate, false);
                view.setBackgroundColor(Color.TRANSPARENT);
                final int intColor = Color.parseColor(color);
                final View img = view.findViewById(R.id.itColor);
                TintUtil.setColorSwatch(img, intColor);
                img.setContentDescription(getString(R.string.color));

                colorsLine.addView(view, new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

                view.setOnClickListener(new OnClickListener() {

                    @Override public void onClick(View v) {
                        TintUtil.color = intColor;
                        AppState.get().tintThemeColor = intColor;
                        TempHolder.listHash++;

                        onTintChanged();
                        sendNotifyTintChanged();

                        AppProfile.save(getActivity());

                    }
                });
            }

            View view = inflater.inflate(R.layout.item_color, (ViewGroup) inflate, false);
            view.setBackgroundColor(Color.TRANSPARENT);
            view.setContentDescription(getString(R.string.color));
            final ImageView img = view.findViewById(R.id.itColor);
            img.setImageResource(R.drawable.glyphicons_371_plus);
            img.setColorFilter(AppState.get().userColor);
            TintUtil.setColorSwatchOutline(img, AppState.get().userColor);
            colorsLine.addView(view, new

                    LayoutParams(Dips.dpToPx(30), Dips.

                                                              dpToPx(30)));

            view.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    new HSVColorPickerDialog(getContext(), AppState.get().userColor, new OnColorSelectedListener() {

                        @Override public void colorSelected(Integer color) {
                            AppState.get().userColor = color;
                            AppState.get().tintThemeColor = color;
                            TintUtil.color = color;
                            img.setColorFilter(color);
                            TintUtil.setColorSwatchOutline(img, color);

                            onTintChanged();
                            sendNotifyTintChanged();

                            AppProfile.save(getActivity());

                            TempHolder.listHash++;

                        }
                    }).show();

                }
            });
        }
        ///end colors
        ////
        {
            Runnable onAccent = new Runnable() {
                @Override public void run() {
                    if (AppState.get().isUiTextColor && AppState.get().uiTextColorUser != AppState.get().tintThemeColor) {
                        AppState.get().statusBarColorDay = AppState.get().uiTextColorUser;
                        AppState.get().statusBarColorNight = AppState.get().uiTextColorUser;
                    } else {
                        AppState.get().statusBarColorDay = Color.parseColor(AppState.TEXT_COLOR_DAY);
                        AppState.get().statusBarColorNight = Color.parseColor(AppState.TEXT_COLOR_NIGHT);
                    }

                    TempHolder.listHash++;
                    onTintChanged();
                    sendNotifyTintChanged();
                    ((MainTabs2) getActivity()).updateCurrentFragment();

                    //TxtUtils.updateAllLinks(inflate, true);
                }
            };

            LinearLayout colorsLine = inflate.findViewById(R.id.colorsLine_a);
            colorsLine.removeAllViews();

            CheckBox isAccentTextColor = inflate.findViewById(R.id.isAccentTextColor);

            isAccentTextColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    AppState.get().isUiTextColor = b;
                    onAccent.run();
                }
            });
            isAccentTextColor.setChecked(AppState.get().isUiTextColor);

            for (String color : AppState.ACCENT_COLORS) {
                final int intColor = Color.parseColor(color);
                if (AppState.get().appTheme == AppState.THEME_LIGHT || AppState.get().appTheme == AppState.THEME_INK) {
                    if (intColor == Color.WHITE) {
                        continue;
                    }
                }
                if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
                    if (intColor == Color.BLACK) {
                        continue;
                    }
                }

                View view = inflater.inflate(R.layout.item_color, (ViewGroup) inflate, false);
                view.setBackgroundColor(Color.TRANSPARENT);

                final View img = view.findViewById(R.id.itColor);
                TintUtil.setColorSwatch(img, intColor);
                img.setContentDescription(getString(R.string.color));

                colorsLine.addView(view, new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

                view.setOnClickListener(new OnClickListener() {

                    @Override public void onClick(View v) {

                        AppState.get().isUiTextColor = true;
                        AppState.get().uiTextColor = intColor;
                        AppState.get().uiTextColorUser = intColor;

                        isAccentTextColor.setChecked(AppState.get().isUiTextColor);

                        onAccent.run();

                    }
                });
            }

            View view = inflater.inflate(R.layout.item_color, (ViewGroup) inflate, false);
            view.setBackgroundColor(Color.TRANSPARENT);
            view.setContentDescription(getString(R.string.color));
            final ImageView img = view.findViewById(R.id.itColor);
            img.setImageResource(R.drawable.glyphicons_371_plus);
            img.setColorFilter(AppState.get().userColor);
            TintUtil.setColorSwatchOutline(img, AppState.get().userColor);
            colorsLine.addView(view, new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

            view.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    new HSVColorPickerDialog(getContext(), AppState.get().userColor, new OnColorSelectedListener() {

                        @Override public void colorSelected(Integer color) {
                            AppState.get().isUiTextColor = true;
                            AppState.get().uiTextColor = color;
                            AppState.get().uiTextColorUser = color;

                            img.setColorFilter(color);
                            TintUtil.setColorSwatchOutline(img, color);
                            isAccentTextColor.setChecked(AppState.get().isUiTextColor);

                            onAccent.run();

                        }
                    }).show();

                }
            });
        }
        ////

        asButton(inflate.findViewById(R.id.linksColor)).

                                                                setOnClickListener(new OnClickListener() {

            @Override public void onClick(final View v) {
                closeLeftMenu();
                Dialogs.showLinksColorDialog(getActivity(), new Runnable() {

                    @Override public void run() {
                        TempHolder.listHash++;
                        onTintChanged();
                        sendNotifyTintChanged();
                        ((MainTabs2) getActivity()).updateCurrentFragment();

                        //TxtUtils.updateAllLinks(inflate, true);

                    }
                });
            }
        });

        ///link colors

        ////

        asButton(inflate.findViewById(R.id.onContrast)).

                                                                setOnClickListener(new OnClickListener() {

            @Override public void onClick(final View v) {
                Dialogs.showContrastDialogByUrl(getActivity(), new Runnable() {

                    @Override public void run() {
                        IMG.clearDiscCache();
                        IMG.clearMemoryCache();
                        TempHolder.listHash++;
                        notifyFragment();

                    }
                });
            }
        });

        asButton(inflate.findViewById(R.id.onRateIt)).

                                                              setOnClickListener(new OnClickListener() {

            @Override public void onClick(final View v) {
                Urls.rateIT(getActivity());
            }
        });

        asButton(inflate.findViewById(R.id.openWeb)).

                                                             setOnClickListener(new OnClickListener() {

            @Override public void onClick(final View v) {
                Urls.open(getActivity(), WWW_SITE);
            }
        });

        asButton(inflate.findViewById(R.id.openBeta)).

                                                              setOnClickListener(new OnClickListener() {

            @Override public void onClick(final View v) {
                Urls.open(getActivity(), WWW_BETA_SITE);
            }
        });

        asButton(inflate.findViewById(R.id.openWiki)).

                                                              setOnClickListener(new OnClickListener() {

            @Override public void onClick(final View v) {
                Urls.open(getActivity(), WWW_WIKI_SITE);
            }
        });

        asButton(inflate.findViewById(R.id.onTelegram)).

                                                                setOnClickListener(new OnClickListener() {

            @Override public void onClick(final View v) {
                Urls.open(getActivity(), "https://t.me/LibreraReader");
            }
        });

        TextView proText = inflate.findViewById(R.id.downloadPRO);
        asButton(proText);
        ((View) proText.getParent()).

                                            setOnClickListener(new OnClickListener() {

            @Override public void onClick(final View v) {
                Urls.openPdfPro(getActivity());
            }
        });

        inflate.findViewById(R.id.cleanRecent)
               .

                       setOnClickListener(new View.OnClickListener() {

                   @Override public void onClick(final View v) {
                       final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                       builder.setMessage(getString(R.string.clear_all_recent) + "?");
                       builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                           @Override public void onClick(DialogInterface dialog, int which) {
                               //BookmarksData.get().cleanRecent();
                           }
                       });
                       builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                           @Override public void onClick(DialogInterface dialog, int which) {
                               // TODO Auto-generated method stub

                           }
                       });
                       builder.show();
                   }
               });

        inflate.findViewById(R.id.cleanBookmarks)
               .

                       setOnClickListener(new View.OnClickListener() {

                   @Override public void onClick(final View v) {
                       final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                       builder.setMessage(getString(R.string.clear_all_bookmars) + "?");
                       builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                           @Override public void onClick(DialogInterface dialog, int which) {
                               BookmarksData.get()
                                            .cleanBookmarks();

                           }
                       });
                       builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                           @Override public void onClick(DialogInterface dialog, int which) {
                               // TODO Auto-generated method stub

                           }
                       });
                       builder.show();

                   }
               });

        // licences link
        asButton(inflate.findViewById(R.id.libraryLicenses)).

                                                                     setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.licenses_for_libraries);

                WebView wv = new WebView(getActivity());
                wv.loadUrl("file:///android_asset/licenses.html");
                wv.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });

                alert.setView(wv);
                alert.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog create = alert.create();
                create.setOnDismissListener(new OnDismissListener() {

                    @Override public void onDismiss(DialogInterface dialog) {
                        Keyboards.hideNavigation(getActivity());
                    }
                });
                create.show();
            }
        });

        asButton(inflate.findViewById(R.id.docSearch))
                .

                        setOnClickListener(new OnClickListener() {

                    @Override public void onClick(View v) {
                        MultyDocSearchDialog.show(getActivity());
                    }
                });
        // convert
        final TextView docConverter = inflate.findViewById(R.id.docConverter);
        asButton(docConverter);
        docConverter.setOnClickListener(new

                                                OnClickListener() {

                                                    @Override public void onClick(View v) {
                                                        PopupMenu p = new PopupMenu(getContext(), v);
                                                        for (final String id : AppState.CONVERTERS.keySet()) {
                                                            p.getMenu()
                                                             .add("" + getActivity().getString(
                                                                     R.string.convert_to) + " " + id)
                                                             .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                                                 @Override
                                                                 public boolean onMenuItemClick(MenuItem item) {
                                                                     ShareDialog.showsItemsDialog(getActivity(),
                                                                             getActivity().getString(
                                                                                     R.string.convert_to) + " " + id,
                                                                             AppState.CONVERTERS.get(id));

                                                                     return false;
                                                                 }
                                                             });

                                                        }
                                                        p.show();
                                                    }
                                                });

        final TextView newFile = inflate.findViewById(R.id.newFile);
        asButton(newFile);
        newFile.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {

                AlertDialogs.editFileTxt(getActivity(), null, AppProfile.DOWNLOADS_DIR, new StringResponse() {
                    @Override public boolean onResultRecive(String string) {
                        ExtUtils.openFile(getActivity(), new FileMeta(string));
                        return false;
                    }
                });

            }
        });

        statusBarHack = getActivity().findViewById(R.id.systemBarHack);

        overlay =

                getActivity().

                                     findViewById(R.id.overlay);

        TextView onProfile = inflate.findViewById(R.id.onProfile);

        profileLetter = inflate.findViewById(R.id.profileLetter);

        final String p = AppProfile.getCurrent();

        profileLetter.setText(TxtUtils.getFirstLetter(p));
        profileLetter.setBackgroundDrawable(AppProfile.getProfileColorDrawable(

                getActivity(), p));

        onProfile.setText(p);

        profileLetter.setContentDescription(p + " " + getString(R.string.profile));
        onProfile.setContentDescription(p + " " + getString(R.string.profile));

        asButton(onProfile);
        onProfile.setOnClickListener(v ->

        {

            if (BooksService.isRunning) {
                Toast.makeText(getActivity(), R.string.please_wait_books_are_being_processed_, Toast.LENGTH_SHORT)
                     .show();
                return;
            }

            MyPopupMenu popup = new MyPopupMenu(getActivity(), v);

            // The head names what the rows under it choose between, now that the panel no
            // longer carries the word beside the button.
            popup.getMenu()
                 .add(R.string.profile)
                 .asTitle();

            List<String> all = AppProfile.getAllProfiles();
            for (String profile : all) {

                popup.getMenu()
                     .setDrawable(TxtUtils.getFirstLetter(profile),
                             AppProfile.getProfileColorDrawable(getActivity(), profile))
                     .add(profile)
                     .setOnMenuItemClickListener(menu -> {
                         {
                             if (!profile.equals(AppProfile.getCurrent())) {

                                 AlertDialogs.showOkDialog(getActivity(),
                                         getActivity().getString(R.string.do_you_want_to_switch_profile_),
                                         new Runnable() {

                                             @Override public void run() {
                                                 AppProfile.saveCurrent(getActivity(), profile);
                                                 RecentUpates.updateAll();
                                                 onTheme();
                                             }
                                         });
                             }

                             return false;
                         }
                     });
            }
            popup.show();

        });
        profileLetter.setOnClickListener(v -> onProfile.performClick());

        final View.OnLongClickListener onDefaultProfile = v -> {

            if (BooksService.isRunning) {
                Toast.makeText(getActivity(), R.string.please_wait_books_are_being_processed_, Toast.LENGTH_SHORT)
                     .show();
                return true;
            }

            AlertDialogs.showOkDialog(getActivity(), getString(R.string.restore_defaults_full), new Runnable() {
                @Override public void run() {
                    //AppProfile.clear();
                    DragingPopup.resetCache(getActivity());

                    CacheZipUtils.emptyAllCacheDirs();


                    final BookCSS b = new BookCSS();
                    b.resetToDefault(getActivity());
                    IO.writeObjSync(AppProfile.syncCSS, b);

                    final AppState o = new AppState();
                    o.defaults(getActivity());

                    IO.writeObjSync(AppProfile.syncState, o);

                    AppProfile.syncExclude.delete();

                    File rootFiles = AppProfile.SYNC_FOLDER_DEVICE_PROFILE;
                    if (rootFiles != null && rootFiles.listFiles()!=null) {
                        for (File file : rootFiles.listFiles()) {
                            String name = file.getName();
                            if (name.endsWith(".css")) {
                                file.delete();
                                LOG.d("Delete-css", file);

                            }
                        }
                    }

                    //AppProfile.init(getActivity());
                    //BooksService.startForeground(getActivity(), BooksService.ACTION_SEARCH_ALL);
                    SearchAllBooksWorker.run(getActivity());
                    onTheme();

                }
            });

            return true;
        };
        onProfile.setOnLongClickListener(onDefaultProfile);
        profileLetter.setOnLongClickListener(onDefaultProfile);

        inflate.findViewById(R.id.onProfileEdit)
               .

                       setOnClickListener(v ->

               {

                   if (BooksService.isRunning) {
                       Toast.makeText(getActivity(), R.string.please_wait_books_are_being_processed_,
                                    Toast.LENGTH_SHORT)
                            .show();
                       return;
                   }

                   AppProfile.showDialog(getActivity(), profile -> {
                       if (!profile.equals(AppProfile.getCurrent())) {
                           AlertDialogs.showOkDialog(getActivity(),
                                   getActivity().getString(R.string.do_you_want_to_switch_profile_), new Runnable() {

                                       @Override public void run() {
                                           AppProfile.saveCurrent(getActivity(), profile);
                                           onTheme();
                                       }
                                   });
                       }
                       return false;
                   }, () -> onDefaultProfile.onLongClick(v));
               });

        TxtUtils.updateAllLinks(inflate, true);
        paintLinkButtons(inflate);
        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);
        return inflate;

    }

    private void onEink() {
        AppState.get().appTheme = AppState.THEME_INK;
        AppState.get().blueLightAlpha = 0;
        AppState.get().tintThemeColor = Color.BLACK;
        AppState.get().uiTextColor = Color.BLACK;
        AppState.get().isUiTextColor = true;
        TintUtil.color = Color.BLACK;

        onTintChanged();
        sendNotifyTintChanged();

        AppProfile.save(getActivity());

        getActivity().finish();
        MainTabs2.startActivity(getActivity(), TempHolder.get().currentTab);

    }

    /**
     * A link in the panel is drawn as a button - a ring in the theme colour, cut to the same
     * round the cards are - rather than as an underlined word. Every link that used to be
     * underlined here goes through this instead.
     */
    public View asButton(View text) {
        if (!(text instanceof TextView)) {
            return text;
        }
        TextView button = (TextView) text;
        TintUtil.asLinkButton(button);
        drawIconInside(button);
        paintLinkButton(button);
        return button;
    }

    /** Cuts a button's ring and its mark from the colour its own word is drawn in. */
    private void paintLinkButton(TextView button) {
        int color = button.getCurrentTextColor();
        TintUtil.setRingColor(button, color);
        if (keepsOwnColour(markBeside(button))) {
            return;
        }
        for (Drawable icon : button.getCompoundDrawables()) {
            if (icon != null) {
                icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    /**
     * Draws a picture into a square of the given size, as it is. The icon is taken from a
     * plain raster rather than from the launcher's own resource: what the system hands back
     * for an app icon is already cut to whatever shape the launcher uses - a circle here -
     * and there is no getting the square art back out of it.
     */
    private Drawable sizedPicture(Drawable source, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        source.setBounds(0, 0, size, size);
        source.draw(new Canvas(bitmap));

        BitmapDrawable picture = new BitmapDrawable(getResources(), bitmap);
        picture.setBounds(0, 0, size, size);
        return picture;
    }

    /** The mark standing in front of a link, if there is one. */
    private static ImageView markBeside(TextView button) {
        if (!(button.getParent() instanceof ViewGroup)) {
            return null;
        }
        ViewGroup row = (ViewGroup) button.getParent();
        int position = row.indexOfChild(button);
        if (position <= 0 || !(row.getChildAt(position - 1) instanceof ImageView)) {
            return null;
        }
        return (ImageView) row.getChildAt(position - 1);
    }

    /** Whether a mark is one the app tags as its own to colour - a picture, not a glyph. */
    private boolean keepsOwnColour(View mark) {
        if (mark == null || mark.getTag() == null) {
            return false;
        }
        String tag = mark.getTag()
                         .toString();
        return "no_tint".equals(tag) || getString(R.string.no_tint)
                                                .equals(tag);
    }

    /**
     * The colour pass over the panel's links runs after the buttons are built and leaves their
     * rings behind, so every link is walked once the words have their final colour. A link
     * that is not drawn as a button has no ring to cut, and the walk passes over it.
     */
    private void paintLinkButtons(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                paintLinkButtons(group.getChildAt(i));
            }
        } else if (root instanceof TextView && "textLink".equals(root.getTag())) {
            paintLinkButton((TextView) root);
        }
    }

    /**
     * A link that stands next to a mark takes the mark inside the button, so the two read as
     * one thing to press rather than as a picture parked beside a box.
     */
    private void drawIconInside(TextView button) {
        if (!(button.getParent() instanceof ViewGroup)) {
            return;
        }
        ImageView mark = markBeside(button);
        if (mark == null) {
            return;
        }
        Drawable icon = mark.getDrawable();
        if (icon == null) {
            return;
        }
        icon = icon.mutate();
        // A mark asked to keep its own colours is a picture, not a glyph: it keeps them and is
        // drawn at twice the size a glyph is, so the picture can be made out.
        if (keepsOwnColour(mark)) {
            icon = sizedPicture(icon, Dips.dpToPx(36));
        } else {
            int size = Dips.dpToPx(18);
            icon.setBounds(0, 0, size, size);
            icon.setColorFilter(button.getCurrentTextColor(), PorterDuff.Mode.SRC_IN);
        }
        button.setCompoundDrawables(icon, null, null, null);
        button.setCompoundDrawablePadding(Dips.DP_6);
        // With a mark inside it the button leads with the mark, so the space in front of it is
        // cut back to the same the mark has above and below - the wider inset a button of
        // words starts with would leave the mark adrift from its own edge.
        button.setPadding(Dips.DP_4, Dips.DP_4, Dips.DP_10, Dips.DP_4);
        mark.setVisibility(View.GONE);
    }

    /** The same, for a link whose text is rewritten every time the panel is refreshed. */
    public View asButton(TextView text, CharSequence value) {
        text.setText(value);
        return asButton(text);
    }

    /**
     * The folders the library is built from, one row a folder: a folder mark, the path itself
     * across the width of the panel, and a round button to drop it. Run together in one block
     * of text a long path wrapped into the next and neither could be told from the other. The
     * middle of a path is what gives way when it will not fit - the drive it is on and the
     * folder it ends in are what name it.
     */
    private void showSearchPaths() {
        if (searchPaths == null) {
            return;
        }
        searchPaths.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        for (final String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
            View row = inflater.inflate(R.layout.path_item, searchPaths, false);

            TextView pathView = row.findViewById(R.id.browserPath);
            pathView.setText(path);
            pathView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            // Tagged as a link so the panel's colour pass reaches it, and the marks beside it
            // are cut from whatever colour that pass settles on.
            pathView.setTag("textLink");
            TxtUtils.setLinkTextColor(pathView);
            int rowColor = pathView.getCurrentTextColor();

            View mark = row.findViewById(R.id.image1);
            if (mark instanceof ImageView) {
                TintUtil.setTintImageNoAlpha((ImageView) mark, rowColor);
            }

            View remove = row.findViewById(R.id.delete);
            TintUtil.setRingColor(remove, rowColor);
            if (remove instanceof ImageView) {
                TintUtil.setTintImageNoAlpha((ImageView) remove, rowColor);
            }
            remove.setOnClickListener(v -> {
                BookCSS.get().searchPathsJson = JsonDB.remove(BookCSS.get().searchPathsJson, path);
                showSearchPaths();
                saveChanges();
                LOG.d("Save Changes", 3);
            });

            row.setOnClickListener(v -> onFolderConfigDialog());
            searchPaths.addView(row);
        }
    }

    /**
     * Picks a folder and puts it straight into the library paths. The panel already lists the
     * folders under this button, so the folder dialog is not put in the way of adding one.
     */
    public void onAddFolder() {
        ChooserDialogFragment.chooseFolder(getActivity(), BookCSS.get().dirLastPath)
                             .setOnSelectListener(new ResultResponse2<String, Dialog>() {
                                 @Override public boolean onResultRecive(String nPath, Dialog dialog) {
                                     if (PrefDialogs.addSearchPath(getActivity(), nPath)) {
                                         showSearchPaths();
                                         saveChanges();
                                     }
                                     dialog.dismiss();
                                     return false;
                                 }
                             });
    }

    public void onFolderConfigDialog() {

        PrefDialogs.chooseFolderDialog(getActivity(), new Runnable() {

            @Override public void run() {
                showSearchPaths();
                saveChanges();
                LOG.d("Save Changes", 2);
            }
        }, new Runnable() {

            @Override public void run() {
                onScan();
            }
        });

    }

    @Override public void onResume() {
        super.onResume();

        BrightnessHelper.updateOverlay(overlay);
        BrightnessHelper.showBlueLigthDialogAndBrightness(getActivity(), inflate, new Runnable() {

            @Override public void run() {
                BrightnessHelper.updateOverlay(overlay);
            }
        });

        rotationText();

        ch.setOnCheckedChangeListener(null);
        ch.setChecked(AppState.get().isReverseKeys);
        ch.setOnCheckedChangeListener(reverseListener);

        configSingleClick.update();

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
        nextKeys.setText(String.format("%s: %s", getActivity().getString(R.string.next_keys),
                AppState.keyToString(AppState.get().nextKeys)));
        prevKeys.setText(String.format("%s: %s", getActivity().getString(R.string.prev_keys),
                AppState.keyToString(AppState.get().prevKeys)));
    }

    @Override public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onEmail() {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        String string = getResources().getString(R.string.my_email)
                                      .replace("<u>", "")
                                      .replace("</u>", "");
        final String[] aEmailList = {string};
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                Apps.getApplicationName(getContext()) + " " + Apps.getVersionName(getContext()));
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hi Support, ");

        try {
            startActivity(Intent.createChooser(emailIntent, getActivity().getString(R.string.send_mail)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), R.string.there_are_no_email_applications_installed_, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    public String getFullDeviceInfo() {
        return "(" + Build.BRAND + ", " + Build.MODEL + ", " + android.os.Build.VERSION.RELEASE + ", " + Dips.screenWidthDP() + "dp" + ")";
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

        // A scan already under way is left to finish: starting a second one over the same
        // folders would have the two writing the library out from under each other. The
        // reader is told why nothing happened, as everywhere else that waits on this.
        if (PrefDialogs.isBookSeriviceIsRunning(getActivity())) {
            return;
        }
        AppProfile.save(getActivity());
        closeLeftMenu();

        //BooksService.startForeground(getActivity(), BooksService.ACTION_SEARCH_ALL);
        SearchAllBooksWorker.run(getActivity());

        Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                                                                 .putExtra(MainTabs2.EXTRA_PAGE_NUMBER,
                                                                         UITab.getCurrentTabIndex(
                                                                                 UITab.SearchFragment));//

        LocalBroadcastManager.getInstance(getActivity())
                             .sendBroadcast(intent);

        ((AdsFragmentActivity) PrefFragment2.this.getActivity()).showInterstitialNoFinish();
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
        screenOrientation.setText(DocumentController.getRotationText());
        asButton(screenOrientation);
        DocumentController.doRotation(getActivity());
    }

    @Override public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rotationText();

        if (AppState.get().isSystemThemeColor) {
            themeColor.setText(getString(R.string.system));
        } else if (AppState.get().appTheme == AppState.THEME_INK) {
            themeColor.setText("Ink");
        } else if (AppState.get().appTheme == AppState.THEME_LIGHT) {
            themeColor.setText(getString(R.string.light));
        } else if (AppState.get().appTheme == AppState.THEME_DARK) {
            themeColor.setText(getString(R.string.black));
        } else if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
            themeColor.setText(getString(R.string.dark_oled));
        } else {
            themeColor.setText("unknown");

        }
        asButton(themeColor);
    }

    private void saveChanges() {
        if (getActivity() != null) {
            AppProfile.save(getActivity());
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
