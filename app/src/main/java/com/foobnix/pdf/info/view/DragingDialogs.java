package com.foobnix.pdf.info.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.foobnix.StringResponse;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.BaseItemAdapter;
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MemoryUtils;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.android.utils.Views;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.hypen.HyphenPattern;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.SlidingTabLayout;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FontExtractor;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.WebViewHepler;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.BookCSS.FontPack;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.presentation.BookmarksAdapter;
import com.foobnix.pdf.info.presentation.OutlineAdapter;
import com.foobnix.pdf.info.presentation.PageThumbnailAdapter;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.widget.ColorsDialog;
import com.foobnix.pdf.info.widget.ColorsDialog.ColorsDialogResult;
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;
import com.foobnix.pdf.info.widget.FontDialog;
import com.foobnix.pdf.info.widget.TapZoneDialog;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.ListBoxHelper;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.activity.PageImageState;
import com.foobnix.pdf.search.activity.msg.FlippingStart;
import com.foobnix.pdf.search.activity.msg.FlippingStop;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MovePageAction;
import com.foobnix.pdf.search.menu.MenuBuilderM;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSControlsView;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSService;
import com.foobnix.tts.TTSTracks;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.adapter.TabsAdapter2;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.foobnix.ui2.fragment.FavoritesFragment2;
import com.foobnix.ui2.fragment.RecentFragment2;
import com.foobnix.ui2.fragment.SearchFragment2;
import com.foobnix.ui2.fragment.UIFragment;
import com.jmedeisis.draglinearlayout.DragLinearLayout;

import org.ebookdroid.BookType;
import org.ebookdroid.common.settings.CoreSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.droids.mupdf.codec.MuPdfOutline;
import org.greenrobot.eventbus.EventBus;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class DragingDialogs {

    public final static int PREF_WIDTH = 330;
    public final static int PREF_HEIGHT = 560;

    public static final String EDIT_COLORS_PANEL = "editColorsPanel";
    static String lastSearchText = "";

    public static void sample(final FrameLayout anchor, final DocumentController controller) {
        if (controller == null) {
            return;
        }

        DragingPopup dialog = new DragingPopup(R.string.loading_failed, anchor, 300, 440) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final Activity activity = controller.getActivity();
                final View view = inflater.inflate(R.layout.dialog_tts, null, false);
                return view;
            }
        };
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {

            }
        });
        dialog.show("Sample");
    }

    public static void webView(final FrameLayout anchor, String url) {
        if (anchor == null) {
            return;
        }
        LOG.d("webView DragingDialog",url);

        DragingPopup dialog = new DragingPopup(url, anchor, 300, 440) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final WebView wv = new WebView(anchor.getContext());
                wv.getSettings().setUserAgentString(OPDS.USER_AGENT);
                wv.getSettings().setJavaScriptEnabled(true);
                wv.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);


                wv.loadUrl(url);

                wv.setFocusable(true);
                wv.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }

                });


                return wv;
            }
        };
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {

            }
        });
        dialog.show("WebView",false, true);
    }


    public static void textReplaces(final FrameLayout anchor, final DocumentController controller) {
        if (controller == null) {
            return;
        }

        DragingPopup dialog = new DragingPopup(R.string.word_replacement, anchor, 300, 440) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final Activity activity = controller.getActivity();


                List<SimpleMeta> items = AppData.get().getAllTextReplaces();

                final DragLinearLayout root = new DragLinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);

                CheckBox isEnableTextReplacement = new CheckBox(activity);
                isEnableTextReplacement.setText(R.string.enable);
                isEnableTextReplacement.setChecked(AppState.get().isEnableTextReplacement);
                isEnableTextReplacement.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isEnableTextReplacement = isChecked;
                    }
                });

                root.addView(isEnableTextReplacement);


                for (SimpleMeta it : items) {

                    String key = it.getName();
                    String value = it.getPath();

                    LOG.d("TTS-load-key", key, value);

                    LinearLayout h = new LinearLayout(activity);
                    root.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);
                    h.setWeightSum(2);
                    h.setOrientation(LinearLayout.HORIZONTAL);
                    h.setGravity(Gravity.CENTER_VERTICAL);

                    EditText from = new EditText(activity);
                    from.setTextSize(14);
                    from.setWidth(Dips.DP_120);
                    from.setText(key);
                    from.setSingleLine();


                    TextView text = new TextView(activity);
                    text.setText(">");
                    root.setPadding(Dips.DP_10, Dips.DP_0, Dips.DP_10, Dips.DP_0);


                    EditText to = new EditText(activity);
                    to.setTextSize(14);
                    to.setWidth(Dips.DP_120);
                    to.setText(value);
                    to.setSingleLine();
                    to.setHint("_");


                    ImageView img = new ImageView(activity);
                    img.setPadding(Dips.DP_4, Dips.DP_4, Dips.DP_4, Dips.DP_4);
                    img.setMaxWidth(Dips.DP_20);
                    img.setMaxHeight(Dips.DP_20);

                    img.setImageResource(R.drawable.glyphicons_599_menu_close);
                    TintUtil.setTintImageWithAlpha(img);

                    img.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            root.removeView(h);
                        }
                    });


                    ImageView move = new ImageView(activity);
                    move.setPadding(Dips.DP_4, Dips.DP_4, Dips.DP_4, Dips.DP_4);
                    move.setMaxWidth(Dips.DP_20);
                    move.setMaxHeight(Dips.DP_20);

                    move.setImageResource(R.drawable.glyphicons_600_menu);
                    TintUtil.setTintImageWithAlpha(move);


                    from.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                    text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
                    to.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                    img.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
                    move.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));

                    h.addView(from);
                    h.addView(text);
                    h.addView(to);
                    h.addView(img);
                    h.addView(move);

                    root.addView(h);
                    root.setViewDraggable(h, move);
                }

                TextView add = new TextView(activity, null, R.style.textLink);
                add.setText(activity.getString(R.string.add));
                add.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);

                TxtUtils.underlineTextView(add);
                add.setTag(false);
                add.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        LinearLayout h = new LinearLayout(activity);
                        h.setOrientation(LinearLayout.HORIZONTAL);

                        EditText from = new EditText(activity);
                        from.setWidth(Dips.DP_120);
                        from.setSingleLine();
                        from.requestFocus();


                        TextView text = new TextView(activity);
                        text.setText("->");
                        root.setPadding(Dips.DP_10, Dips.DP_0, Dips.DP_10, Dips.DP_0);


                        EditText to = new EditText(activity);
                        to.setWidth(Dips.DP_120);
                        to.setSingleLine();
                        to.setHint("_");

                        h.addView(from);
                        h.addView(text);
                        h.addView(to);
                        root.addView(h, root.getChildCount() - 2);

                        if (add.getTag().equals(true) && items.size() > 0) {
                            SimpleMeta last = items.get(items.size() - 1);
                            from.setText(last.name);
                            to.setText(last.path);
                        }

                        AppState.get().isEnableTextReplacement = true;
                        isEnableTextReplacement.setChecked(true);
                        add.setTag(false);
                    }
                });
                add.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        add.setTag(true);
                        return add.performClick();
                    }
                });

                Button save = new Button(controller.getActivity());
                save.setText(R.string.save);
                save.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean hasErrors = false;
                        List<SimpleMeta> items = new ArrayList<>();

                        for (int i = 0; i < root.getChildCount(); i++) {
                            final View childAt = root.getChildAt(i);
                            if (childAt instanceof LinearLayout) {
                                final LinearLayout line = (LinearLayout) childAt;
                                if (line.getOrientation() == LinearLayout.VERTICAL) {
                                    continue;
                                }
                                EditText childFrom = (EditText) line.getChildAt(0);
                                String from = childFrom.getText().toString();
                                String to = ((EditText) line.getChildAt(2)).getText().toString();
                                from = from.replace(" ", "").trim();


                                LOG.d("TTS-add", from, to);

                                try {
                                    if (from.startsWith("*")) {
                                        try {
                                            Pattern.compile(from.substring(1));
                                            //res.put(from, to);
                                            items.add(new SimpleMeta(from, to, i));
                                        } catch (Exception e) {
                                            LOG.d("TTS-incorrect value", from, to);
                                            hasErrors = true;
                                            childFrom.requestFocus();
                                            Toast.makeText(activity, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                                        }
                                    } else if (TxtUtils.isNotEmpty(from)) {
                                        items.add(new SimpleMeta(from, to, i));
                                    }
                                } catch (Exception e) {
                                    LOG.e(e);
                                }


                            }
                        }
                        if (!hasErrors) {
                            Keyboards.close(activity);
                            AppData.get().saveAllTextReplaces(items);
                            //Toast.makeText(activity, R.string.success, Toast.LENGTH_SHORT).show();
                            realod();
                        }
                    }
                });

                root.addView(add);
                root.addView(save);


                ScrollView scroll = new ScrollView(activity);
                scroll.setOverScrollMode(ScrollView.OVER_SCROLL_IF_CONTENT_SCROLLS);
                scroll.setVerticalScrollBarEnabled(true);
                scroll.addView(root);

                return scroll;
            }
        };
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                List<SimpleMeta> items = AppData.get().getAllTextReplaces();
                long hashNew = AppData.get().calculateHash(items);

                if (AppState.get().textReplacementHash != hashNew) {
                    AppState.get().textReplacementHash = hashNew;
                    controller.restartActivity();
                }
            }
        });
        dialog.show("Sample");
    }

    public static void customCropDialog(final FrameLayout anchor, final DocumentController controller, final Runnable onCropChange) {
        if (controller == null) {
            return;
        }

        DragingPopup dialog = new DragingPopup(R.string.crop_white_borders, anchor, 360, 400) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final Activity activity = controller.getActivity();
                final View inflate = inflater.inflate(R.layout.dialog_custom_crop, null, false);

                // Margins
                final CustomSeek marginTop = inflate.findViewById(R.id.marginTop);
                final CustomSeek marginBottom = inflate.findViewById(R.id.marginBottom);

                int max = 30;
                marginTop.init(0, max, AppState.get().cropTop, "%");
                marginTop.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().cropTop = result;
                        onCropChange.run();
                        if (AppSP.get().isCropSymetry) {
                            marginBottom.reset(result);
                        }
                        return false;
                    }
                });

                marginBottom.init(0, max, AppState.get().cropBottom, "%");
                marginBottom.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().cropBottom = result;
                        onCropChange.run();
                        if (AppSP.get().isCropSymetry) {
                            marginTop.reset(result);
                        }
                        return false;
                    }
                });

                final CustomSeek marginLeft = inflate.findViewById(R.id.marginLeft);
                final CustomSeek marginRight = inflate.findViewById(R.id.marginRight);

                marginLeft.init(0, max, AppState.get().cropLeft, "%");
                marginLeft.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().cropLeft = result;
                        onCropChange.run();
                        if (AppSP.get().isCropSymetry) {
                            marginRight.reset(result);
                        }
                        return false;
                    }
                });

                marginRight.init(0, max, AppState.get().cropRigth, "%");
                marginRight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().cropRigth = result;
                        onCropChange.run();
                        if (AppSP.get().isCropSymetry) {
                            marginLeft.reset(result);
                        }
                        return false;
                    }
                });

                CheckBox isCropSymetry = inflate.findViewById(R.id.isCropSymetry);
                isCropSymetry.setChecked(AppSP.get().isCropSymetry);
                isCropSymetry.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppSP.get().isCropSymetry = isChecked;
                        if (isChecked) {
                            marginBottom.reset(marginTop.getCurrentValue());
                            marginRight.reset(marginLeft.getCurrentValue());
                        }
                    }
                });

                final CheckBox isEnableCrop = inflate.findViewById(R.id.isEnableCrop);
                isEnableCrop.setChecked(AppSP.get().isCrop);
                isEnableCrop.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isEnableCrop) {
                        AppSP.get().isCrop = isEnableCrop;
                        onCropChange.run();
                    }
                });

                final IntegerResponse updateCrop = new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        marginTop.reset(result);
                        marginBottom.reset(result);
                        marginLeft.reset(result);
                        marginRight.reset(result);

                        AppState.get().cropTop = result;
                        AppState.get().cropBottom = result;
                        AppState.get().cropLeft = result;
                        AppState.get().cropRigth = result;

                        AppSP.get().isCrop = true;
                        isEnableCrop.setChecked(true);
                        onCropChange.run();

                        return false;
                    }
                };

                inflate.findViewById(R.id.cropAuto).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        updateCrop.onResultRecive(0);
                    }
                });
                inflate.findViewById(R.id.v5).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        updateCrop.onResultRecive(5);
                    }
                });
                inflate.findViewById(R.id.v10).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        updateCrop.onResultRecive(10);
                    }
                });
                inflate.findViewById(R.id.v15).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        updateCrop.onResultRecive(15);

                    }
                });

                return inflate;
            }
        };
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {

            }
        });
        dialog.show("Sample");

    }

    public static void contrastAndBrigtness(final FrameLayout anchor, final DocumentController controller, final Runnable onRealod, final Runnable onRestart) {
        if (controller == null) {
            return;
        }

        DragingPopup dialog = new DragingPopup(R.string.contrast_and_brightness, anchor, 300, 280) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final Activity c = controller.getActivity();
                return Dialogs.getBCView(c, onRealod);
            }
        };
        if (onRestart != null) {
            dialog.setOnCloseListener(onRestart);
        }
        dialog.show("contrastAndBrigtness");

    }

    public static void onMoveDialog(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {
        if (controller == null) {
            return;
        }

        DragingPopup dialog = new DragingPopup(controller.getString(R.string.page_position), anchor, 280, 250) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final Activity activity = controller.getActivity();
                final View view = inflater.inflate(R.layout.dialog_move_manually, null, false);
                ImageView onUp = view.findViewById(R.id.onUp);
                ImageView onDonw = view.findViewById(R.id.onDown);
                ImageView onLeft = view.findViewById(R.id.onLeft);
                ImageView onRight = view.findViewById(R.id.onRight);
                ImageView onPlus = view.findViewById(R.id.onPlus);
                ImageView onMinus = view.findViewById(R.id.onMinus);
                ImageView onCenter = view.findViewById(R.id.onCenter);
                final ImageView onCrop = view.findViewById(R.id.onCrop);

                if (AppState.get().isDayNotInvert) {
                    TintUtil.setTintImageWithAlpha(onUp);
                    TintUtil.setTintImageWithAlpha(onDonw);
                    TintUtil.setTintImageWithAlpha(onLeft);
                    TintUtil.setTintImageWithAlpha(onRight);
                    TintUtil.setTintImageWithAlpha(onPlus);
                    TintUtil.setTintImageWithAlpha(onMinus);
                    TintUtil.setTintImageWithAlpha(onCenter);
                    TintUtil.setTintImageWithAlpha(onCrop);
                } else {
                    TintUtil.setTintImageWithAlpha(onUp, Color.WHITE);
                    TintUtil.setTintImageWithAlpha(onDonw, Color.WHITE);
                    TintUtil.setTintImageWithAlpha(onLeft, Color.WHITE);
                    TintUtil.setTintImageWithAlpha(onRight, Color.WHITE);
                    TintUtil.setTintImageWithAlpha(onPlus, Color.WHITE);
                    TintUtil.setTintImageWithAlpha(onMinus, Color.WHITE);
                    TintUtil.setTintImageWithAlpha(onCenter, Color.WHITE);
                    TintUtil.setTintImageWithAlpha(onCrop, Color.WHITE);
                }

                onCrop.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppSP.get().isCrop = !AppSP.get().isCrop;
                        SettingsManager.getBookSettings().updateFromAppState();
                        updateUIRefresh.run();

                        if (AppSP.get().isCrop) {
                            TintUtil.setTintImageWithAlpha(onCrop, TintUtil.COLOR_ORANGE);
                        } else {
                            TintUtil.setTintImageWithAlpha(onCrop, AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);
                        }
                    }
                });

                if (AppSP.get().isCrop) {
                    TintUtil.setTintImageWithAlpha(onCrop, TintUtil.COLOR_ORANGE);
                } else {
                    TintUtil.setTintImageWithAlpha(onCrop, AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);
                }

                OnClickListener listner = new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        int id = v.getId();
                        int aciton = -1;

                        if (id == R.id.onUp) {
                            aciton = MovePageAction.UP;
                        } else if (id == R.id.onDown) {
                            aciton = MovePageAction.DOWN;
                        } else if (id == R.id.onLeft) {
                            aciton = MovePageAction.LEFT;
                        } else if (id == R.id.onRight) {
                            aciton = MovePageAction.RIGHT;
                        } else if (id == R.id.onPlus) {
                            aciton = MovePageAction.ZOOM_PLUS;
                        } else if (id == R.id.onMinus) {
                            aciton = MovePageAction.ZOOM_MINUS;
                        } else if (id == R.id.onCenter) {
                            aciton = MovePageAction.CENTER;
                        }
                        EventBus.getDefault().post(new MovePageAction(aciton, controller.getCurentPage()));

                    }
                };

                onUp.setOnClickListener(listner);
                onDonw.setOnClickListener(listner);
                onLeft.setOnClickListener(listner);
                onRight.setOnClickListener(listner);
                onPlus.setOnClickListener(listner);
                onMinus.setOnClickListener(listner);
                onCenter.setOnClickListener(listner);

                return view;
            }
        };
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {

            }

        });
        dialog.show("MovePage");

    }

    public static void textToSpeachDialog(final FrameLayout anchor, final DocumentController controller) {
        if (controller == null) {
            return;
        }
        textToSpeachDialog(anchor, controller, "");
    }

    public static void textToSpeachDialog(final FrameLayout anchor, final DocumentController controller, final String textToRead) {
        if (controller == null) {
            return;
        }

        if (TTSEngine.get().hasNoEngines()) {
            Urls.openTTS(controller.getActivity());
            return;
        }

        DragingPopup dialog = new DragingPopup(R.string.text_to_speech, anchor, 300, 480) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {

                final Activity activity = controller.getActivity();
                final View view = inflater.inflate(R.layout.dialog_tts, null, false);

                final TextView textBGwarning = view.findViewById(R.id.textBGwarning);

                textBGwarning.setVisibility(View.GONE);
                if (Build.VERSION.SDK_INT >= 28) {
                    ActivityManager activityManager = (ActivityManager)
                            controller.getActivity().getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager.isBackgroundRestricted()) {

                        textBGwarning.setVisibility(View.VISIBLE);
                        TxtUtils.underlineTextView(textBGwarning);
                        textBGwarning.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", Apps.getPackageName(controller.getActivity()), null));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    controller.getActivity().startActivity(intent);
                                    closeDialog();
                                } catch (Exception e) {
                                    Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                                    LOG.e(e);
                                }
                            }
                        });
                    }
                }

                final TextView ttsPage = view.findViewById(R.id.ttsPage);

                final TextView textEngine = view.findViewById(R.id.ttsEngine);

                final TextView timerTime = view.findViewById(R.id.timerTime);

                final TextView timerStart = view.findViewById(R.id.timerStart);
                final TextView ttsPlayMusicFile = view.findViewById(R.id.ttsPlayMusicFile);

                view.findViewById(R.id.onHelp).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        MyPopupMenu menu = new MyPopupMenu(v);
                        for (final String key : AppState.TTS_ENGINES.keySet()) {
                            menu.getMenu().add(key).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    String value = AppState.TTS_ENGINES.get(key);
                                    String play = value.replace("https://play.google.com/store/apps/details?", "market://details?");
                                    try {
                                        Urls.open(v.getContext(), play);
                                    } catch (Exception e) {
                                        Urls.open(v.getContext(), value);
                                    }
                                    return false;
                                }
                            });
                        }
                        menu.show();

                    }
                });

                final TTSControlsView tts = view.findViewById(R.id.ttsActive);
                tts.setDC(controller);

                TextView ttsSkeakToFile = view.findViewById(R.id.ttsSkeakToFile);

                final TextView ttsLang = view.findViewById(R.id.ttsLang);
                // TxtUtils.underlineTextView(ttsLang);

                final TextView ttsPauseDuration = view.findViewById(R.id.ttsPauseDuration);
                if (AppState.get().ttsPauseDuration > 1000) {
                    ttsPauseDuration.setText("" + AppState.get().ttsPauseDuration / 1000 + " sec");
                } else {
                    ttsPauseDuration.setText("" + AppState.get().ttsPauseDuration + " ms");
                }
                TxtUtils.underlineTextView(ttsPauseDuration);

                ttsPauseDuration.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        for (int i = 0; i <= 500; i += (i <= 100 ? 10 : 25)) {
                            final int j = i;
                            popupMenu.getMenu().add(i + " ms").setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    TTSEngine.get().stop();
                                    AppState.get().ttsPauseDuration = j;
                                    ttsPauseDuration.setText("" + AppState.get().ttsPauseDuration + " ms");
                                    TxtUtils.underlineTextView(ttsPauseDuration);
                                    return false;
                                }
                            });
                        }

                        for (int i = 1; i <= 10; i += 1) {
                            final int j = i;
                            popupMenu.getMenu().add(i + " sec").setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    TTSEngine.get().stop();
                                    AppState.get().ttsPauseDuration = j * 1000;
                                    ttsPauseDuration.setText("" + j + " sec");
                                    TxtUtils.underlineTextView(ttsPauseDuration);
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });

                ttsLang.setVisibility(TxtUtils.visibleIf(Build.VERSION.SDK_INT >= 21));

                timerTime.setText(AppState.get().ttsTimer + " " + controller.getString(R.string.minutes).toLowerCase(Locale.US));
                timerTime.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                        int[] items = {1, 15, 30, 45, 60, 90, 120, 240, 360};
                        for (final int i : items) {
                            popupMenu.getMenu().add("" + i).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().ttsTimer = i;
                                    timerTime.setText(AppState.get().ttsTimer + " " + controller.getString(R.string.minutes).toLowerCase(Locale.US));
                                    TxtUtils.underlineTextView(timerTime);
                                    TTSService.updateTimer();
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();
                    }
                });

                timerStart.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (TempHolder.get().timerFinishTime == 0) {
                            TempHolder.get().timerFinishTime = System.currentTimeMillis() + (long) AppState.get().ttsTimer * 60 * 1000;
                        } else {
                            TempHolder.get().timerFinishTime = 0;
                        }

                        timerStart.setText(TempHolder.get().timerFinishTime == 0 ? R.string.start : R.string.cancel);
                        ttsPage.setText(TempHolder.get().timerFinishTime == 0 ? "" : controller.getString(R.string.reading_will_be_stopped) + " " + DateFormat.getTimeFormat(activity).format(TempHolder.get().timerFinishTime));
                        ttsPage.setVisibility(TxtUtils.visibleIf(TempHolder.get().timerFinishTime > 0));

                        TxtUtils.underlineTextView(timerStart);

                    }
                });

                ttsPage.setText(TempHolder.get().timerFinishTime == 0 ? "" : controller.getString(R.string.reading_will_be_stopped) + " " + DateFormat.getTimeFormat(activity).format(TempHolder.get().timerFinishTime));
                timerStart.setText(TempHolder.get().timerFinishTime == 0 ? R.string.start : R.string.cancel);
                ttsPage.setVisibility(TxtUtils.visibleIf(TempHolder.get().timerFinishTime > 0));

                ttsPage.setVisibility(View.GONE);
                timerStart.setVisibility(View.GONE);

                TTSEngine.get().getTTS(new OnInitListener() {

                    @Override
                    public void onInit(int status) {
                        textEngine.setText(TTSEngine.get().getCurrentEngineName());
                        ttsLang.setText(TTSEngine.get().getCurrentLang());
                        TxtUtils.bold(ttsLang);

                    }
                });

                controller.runTimer(1000, new Runnable() {

                    @Override
                    public void run() {
                        textEngine.setText(TTSEngine.get().getCurrentEngineName());
                        ttsLang.setText(TTSEngine.get().getCurrentLang());
                        TxtUtils.bold(ttsLang);

                    }
                });

                textEngine.setText(TTSEngine.get().getCurrentEngineName());
                ttsLang.setText(TTSEngine.get().getCurrentLang());
                TxtUtils.bold(ttsLang);

                View ttsSettings = view.findViewById(R.id.ttsSettings);
                textEngine.setOnClickListener((v) -> ttsSettings.performClick());
                ttsLang.setOnClickListener((v) -> ttsSettings.performClick());

                TxtUtils.underlineTextView(ttsSettings).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            TTSEngine.get().stop();
                            TTSEngine.get().stopDestroy();

                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setAction("com.android.settings.TTS_SETTINGS");
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                            LOG.e(e);
                        }
                    }
                });

                final CustomSeek seekBarSpeed = view.findViewById(R.id.seekBarSpeed);
                seekBarSpeed.init(0, 600, (int) (AppState.get().ttsSpeed * 100));
                seekBarSpeed.setStep(10);
                seekBarSpeed.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        TTSEngine.get().stop();
                        AppState.get().ttsSpeed = (float) result / 100;
                        LOG.d("TTS-ttsSpeed 2", AppState.get().ttsSpeed);
                        return false;
                    }
                });

                final MyPopupMenu menu = new MyPopupMenu(seekBarSpeed.getContext(), seekBarSpeed);
                List<Float> values = Arrays.asList(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f, 5f, 6f);
                for (final float i : values) {
                    menu.getMenu().add(String.format("%s x", i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            seekBarSpeed.reset((int) (i * 100));
                            seekBarSpeed.sendProgressChanged();
                            return false;
                        }
                    });
                }

                seekBarSpeed.addMyPopupMenu(menu);
                TxtUtils.setLinkTextColor(seekBarSpeed.getTitleText());

                final CustomSeek seekBarPitch = view.findViewById(R.id.seekBarPitch);
                seekBarPitch.init(0, 200, (int) (AppState.get().ttsPitch * 100));
                seekBarPitch.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        TTSEngine.get().stop();
                        AppState.get().ttsPitch = (float) result / 100;
                        return false;
                    }
                });

                final AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

                final CustomSeek seekVolume = view.findViewById(R.id.seekVolume);
                seekVolume.init(0, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                seekVolume.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, result, 0);
                        return false;
                    }
                });

                final EditText ttsSentecesDivs = view.findViewById(R.id.ttsSentecesDivs);

                TxtUtils.underlineTextView(view.findViewById(R.id.restore_defaults)).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        AlertDialogs.showOkDialog(controller.getActivity(), controller.getString(R.string.restore_defaults_full), new Runnable() {

                            @Override
                            public void run() {
                                seekBarPitch.reset(100);
                                seekBarSpeed.reset(100);
                                seekVolume.reset(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2);

                                AppState.get().ttsPitch = (float) 1.0;
                                AppState.get().ttsSpeed = (float) 1.0;

                                TTSEngine.get().shutdown();
                                TTSEngine.get().getTTS();

                                textEngine.setText(TTSEngine.get().getCurrentEngineName());
                                ttsLang.setText(TTSEngine.get().getCurrentLang());
                                // TxtUtils.underlineTextView(textEngine);

                                AppState.get().ttsSentecesDivs = AppState.TTS_PUNCUATIONS;
                                ttsSentecesDivs.setText(AppState.get().ttsSentecesDivs);
                            }
                        });

                    }
                });
                //

                CheckBox stopReadingOnCall = view.findViewById(R.id.stopReadingOnCall);
                stopReadingOnCall.setChecked(AppState.get().stopReadingOnCall);
                stopReadingOnCall.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().stopReadingOnCall = isChecked;
                    }
                });

                // read by sentences
                ttsSentecesDivs.setText(AppState.get().ttsSentecesDivs);
                ttsSentecesDivs.addTextChangedListener(new SmallTextWatcher() {

                    @Override
                    public void onTextChanged(String text) {
                        AppState.get().ttsSentecesDivs = text;
                        TTSEngine.get().stop();
                    }
                });
                ttsSentecesDivs.setEnabled(AppState.get().ttsReadBySentences);

                CheckBox ttsReadBySentences = view.findViewById(R.id.ttsReadBySentences);
                ttsReadBySentences.setChecked(AppState.get().ttsReadBySentences);
                ttsReadBySentences.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().ttsReadBySentences = isChecked;
                        ttsSentecesDivs.setEnabled(AppState.get().ttsReadBySentences);
                        TTSEngine.get().stop();
                    }
                });

                CheckBox isEnalbeTTSReplacements = view.findViewById(R.id.isEnalbeTTSReplacements);
                isEnalbeTTSReplacements.setChecked(AppState.get().isEnalbeTTSReplacements);
                isEnalbeTTSReplacements.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isEnalbeTTSReplacements = isChecked;
                        TTSEngine.get().stop();
                    }
                });

                TxtUtils.underlineTextView(view.findViewById(R.id.replaces)).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TTSEngine.get().stop();
                        Dialogs.replaceTTSDialog(activity);

                    }
                });
                CheckBox allowOtherMusic = view.findViewById(R.id.allowOtherMusic);
                allowOtherMusic.setChecked(AppState.get().allowOtherMusic);
                allowOtherMusic.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().allowOtherMusic = isChecked;
                    }
                });

                TxtUtils.underlineTextView(view.findViewById(R.id.showDebug)).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialogs.showTTSDebug(controller);

                    }
                });

                CheckBox isFastBookmarkByTTS = view.findViewById(R.id.isFastBookmarkByTTS);
                isFastBookmarkByTTS.setChecked(AppState.get().isFastBookmarkByTTS);
                isFastBookmarkByTTS.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isFastBookmarkByTTS = isChecked;
                    }
                });

                CheckBox ttsTunnOnLastWord = view.findViewById(R.id.ttsTunnOnLastWord);
                ttsTunnOnLastWord.setChecked(AppState.get().ttsTunnOnLastWord);
                ttsTunnOnLastWord.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().ttsTunnOnLastWord = isChecked;
                        TTSEngine.get().stop();
                    }
                });

                // TTS Play music File
                ttsPlayMusicFile.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        ChooserDialogFragment.chooseFileorFolder((FragmentActivity) controller.getActivity(), controller.getString(R.string.open_file)).setOnSelectListener(new ResultResponse2<String, Dialog>() {

                            @Override
                            public boolean onResultRecive(String result1, Dialog result2) {
                                LOG.d("onResultRecive", result1);
                                if (new File(result1).isDirectory()) {
                                    LOG.d("onResultRecive Directory", result1);
                                    String firstMp3Infoder = TTSTracks.getFirstMp3Infoder(result1);
                                    if (firstMp3Infoder != null) {
                                        result1 = firstMp3Infoder;
                                    }
                                    LOG.d("onResultRecive firstMp3Infoder", result1);
                                }
                                if (ExtUtils.isAudioContent(result1)) {

                                    BookCSS.get().mp3BookPath(result1);
                                    AppState.get().mp3seek = 0;

                                    tts.udateButtons();

                                    TTSEngine.get().mp3Destroy();
                                    TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), "", controller.getBookWidth(), controller.getBookHeight(), BookCSS.get().fontSizeSp, controller.getTitle());
                                } else {
                                    Toast.makeText(controller.getActivity(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                                }
                                result2.dismiss();
                                return false;
                            }
                        });

                    }
                });
                TxtUtils.underlineTextView(ttsPlayMusicFile);

                ttsSkeakToFile.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(controller.getActivity());
                        dialog.setTitle(R.string.speak_into_file_wav);

                        View inflate = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_tts_wav, null, false);
                        final TextView ttsSpeakPath = inflate.findViewById(R.id.ttsSpeakPath);
                        final TextView progressText = inflate.findViewById(R.id.progressText);
                        final MyProgressBar MyProgressBar1 = inflate.findViewById(R.id.MyProgressBarTTS);
                        final Button start = inflate.findViewById(R.id.start);
                        final Button stop = inflate.findViewById(R.id.stop);
                        final Button delete = inflate.findViewById(R.id.delete);
                        final CheckBox isConvertToMp3 = inflate.findViewById(R.id.isConvertToMp3);
                        isConvertToMp3.setChecked(AppState.get().isConvertToMp3);
                        isConvertToMp3.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isConvertToMp3 = isChecked);

                        final EditText from = inflate.findViewById(R.id.from);
                        final EditText to = inflate.findViewById(R.id.to);

                        from.setText("" + 1);
                        to.setText("" + controller.getPageCount());

                        MyProgressBar1.setVisibility(View.GONE);
                        progressText.setText("");

                        ttsSpeakPath.setText(Html.fromHtml("<u>" + BookCSS.get().ttsSpeakPath + "/<b>" + controller.getCurrentBook().getName() + "</b></u>"));
                        ttsSpeakPath.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                ChooserDialogFragment.chooseFolder((FragmentActivity) controller.getActivity(), BookCSS.get().ttsSpeakPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                                    @Override
                                    public boolean onResultRecive(String nPath, Dialog dialog) {
                                        BookCSS.get().ttsSpeakPath = nPath;
                                        ttsSpeakPath.setText(Html.fromHtml("<u>" + BookCSS.get().ttsSpeakPath + "/<b>" + controller.getCurrentBook().getName() + "</b></u>"));
                                        dialog.dismiss();
                                        return false;
                                    }
                                });

                            }
                        });

                        delete.setOnClickListener(v1 -> {
                            if (TempHolder.isRecordTTS) {
                                Toast.makeText(controller.getActivity(), R.string.please_wait, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            File dirFolder = new File(BookCSS.get().ttsSpeakPath, "TTS_" + controller.getCurrentBook().getName());
                            boolean res = CacheZipUtils.removeFiles(dirFolder.listFiles(pathname -> pathname.getName().endsWith(TTSEngine.WAV) || pathname.getName().endsWith(TTSEngine.MP3)));
                            if (res) {
                                Toast.makeText(controller.getActivity(), R.string.success, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(controller.getActivity(), R.string.fail, Toast.LENGTH_LONG).show();
                            }

                        });

                        final ResultResponse<String> info = new ResultResponse<String>() {
                            @Override
                            public boolean onResultRecive(final String result) {
                                controller.getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        progressText.setText(result);
                                        if (result.equals(controller.getString(R.string.success))) {
                                            MyProgressBar1.setVisibility(View.GONE);
                                        }

                                    }
                                });
                                return false;
                            }
                        };

                        stop.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                TempHolder.isRecordTTS = false;
                                MyProgressBar1.setVisibility(View.GONE);
                            }
                        });

                        start.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                boolean hasErorrs = false;
                                try {
                                    TempHolder.isRecordFrom = Integer.parseInt(from.getText().toString());
                                    if (TempHolder.isRecordFrom <= 0) {
                                        hasErorrs = true;
                                    }
                                } catch (Exception e) {
                                    hasErorrs = true;
                                    from.requestFocus();
                                }

                                try {
                                    TempHolder.isRecordTo = Integer.parseInt(to.getText().toString());

                                    if (TempHolder.isRecordTo > controller.getPageCount()) {
                                        hasErorrs = true;
                                        to.requestFocus();
                                    }

                                    if (TempHolder.isRecordFrom > TempHolder.isRecordTo) {
                                        hasErorrs = true;
                                        from.requestFocus();
                                    }
                                } catch (Exception e) {
                                    hasErorrs = true;
                                    to.requestFocus();
                                }

                                if (hasErorrs) {
                                    Toast.makeText(controller.getActivity(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                                }

                                if (!hasErorrs && !TempHolder.isRecordTTS) {
                                    TempHolder.isRecordTTS = true;
                                    MyProgressBar1.setVisibility(View.VISIBLE);
                                    TTSEngine.get().speakToFile(controller, info, TempHolder.isRecordFrom, TempHolder.isRecordTo);
                                }
                            }
                        });

                        dialog.setView(inflate);

                        dialog.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TempHolder.isRecordTTS = false;
                            }
                        });
                        AlertDialog create = dialog.create();
                        create.setOnDismissListener(new OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                TempHolder.isRecordTTS = false;
                            }
                        });

                        create.show();
                    }
                });

                TxtUtils.underlineTextView(timerStart);
                TxtUtils.underlineTextView(timerTime);
                TxtUtils.underlineTextView(ttsSkeakToFile);

                return view;
            }
        };
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                controller.stopTimer();

            }
        });
        dialog.show("TTS", false, true);

    }

    public static void searchMenu(final FrameLayout anchor, final DocumentController controller, String text) {
        if (controller == null) {
            return;
        }

        DragingPopup dialog = new DragingPopup(R.string.search, anchor, 250, 150) {
            @Override
            public View getContentView(LayoutInflater inflater) {
                final View view = inflater.inflate(R.layout.search_dialog, null, false);

                final EditText searchEdit = view.findViewById(R.id.edit1);
                //searchEdit.setText(text);
                if (TxtUtils.isNotEmpty(text)) {
                    searchEdit.setText(text);
                } else {
                    searchEdit.setText(lastSearchText);
                }

                final MyProgressBar MyProgressBar = view.findViewById(R.id.progressBarSearch);
                final TextView searchingMsg = view.findViewById(R.id.searching);
                final GridView gridView = view.findViewById(R.id.grid1);
                gridView.setColumnWidth(Dips.dpToPx(80));

                final BaseItemLayoutAdapter<Integer> adapter = new BaseItemLayoutAdapter<Integer>(anchor.getContext(), android.R.layout.simple_spinner_dropdown_item) {

                    @Override
                    public void populateView(View inflate, int arg1, Integer page) {
                        final TextView text = Views.text(inflate, android.R.id.text1, TxtUtils.deltaPage(page + 1, 0));
                        text.setGravity(Gravity.CENTER);
                        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    }

                    @Override
                    public long getItemId(int position) {
                        return getItem(position) + 1;
                    }

                };

                gridView.setAdapter(adapter);
                gridView.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        controller.onGoToPage((int) id);

                    }
                });

                ImageView onClear = view.findViewById(R.id.imageClear);
                onClear.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        boolean isRun = TempHolder.isSeaching;
                        TempHolder.isSeaching = false;
                        if (!isRun) {
                            lastSearchText = "";
                            searchEdit.setText("");
                            controller.clearSelectedText();
                            searchingMsg.setVisibility(View.GONE);
                            adapter.getItems().clear();
                            adapter.notifyDataSetChanged();
                        }

                    }
                });

                final View onSearch = view.findViewById(R.id.onSearch);
                TintUtil.setTintBg(onSearch);

                EditTextHelper.enableKeyboardSearch(searchEdit, new Runnable() {

                    @Override
                    public void run() {
                        onSearch.performClick();
                    }
                });

                final String searchingString = anchor.getContext().getString(R.string.searching_please_wait_);
                final int count = controller.getPageCount();

                final Handler hMessage = new Handler() {
                    @Override
                    public void handleMessage(android.os.Message msg) {
                        int pageNumber = msg.what;
                        LOG.d("Receive page", pageNumber);
                        MyProgressBar.setVisibility(View.GONE);
                        gridView.setVisibility(View.VISIBLE);

                        if (pageNumber < -1) {
                            searchingMsg.setVisibility(View.VISIBLE);
                            searchingMsg.setText(searchingString + " " + Math.abs(pageNumber) + "/" + count);
                            return;
                        }

                        if (pageNumber == -1) {
                            if (adapter.getItems().size() == 0) {
                                searchingMsg.setVisibility(View.VISIBLE);
                                searchingMsg.setText(R.string.msg_no_text_found);
                            } else {
                                searchingMsg.setVisibility(View.GONE);
                            }
                        }

                        if (pageNumber == Integer.MAX_VALUE) {
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        if (pageNumber >= 0) {
                            pageNumber = PageUrl.realToFake(pageNumber);
                            searchingMsg.setVisibility(View.VISIBLE);
                            adapter.getItems().add(pageNumber);
                            adapter.notifyDataSetChanged();
                        }

                    }

                };

                onSearch.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (TempHolder.isSeaching) {
                            TempHolder.isSeaching = false;
                            return;
                        }
                        String searchString = searchEdit.getText().toString().trim();
                        if (searchString.length() < 2) {
                            Toast.makeText(controller.getActivity(), R.string.please_enter_more_characters_to_search, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        lastSearchText = searchString;
                        TempHolder.isSeaching = true;

                        searchingMsg.setText(R.string.searching_please_wait_);
                        searchingMsg.setVisibility(View.VISIBLE);

                        MyProgressBar.setVisibility(View.VISIBLE);
                        gridView.setVisibility(View.GONE);
                        adapter.getItems().clear();
                        adapter.notifyDataSetChanged();

                        Keyboards.close(searchEdit);
                        hMessage.removeCallbacksAndMessages(null);
                        controller.doSearch(searchString, new ResultResponse<Integer>() {
                            @Override
                            public boolean onResultRecive(final Integer pageNumber) {
                                hMessage.sendEmptyMessage(pageNumber);
                                return false;
                            }
                        });
                    }
                });

                return view;
            }
        };
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                TempHolder.isSeaching = false;

            }
        });
        dialog.show("searchMenu");

    }

    @SuppressLint("NewApi")
    public static DragingPopup showFootNotes(final FrameLayout anchor, final DocumentController controller, final Runnable updateLinks) {
        return new DragingPopup(R.string.foot_notes, anchor, 280, 300) {
            @Override
            public View getContentView(LayoutInflater inflater) {
                View inflate = inflater.inflate(R.layout.dialog_footer_notes, null, false);
                final int page = TempHolder.get().linkPage + 1;
                String selectedText = AppState.get().selectedText;

                final int currentPage = controller.getCurentPageFirst1();

                TextView goTo = inflate.findViewById(R.id.goTo);
                TextView footerNumber = inflate.findViewById(R.id.footerNumber);
                TextView goBack = inflate.findViewById(R.id.goBack);
                TextView text = inflate.findViewById(R.id.text);
                float size = Math.max(14, BookCSS.get().fontSizeSp * 0.85f);
                text.setTextSize(size);
                LOG.d("FONT-SIZE", size, selectedText);

                footerNumber.setText(TxtUtils.getFooterNoteNumber(selectedText));

                goTo.setText(controller.getString(R.string.go_to_page_dialog) + " " + page);
                String currentChapterFile = controller.getCurrentChapterFile();

                String footNote = controller.getFootNote(selectedText, currentChapterFile);
                text.setText(footNote);
                if (page == -1 || page == 0 || AppSP.get().isDouble) {
                    goTo.setVisibility(View.GONE);
                    goBack.setVisibility(View.GONE);
                }

                final Integer offsetY = Integer.valueOf((int) controller.getOffsetY());

                goTo.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (!controller.getLinkHistory().contains(offsetY)) {
                            controller.getLinkHistory().add(offsetY);
                        }
                        controller.onGoToPage(page);
                        if (updateLinks != null) {
                            updateLinks.run();
                        }
                    }
                });

                goBack.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.onGoToPage(currentPage);
                        controller.getLinkHistory().remove(offsetY);
                        if (updateLinks != null) {
                            updateLinks.run();
                        }
                    }
                });

                TxtUtils.underlineTextView(goTo);
                TxtUtils.underlineTextView(goBack);

                return inflate;
            }
        }.show("footNotes", true).setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                controller.clearSelectedText();
            }
        });
    }

    public static DragingPopup selectTextMenu(final FrameLayout anchor, final DocumentController controller, boolean withAnnotation1, final Runnable reloadUI) {

        final boolean withAnnotation = AppsConfig.isPDF_DRAW_ENABLE() && withAnnotation1;

        // try {
        // int number = Integer.parseInt(AppState.get().selectedText);
        // Dialogs.showDeltaPage(anchor, controller, number, reloadUI);
        // return null;
        // } catch (Exception e) {
        // }

        return new DragingPopup(R.string.text, anchor, 300, 400) {
            @Override
            public View getContentView(LayoutInflater inflater) {
                final View view = inflater.inflate(R.layout.dialog_selected_text, null, false);
                final LinearLayout linearLayoutColor = view.findViewById(R.id.colorsLine);
                linearLayoutColor.removeAllViews();
                List<String> colors = new ArrayList<String>(AppState.COLORS);
                colors.remove(0);
                colors.remove(0);

                final ImageView underLine = view.findViewById(R.id.onUnderline);
                final ImageView strike = view.findViewById(R.id.onStrike);
                final ImageView selection = view.findViewById(R.id.onSelection);
                final ImageView onAddCustom = view.findViewById(R.id.onAddCustom);

                final LinearLayout customsLayout = view.findViewById(R.id.customsLayout);

                final Runnable updateConfigRunnable = new Runnable() {

                    @Override
                    public void run() {

                        customsLayout.removeAllViews();
                        for (final String line : AppState.get().customConfigColors.split(",")) {
                            if (TxtUtils.isEmpty(line)) {
                                continue;
                            }
                            final ImageView image = new ImageView(controller.getActivity());
                            if (line.startsWith("H")) {
                                image.setImageResource(R.drawable.glyphicons_695_text_background);
                            } else if (line.startsWith("U")) {
                                image.setImageResource(R.drawable.glyphicons_104_underline);
                            } else if (line.startsWith("S")) {
                                image.setImageResource(R.drawable.glyphicons_105_strikethrough);
                            }
                            String color = line.substring(1);
                            final int colorInt = Color.parseColor(color);
                            TintUtil.setTintImageWithAlpha(image, colorInt);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Dips.dpToPx(35), Dips.dpToPx(35));
                            int pd = Dips.dpToPx(5);
                            params.leftMargin = pd;
                            image.setPadding(pd, pd, pd, pd);
                            customsLayout.addView(image, params);

                            image.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    if (line.startsWith("H")) {
                                        controller.underlineText(colorInt, 2.0f, AnnotationType.HIGHLIGHT);
                                    } else if (line.startsWith("U")) {
                                        controller.underlineText(colorInt, 2.0f, AnnotationType.UNDERLINE);
                                    } else if (line.startsWith("S")) {
                                        controller.underlineText(colorInt, 2.0f, AnnotationType.STRIKEOUT);
                                    }

                                    closeDialog();
                                    controller.saveAnnotationsToFile();

                                }
                            });
                            image.setOnLongClickListener(new OnLongClickListener() {

                                @Override
                                public boolean onLongClick(View v) {
                                    AppState.get().customConfigColors = AppState.get().customConfigColors.replace(line, "");
                                    customsLayout.removeView(image);
                                    return true;
                                }
                            });
                        }
                    }
                };
                updateConfigRunnable.run();

                onAddCustom.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PopupMenu menu = new PopupMenu(v.getContext(), v);

                        Drawable highlight = controller.getActivity().getResources().getDrawable(R.drawable.glyphicons_695_text_background);
                        highlight.setColorFilter(Color.parseColor(AppState.get().annotationTextColor), Mode.SRC_ATOP);

                        Drawable underline = controller.getActivity().getResources().getDrawable(R.drawable.glyphicons_104_underline);
                        underline.setColorFilter(Color.parseColor(AppState.get().annotationTextColor), Mode.SRC_ATOP);

                        Drawable strikeout = controller.getActivity().getResources().getDrawable(R.drawable.glyphicons_105_strikethrough);
                        strikeout.setColorFilter(Color.parseColor(AppState.get().annotationTextColor), Mode.SRC_ATOP);

                        menu.getMenu().add(R.string.highlight_of_text).setIcon(highlight).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().customConfigColors += "H" + AppState.get().annotationTextColor + ",";
                                updateConfigRunnable.run();
                                return false;
                            }
                        });
                        menu.getMenu().add(R.string.underline_of_text).setIcon(underline).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().customConfigColors += "U" + AppState.get().annotationTextColor + ",";
                                updateConfigRunnable.run();
                                return false;
                            }
                        });
                        menu.getMenu().add(R.string.strikethrough_of_text).setIcon(strikeout).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().customConfigColors += "S" + AppState.get().annotationTextColor + ",";
                                updateConfigRunnable.run();
                                return false;
                            }
                        });
                        menu.show();

                        PopupHelper.initIcons(menu, Color.parseColor(AppState.get().annotationTextColor));

                    }
                });

                underLine.setColorFilter(Color.parseColor(AppState.get().annotationTextColor));
                strike.setColorFilter(Color.parseColor(AppState.get().annotationTextColor));
                selection.setColorFilter(Color.parseColor(AppState.get().annotationTextColor));

                for (final String colorName : colors) {
                    final View inflate = LayoutInflater.from(linearLayoutColor.getContext()).inflate(R.layout.item_color, linearLayoutColor, false);
                    inflate.setBackgroundResource(R.drawable.bg_border_2_lines);
                    final View img = inflate.findViewById(R.id.itColor);
                    final int colorId = Color.parseColor(colorName);
                    img.setBackgroundColor(colorId);
                    inflate.setTag(colorName);

                    linearLayoutColor.addView(inflate);

                    inflate.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            // Views.unselectChilds(linearLayoutColor);
                            // v.setSelected(true);
                            AppState.get().annotationTextColor = colorName;
                            underLine.setColorFilter(Color.parseColor(colorName));
                            strike.setColorFilter(Color.parseColor(colorName));
                            selection.setColorFilter(Color.parseColor(colorName));
                        }
                    });

                }

                final EditText editText = view.findViewById(R.id.editText);
                final String selectedText = AppState.get().selectedText;
                // AppState.get().selectedText = null;

                editText.setText(selectedText);

                view.findViewById(R.id.onTranslate).setOnClickListener(v -> {
                    anchor.removeAllViews();
                    final PopupMenu popupMenu = new PopupMenu(v.getContext(), view);
                    final Map<String, String> providers = AppData.get().getWebDictionaries(editText.getText().toString().trim());
                    for (final String name : providers.keySet()) {
                        popupMenu.getMenu().add(name).setOnMenuItemClickListener(item -> {
                            String url = providers.get(name).trim();
                            //Urls.open(anchor.getContext(), url);
                            closeDialog();
                            webView(anchor, url);

                            return false;
                        });
                    }
                    popupMenu.show();
                });

                view.findViewById(R.id.onGoogle).setOnClickListener(v -> {
                    anchor.removeAllViews();
                    final PopupMenu popupMenu = new PopupMenu(v.getContext(), view);
                    final Map<String, String> providers = AppData.get().getWebSearch(editText.getText().toString().trim());
                    for (final String name : providers.keySet()) {
                        popupMenu.getMenu().add(name).setOnMenuItemClickListener(item -> {
                            String url = providers.get(name).trim();
                            //Urls.open(anchor.getContext(), providers.get(name).trim());
                            closeDialog();
                            webView(anchor, url);
                            return false;
                        });
                    }
                    popupMenu.show();
                });

                view.findViewById(R.id.onAddToBookmark).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.clearSelectedText();
                        closeDialog();
                        ListBoxHelper.showAddDialog(controller, null, null, editText.getText().toString().trim(), null);
                    }
                });

                view.findViewById(R.id.readTTS).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        TTSEngine.get().stop();

                        String text = editText.getText().toString().trim();
                        text = TxtUtils.replaceHTMLforTTS(text);
                        text = text.replace(TxtUtils.TTS_PAUSE, "");

                        TTSEngine.get().speek(text);
                        Toast.makeText(controller.getActivity(), text, Toast.LENGTH_SHORT).show();
                    }
                });
                view.findViewById(R.id.readTTS).setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        TTSEngine.get().stop();
                        String text = editText.getText().toString().trim();
                        TTSEngine.get().speek(text);
                        Toast.makeText(controller.getActivity(), text, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

                view.findViewById(R.id.readTTSNext).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        TTSEngine.get().stop();
                        AppSP.get().lastBookParagraph = 0;
                        TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), editText.getText().toString().trim(), controller.getBookWidth(), controller.getBookHeight(), BookCSS.get().fontSizeSp, controller.getTitle());
                    }
                });

                View onShare = view.findViewById(R.id.onShare);
                onShare.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {


                        final Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        String trimText = editText.getText().toString().trim();
                        FileMeta meta = controller.getBookFileMeta();

                        List<String> list = Arrays.asList(
                                "Format: Quote",
                                "Format: \"Quote\"\n-- Author",
                                "Format: \"Quote\"\n-- Author\nTitle"
                        );

                        MyPopupMenu menu = new MyPopupMenu(v);
                        for (String line : list) {
                            menu.getMenu().add(line).setOnMenuItemClickListener(menuItem -> {
                                String res = line.replace("Format: ", "");
                                res = res.replace("Author", meta.getAuthor());
                                res = res.replace("Title", meta.getTitle());
                                res = res.replace("Quote", trimText);
                                intent.putExtra(Intent.EXTRA_TEXT, res);

                                controller.getActivity().startActivity(Intent.createChooser(intent, controller.getString(R.string.share)));
                                controller.clearSelectedText();
                                closeDialog();
                                return false;
                            });
                        }
                        menu.show();


                    }
                });

                view.findViewById(R.id.onCopy).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.clearSelectedText();
                        Context c = anchor.getContext();
                        String trim = editText.getText().toString().trim();
                        ClipboardManager clipboard = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(c.getString(R.string.copied_text), trim);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(c, c.getString(R.string.copied_text) + ": " + trim, Toast.LENGTH_SHORT).show();
                        closeDialog();
                    }
                });

                View onBookSearch = view.findViewById(R.id.onBookSearch);
                // onBookSearch.setText(controller.getString(R.string.search_in_the_book)
                // + " \"" + AppState.get().selectedText + "\"");
                if (onBookSearch != null) {

                    onBookSearch.setVisibility(selectedText != null && selectedText.contains(" ") ? View.GONE : View.VISIBLE);
                    onBookSearch.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            controller.clearSelectedText();
                            searchMenu(anchor, controller, selectedText);
                        }
                    });
                }

                LinearLayout dictLayout = view.findViewById(R.id.dictionaryLine);
                dictLayout.removeAllViews();

                final Intent intentProccessText = new Intent();
                if (Build.VERSION.SDK_INT >= 23) {
                    intentProccessText.setAction(Intent.ACTION_PROCESS_TEXT);
                }
                intentProccessText.setType("text/plain");

                final Intent intentSearch = new Intent();
                intentSearch.setAction(Intent.ACTION_SEARCH);

                final Intent intentSend = new Intent();
                intentSend.setAction(Intent.ACTION_SEND);
                intentSend.setType("text/plain");

                final Intent intentCustom = new Intent("colordict.intent.action.SEARCH");

                PackageManager pm = anchor.getContext().getPackageManager();

                final List<ResolveInfo> proccessTextList = DictsHelper.resolveInfosList(intentProccessText, pm);
                final List<ResolveInfo> searchList = DictsHelper.resolveInfosList(intentSearch, pm);
                final List<ResolveInfo> sendList = DictsHelper.resolveInfosList(intentSend, pm);
                final List<ResolveInfo> customList = DictsHelper.resolveInfosList(intentCustom, pm);

                final List<ResolveInfo> all = new ArrayList<ResolveInfo>();
                all.addAll(customList);

                if (Build.VERSION.SDK_INT >= 23) {
                    all.addAll(proccessTextList);
                }
                all.addAll(searchList);
                all.addAll(sendList);

                final SharedPreferences sp = anchor.getContext().getSharedPreferences("lastDict", Context.MODE_PRIVATE);
                final String lastID = sp.getString("last", "");

                List<String> cache = new ArrayList<String>();
                for (final ResolveInfo app : all) {
                    for (final String pkgKey : AppState.appDictionariesKeys) {
                        String pkg = app.activityInfo.packageName;

                        if (pkg.toLowerCase(Locale.US).contains(pkgKey) || DictsHelper.getHash(app.activityInfo) == AppState.get().rememberDictHash2) {

                            if (cache.contains(app.activityInfo.name)) {
                                continue;
                            }
                            cache.add(app.activityInfo.name);

                            LOG.d("Add APP", app.activityInfo.name);
                            try {
                                ImageView image = new ImageView(anchor.getContext());
                                image.setTag(controller.getString(R.string.no_tint));
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Dips.dpToPx(44), Dips.dpToPx(44));
                                layoutParams.rightMargin = Dips.dpToPx(8);
                                image.setLayoutParams(layoutParams);
                                Drawable icon = anchor.getContext().getPackageManager().getApplicationIcon(app.activityInfo.packageName);
                                image.setImageDrawable(icon);
                                image.setOnClickListener(new OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        String selecteText = editText.getText().toString().trim();
                                        closeDialog();
                                        final ActivityInfo activity = app.activityInfo;
                                        final ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);

                                        if (customList.contains(app)) {
                                            LOG.d("dict-intent", "customList");

                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                                intentCustom.addCategory(Intent.CATEGORY_LAUNCHER);
                                            }
                                            intentCustom.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentCustom.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                            intentCustom.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                            intentCustom.setComponent(name);

                                            intentCustom.putExtra("EXTRA_QUERY", selecteText);
                                            DictsHelper.updateExtraGoldenDict(intentCustom);

                                            LOG.d("intentCustom", intentCustom, intentCustom.getExtras());

                                            try {
                                                controller.getActivity().startActivity(intentCustom);
                                            } catch (Exception e) {
                                                Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                                                LOG.e(e);
                                            }

                                            // controller.getActivity().overridePendingTransition(0, 0);

                                        } else if (proccessTextList.contains(app)) {
                                            LOG.d("dict-intent", "proccessTextList");

                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                                intentProccessText.addCategory(Intent.CATEGORY_LAUNCHER);
                                            }

                                            intentProccessText.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentProccessText.setComponent(name);

                                            intentProccessText.putExtra(Intent.EXTRA_TEXT, selecteText);
                                            intentProccessText.putExtra(Intent.EXTRA_PROCESS_TEXT, selecteText);
                                            intentProccessText.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, selecteText);

                                            try {
                                                controller.getActivity().startActivity(intentProccessText);
                                            } catch (Exception e) {
                                                Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                                                LOG.e(e);
                                            }
                                            LOG.d("dict-intent", intentProccessText);
                                        } else if (searchList.contains(app)) {
                                            LOG.d("dict-intent", "searchList");
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                                intentSearch.addCategory(Intent.CATEGORY_LAUNCHER);
                                            }
                                            intentSearch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentSearch.setComponent(name);

                                            intentSearch.putExtra(SearchManager.QUERY, selecteText);
                                            intentSearch.putExtra(Intent.EXTRA_TEXT, selecteText);

                                            try {
                                                controller.getActivity().startActivity(intentSearch);
                                            } catch (Exception e) {
                                                Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                                                LOG.e(e);
                                            }
                                            LOG.d("dict-intent", intentSearch);
                                        } else if (sendList.contains(app)) {
                                            LOG.d("dict-intent", "sendList");
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                                intentSend.addCategory(Intent.CATEGORY_LAUNCHER);
                                            }
                                            intentSend.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentSend.setComponent(name);

                                            intentSend.putExtra(Intent.EXTRA_TEXT, selecteText);
                                            try {
                                                controller.getActivity().startActivity(intentSend);
                                            } catch (Exception e) {
                                                Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                                                LOG.e(e);

                                            }
                                            LOG.d("dict-intent", intentSend);
                                        }
                                        sp.edit().putString("last", app.activityInfo.name).commit();

                                        controller.clearSelectedText();

                                    }
                                });
                                if (app.activityInfo.name.equals(lastID) || lastID.equals("" + DictsHelper.getHash(app.activityInfo))) {
                                    dictLayout.addView(image, 0);
                                } else {
                                    dictLayout.addView(image);
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                LOG.d(e);
                            }
                        }
                    }

                }
                ImageView image = new ImageView(anchor.getContext());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Dips.dpToPx(44), Dips.dpToPx(44));
                image.setLayoutParams(layoutParams);
                image.setImageResource(R.drawable.glyphicons_371_plus);
                image.setBackgroundResource(R.drawable.bg_border_ltgray);

                TintUtil.setTintImageWithAlpha(image, Color.LTGRAY);

                image.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        DialogTranslateFromTo.show(controller.getActivity(), true, new Runnable() {

                            @Override
                            public void run() {
                                sp.edit().putString("last", "" + AppState.get().rememberDictHash2).commit();
                                selectTextMenu(anchor, controller, withAnnotation, reloadUI);
                            }
                        }, true);

                    }
                });

                FrameLayout fr = new FrameLayout(controller.getActivity());
                image.setPadding(Dips.DP_10, Dips.DP_10, Dips.DP_10, Dips.DP_10);
                fr.addView(image);

                dictLayout.addView(fr);

                view.findViewById(R.id.onUnderline).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.underlineText(Color.parseColor(AppState.get().annotationTextColor), 2.0f, AnnotationType.UNDERLINE);
                        closeDialog();
                        controller.saveAnnotationsToFile();
                    }
                });
                view.findViewById(R.id.onStrike).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.underlineText(Color.parseColor(AppState.get().annotationTextColor), 2.0f, AnnotationType.STRIKEOUT);
                        closeDialog();
                        controller.saveAnnotationsToFile();
                    }
                });
                view.findViewById(R.id.onSelection).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.underlineText(Color.parseColor(AppState.get().annotationTextColor), 2.0f, AnnotationType.HIGHLIGHT);
                        closeDialog();
                        controller.saveAnnotationsToFile();
                    }
                });

                if (!BookType.PDF.is(controller.getCurrentBook().getPath()) || !withAnnotation || controller.getActivity() instanceof HorizontalViewActivity || controller.isPasswordProtected()) {
                    linearLayoutColor.setVisibility(View.GONE);
                    view.findViewById(R.id.onUnderline).setVisibility(View.GONE);
                    view.findViewById(R.id.onStrike).setVisibility(View.GONE);
                    view.findViewById(R.id.onSelection).setVisibility(View.GONE);
                    onAddCustom.setVisibility(View.GONE);
                    customsLayout.setVisibility(View.GONE);
                }

                return view;
            }

        }.show("text", true).setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                AppState.get().selectedText = null;
            }
        });

    }


    public static DragingPopup gotoPageDialog(final FrameLayout anchor, final DocumentController dc) {
        if (dc == null) {
            return null;
        }
        DragingPopup popup = new DragingPopup(R.string.go_to_page_dialog, anchor, 300, 400) {
            View searchLayout;
            GridView grid;

            @Override
            public void beforeCreate() {
                setTitlePopupIcon(R.drawable.glyphicons_498_more_vertical);
                titlePopupMenu = new MyPopupMenu(anchor.getContext(), anchor);

                titlePopupMenu.getMenu().addCheckbox(dc.getString(R.string.show_search_bar), AppState.get().isShowSearchBar, (buttonView, isChecked) -> {
                    AppState.get().isShowSearchBar = isChecked;
                    searchLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    beforeCreate();
                });

                titlePopupMenu.getMenu().addCheckbox(dc.getString(R.string.show_fast_scroll), AppState.get().isShowFastScroll, (buttonView, isChecked) -> {
                    AppState.get().isShowFastScroll = isChecked;
                    grid.setFastScrollEnabled(AppState.get().isShowFastScroll);
                    beforeCreate();
                });
            }

            @Override
            public View getContentView(LayoutInflater inflater) {
                View view = inflater.inflate(R.layout.dialog_go_to_page, null, false);
                searchLayout = view.findViewById(R.id.searchLayout);
                Views.visible(searchLayout, AppState.get().isShowSearchBar);

                final EditText number = view.findViewById(R.id.edit1);
                number.clearFocus();
                number.setText("" + dc.getCurentPageFirst1());
                if (TempHolder.get().pageDelta != 0) {
                    int i = dc.getCurentPageFirst1() + TempHolder.get().pageDelta;
                    number.setText("[" + i + "]");
                }
                grid = view.findViewById(R.id.grid1);
                int dpToPx = Dips.dpToPx(AppState.get().coverSmallSize);

                if (AppSP.get().isDouble && !dc.isTextFormat()) {
                    dpToPx = dpToPx * 2;
                }
                grid.setColumnWidth(dpToPx);
                grid.setFastScrollEnabled(AppState.get().isShowFastScroll);

                final File currentBook = dc.getCurrentBook();
                if (ExtUtils.isValidFile(currentBook)) {
                    grid.setAdapter(new PageThumbnailAdapter(anchor.getContext(), dc.getPageCount(), dc.getCurentPageFirst1() - 1) {
                        @Override
                        public PageUrl getPageUrl(int page) {
                            return PageUrl.buildSmall(currentBook.getPath(), page);
                        }

                    });

                }

                grid.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        dc.onGoToPage(position + 1);
                        ((PageThumbnailAdapter) grid.getAdapter()).setCurrentPage(position);
                        ((PageThumbnailAdapter) grid.getAdapter()).notifyDataSetChanged();
                    }

                });
                grid.setOnItemLongClickListener(new OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        Vibro.vibrate();

                        MyPopupMenu menu = new MyPopupMenu(view);
                        menu.getMenu().add(R.string.share_as_text).setIcon(R.drawable.glyphicons_578_share).setOnMenuItemClickListener((it) -> {
                            final Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_TEXT, dc.getTextForPage(position));
                            dc.getActivity().startActivity(Intent.createChooser(intent, dc.getActivity().getString(R.string.share)));
                            return true;
                        });
                        menu.getMenu().add(R.string.share_as_image).setIcon(R.drawable.glyphicons_38_picture).setOnMenuItemClickListener((it) -> {
                            ExtUtils.sharePage(dc.getActivity(), dc.getCurrentBook(), position, dc.getPageUrl(position).toString());
                            return true;
                        });
                        menu.getMenu().add(R.string.copy_text).setIcon(R.drawable.glyphicons_614_copy).setOnMenuItemClickListener((it) -> {
                            ClipboardManager clipboard = (ClipboardManager) dc.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(dc.getString(R.string.copy_text), dc.getTextForPage(position));
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(dc.getActivity(), R.string.copy_text, Toast.LENGTH_SHORT).show();
                            return true;
                        });
                        menu.show();

                        return true;
                    }
                });

                grid.setSelection(dc.getCurentPage() - 1);

                grid.setOnScrollListener(new OnScrollListener() {

                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
                            IMG.pauseRequests(grid.getContext());
                        } else {
                            IMG.resumeRequests(grid.getContext());
                        }
                        LOG.d("onScrollStateChanged", scrollState);
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        LOG.d("onScroll", firstVisibleItem, Math.abs(firstVisibleItem - dc.getCurentPage()));
                        if (firstVisibleItem < 3 || Math.abs(firstVisibleItem - dc.getCurentPage()) < 20) {
                            // searchLayout.setVisibility(View.VISIBLE);
                        } else {
                            // searchLayout.setVisibility(View.GONE);
                        }
                    }
                });
                final View onLink = view.findViewById(R.id.onLink);
                TintUtil.setTintBg(onLink);

                if (dc.isEpub3 == null) {
                    onLink.setVisibility(View.GONE);
                    dc.isEpub3 = false;
                    if (dc.getCurrentOutline() != null) {
                        for (OutlineLinkWrapper line : dc.getCurrentOutline()) {
                            if (line.getTitleAsString().equals(MuPdfOutline.EPUB_3_PAGES)) {
                                onLink.setVisibility(View.VISIBLE);
                                dc.isEpub3 = true;
                                break;
                            }
                        }
                    }
                }
                onLink.setVisibility(dc.isEpub3 ? View.VISIBLE : View.GONE);

                onLink.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            String txt = number.getText().toString();
                            int page = Integer.valueOf(txt);
                            for (OutlineLinkWrapper line : dc.getCurrentOutline()) {
                                if (line.getTitleAsString().equals("Page " + page)) {
                                    dc.onGoToPage(line.targetPage);
                                    grid.setSelection(line.targetPage - 1);
                                    Keyboards.close(number);
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            LOG.e(e);
                        }

                    }
                });


                final View onSearch = view.findViewById(R.id.onSearch);
                TintUtil.setTintBg(onSearch);

                number.setOnKeyListener(new OnKeyListener() {

                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            onSearch.performClick();
                            return true;
                        }
                        return false;
                    }
                });

                onSearch.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        int page = 1;
                        try {
                            String txt = number.getText().toString();
                            txt = txt.replace("[", "").replace("]", "");

                            if (txt.contains("%") || txt.contains(".") || txt.contains(",")) {
                                txt = txt.replace("%", "").replace(",", ".");
                                float parseFloat = Float.parseFloat(txt);
                                if (parseFloat > 100) {
                                    Toast.makeText(dc.getActivity(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                                }
                                page = (int) (dc.getPageCount() * parseFloat) / 100;
                                page = page + 1;
                            } else {
                                page = Integer.valueOf(txt);
                            }

                        } catch (Exception e) {
                            LOG.e(e);
                            number.setText("1");
                        }

                        if (TempHolder.get().pageDelta != 0) {
                            page -= TempHolder.get().pageDelta;
                        }

                        if (page >= 0 && page <= dc.getPageCount()) {
                            dc.onGoToPage(page);
                            grid.setSelection(page - 1);
                            Keyboards.close(number);
                        }
                    }
                });

                number.setOnEditorActionListener(new OnEditorActionListener() {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            onSearch.performClick();
                            return true;
                        }
                        return false;
                    }
                });

                return view;
            }

        }.show("gotoPageDialog", false, true).setOnCloseListener(new Runnable() {

            @Override
            public void run() {
            }

        });
        return popup;

    }

    public static void editColorsPanel(final FrameLayout anchor, final DocumentController controller, final DrawView drawView, final boolean force) {
        if (controller == null) {
            return;
        }
        drawView.setOnFinishDraw(new Runnable() {

            @Override
            public void run() {
                String annotationDrawColor = AppState.get().annotationDrawColor;
                if (TxtUtils.isEmpty(annotationDrawColor)) {
                    annotationDrawColor = AppState.COLORS.get(0);
                }

                controller.saveChanges(drawView.getPoints(), Color.parseColor(annotationDrawColor));
                drawView.clear();
                controller.saveAnnotationsToFile();
            }

        });

        new DragingPopup(R.string.annotations_draw, anchor, 250, 150) {

            @Override
            public void closeDialog() {
                super.closeDialog();
                AppState.get().editWith = AppState.EDIT_NONE;
                AppState.get().annotationDrawColor = "";
                drawView.setVisibility(View.GONE);
                drawView.clear();
            }

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View a = inflater.inflate(R.layout.edit_panel, null, false);
                final GridView grid = a.findViewById(R.id.gridColors);

                if (AppState.get().editWith == AppState.EDIT_DELETE) {
                    AppState.get().annotationDrawColor = AppState.COLORS.get(0);
                }

                final BaseItemAdapter<String> adapter = new BaseItemAdapter<String>(AppState.COLORS) {

                    @Override
                    public View getView(int pos, View arg1, ViewGroup arg2, String color) {
                        View view = null;
                        if (pos == 0) {
                            view = inflater.inflate(R.layout.item_color_cut, arg2, false);
                        } else if (pos == 1) {
                            view = inflater.inflate(R.layout.item_color_spinner, arg2, false);
                            final Spinner spinner = view.findViewById(R.id.spinner1);
                            final List<Float> values = Arrays.asList(0.5f, 0.75f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f);
                            spinner.setAdapter(new BaseItemLayoutAdapter<Float>(anchor.getContext(), android.R.layout.simple_spinner_dropdown_item, values) {

                                @Override
                                public void populateView(View inflate, int arg1, Float value) {
                                    TextView text = Views.text(inflate, android.R.id.text1, "" + value + "");

                                }
                            });
                            spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    spinner.setSelection(position);
                                    AppState.get().editLineWidth = values.get(position);
                                    int acolor = IMG.alphaColor(AppState.get().editAlphaColor, AppState.get().annotationDrawColor);
                                    drawView.setColor(acolor, AppState.get().editLineWidth);

                                    try {
                                        TextView textView = (TextView) spinner.getChildAt(0);
                                        textView.setTextAppearance(controller.getActivity(), R.style.textLinkStyle);
                                    } catch (Exception e) {
                                    }

                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {

                                }
                            });
                            spinner.setSelection(values.indexOf(AppState.get().editLineWidth));

                        } else if (pos == 2121) {
                            view = inflater.inflate(R.layout.item_color_spinner, arg2, false);
                            final Spinner spinner = view.findViewById(R.id.spinner1);
                            final List<Integer> values = Arrays.asList(100, 90, 80, 70, 60, 50, 40, 30, 20, 10);
                            spinner.setAdapter(new BaseItemLayoutAdapter<Integer>(anchor.getContext(), android.R.layout.simple_spinner_dropdown_item, values) {

                                @Override
                                public void populateView(View inflate, int arg1, Integer value) {
                                    TextView text = Views.text(inflate, android.R.id.text1, "" + value + "%");
                                }
                            });
                            spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    spinner.setSelection(position);
                                    AppState.get().editAlphaColor = values.get(position);
                                    int acolor = IMG.alphaColor(AppState.get().editAlphaColor, AppState.get().annotationDrawColor);
                                    drawView.setColor(acolor, AppState.get().editLineWidth);
                                    // ((TextView)
                                    // spinner.getChildAt(0)).setTextColor(anchor.getResources().getColor(R.color.grey_800));
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {

                                }
                            });
                            spinner.setSelection(values.indexOf(AppState.get().editAlphaColor));

                        } else {
                            view = inflater.inflate(R.layout.item_color, arg2, false);
                            int acolor = IMG.alphaColor(AppState.get().editAlphaColor, color);
                            view.findViewById(R.id.itColor).setBackgroundColor(acolor);
                        }
                        view.setTag(color);
                        if (color.equals(AppState.get().annotationDrawColor)) {
                            view.setBackgroundResource(R.drawable.bg_border_1_lines);
                        } else {
                            view.setBackgroundColor(Color.TRANSPARENT);
                        }
                        return view;
                    }
                };
                grid.setAdapter(adapter);
                grid.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        AppState.get().editWith = AppState.EDIT_PEN;
                        if (view.getTag().equals(AppState.get().annotationDrawColor)) {
                            AppState.get().annotationDrawColor = "";
                            drawView.setVisibility(View.GONE);
                        } else {
                            AppState.get().annotationDrawColor = (String) view.getTag();
                            int acolor = IMG.alphaColor(AppState.get().editAlphaColor, AppState.get().annotationDrawColor);
                            drawView.setColor(acolor, AppState.get().editLineWidth);
                            drawView.setVisibility(View.VISIBLE);
                            if (position == 0) {
                                drawView.setVisibility(View.GONE);
                                AppState.get().editWith = AppState.EDIT_DELETE;
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                });

                return a;
            }
        }.show(EDIT_COLORS_PANEL, force);
    }

    public static void recentBooks(final FrameLayout anchor, final DocumentController controller) {
        if (controller == null) {
            return;
        }

        new DragingPopup(R.string.library, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View root = inflater.inflate(R.layout.fragment_include_library, null, false);

                SlidingTabLayout indicator = root.findViewById(R.id.slidingTabs);
                ViewPager pager = root.findViewById(R.id.pager);
                List<UIFragment> tabFragments = new ArrayList<>();

                tabFragments.add(new SearchFragment2());
                tabFragments.add(new BrowseFragment2());
                tabFragments.add(new RecentFragment2());
                tabFragments.add(new FavoritesFragment2());

                TabsAdapter2 adapter = new TabsAdapter2((FragmentActivity) controller.getActivity(), tabFragments);
                pager.setAdapter(adapter);

                indicator.setViewPager(pager);

                pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {
                        tabFragments.get(position).onSelectFragment();
                        AppState.get().tabPositionInRecentDialog = position;
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });

                indicator.setVisibility(View.VISIBLE);
                indicator.init();

                indicator.setDividerColors(controller.getActivity().getResources().getColor(R.color.tint_divider));
                indicator.setSelectedIndicatorColors(Color.WHITE);
                indicator.setBackgroundColor(TintUtil.color);

                pager.setOffscreenPageLimit(10);
                pager.setCurrentItem(AppState.get().tabPositionInRecentDialog);

                return root;
            }
        }.show("recentBooks");
    }

    public static void recentBooksOld(final FrameLayout anchor, final DocumentController controller) {
        if (controller == null) {
            return;
        }

        new DragingPopup(R.string.recent_favorites_tags, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public View getContentView(final LayoutInflater inflater) {
                RecyclerView recyclerView = new RecyclerView(controller.getActivity());
                FileMetaAdapter recentAdapter = new FileMetaAdapter();
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(controller.getActivity());
                recyclerView.setLayoutManager(mLayoutManager);
                recentAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST);
                recyclerView.setAdapter(recentAdapter);
                recentAdapter.tempValue2 = FileMetaAdapter.TEMP2_RECENT_FROM_BOOK;

                List<FileMeta> all = AppData.get().getAllRecent(false);
                FileMeta stars = new FileMeta();
                stars.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_STARS);
                all.add(0, stars);

                recentAdapter.getItemsList().clear();
                recentAdapter.getItemsList().addAll(all);
                recentAdapter.notifyDataSetChanged();

                DefaultListeners.bindAdapter(controller.getActivity(), recentAdapter, controller, new Runnable() {

                    @Override
                    public void run() {
                        closeDialog();
                    }
                });

                return recyclerView;
            }
        }.show("recentBooks");
    }

    public static void addBookmarksLong(final FrameLayout anchor, final DocumentController controller) {
        if (controller == null) {
            return;
        }
        TTSEngine.fastTTSBookmakr(controller);

    }

    public static void showBookmarksDialog(final FrameLayout anchor, final DocumentController controller, final Runnable onRefeshUI) {
        if (controller == null) {
            return;
        }
        final List<AppBookmark> objects = new ArrayList<AppBookmark>();
        final BookmarksAdapter bookmarksAdapter = new BookmarksAdapter(anchor.getContext(), objects, true, controller, onRefeshUI);

        final View.OnClickListener onAddBookmark = new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                ListBoxHelper.showAddDialog(controller, objects, bookmarksAdapter, "", onRefeshUI);

            }
        };

        final OnItemClickListener onItem = new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {

                final AppBookmark appBookmark = objects.get(position);
                if (appBookmark.isF) {
                    controller.floatingBookmark = appBookmark;
                } else {
                    controller.floatingBookmark = null;
                }

                LOG.d("onItem", appBookmark);

                int page = appBookmark.getPage(controller.getPageCount());

                controller.onGoToPage(page);

                onRefeshUI.run();

            }
        };

        final OnItemLongClickListener onBooksLong = new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                ListBoxHelper.showEditDeleteDialog(objects.get(position), controller, bookmarksAdapter, objects, onRefeshUI);
                return true;
            }

        };

        new DragingPopup(R.string.bookmarks, anchor, 300, 400) {

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View a = inflater.inflate(R.layout.dialog_bookmarks, null, false);
                final ListView contentList = a.findViewById(R.id.contentList);
                contentList.setDivider(new ColorDrawable(Color.TRANSPARENT));
                contentList.setVerticalScrollBarEnabled(false);
                contentList.setAdapter(bookmarksAdapter);
                contentList.setOnItemClickListener(onItem);
                contentList.setOnItemLongClickListener(onBooksLong);
                a.findViewById(R.id.addBookmarkNormal).setOnClickListener(onAddBookmark);

                final View.OnClickListener onQuickBookmark = new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        TTSEngine.fastTTSBookmakr(controller);
                        closeDialog();
                        onRefeshUI.run();
                    }
                };

                a.findViewById(R.id.addPageBookmarkQuick).setOnClickListener(onQuickBookmark);

                objects.clear();

                List<AppBookmark> bookmarksByBook = BookmarksData.get().getBookmarksByBook(controller.getCurrentBook());

                final Comparator<AppBookmark> cmp = new Comparator<AppBookmark>() {
                    @Override
                    public int compare(AppBookmark o1, AppBookmark o2) {
                        switch (AppState.get().sortBookmarksOrder) {
                            case AppState.BOOKMARK_SORT_PAGE_ASC:
                                return Float.compare(o1.getPercent(), o2.getPercent());
                            case AppState.BOOKMARK_SORT_PAGE_DESC:
                                return Float.compare(o2.getPercent(), o1.getPercent());
                            case AppState.BOOKMARK_SORT_DATE_ASC:
                                return Long.compare(o2.getTime(), o1.getTime());
                            case AppState.BOOKMARK_SORT_DATE_DESC:
                                return Long.compare(o1.getTime(), o2.getTime());
                        }
                        return Float.compare(o1.getPercent(), o2.getPercent());
                    }
                };
                objects.addAll(bookmarksByBook);

                Collections.sort(objects, cmp);

                bookmarksAdapter.notifyDataSetChanged();

                setTitlePopupIcon(R.drawable.glyphicons_498_more_vertical);
                titlePopupMenu = new MyPopupMenu(controller.getActivity(), null);

                titlePopupMenu.getMenu(R.drawable.glyphicons_578_share, R.string.share,
                        () -> ExtUtils.sendBookmarksTo(controller.getActivity(), controller.getCurrentBook())
                );

                titlePopupMenu.getMenu(R.drawable.glyphicons_222_chevron_up, R.string.by_pages,
                        () -> {
                            AppState.get().sortBookmarksOrder = AppState.BOOKMARK_SORT_PAGE_ASC;
                            Collections.sort(objects, cmp);
                            bookmarksAdapter.notifyDataSetChanged();
                        });

                titlePopupMenu.getMenu(R.drawable.glyphicons_221_chevron_down, R.string.by_pages,
                        () -> {
                            AppState.get().sortBookmarksOrder = AppState.BOOKMARK_SORT_PAGE_DESC;
                            Collections.sort(objects, cmp);
                            bookmarksAdapter.notifyDataSetChanged();
                        }
                );

                titlePopupMenu.getMenu(R.drawable.glyphicons_222_chevron_up, R.string.by_date,
                        () -> {
                            AppState.get().sortBookmarksOrder = AppState.BOOKMARK_SORT_DATE_ASC;
                            Collections.sort(objects, cmp);
                            bookmarksAdapter.notifyDataSetChanged();
                        }
                );

                titlePopupMenu.getMenu(R.drawable.glyphicons_221_chevron_down, R.string.by_date,
                        () -> {
                            AppState.get().sortBookmarksOrder = AppState.BOOKMARK_SORT_DATE_DESC;
                            Collections.sort(objects, cmp);
                            bookmarksAdapter.notifyDataSetChanged();
                        }
                );

                return a;
            }
        }.

                show("addBookmarks", false, true);
    }

    public static DragingPopup showContent(final FrameLayout anchor, final DocumentController controller) {

        final ItemClickListenerWithReference<DragingPopup> onClickContent = new ItemClickListenerWithReference<DragingPopup>() {

            int prev = -1;

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                final OutlineLinkWrapper link = (OutlineLinkWrapper) parent.getItemAtPosition(position);
                // if (true) {
                // int linkPage = MuPdfLinks.getLinkPageWrapper(link.docHandle, link.linkUri) +
                // 1;
                // LOG.d("targetUrl page", linkPage, link.linkUri);
                // controller.onGoToPage(linkPage);
                //
                // return;
                // }

                if (link.targetPage != -1) {
                    int pageCount = controller.getPageCount();
                    if (link.targetPage < 1 || link.targetPage > pageCount) {
                        Toast.makeText(anchor.getContext(), "no", Toast.LENGTH_SHORT).show();
                    } else {
                        controller.onGoToPage(link.targetPage);
                        // ((ListView) parent).requestFocusFromTouch();
                        // ((ListView) parent).setSelection(position);
                        if (position == prev) {
                            reference.closeDialog();
                        }
                        prev = position;

                    }
                }

            }
        };
        DragingPopup dragingPopup = new DragingPopup(anchor.getContext().getString(R.string.content_of_book), anchor, 300, 400) {

            @Override
            public View getContentView(LayoutInflater inflater) {
                View view = inflater.inflate(R.layout.dialog_recent_books, null, false);
                if (controller == null) {
                    return view;
                }

                LinearLayout attachemnts = view.findViewById(R.id.mediaAttachments);
                List<String> mediaAttachments = controller.getMediaAttachments();
                if (mediaAttachments != null && !mediaAttachments.isEmpty()) {
                    view.findViewById(R.id.mediaAttachmentsScroll).setVisibility(View.VISIBLE);
                    for (final String fname : mediaAttachments) {
                        String[] split = fname.split(",");
                        final String nameFull = split[0];
                        String name = nameFull;
                        if (name.contains("/")) {
                            name = name.substring(name.lastIndexOf("/") + 1);
                        }
                        long size = Long.parseLong(split[1]);

                        TextView t = new TextView(anchor.getContext());
                        t.setText(TxtUtils.underline(name + " (" + ExtUtils.readableFileSize(size) + ")"));
                        t.setPadding(Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2), Dips.dpToPx(2));
                        t.setBackgroundResource(R.drawable.bg_clickable);
                        attachemnts.addView(t);
                        t.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                new AsyncTask<Void, Void, File>() {
                                    ProgressDialog dialog;

                                    @Override
                                    protected void onPreExecute() {
                                        dialog = MyProgressDialog.show(controller.getActivity(), controller.getString(R.string.msg_loading));
                                    }

                                    @Override
                                    protected File doInBackground(Void... params) {
                                        return EpubExtractor.extractAttachment(controller.getCurrentBook(), nameFull);
                                    }

                                    @Override
                                    protected void onPostExecute(File aPath) {
                                        try {
                                            dialog.dismiss();
                                            if (aPath != null && aPath.isFile()) {
                                                LOG.d("Try to open path", aPath);
                                                if (ExtUtils.isAudioContent(aPath.getPath())) {
                                                    TTSEngine.get().mp3Destroy();
                                                    BookCSS.get().mp3BookPath(aPath.getPath());
                                                    AppState.get().mp3seek = 0;
                                                    TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), "", controller.getBookWidth(), controller.getBookHeight(), BookCSS.get().fontSizeSp, controller.getTitle());
                                                } else {
                                                    ExtUtils.openWith(anchor.getContext(), aPath);
                                                }

                                            } else {
                                                Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                                            }
                                        } catch (Exception e) {
                                            LOG.e(e);
                                        }

                                    }

                                }.execute();

                            }
                        });
                    }
                } else {
                    view.findViewById(R.id.mediaAttachmentsScroll).setVisibility(View.GONE);
                }

                final ListView contentList = view.findViewById(R.id.contentList);
                contentList.setSelector(android.R.color.transparent);
                contentList.setVerticalScrollBarEnabled(false);

                final Runnable showOutline = new Runnable() {

                    @Override
                    public void run() {
                        controller.getOutline(new ResultResponse<List<OutlineLinkWrapper>>() {
                            @Override
                            public boolean onResultRecive(final List<OutlineLinkWrapper> outline) {
                                contentList.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        if (outline != null && outline.size() > 0) {
                                            contentList.clearChoices();
                                            OutlineLinkWrapper currentByPageNumber = OutlineHelper.getCurrentChapter(controller);
                                            final OutlineAdapter adapter = new OutlineAdapter(controller.getActivity(), outline, currentByPageNumber, controller.getPageCount());
                                            contentList.setAdapter(adapter);
                                            contentList.setOnItemClickListener(onClickContent);
                                            contentList.setSelection(adapter.getItemPosition(currentByPageNumber) - 3);
                                        }
                                    }
                                });
                                return false;
                            }
                        }, true);

                    }
                };
                contentList.postDelayed(showOutline, 50);

                if (false) {
                    setTitlePopupIcon(AppState.get().outlineMode == AppState.OUTLINE_ONLY_HEADERS ? R.drawable.my_glyphicons_114_paragraph_justify : R.drawable.my_glyphicons_114_justify_sub);
                    titlePopupMenu = new MyPopupMenu(controller.getActivity(), null);

                    List<Integer> names = Arrays.asList(R.string.headings_only, R.string.heading_and_subheadings);
                    final List<Integer> icons = Arrays.asList(R.drawable.my_glyphicons_114_paragraph_justify, R.drawable.my_glyphicons_114_justify_sub);
                    final List<Integer> actions = Arrays.asList(AppState.OUTLINE_ONLY_HEADERS, AppState.OUTLINE_HEADERS_AND_SUBHEADERES);

                    for (int i = 0; i < names.size(); i++) {
                        final int index = i;
                        titlePopupMenu.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().outlineMode = actions.get(index);
                                setTitlePopupIcon(icons.get(index));
                                showOutline.run();
                                return false;
                            }
                        });
                    }
                }

                return view;
            }

        }.show("showContent", false, true);
        onClickContent.setReference(dragingPopup);
        return dragingPopup;

    }

    public static void sliceDialog(final FrameLayout anchor, final DocumentController controller, final Runnable onRefreshDoc, final ResultResponse<Integer> onMoveCut) {

        if (controller == null) {
            return;
        }

        new DragingPopup(anchor.getContext().getString(R.string.split_pages_in_two), anchor, 300, 200) {
            SeekBar seek;
            EditText editPercent;
            final Runnable updateUI = new Runnable() {

                @Override
                public void run() {
                    seek.setProgress(AppState.get().cutP);
                    editPercent.setText(AppState.get().cutP + "%");
                    if (onMoveCut != null) {
                        onMoveCut.onResultRecive(AppState.get().cutP);
                    }
                    PageImageState.get().isShowCuttingLine = true;
                }
            };

            @Override
            public View getContentView(LayoutInflater inflater) {
                PageImageState.get().isShowCuttingLine = false;

                View view = inflater.inflate(R.layout.slice_dialog, null, false);
                editPercent = view.findViewById(R.id.editPercent);

                seek = view.findViewById(R.id.seekBar);
                seek.setMax(100);

                view.findViewById(R.id.imagePlus).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (AppState.get().cutP < 80) {
                            AppState.get().cutP += 1;
                        }
                        updateUI.run();

                    }
                });
                view.findViewById(R.id.imageMinus).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (AppState.get().cutP > 20) {
                            AppState.get().cutP -= 1;
                        }
                        updateUI.run();
                    }
                });
                seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (progress <= 20) {
                            progress = 20;
                        }
                        if (progress >= 80) {
                            progress = 80;
                        }
                        AppState.get().cutP = progress;
                        updateUI.run();
                    }
                });

                Button buttonCancel = view.findViewById(R.id.buttonCancel);
                buttonCancel.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PageImageState.get().isShowCuttingLine = false;
                        AppSP.get().isCut = false;
                        AppBook bookSettings = SettingsManager.getBookSettings(controller.getCurrentBook().getPath());
                        boolean wasSplit = bookSettings.sp;
                        bookSettings.sp = false;
                        onRefreshDoc.run();
                        closeDialog();
                        if (wasSplit) {
                            controller.onGoToPage(controller.getCurentPage() / 2 + 1);
                        }
                    }
                });

                Button buttonApply = view.findViewById(R.id.buttonApply);

                buttonApply.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PageImageState.get().isShowCuttingLine = false;
                        AppSP.get().isCut = true;
                        AppSP.get().isCrop = false;
                        boolean init = SettingsManager.getBookSettings().sp;
                        SettingsManager.getBookSettings().updateFromAppState();
                        onRefreshDoc.run();
                        closeDialog();
                        if (!init) {
                            controller.onGoToPage(controller.getCurentPage() * 2 + 1);
                        }

                    }
                });

                updateUI.run();

                return view;
            }
        }.show("cutDialog").setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                PageImageState.get().isShowCuttingLine = false;
                EventBus.getDefault().post(new InvalidateMessage());
            }
        });

    }

    public static void pageFlippingDialog(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh) {
        if (controller == null) {
            return;
        }

        new DragingPopup(anchor.getContext().getString(R.string.automatic_page_flipping), anchor, 300, 380) {

            @Override
            public View getContentView(LayoutInflater inflater) {
                View inflate = inflater.inflate(R.layout.dialog_flipping_pages, null, false);

                CheckBox isScrollAnimation = inflate.findViewById(R.id.isScrollAnimation);
                isScrollAnimation.setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);
                isScrollAnimation.setChecked(AppState.get().isScrollAnimation);
                isScrollAnimation.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isScrollAnimation = isChecked;
                    }
                });

                CheckBox isLoopAutoplay = inflate.findViewById(R.id.isLoopAutoplay);
                isLoopAutoplay.setChecked(AppState.get().isLoopAutoplay);
                isLoopAutoplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isLoopAutoplay = isChecked;
                    }
                });

                CheckBox isShowToolBar = inflate.findViewById(R.id.isShowToolBar);
                isShowToolBar.setChecked(AppState.get().isShowToolBar);
                isShowToolBar.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowToolBar = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                final CustomSeek flippingInterval = inflate.findViewById(R.id.flippingInterval);
                flippingInterval.init(1, 240, AppState.get().flippingInterval);
                flippingInterval.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        flippingInterval.setValueText("" + result);
                        AppState.get().flippingInterval = result;
                        return false;
                    }
                });
                flippingInterval.setValueText("" + AppState.get().flippingInterval);

                inflate.findViewById(R.id.flippingStart).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        EventBus.getDefault().post(new FlippingStart());
                    }
                });

                inflate.findViewById(R.id.flippingStop).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        EventBus.getDefault().post(new FlippingStop());

                    }
                });

                return inflate;
            }
        }.show("pageFlippingDialog").setOnCloseListener(new Runnable() {

            @Override
            public void run() {

            }

        });

    }

    public static DragingPopup statusBarSettings(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {

        DragingPopup dialog = new DragingPopup(R.string.status_bar, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public void beforeCreate() {
                titleAction = controller.getString(R.string.preferences);
                titleRunnable = new Runnable() {

                    @Override
                    public void run() {
                        preferences(anchor, controller, onRefresh, updateUIRefresh);
                    }
                };
            }

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View inflate = inflater.inflate(R.layout.dialog_status_bar_settings, null, false);

                final CheckBox isShowReadingProgress = inflate.findViewById(R.id.isShowReadingProgress);
                final CheckBox isShowChaptersOnProgress = inflate.findViewById(R.id.isShowChaptersOnProgress);
                final CheckBox isShowSubChaptersOnProgress = inflate.findViewById(R.id.isShowSubChaptersOnProgress);

                isShowReadingProgress.setChecked(AppState.get().isShowReadingProgress);
                isShowReadingProgress.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowReadingProgress = isChecked;
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        isShowChaptersOnProgress.setChecked(isChecked);
                        isShowChaptersOnProgress.setEnabled(isChecked);

                        isShowSubChaptersOnProgress.setEnabled(isChecked);
                        if (!isChecked) {
                            isShowSubChaptersOnProgress.setChecked(isChecked);
                        }
                    }
                });

                isShowChaptersOnProgress.setChecked(AppState.get().isShowChaptersOnProgress);
                isShowChaptersOnProgress.setEnabled(AppState.get().isShowReadingProgress);
                isShowChaptersOnProgress.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowChaptersOnProgress = isChecked;
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        if (isChecked) {
                            isShowReadingProgress.setChecked(true);
                        } else {
                            AppState.get().isShowSubChaptersOnProgress = false;
                            isShowSubChaptersOnProgress.setChecked(false);

                        }

                    }
                });

                isShowSubChaptersOnProgress.setChecked(AppState.get().isShowSubChaptersOnProgress);
                isShowSubChaptersOnProgress.setEnabled(AppState.get().isShowReadingProgress);
                isShowSubChaptersOnProgress.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowSubChaptersOnProgress = isChecked;

                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        if (isChecked) {
                            AppState.get().isShowChaptersOnProgress = true;
                            isShowChaptersOnProgress.setChecked(true);
                            isShowReadingProgress.setChecked(true);
                        }
                    }
                });

                final EditText musicText = inflate.findViewById(R.id.musicText);
                musicText.setText(AppState.get().musicText);
                ((View) musicText.getParent()).setVisibility(AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN ? View.VISIBLE : View.GONE);
                inflate.findViewById(R.id.musicTextOk).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppState.get().musicText = musicText.getText().toString();
                        if (onRefresh != null) {
                            onRefresh.run();
                        }

                    }
                });
                ///

                CheckBox isShowRectangularTapZones = inflate.findViewById(R.id.isShowRectangularTapZones);
                isShowRectangularTapZones.setVisibility(AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN ? View.VISIBLE : View.GONE);
                isShowRectangularTapZones.setChecked(AppState.get().isShowRectangularTapZones);
                isShowRectangularTapZones.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    AppState.get().isShowRectangularTapZones = isChecked;
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                });

                CheckBox isShowLineDividing = inflate.findViewById(R.id.isShowLineDividing);
                isShowLineDividing.setVisibility(AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN ? View.VISIBLE : View.GONE);
                isShowLineDividing.setChecked(AppState.get().isShowLineDividing);
                isShowLineDividing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    AppState.get().isShowLineDividing = isChecked;
                    if (onRefresh != null) {
                        controller.updateRendering();
                        //onRefresh.run();
                    }
                });

                CheckBox isShowLastPageRed = inflate.findViewById(R.id.isShowLastPageRed);
                isShowLastPageRed.setVisibility(AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN ? View.VISIBLE : View.GONE);
                isShowLastPageRed.setChecked(AppState.get().isShowLastPageRed);
                isShowLastPageRed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    AppState.get().isShowLastPageRed = isChecked;
                    if (onRefresh != null) {
                        controller.updateRendering();
                        //onRefresh.run();

                    }
                });

                ///

                CheckBox isRewindEnable = inflate.findViewById(R.id.isRewindEnable);
                isRewindEnable.setChecked(AppState.get().isRewindEnable);
                isRewindEnable.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isRewindEnable = isChecked;
                    }
                });
                // isShowBookmarsPanel
                CheckBox isShowBookmarsPanelInMusicMode = inflate.findViewById(R.id.isShowBookmarsPanelInMusicMode);

                isShowBookmarsPanelInMusicMode.setChecked(AppState.get().isShowBookmarsPanelInMusicMode);
                isShowBookmarsPanelInMusicMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowBookmarsPanelInMusicMode = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                CheckBox isShowBookmarsPanelInScrollMode = inflate.findViewById(R.id.isShowBookmarsPanelInScrollMode);

                isShowBookmarsPanelInScrollMode.setChecked(AppState.get().isShowBookmarsPanelInScrollMode);
                isShowBookmarsPanelInScrollMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowBookmarsPanelInScrollMode = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                CheckBox isShowBookmarsPanelInBookMode = inflate.findViewById(R.id.isShowBookmarsPanelInBookMode);

                isShowBookmarsPanelInBookMode.setChecked(AppState.get().isShowBookmarsPanelInBookMode);
                isShowBookmarsPanelInBookMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowBookmarsPanelInBookMode = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                CheckBox isShowBookmarsPanelText = inflate.findViewById(R.id.isShowBookmarsPanelText);
                isShowBookmarsPanelText.setChecked(AppState.get().isShowBookmarsPanelText);
                isShowBookmarsPanelText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowBookmarsPanelText = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                //

                final CheckBox isShowToolBar = inflate.findViewById(R.id.isShowToolBar);
                isShowToolBar.setChecked(AppState.get().isShowToolBar);
                isShowToolBar.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowToolBar = isChecked;
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                final CheckBox isShowPanelBookName = inflate.findViewById(R.id.isShowPanelBookName);

                if (controller.isBookMode()) {
                    isShowPanelBookName.setEnabled(AppState.get().statusBarPosition != AppState.STATUSBAR_POSITION_TOP);
                }

                isShowPanelBookName.setChecked(controller.isBookMode() ? AppState.get().isShowPanelBookNameBookMode : AppState.get().isShowPanelBookNameScrollMode);
                isShowPanelBookName.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (controller.isBookMode()) {
                            AppState.get().isShowPanelBookNameBookMode = isChecked;
                        } else {
                            AppState.get().isShowPanelBookNameScrollMode = isChecked;

                        }
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                final CheckBox isShowTime = inflate.findViewById(R.id.isShowTime);
                isShowTime.setChecked(AppState.get().isShowTime);
                isShowTime.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowTime = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        if (isChecked) {
                            isShowToolBar.setChecked(true);
                        }
                    }
                });

                final CheckBox isShowBattery = inflate.findViewById(R.id.isShowBattery);
                isShowBattery.setChecked(AppState.get().isShowBattery);
                isShowBattery.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowBattery = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        if (isChecked) {
                            isShowToolBar.setChecked(true);
                        }
                    }
                });
                // position

                // Seek format

                {
                    final List<Integer> modeIds = Arrays.asList(//
                            AppState.STATUSBAR_POSITION_TOP, //
                            AppState.STATUSBAR_POSITION_BOTTOM //
                    );//

                    final List<String> modeStrings = Arrays.asList(//
                            controller.getString(R.string.top), //
                            controller.getString(R.string.bottom) //
                    );//

                    final View statusBarPositionParent = inflate.findViewById(R.id.statusBarPositionParent);
                    final TextView statusBarPosition = inflate.findViewById(R.id.statusBarPosition);
                    statusBarPosition.setText(modeStrings.get(modeIds.indexOf(AppState.get().statusBarPosition)));
                    TxtUtils.underlineTextView(statusBarPosition);

                    statusBarPositionParent.setVisibility(TxtUtils.visibleIf(controller.getActivity() instanceof HorizontalViewActivity));

                    statusBarPosition.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            final MyPopupMenu popupMenu = new MyPopupMenu(v.getContext(), v);
                            for (int i = 0; i < modeStrings.size(); i++) {
                                final int j = i;
                                popupMenu.getMenu().add(modeStrings.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        AppState.get().statusBarPosition = modeIds.get(j);
                                        statusBarPosition.setText(modeStrings.get(modeIds.indexOf(AppState.get().statusBarPosition)));
                                        TxtUtils.underlineTextView(statusBarPosition);

                                        if (item.getTitle().equals(controller.getString(R.string.top))) {
                                            AppState.get().isShowPanelBookNameBookMode = false;
                                            isShowPanelBookName.setChecked(false);
                                            isShowPanelBookName.setEnabled(false);
                                        } else {
                                            isShowPanelBookName.setEnabled(true);

                                        }

                                        if (onRefresh != null) {
                                            onRefresh.run();
                                        }
                                        return false;
                                    }
                                });
                            }

                            popupMenu.show();
                        }
                    });
                }

                // Seek format

                final List<Integer> modeIds = Arrays.asList(//
                        AppState.PAGE_NUMBER_FORMAT_NUMBER, //
                        AppState.PAGE_NUMBER_FORMAT_PERCENT //
                );//

                final List<String> modeStrings = Arrays.asList(//
                        controller.getString(R.string.number), //
                        controller.getString(R.string.percent) //
                );//

                final TextView pageNumberFormat = inflate.findViewById(R.id.pageNumberFormat);
                pageNumberFormat.setText(modeStrings.get(modeIds.indexOf(AppState.get().pageNumberFormat)));
                TxtUtils.underlineTextView(pageNumberFormat);

                pageNumberFormat.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final MyPopupMenu popupMenu = new MyPopupMenu(v.getContext(), v);
                        for (int i = 0; i < modeStrings.size(); i++) {
                            final int j = i;
                            popupMenu.getMenu().add(modeStrings.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().pageNumberFormat = modeIds.get(j);
                                    pageNumberFormat.setText(modeStrings.get(modeIds.indexOf(AppState.get().pageNumberFormat)));
                                    TxtUtils.underlineTextView(pageNumberFormat);
                                    if (onRefresh != null) {
                                        onRefresh.run();
                                    }
                                    return false;
                                }
                            });
                        }

                        popupMenu.show();
                    }
                });

                // Chpater format

                final TextView chapterFormat = inflate.findViewById(R.id.chapterFormat);
                chapterFormat.setText(OutlineHelper.CHAPTER_STRINGS.get(OutlineHelper.CHAPTER_FORMATS.indexOf(AppState.get().chapterFormat)));
                TxtUtils.underlineTextView(chapterFormat);

                chapterFormat.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final MyPopupMenu popupMenu = new MyPopupMenu(v.getContext(), v);
                        for (int i = 0; i < OutlineHelper.CHAPTER_STRINGS.size(); i++) {
                            final int j = i;
                            popupMenu.getMenu().add(OutlineHelper.CHAPTER_STRINGS.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().chapterFormat = OutlineHelper.CHAPTER_FORMATS.get(j);
                                    chapterFormat.setText(OutlineHelper.CHAPTER_STRINGS.get(OutlineHelper.CHAPTER_FORMATS.indexOf(AppState.get().chapterFormat)));
                                    TxtUtils.underlineTextView(chapterFormat);
                                    if (onRefresh != null) {
                                        onRefresh.run();
                                    }
                                    return false;
                                }
                            });
                        }

                        popupMenu.show();
                    }
                });

                /// asd

                final CustomSeek statusBarTextSize = inflate.findViewById(R.id.statusBarTextSize);
                statusBarTextSize.setTitleTextWidth(Dips.dpToPx(100));

                statusBarTextSize.init(5, 30, controller.isBookMode() ? AppState.get().statusBarTextSizeEasy : AppState.get().statusBarTextSizeAdv);
                statusBarTextSize.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        isShowToolBar.setChecked(true);
                        if (controller.isBookMode()) {
                            AppState.get().statusBarTextSizeEasy = result;
                        } else {
                            AppState.get().statusBarTextSizeAdv = result;
                        }
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        return false;
                    }
                });

                final CustomSeek progressLineHeight = inflate.findViewById(R.id.progressLineHeight);
                progressLineHeight.setTitleTextWidth(Dips.dpToPx(100));
                progressLineHeight.init(0, 10, AppState.get().progressLineHeight);
                progressLineHeight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        isShowReadingProgress.setChecked(true);
                        AppState.get().progressLineHeight = result;
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        return false;
                    }
                });

                final CustomColorView statusBarColorDay = inflate.findViewById(R.id.statusBarColorDay);
                statusBarColorDay.withDefaultColors(StringDB.converToColor(AppState.get().statusBarColorDays, AppState.get().tintColor));
                statusBarColorDay.init(AppState.get().statusBarColorDay);
                statusBarColorDay.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        isShowToolBar.setChecked(true);
                        AppState.get().statusBarColorDay = Color.parseColor(string);
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        Keyboards.hideNavigation(controller.getActivity());
                        return false;
                    }
                });

                statusBarColorDay.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        Dialogs.showEditDialog(v.getContext(), false, statusBarColorDay.getTextString(), AppState.get().statusBarColorDays, new ResultResponse<String>() {

                            @Override
                            public boolean onResultRecive(String result) {
                                AppState.get().statusBarColorDays = result;
                                statusBarColorDay.withDefaultColors(StringDB.converToColor(AppState.get().statusBarColorDays, AppState.get().tintColor));
                                return true;
                            }
                        });
                        return true;
                    }
                });

                final CustomColorView statusBarColorNight = inflate.findViewById(R.id.statusBarColorNight);
                statusBarColorNight.withDefaultColors(StringDB.converToColor(AppState.get().statusBarColorNights, AppState.get().tintColor));
                statusBarColorNight.init(AppState.get().statusBarColorNight);
                statusBarColorNight.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        isShowToolBar.setChecked(true);
                        AppState.get().statusBarColorNight = Color.parseColor(string);
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        Keyboards.hideNavigation(controller.getActivity());
                        return false;
                    }
                });

                statusBarColorNight.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        Dialogs.showEditDialog(v.getContext(), false, statusBarColorNight.getTextString(), AppState.get().statusBarColorNights, new ResultResponse<String>() {

                            @Override
                            public boolean onResultRecive(String result) {
                                AppState.get().statusBarColorNights = result;
                                statusBarColorNight.withDefaultColors(StringDB.converToColor(AppState.get().statusBarColorNights, AppState.get().tintColor));
                                return true;
                            }
                        });
                        return true;
                    }
                });

//                statusBarColorDay.getText1().getLayoutParams().width = Dips.dpToPx(150);
//                statusBarColorNight.getText1().getLayoutParams().width = Dips.dpToPx(150);

                return inflate;
            }
        };

        dialog.show(DragingPopup.PREF + "_statusBarSettings");

        return dialog;
    }

    public static DragingPopup performanceSettings(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {
        AppProfile.save(controller.getActivity());

        final int initHash = Objects.appHash();

        DragingPopup dialog = new DragingPopup(R.string.advanced_settings, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public void beforeCreate() {
                titleAction = controller.getString(R.string.preferences);
                titleRunnable = new Runnable() {

                    @Override
                    public void run() {
                        if (initHash != Objects.appHash()) {
                            AlertDialogs.showDialog(controller.getActivity(), controller.getString(R.string.you_neet_to_apply_the_new_settings), controller.getString(R.string.apply), new Runnable() {

                                @Override
                                public void run() {
                                    closeDialog();
                                }
                            });

                        } else {
                            preferences(anchor, controller, onRefresh, updateUIRefresh);
                        }
                    }
                };
            }

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View inflate = inflater.inflate(R.layout.dialog_adv_preferences, null, false);

                CheckBox isLoopAutoplay = inflate.findViewById(R.id.isLoopAutoplay);
                isLoopAutoplay.setChecked(AppState.get().isLoopAutoplay);
                // isLoopAutoplay.setVisibility(AppSP.get().readingMode ==
                // AppState.READEING_MODE_BOOK ?
                // View.GONE : View.VISIBLE);
                isLoopAutoplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isLoopAutoplay = isChecked;
                    }
                });

                CheckBox isScrollSpeedByVolumeKeys = inflate.findViewById(R.id.isScrollSpeedByVolumeKeys);
                isScrollSpeedByVolumeKeys.setChecked(AppState.get().isScrollSpeedByVolumeKeys);
                isScrollSpeedByVolumeKeys.setVisibility(View.GONE);
                isScrollSpeedByVolumeKeys.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isScrollSpeedByVolumeKeys = isChecked;
                    }
                });

                CheckBox isBrighrnessEnable = inflate.findViewById(R.id.isBrighrnessEnable);
                isBrighrnessEnable.setChecked(AppState.get().isBrighrnessEnable);
                isBrighrnessEnable.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isBrighrnessEnable = isChecked;
                    }
                });

                CheckBox isAllowMinBrigthness = inflate.findViewById(R.id.isAllowMinBrigthness);
                isAllowMinBrigthness.setChecked(AppState.get().isAllowMinBrigthness);
                isAllowMinBrigthness.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isAllowMinBrigthness = isChecked;
                        BrightnessHelper.applyBrigtness(controller.getActivity());
                    }
                });

                CheckBox isShowLongBackDialog = inflate.findViewById(R.id.isShowLongBackDialog);
                isShowLongBackDialog.setChecked(AppState.get().isShowLongBackDialog);
                isShowLongBackDialog.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowLongBackDialog = isChecked;
                    }
                });

                CheckBox highlightByLetters = inflate.findViewById(R.id.highlightByLetters);
                highlightByLetters.setChecked(AppState.get().selectingByLetters);
                highlightByLetters.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().selectingByLetters = isChecked;
                    }
                });

                CheckBox isSelectTexByTouch = inflate.findViewById(R.id.isSelectTexByTouch);
                isSelectTexByTouch.setChecked(AppState.get().isSelectTexByTouch);
                isSelectTexByTouch.setVisibility(TxtUtils.visibleIf(controller.isBookMode()));
                isSelectTexByTouch.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isSelectTexByTouch = isChecked);

                CheckBox isAllowTextSelection = inflate.findViewById(R.id.isAllowTextSelection);
                isAllowTextSelection.setChecked(AppState.get().isAllowTextSelection);
                isAllowTextSelection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    AppState.get().isAllowTextSelection = isChecked;
                    if (!isChecked) {
                        AppState.get().selectingByLetters = false;
                        AppState.get().isSelectTexByTouch = false;
                        isSelectTexByTouch.setChecked(false);
                        highlightByLetters.setChecked(false);
                    }

                    highlightByLetters.setEnabled(isChecked);
                    isSelectTexByTouch.setEnabled(isChecked);

                });
                highlightByLetters.setEnabled(AppState.get().isAllowTextSelection);
                isSelectTexByTouch.setEnabled(AppState.get().isAllowTextSelection);

                CheckBox isZoomInOutWithVolueKeys = inflate.findViewById(R.id.isZoomInOutWithVolueKeys);
                isZoomInOutWithVolueKeys.setChecked(AppState.get().isZoomInOutWithVolueKeys);
                isZoomInOutWithVolueKeys.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isZoomInOutWithVolueKeys = isChecked);

                CheckBox isZoomInOutWithLock = inflate.findViewById(R.id.isZoomInOutWithLock);
                isZoomInOutWithLock.setChecked(AppState.get().isZoomInOutWithLock);
                isZoomInOutWithLock.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isZoomInOutWithLock = isChecked);

                final CustomSeek mouseWheelSpeed = inflate.findViewById(R.id.seekWheelSpeed);
                mouseWheelSpeed.getTitleText().setSingleLine(false);
                mouseWheelSpeed.init(1, 200, AppState.get().mouseWheelSpeed);
                mouseWheelSpeed.setOnSeekChanged(result -> {
                    AppState.get().mouseWheelSpeed = result;
                    return false;
                });

                CheckBox isScrollAnimation = inflate.findViewById(R.id.isScrollAnimation);
                isScrollAnimation.setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);
                isScrollAnimation.setChecked(AppState.get().isScrollAnimation);
                isScrollAnimation.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isScrollAnimation = isChecked;
                    }
                });

                CheckBox isDisableSwipe = inflate.findViewById(R.id.isEnableVerticalSwipe);
                isDisableSwipe.setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);
                isDisableSwipe.setChecked(AppState.get().isEnableVerticalSwipe);
                isDisableSwipe.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isEnableVerticalSwipe = isChecked;
                    }
                });

                final ImageView isSwipeGestureReverse = inflate.findViewById(R.id.isSwipeGestureReverse);
                isSwipeGestureReverse.setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);
                isSwipeGestureReverse.setImageResource(AppState.get().isSwipeGestureReverse ? R.drawable.glyphicons_212_arrow_up : R.drawable.glyphicons_211_arrow_down);
                isSwipeGestureReverse.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppState.get().isSwipeGestureReverse = !AppState.get().isSwipeGestureReverse;
                        isSwipeGestureReverse.setImageResource(AppState.get().isSwipeGestureReverse ? R.drawable.glyphicons_212_arrow_up : R.drawable.glyphicons_211_arrow_down);
                    }
                });

                CheckBox isEnableHorizontalSwipe = inflate.findViewById(R.id.isEnableHorizontalSwipe);
                isEnableHorizontalSwipe.setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);
                isEnableHorizontalSwipe.setChecked(AppState.get().isEnableHorizontalSwipe);
                isEnableHorizontalSwipe.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isEnableHorizontalSwipe = isChecked;
                    }
                });

                CheckBox isVibration = inflate.findViewById(R.id.isVibration);
                isVibration.setChecked(AppState.get().isVibration);
                isVibration.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isVibration = isChecked;
                    }
                });
                CheckBox isExperimental = inflate.findViewById(R.id.isExperimental);
                isExperimental.setVisibility(TxtUtils.visibleIf(BookType.EPUB.is(controller.getCurrentBook().getPath())));
                isExperimental.setText(isExperimental.getText() + " (SVG, MathML)");
                isExperimental.setChecked(AppState.get().isExperimental);
                isExperimental.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isExperimental = isChecked;
                    }
                });

                CheckBox isReferenceMode = inflate.findViewById(R.id.isReferenceMode);
                isReferenceMode.setVisibility(TxtUtils.visibleIf(BookType.EPUB.is(controller.getCurrentBook().getPath())));
                isReferenceMode.setChecked(AppState.get().isReferenceMode);
                isReferenceMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isReferenceMode = isChecked;
                        BookCSS.get().isAutoHypens = true;
                    }
                });
                CheckBox isShowPageNumbers = inflate.findViewById(R.id.isShowPageNumbers);
                isShowPageNumbers.setVisibility(TxtUtils.visibleIf(BookType.EPUB.is(controller.getCurrentBook().getPath())));
                isShowPageNumbers.setChecked(AppState.get().isShowPageNumbers);
                isShowPageNumbers.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowPageNumbers = isChecked;
                        BookCSS.get().isAutoHypens = true;
                    }
                });


                CheckBox isOLED = inflate.findViewById(R.id.isOLED);
                isOLED.setChecked(AppState.get().isOLED);
                isOLED.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isOLED = isChecked;
                    }
                });

                CheckBox isLockPDF = inflate.findViewById(R.id.isLockPDF);
                isLockPDF.setChecked(AppState.get().isLockPDF);
                isLockPDF.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isLockPDF.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isLockPDF = isChecked;
                    }
                });

                CheckBox alwaysTwoPages = inflate.findViewById(R.id.alwaysTwoPages);
                alwaysTwoPages.setVisibility(TxtUtils.visibleIf(AppSP.get().readingMode == AppState.READING_MODE_BOOK));
                alwaysTwoPages.setChecked(AppState.get().alwaysTwoPages);
                alwaysTwoPages.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().alwaysTwoPages = isChecked;
                    }
                });

                CheckBox isMirrorImage = inflate.findViewById(R.id.isMirrorImage);
                isMirrorImage.setChecked(AppState.get().isMirrorImage);
                isMirrorImage.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isMirrorImage = isChecked;
                    }
                });

                CheckBox isBionicMode = inflate.findViewById(R.id.isBionicMode);
                isBionicMode.setChecked(AppState.get().isBionicMode);
                isBionicMode.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().isBionicMode = isChecked);
                isBionicMode.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);

                String txt = TxtUtils.toBionicText(isBionicMode.getText().toString());
                isBionicMode.setText(TxtUtils.fromHtml(txt));

                CheckBox isCropPDF = inflate.findViewById(R.id.isCropPDF);
                isCropPDF.setChecked(AppState.get().isCropPDF);
                isCropPDF.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isCropPDF.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isCropPDF = isChecked;
                    }
                });

                CheckBox isCustomizeBgAndColors = inflate.findViewById(R.id.isCustomizeBgAndColors);
                isCustomizeBgAndColors.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isCustomizeBgAndColors.setChecked(AppState.get().isCustomizeBgAndColors);
                isCustomizeBgAndColors.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isCustomizeBgAndColors = isChecked;
                        if (!isChecked) {
                            AppState.get().colorDayText = AppState.COLOR_BLACK;
                            AppState.get().colorDayBg = AppState.COLOR_WHITE;

                            AppState.get().colorNigthText = AppState.COLOR_WHITE;
                            AppState.get().colorNigthBg = AppState.COLOR_BLACK;
                        }
                    }
                });
                final CheckBox isReplaceWhite = inflate.findViewById(R.id.isReplaceWhite);
                isReplaceWhite.setChecked(AppState.get().isReplaceWhite);
                isReplaceWhite.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);

                isReplaceWhite.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isReplaceWhite = isChecked;

                    }
                });

                CheckBox isIgnoreAnnotatations = inflate.findViewById(R.id.isIgnoreAnnotatations);
                isIgnoreAnnotatations.setChecked(AppState.get().isIgnoreAnnotatations);
                isIgnoreAnnotatations.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isIgnoreAnnotatations = isChecked;

                    }
                });

                isIgnoreAnnotatations.setVisibility(!(AppSP.get().readingMode == AppState.READING_MODE_BOOK) && BookType.PDF.is(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                CheckBox isSaveAnnotatationsAutomatically = inflate.findViewById(R.id.isSaveAnnotatationsAutomatically);
                isSaveAnnotatationsAutomatically.setChecked(AppState.get().isSaveAnnotatationsAutomatically);
                isSaveAnnotatationsAutomatically.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isSaveAnnotatationsAutomatically = isChecked;

                    }
                });

                isSaveAnnotatationsAutomatically.setVisibility(!(AppSP.get().readingMode == AppState.READING_MODE_BOOK) && BookType.PDF.is(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                CheckBox isCutRTL = inflate.findViewById(R.id.isCutRTL);
                isCutRTL.setChecked(AppState.get().isCutRTL);
                isCutRTL.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isCutRTL = isChecked;
                    }
                });

                final TextView pageQuality = inflate.findViewById(R.id.pageQuality);
                ((ViewGroup) pageQuality.getParent()).setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK && !ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);
                pageQuality.setText((int) (AppState.get().pageQuality * 100) + "%");
                TxtUtils.underlineTextView(pageQuality);
                pageQuality.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        for (float i = 0.5f; i < 2.1f; i += 0.1f) {
                            final float quality = i;
                            popupMenu.getMenu().add((int) (i * 100) + "%").setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().pageQuality = quality;
                                    pageQuality.setText((int) (AppState.get().pageQuality * 100) + "%");
                                    TxtUtils.underlineTextView(pageQuality);
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });

                final TextView pagesInMemory = inflate.findViewById(R.id.pagesInMemory);
                ((ViewGroup) pagesInMemory.getParent()).setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);

                pagesInMemory.setText("" + AppState.get().pagesInMemory);
                TxtUtils.underlineTextView(pagesInMemory);
                pagesInMemory.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                        popupMenu.getMenu().add("" + 1).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().pagesInMemory = 1;
                                pagesInMemory.setText("" + AppState.get().pagesInMemory);
                                TxtUtils.underlineTextView(pagesInMemory);
                                return false;
                            }
                        });

                        popupMenu.getMenu().add("" + 3).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().pagesInMemory = 3;
                                pagesInMemory.setText("" + AppState.get().pagesInMemory);
                                TxtUtils.underlineTextView(pagesInMemory);
                                return false;
                            }
                        });

                        popupMenu.getMenu().add("" + 5).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().pagesInMemory = 5;
                                pagesInMemory.setText("" + AppState.get().pagesInMemory);
                                TxtUtils.underlineTextView(pagesInMemory);
                                return false;
                            }
                        });

                        popupMenu.show();

                    }
                });

                ///
                final TextView inactivityTime = inflate.findViewById(R.id.inactivityTime);
                inactivityTime.setText(AppState.get().inactivityTime == -1 ? controller.getString(R.string.system) : "" + AppState.get().inactivityTime);
                TxtUtils.underlineTextView(inactivityTime);
                inactivityTime.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                        popupMenu.getMenu().add("" + controller.getString(R.string.system)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().inactivityTime = -1;
                                controller.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                inactivityTime.setText(R.string.system);
                                TxtUtils.underlineTextView(inactivityTime);
                                LOG.d("FLAG clearFlags", "FLAG_KEEP_SCREEN_ON", "clear", AppState.get().inactivityTime);

                                return false;
                            }
                        });

                        List<Integer> times = Arrays.asList(1, 2, 3, 4, 5, 10, 20, 30, 60);
                        for (int i : times) {
                            final int number = i;
                            popupMenu.getMenu().add("" + i).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    controller.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                                    AppState.get().inactivityTime = number;
                                    inactivityTime.setText("" + AppState.get().inactivityTime);
                                    TxtUtils.underlineTextView(inactivityTime);
                                    LOG.d("FLAG clearFlags", "FLAG_KEEP_SCREEN_ON", "add", AppState.get().inactivityTime);

                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });

                ///

                final TextView rotate = inflate.findViewById(R.id.rotate);
                ((ViewGroup) rotate.getParent()).setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);

                rotate.setText("" + AppState.get().rotate + "°");
                TxtUtils.underlineTextView(rotate);
                rotate.setOnClickListener(new OnClickListener() {

                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        MenuBuilderM.addRotateMenu(rotate, null, new Runnable() {

                            @Override
                            public void run() {
                                rotate.setText("" + AppState.get().rotate + "°");
                                TxtUtils.underlineTextView(rotate);
                            }
                        }).show();
                    }
                });
                {
                    final TextView tapzoneSize = inflate.findViewById(R.id.tapzoneSize);
                    tapzoneSize.setText("" + AppState.get().tapzoneSize + "%");
                    TxtUtils.underlineTextView(tapzoneSize);
                    tapzoneSize.setOnClickListener(new OnClickListener() {

                        @SuppressLint("NewApi")
                        @Override
                        public void onClick(View v) {
                            final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                            for (int i = 0; i <= 45f; i += 5) {
                                final int number = i;
                                popupMenu.getMenu().add("" + i + "%").setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        AppState.get().tapzoneSize = number;
                                        tapzoneSize.setText("" + AppState.get().tapzoneSize + "%");
                                        TxtUtils.underlineTextView(tapzoneSize);
                                        return false;
                                    }
                                });
                            }
                            popupMenu.show();
                        }
                    });
                }
                // double tap
                {
                    final TextView antiAliasLevel = inflate.findViewById(R.id.antiAliasLevel);
                    antiAliasLevel.setText("" + AppState.get().antiAliasLevel);
                    TxtUtils.underlineTextView(antiAliasLevel);
                    antiAliasLevel.setOnClickListener(new OnClickListener() {

                        @SuppressLint("NewApi")
                        @Override
                        public void onClick(View v) {
                            final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                            for (int i = 1; i <= 8; i += 1) {
                                final int number = i;
                                popupMenu.getMenu().add("" + i).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        AppState.get().antiAliasLevel = number;
                                        antiAliasLevel.setText("" + AppState.get().antiAliasLevel);
                                        TxtUtils.underlineTextView(antiAliasLevel);
                                        return false;
                                    }
                                });
                            }
                            popupMenu.show();
                        }
                    });
                }

                final List<String> doubleTapNames = Arrays.asList(//
                        controller.getString(R.string.db_auto_scroll), //
                        controller.getString(R.string.db_auto_alignemnt), //
                        controller.getString(R.string.db_auto_center_horizontally), //
                        controller.getString(R.string.zoom_in_zoom_out), //
                        controller.getString(R.string.close_book), //
                        controller.getString(R.string.close_book_and_application), //
                        controller.getString(R.string.hide_app), //
                        controller.getString(R.string.db_do_nothing), //
                        controller.getString(R.string.read_out_loud_with_tts) //

                );

                final List<Integer> doubleTapIDS = Arrays.asList(//
                        AppState.DOUBLE_CLICK_AUTOSCROLL, //
                        AppState.DOUBLE_CLICK_ADJUST_PAGE, //
                        AppState.DOUBLE_CLICK_CENTER_HORIZONTAL, //
                        AppState.DOUBLE_CLICK_ZOOM_IN_OUT, //
                        AppState.DOUBLE_CLICK_CLOSE_BOOK, //
                        AppState.DOUBLE_CLICK_CLOSE_BOOK_AND_APP, //
                        AppState.DOUBLE_CLICK_CLOSE_HIDE_APP, //
                        AppState.DOUBLE_CLICK_NOTHING, //
                        AppState.DOUBLE_CLICK_START_STOP_TTS //
                );//
                final TextView doubleClickAction1 = inflate.findViewById(R.id.doubleTapValue);
                doubleClickAction1.setText(doubleTapNames.get(doubleTapIDS.indexOf(AppState.get().doubleClickAction1)));
                TxtUtils.underlineTextView(doubleClickAction1);

                doubleClickAction1.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        MyPopupMenu popup = new MyPopupMenu(controller.getActivity(), v);
                        for (int i = 0; i < doubleTapNames.size(); i++) {
                            final int j = i;
                            final String fontName = doubleTapNames.get(i);
                            popup.getMenu().add(fontName).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().doubleClickAction1 = doubleTapIDS.get(j);
                                    doubleClickAction1.setText(doubleTapNames.get(doubleTapIDS.indexOf(AppState.get().doubleClickAction1)));
                                    TxtUtils.underlineTextView(doubleClickAction1);
                                    return false;
                                }
                            });
                        }
                        popup.show();

                    }
                });

                final TextView tapzoneCustomize = inflate.findViewById(R.id.tapzoneCustomize);
                TxtUtils.underlineTextView(tapzoneCustomize);
                tapzoneCustomize.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        TapZoneDialog.show(controller.getActivity());
                    }
                });

                final TextView allocatedMemorySize = inflate.findViewById(R.id.allocatedMemorySize);
                allocatedMemorySize.setText("" + AppState.get().allocatedMemorySize + "Mb");
                TxtUtils.underlineTextView(allocatedMemorySize);
                allocatedMemorySize.setOnClickListener(new OnClickListener() {

                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        for (int i = 16; i <= 512 && i < MemoryUtils.MAX_MEMORY_SIZE; i += i) {
                            final int number = i;
                            popupMenu.getMenu().add("" + i + "Mb").setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().allocatedMemorySize = number;
                                    allocatedMemorySize.setText("" + AppState.get().allocatedMemorySize + "Mb");
                                    TxtUtils.underlineTextView(allocatedMemorySize);
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();
                    }
                });

                // remind rest time
                final TextView remindRestTime = inflate.findViewById(R.id.remindRestTime);
                final String minutesString = controller.getString(R.string.minutes).toLowerCase(Locale.US);
                if (AppState.get().remindRestTime == -1) {
                    remindRestTime.setText(R.string.never);
                } else {
                    remindRestTime.setText(AppState.get().remindRestTime + " " + minutesString);
                }
                TxtUtils.underlineTextView(remindRestTime);
                remindRestTime.setOnClickListener(new OnClickListener() {

                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                        popupMenu.getMenu().add(R.string.never).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().remindRestTime = -1;
                                remindRestTime.setText(R.string.never);
                                TxtUtils.underlineTextView(remindRestTime);
                                return false;
                            }
                        });

                        for (int i = 10; i <= 240; i += 10) {
                            final int j = i;
                            popupMenu.getMenu().add(i + " " + minutesString).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().remindRestTime = j;
                                    remindRestTime.setText(AppState.get().remindRestTime + " " + minutesString);
                                    TxtUtils.underlineTextView(remindRestTime);
                                    return false;
                                }
                            });
                        }

                        popupMenu.show();

                    }
                });

                // rotate

                final TextView rotateViewPager = inflate.findViewById(R.id.rotateViewPager);
                ((ViewGroup) rotateViewPager.getParent()).setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);
                rotateViewPager.setText(AppState.get().rotateViewPager == 0 ? R.string.horizontal : R.string.vertical);
                TxtUtils.underlineTextView(rotateViewPager);
                rotateViewPager.setOnClickListener(new OnClickListener() {

                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        popupMenu.getMenu().add(R.string.horizontal).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().rotateViewPager = 0;
                                rotateViewPager.setText(AppState.get().rotateViewPager == 0 ? R.string.horizontal : R.string.vertical);
                                TxtUtils.underlineTextView(rotateViewPager);
                                return false;
                            }
                        });
                        popupMenu.getMenu().add(R.string.vertical).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().rotateViewPager = 90;
                                rotateViewPager.setText(AppState.get().rotateViewPager == 0 ? R.string.horizontal : R.string.vertical);
                                TxtUtils.underlineTextView(rotateViewPager);
                                return false;
                            }
                        });
                        popupMenu.show();

                    }
                });
                // rtl
                final TextView rtlText = inflate.findViewById(R.id.rtlText);
                ((ViewGroup) rtlText.getParent()).setVisibility(AppSP.get().readingMode == AppState.READING_MODE_BOOK ? View.VISIBLE : View.GONE);
                if (AppState.get().isRTL) {
                    rtlText.setText(R.string.right_to_left);
                } else {
                    rtlText.setText(R.string.left_to_rigth);
                }
                TxtUtils.underlineTextView(rtlText);
                rtlText.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        popupMenu.getMenu().add(R.string.left_to_rigth).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().isRTL = false;
                                rtlText.setText(R.string.left_to_rigth);
                                TxtUtils.underlineTextView(rtlText);

                                AppState.get().tapZoneTop = AppState.TAP_PREV_PAGE;
                                AppState.get().tapZoneBottom = AppState.TAP_NEXT_PAGE;
                                AppState.get().tapZoneLeft = AppState.TAP_PREV_PAGE;
                                AppState.get().tapZoneRight = AppState.TAP_NEXT_PAGE;

                                return false;
                            }
                        });
                        popupMenu.getMenu().add(R.string.right_to_left).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().isRTL = true;
                                rtlText.setText(R.string.right_to_left);
                                TxtUtils.underlineTextView(rtlText);

                                AppState.get().tapZoneTop = AppState.TAP_PREV_PAGE;
                                AppState.get().tapZoneBottom = AppState.TAP_NEXT_PAGE;
                                AppState.get().tapZoneLeft = AppState.TAP_NEXT_PAGE;
                                AppState.get().tapZoneRight = AppState.TAP_PREV_PAGE;

                                return false;
                            }
                        });
                        popupMenu.show();
                    }
                });

                // BETA

                final CustomSeek decodingThreadPriority = inflate.findViewById(R.id.decodingThreadPriority);
                decodingThreadPriority.init(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, CoreSettings.get().decodingThreadPriority);
                decodingThreadPriority.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        CoreSettings.get().decodingThreadPriority = result;
                        AppState.get().helpHash++;
                        return false;
                    }
                });
                Views.visibleInBeta(decodingThreadPriority);

                final CustomSeek drawThreadPriority = inflate.findViewById(R.id.drawThreadPriority);
                drawThreadPriority.init(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, CoreSettings.get().drawThreadPriority);
                drawThreadPriority.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        CoreSettings.get().drawThreadPriority = result;
                        AppState.get().helpHash++;
                        return false;
                    }
                });
                Views.visibleInBeta(drawThreadPriority);

                return inflate;
            }
        };
        dialog.show(DragingPopup.PREF + "_performanceSettings");
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                if (initHash != Objects.appHash()) {
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    controller.restartActivity();
                }
            }
        });
        return dialog;
    }

    public static DragingPopup moreBookSettings(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {
        final int initHash = Objects.appHash();

        DragingPopup dialog = new DragingPopup(R.string.reading_settings, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public void beforeCreate() {
                titleAction = controller.getString(R.string.preferences);
                titleRunnable = new Runnable() {

                    @Override
                    public void run() {
                        if (initHash != Objects.appHash()) {
                            AlertDialogs.showDialog(controller.getActivity(), controller.getString(R.string.you_neet_to_apply_the_new_settings), controller.getString(R.string.apply), new Runnable() {

                                @Override
                                public void run() {
                                    closeDialog();
                                }
                            });

                        } else {
                            preferences(anchor, controller, onRefresh, updateUIRefresh);
                        }
                    }
                };
            }

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View inflate = inflater.inflate(R.layout.dialog_reading_pref, null, false);

                final CustomSeek fontWeight = inflate.findViewById(R.id.fontWeight);
                fontWeight.init(1, 9, BookCSS.get().fontWeight / 100);
                fontWeight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        fontWeight.setValueText("" + (result * 100));
                        BookCSS.get().fontWeight = result * 100;
                        return false;
                    }
                });
                fontWeight.setValueText("" + BookCSS.get().fontWeight);

// begin styles
                final List<String> docStyles = Arrays.asList(//
                        controller.getString(R.string.document_styles) + " + " + controller.getString(R.string.user_styles), //
                        controller.getString(R.string.document_styles),
                        controller.getString(R.string.user_styles)
                );

                final TextView docStyle = inflate.findViewById(R.id.documentStyle);

                docStyle.setText(docStyles.get(BookCSS.get().documentStyle));
                TxtUtils.underlineTextView(docStyle);

                inflate.findViewById(R.id.documentStyleLayout).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                docStyle.setOnClickListener(new OnClickListener() {

                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        for (int i = 0; i < docStyles.size(); i++) {
                            String type = docStyles.get(i);
                            final int j = i;

                            popupMenu.getMenu().add(type).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    BookCSS.get().documentStyle = j;
                                    docStyle.setText(docStyles.get(BookCSS.get().documentStyle));
                                    TxtUtils.underlineTextView(docStyle);

                                    return false;
                                }
                            });
                        }

                        popupMenu.show();

                    }
                });
                //user styles
                inflate.findViewById(R.id.userStyles).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);
                final TextView userStyleCss = inflate.findViewById(R.id.userStyleCss);
                userStyleCss.setText(BookCSS.get().userStyleCss);
                TxtUtils.underlineTextView(userStyleCss);

                userStyleCss.setOnClickListener(v -> {
                    final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    File rootFiles = AppProfile.SYNC_FOLDER_DEVICE_PROFILE;

                    for (File file : rootFiles.listFiles()) {
                        String name = file.getName();
                        if (name.endsWith(".css")) {
                            popupMenu.getMenu().add(name).setOnMenuItemClickListener(item -> {
                                BookCSS.get().userStyleCss = name;
                                userStyleCss.setText(BookCSS.get().userStyleCss);
                                TxtUtils.underlineTextView(userStyleCss);

                                return false;
                            });
                        }
                    }

                    popupMenu.show();
                });

                // end styles

                // hypens
                boolean isSupportHypens = controller.isTextFormat();

                CheckBox isAutoHypens = inflate.findViewById(R.id.isAutoHypens);
                isAutoHypens.setVisibility(isSupportHypens ? View.VISIBLE : View.GONE);

                isAutoHypens.setChecked(BookCSS.get().isAutoHypens);
                isAutoHypens.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        BookCSS.get().isAutoHypens = isChecked;
                    }
                });

                final TextView hypenLangLabel = inflate.findViewById(R.id.hypenLangLabel);

                final TextView hypenLang = inflate.findViewById(R.id.hypenLang);

                hypenLang.setVisibility(isSupportHypens ? View.VISIBLE : View.GONE);
                hypenLangLabel.setVisibility(isSupportHypens ? View.VISIBLE : View.GONE);

                // hypenLang.setVisibility(View.GONE);
                // hypenLangLabel.setVisibility(View.GONE);

                String lanuageByCode = DialogTranslateFromTo.getLanuageByCode(AppSP.get().hypenLang);
                if (TxtUtils.isEmpty(lanuageByCode)) {
                    hypenLang.setText(R.string.choose_);
                } else {
                    hypenLang.setText(lanuageByCode);
                }

                TxtUtils.underlineTextView(hypenLang);

                hypenLang.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                        HyphenPattern[] values = HyphenPattern.values();

                        List<String> all = new ArrayList<String>();

                        for (HyphenPattern p : values) {
                            String e = DialogTranslateFromTo.getLanuageByCode(p.lang) + ":" + p.lang;
                            all.add(e);

                        }
                        Collections.sort(all);

                        for (final String langFull : all) {
                            String[] split = langFull.split(":");
                            final String titleLang = split[0];
                            final String code = split[1];
                            popupMenu.getMenu().add(titleLang).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppSP.get().hypenLang = code;
                                    hypenLang.setText(titleLang);
                                    TxtUtils.underlineTextView(hypenLang);
                                    FileMeta load = AppDB.get().load(controller.getCurrentBook().getPath());
                                    if (load != null) {
                                        load.setLang(code);
                                        AppDB.get().update(load);
                                    }
                                    final AppBook load1 = SharedBooks.load(load.getPath());
                                    if (load1 != null) {
                                        load.setLang(code);
                                        SharedBooks.save(load1);
                                    }
                                    if (AppState.get().isDefaultHyphenLanguage) {
                                        AppState.get().defaultHyphenLanguageCode = code;
                                    }

                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });

                CheckBox isDefaultHyphenLanguage = inflate.findViewById(R.id.isDefaultHyphenLanguage);
                isDefaultHyphenLanguage.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);
                isDefaultHyphenLanguage.setChecked(AppState.get().isDefaultHyphenLanguage);
                isDefaultHyphenLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    AppState.get().isDefaultHyphenLanguage = isChecked;
                    if (isChecked) {
                        AppState.get().defaultHyphenLanguageCode = AppSP.get().hypenLang;
                    }
                });

                // - hypens
                //
                CheckBox isAccurateFontSize = inflate.findViewById(R.id.isAccurateFontSize);
                isAccurateFontSize.setVisibility(View.GONE);
                isAccurateFontSize.setChecked(AppState.get().isAccurateFontSize);
                isAccurateFontSize.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isAccurateFontSize = isChecked;
                    }
                });

                CheckBox isShowFooterNotesInText = inflate.findViewById(R.id.isShowFooterNotesInText);
                isShowFooterNotesInText.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);
                isShowFooterNotesInText.setChecked(AppState.get().isShowFooterNotesInText);
                isShowFooterNotesInText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowFooterNotesInText = isChecked;
                    }
                });

                View customCSS = inflate.findViewById(R.id.customCSS);
                // TxtUtils.underlineTextView(customCSS);
                customCSS.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle(R.string.custom_css);
                        final EditText edit = new EditText(v.getContext());
                        edit.setMinWidth(Dips.dpToPx(1000));
                        edit.setLines(8);
                        edit.setGravity(Gravity.TOP);
                        edit.setText(BookCSS.get().customCSS2);
                        builder.setView(edit);

                        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, final int id) {
                                BookCSS.get().customCSS2 = edit.getText().toString();
                                AppProfile.save(v.getContext());
                            }
                        });
                        builder.show();

                    }
                });


                final CustomSeek imageScale = inflate.findViewById(R.id.imageScale);
                imageScale.setFloatResult(true);
                imageScale.init(1, 50, (int) (BookCSS.get().imageScale * 10), "x");
                imageScale.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().imageScale = (float) result / 10;
                        return false;
                    }
                });

                CheckBox enableImageScale = inflate.findViewById(R.id.enableImageScale);
                enableImageScale.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);
                enableImageScale.setChecked(AppState.get().enableImageScale);
                imageScale.setEnabled(AppState.get().enableImageScale);
                enableImageScale.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().enableImageScale = isChecked;
                        imageScale.setEnabled(isChecked);
                    }
                });

                final CustomSeek lineHeight = inflate.findViewById(R.id.lineHeight);
                lineHeight.init(10, 30, BookCSS.get().lineHeight12);
                lineHeight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().lineHeight12 = result;
                        return false;
                    }
                });

                final CustomSeek paragraphHeight = inflate.findViewById(R.id.paragraphHeight);
                paragraphHeight.init(0, 20, BookCSS.get().paragraphHeight);
                paragraphHeight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().paragraphHeight = result;
                        return false;
                    }
                });

                final CustomSeek fontParagraph = inflate.findViewById(R.id.fontParagraph);
                fontParagraph.init(0, 30, BookCSS.get().textIndent);
                fontParagraph.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().textIndent = result;
                        return false;
                    }
                });

                final CustomSeek emptyLine = inflate.findViewById(R.id.emptyLine);
                boolean isShow =
                        //
                        BookType.FB2.is(controller.getCurrentBook().getPath()) || //
                                BookType.DOCX.is(controller.getCurrentBook().getPath());
                // BookType.TXT.is(controller.getCurrentBook().getPath());//

                emptyLine.setVisibility(isShow ? View.VISIBLE : View.GONE);
                emptyLine.init(0, 30, BookCSS.get().emptyLine);
                emptyLine.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().emptyLine = result;
                        return false;
                    }
                });

// Margins

                final CustomSeek marginTop = inflate.findViewById(R.id.marginTop);
                int maxMargin = Dips.isLargeOrXLargeScreen() ? 400 : 50;
                marginTop.init(0, maxMargin, BookCSS.get().marginTop);
                marginTop.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginTop = result;
                        return false;
                    }
                });

                final CustomSeek marginBottom = inflate.findViewById(R.id.marginBottom);
                marginBottom.init(0, maxMargin, BookCSS.get().marginBottom);
                marginBottom.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginBottom = result;
                        return false;
                    }
                });

                final CustomSeek marginLeft = inflate.findViewById(R.id.marginLeft);
                marginLeft.init(0, maxMargin, BookCSS.get().marginLeft);
                marginLeft.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginLeft = result;
                        return false;
                    }
                });

                final CustomSeek marginRight = inflate.findViewById(R.id.marginRight);
                marginRight.init(0, maxMargin, BookCSS.get().marginRight);
                marginRight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginRight = result;
                        return false;
                    }
                });

                // font folder
                LOG.d("fontFolder2-2", BookCSS.get().fontFolder);
                final TextView fontsFolder = inflate.findViewById(R.id.fontsFolder);
                TxtUtils.underline(fontsFolder, TxtUtils.lastTwoPath(BookCSS.get().fontFolder));
                fontsFolder.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ChooserDialogFragment.chooseFolder((FragmentActivity) controller.getActivity(), BookCSS.get().fontFolder).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                            @Override
                            public boolean onResultRecive(String nPath, Dialog dialog) {
                                File result = new File(nPath);
                                BookCSS.get().fontFolder = result.getPath();
                                TxtUtils.underline(fontsFolder, TxtUtils.lastTwoPath(BookCSS.get().fontFolder));
                                AppProfile.save(controller.getActivity());
                                dialog.dismiss();
                                return false;
                            }
                        });

                    }

                });

                final View downloadFonts = inflate.findViewById(R.id.downloadFonts);
                //downloadFonts.setVisibility(TxtUtils.visibleIf(!AppsConfig.IS_FDROID));
                downloadFonts.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        FontExtractor.showDownloadFontsDialog(controller.getActivity(), downloadFonts, fontsFolder);
                    }
                });

/// aling

                final Map<Integer, String> alignConst = new LinkedHashMap<Integer, String>();
                alignConst.put(BookCSS.TEXT_ALIGN_JUSTIFY, controller.getString(R.string.width));
                alignConst.put(BookCSS.TEXT_ALIGN_LEFT, controller.getString(R.string.left));
                alignConst.put(BookCSS.TEXT_ALIGN_RIGHT, controller.getString(R.string.right));
                alignConst.put(BookCSS.TEXT_ALIGN_CENTER, controller.getString(R.string.center));

// align
                final TextView textAlign = inflate.findViewById(R.id.textAlign);
                textAlign.setText(TxtUtils.underline(alignConst.get(BookCSS.get().textAlign)));
                textAlign.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        for (final int key : alignConst.keySet()) {
                            String name = alignConst.get(key);
                            popupMenu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    BookCSS.get().textAlign = key;
                                    textAlign.setText(TxtUtils.underline(alignConst.get(BookCSS.get().textAlign)));
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });

// link color
                final CustomColorView linkColorDay = inflate.findViewById(R.id.linkColorDay);

                linkColorDay.withDefaultColors(StringDB.converToColor(BookCSS.get().linkColorDays));
                linkColorDay.init(Color.parseColor(BookCSS.get().linkColorDay));
                linkColorDay.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        BookCSS.get().linkColorDay = string;
                        return false;
                    }
                });
                linkColorDay.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        Dialogs.showEditDialog(v.getContext(), false, linkColorDay.getTextString(), BookCSS.get().linkColorDays, new ResultResponse<String>() {

                            @Override
                            public boolean onResultRecive(String result) {
                                BookCSS.get().linkColorDays = result;
                                linkColorDay.withDefaultColors(StringDB.converToColor(BookCSS.get().linkColorDays));
                                return true;
                            }
                        });
                        return true;
                    }
                });
//                linkColorDay.getText1().getLayoutParams().width = Dips.dpToPx(150);

                final CustomColorView linkColorNight = inflate.findViewById(R.id.linkColorNight);
                linkColorNight.withDefaultColors(StringDB.converToColor(BookCSS.get().linkColorNigths));
                linkColorNight.init(Color.parseColor(BookCSS.get().linkColorNight));
                linkColorNight.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        BookCSS.get().linkColorNight = string;
                        return false;
                    }
                });

                linkColorNight.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        Dialogs.showEditDialog(v.getContext(), false, linkColorNight.getTextString(), BookCSS.get().linkColorNigths, new ResultResponse<String>() {

                            @Override
                            public boolean onResultRecive(String result) {
                                BookCSS.get().linkColorNigths = result;
                                linkColorNight.withDefaultColors(StringDB.converToColor(BookCSS.get().linkColorNigths));
                                return true;
                            }
                        });
                        return true;
                    }
                });
//                linkColorNight.getText1().getLayoutParams().width = Dips.dpToPx(150);

                TxtUtils.underlineTextView(inflate.findViewById(R.id.onResetStyles)).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        AlertDialogs.showOkDialog(controller.getActivity(), controller.getString(R.string.restore_defaults_full), new Runnable() {

                            @Override
                            public void run() {
                                BookCSS.get().resetToDefault(controller.getActivity());

                                fontsFolder.setText(TxtUtils.underline(TxtUtils.lastTwoPath(BookCSS.get().fontFolder)));
                                textAlign.setText(TxtUtils.underline(alignConst.get(BookCSS.get().textAlign)));

                                fontWeight.reset(BookCSS.get().fontWeight / 100);
                                fontWeight.setValueText("" + BookCSS.get().fontWeight);

                                lineHeight.reset(BookCSS.get().lineHeight12);
                                paragraphHeight.reset(BookCSS.get().paragraphHeight);

                                fontParagraph.reset(BookCSS.get().textIndent);
                                //
                                marginTop.reset(BookCSS.get().marginTop);
                                marginBottom.reset(BookCSS.get().marginBottom);
                                marginLeft.reset(BookCSS.get().marginLeft);
                                marginRight.reset(BookCSS.get().marginRight);

                                emptyLine.reset(BookCSS.get().emptyLine);

                                linkColorDay.init(Color.parseColor(BookCSS.get().linkColorDay));
                                linkColorNight.init(Color.parseColor(BookCSS.get().linkColorNight));
                                linkColorDay.withDefaultColors(StringDB.converToColor(BookCSS.get().linkColorDays));
                                linkColorNight.withDefaultColors(StringDB.converToColor(BookCSS.get().linkColorNigths));

                            }
                        });

                    }
                });

                return inflate;
            }
        };
        dialog.show(DragingPopup.PREF + "_moreBookSettings");
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                if (initHash != Objects.appHash()) {
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    controller.restartActivity();
                }
            }
        });
        return dialog;
    }

    public static DragingPopup preferences(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {
        final int initHash = Objects.appHash();

        // LOG.d("ojectAsString1", Objects.ojectAsString(AppState.get()));

        if (ExtUtils.isNotValidFile(controller.getCurrentBook())) {
            DragingPopup dialog = new DragingPopup(R.string.preferences, anchor, PREF_WIDTH, PREF_HEIGHT) {

                @Override
                public View getContentView(final LayoutInflater inflater) {
                    TextView txt = new TextView(anchor.getContext());
                    txt.setText(R.string.file_not_found);
                    return txt;
                }
            };
            return dialog;
        }

        DragingPopup dialog = new DragingPopup(R.string.preferences, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View inflate = inflater.inflate(R.layout.dialog_prefs, null, false);

                // TOP panel start
                View topPanelLine = inflate.findViewById(R.id.topPanelLine);
                View topPanelLineDiv = inflate.findViewById(R.id.topPanelLineDiv);
                // topPanelLine.setVisibility(controller instanceof
                // DocumentControllerHorizontalView ? View.VISIBLE : View.GONE);
                topPanelLine.setVisibility(View.GONE);
                topPanelLineDiv.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);

                //inflate.findViewById(R.id.allBGConfig).setVisibility(Dips.isEInk() ? View.GONE : View.VISIBLE);

                View onRecent = inflate.findViewById(R.id.onRecent);
                onRecent.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        closeDialog();
                        DragingDialogs.recentBooks(anchor, controller);
                    }
                });
                final View onRotate = inflate.findViewById(R.id.onRotate);
                onRotate.setOnClickListener(new OnClickListener() {

                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        // closeDialog();
                        MenuBuilderM.addRotateMenu(onRotate, null, updateUIRefresh).show();
                    }
                });

                inflate.findViewById(R.id.onPageFlip).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        closeDialog();
                        DragingDialogs.pageFlippingDialog(anchor, controller, onRefresh);

                    }
                });

                ImageView brightness = inflate.findViewById(R.id.onBrightness);
                brightness.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        AppState.get().isDayNotInvert = !AppState.get().isDayNotInvert;
                        controller.restartActivity();
                    }
                });
                brightness.setImageResource(!AppState.get().isDayNotInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_231_moon);

                final ImageView isCrop = inflate.findViewById(R.id.onCrop);
                // isCrop.setVisibility(controller.isTextFormat() ||
                // AppSP.get().isCut ? View.GONE : View.VISIBLE);
                isCrop.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        AppSP.get().isCrop = !AppSP.get().isCrop;
                        SettingsManager.getBookSettings().updateFromAppState();
                        TintUtil.setTintImageWithAlpha(isCrop, !AppSP.get().isCrop ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);
                        updateUIRefresh.run();
                    }
                });
                TintUtil.setTintImageWithAlpha(isCrop, !AppSP.get().isCrop ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);

                final ImageView bookCut = inflate.findViewById(R.id.bookCut);
                // bookCut.setVisibility(controller.isTextFormat() ? View.GONE :
                // View.VISIBLE);
                bookCut.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        closeDialog();
                        DragingDialogs.sliceDialog(anchor, controller, updateUIRefresh, new ResultResponse<Integer>() {

                            @Override
                            public boolean onResultRecive(Integer result) {
                                TintUtil.setTintImageWithAlpha(bookCut, !AppSP.get().isCut ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);
                                SettingsManager.getBookSettings().updateFromAppState();
                                EventBus.getDefault().post(new InvalidateMessage());
                                return false;
                            }
                        });
                    }
                });
                TintUtil.setTintImageWithAlpha(bookCut, !AppSP.get().isCut ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);

                inflate.findViewById(R.id.onFullScreen).setOnClickListener(v -> {

                    DocumentController.showFullScreenPopup(controller.getActivity(), v, id -> {
                        AppState.get().fullScreenMode = id;
                        DocumentController.chooseFullScreen(controller.getActivity(), AppState.get().fullScreenMode);
                        if (controller.isTextFormat()) {
                            if (onRefresh != null) {
                                onRefresh.run();
                            }
                            controller.restartActivity();
                        }
                        return true;
                    }, AppState.get().fullScreenMode);

                });
                View tts = inflate.findViewById(R.id.onTTS);
                tts.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        closeDialog();
                        DragingDialogs.textToSpeachDialog(anchor, controller);
                    }
                });

                final ImageView pin = inflate.findViewById(R.id.onPin);
                pin.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        AppState.get().isShowToolBar = !AppState.get().isShowToolBar;
                        pin.setImageResource(AppState.get().isShowToolBar ? R.drawable.glyphicons_415_push_pin : R.drawable.glyphicons_305_no_symbol);
                        if (onRefresh != null) {
                            onRefresh.run();
                        }

                    }
                });
                pin.setImageResource(AppState.get().isShowToolBar ? R.drawable.glyphicons_415_push_pin : R.drawable.glyphicons_305_no_symbol);

                // TOP panel end

                CheckBox isPreText = inflate.findViewById(R.id.isPreText);
                isPreText.setChecked(AppState.get().isPreText);
                isPreText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isPreText = isChecked;
                    }
                });

                boolean isTxtOrZip = BookType.TXT.is(controller.getCurrentBook().getPath()) || BookType.ZIP.is(controller.getCurrentBook().getPath());
                isPreText.setVisibility(isTxtOrZip ? View.VISIBLE : View.GONE);

                CheckBox isLineBreaksText = inflate.findViewById(R.id.isLineBreaksText);
                isLineBreaksText.setChecked(AppState.get().isLineBreaksText);
                isLineBreaksText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isLineBreaksText = isChecked;
                    }
                });

                isLineBreaksText.setVisibility(isTxtOrZip ? View.VISIBLE : View.GONE);

                //

                //charsets

                CheckBox isCharacterEncoding = inflate.findViewById(R.id.isCharacterEncoding);

                ((View) isCharacterEncoding.getParent()).setVisibility(isTxtOrZip || controller.getCurrentBook().getPath().endsWith(".pdb") ? View.VISIBLE : View.GONE);
                isCharacterEncoding.setChecked(AppState.get().isCharacterEncoding);
                isCharacterEncoding.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isCharacterEncoding = isChecked;
                    }
                });

                final TextView characterEncoding = inflate.findViewById(R.id.characterEncoding);
                characterEncoding.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);
                characterEncoding.setText(AppState.get().characterEncoding);
                TxtUtils.underlineTextView(characterEncoding);

                characterEncoding.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                        List<String> keys = new ArrayList<>(Charset.availableCharsets().keySet());
                        keys.add(0, "UTF-8");

                        for (final String name : keys) {
                            if (name.startsWith("IBM") || name.startsWith("x-")) {
                                continue;
                            }
                            popupMenu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().characterEncoding = name;
                                    characterEncoding.setText(AppState.get().characterEncoding);
                                    TxtUtils.underlineTextView(characterEncoding);
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });

                TextView moreSettings = inflate.findViewById(R.id.moreSettings);
                moreSettings.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);
                inflate.findViewById(R.id.moreSettingsDiv).setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);

                // View moreSettingsImage =
                // inflate.findViewById(R.id.moreSettingsImage);
                // moreSettingsImage.setVisibility(controller.isTextFormat() ?
                // View.VISIBLE : View.GONE);
                TxtUtils.underlineTextView(moreSettings).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        moreBookSettings(anchor, controller, onRefresh, updateUIRefresh);
                    }
                });

                TextView performanceSettings = TxtUtils.underlineTextView(inflate.findViewById(R.id.performanceSettigns));
                performanceSettings.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        performanceSettings(anchor, controller, onRefresh, updateUIRefresh);
                    }
                });

                TextView statusBarSettings = TxtUtils.underlineTextView(inflate.findViewById(R.id.statusBarSettings));
                statusBarSettings.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        statusBarSettings(anchor, controller, onRefresh, updateUIRefresh);
                    }
                });

                final CustomSeek fontSizeSp = inflate.findViewById(R.id.fontSizeSp);
                fontSizeSp.init(10, 70, BookCSS.get().fontSizeSp);
                fontSizeSp.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().fontSizeSp = result;
                        return false;
                    }
                });
                fontSizeSp.setValueText("" + BookCSS.get().fontSizeSp);

                inflate.findViewById(R.id.fontSizeLayout).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);
                inflate.findViewById(R.id.fontNameSelectionLayout).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                final TextView textFontName = inflate.findViewById(R.id.textFontName);
                textFontName.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final List<FontPack> fontPacks = BookCSS.get().getAllFontsPacks();
                        MyPopupMenu popup = new MyPopupMenu(controller.getActivity(), v);
                        for (final FontPack pack : fontPacks) {
                            LOG.d("pack.normalFont", pack.normalFont);
                            popup.getMenu().add(pack.dispalyName, pack.normalFont).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    BookCSS.get().resetAll(pack);
                                    TxtUtils.underline(textFontName, BookCSS.get().displayFontName);
                                    return false;
                                }
                            });
                        }
                        popup.show();
                    }
                });

                TxtUtils.underline(textFontName, BookCSS.get().displayFontName);

                final View moreFontSettings = inflate.findViewById(R.id.moreFontSettings);
                moreFontSettings.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        FontDialog.show(controller.getActivity(), new Runnable() {

                            @Override
                            public void run() {
                                TxtUtils.underline(textFontName, BookCSS.get().displayFontName);
                            }
                        }, controller);
                    }
                });

                final View downloadFonts = inflate.findViewById(R.id.downloadFonts);
                downloadFonts.setVisibility(FontExtractor.hasZipFonts() ? View.GONE : View.VISIBLE);
                downloadFonts.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        FontExtractor.showDownloadFontsDialog(controller.getActivity(), downloadFonts, textFontName);
                    }
                });

                // crop
                CheckBox isCropBorders = inflate.findViewById(R.id.isCropBorders);
                isCropBorders.setChecked(controller.isCropCurrentBook());
                isCropBorders.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        controller.onCrop();

                    }
                });

// volume
                final CheckBox isReverseKyes = inflate.findViewById(R.id.isReverseKyes);
                isReverseKyes.setChecked(AppState.get().isReverseKeys);
                isReverseKyes.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isReverseKeys = isChecked;
                    }
                });

                isReverseKyes.setEnabled(AppState.get().isUseVolumeKeys);

                CheckBox isUseVolumeKeys = inflate.findViewById(R.id.isUseVolumeKeys);
                isUseVolumeKeys.setChecked(AppState.get().isUseVolumeKeys);
                isUseVolumeKeys.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isUseVolumeKeys = isChecked;
                        isReverseKyes.setEnabled(AppState.get().isUseVolumeKeys);
                    }
                });

// orientation begin

                final TextView screenOrientation = inflate.findViewById(R.id.screenOrientation);
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
                                    DocumentController.doRotation(controller.getActivity());
                                    return false;
                                }
                            });
                        }
                        menu.show();
                    }
                });

                // orientation end

                BrightnessHelper.showBlueLigthDialogAndBrightness(controller.getActivity(), inflate, onRefresh);
// brightness end
// dicts

                final TextView selectedDictionaly = inflate.findViewById(R.id.selectedDictionaly);
                selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
                selectedDictionaly.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        DialogTranslateFromTo.show(controller.getActivity(), false, new Runnable() {

                            @Override
                            public void run() {
                                selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
                            }
                        }, false);
                    }
                });

                ((CheckBox) inflate.findViewById(R.id.isRememberDictionary)).setChecked(AppState.get().isRememberDictionary);
                ((CheckBox) inflate.findViewById(R.id.isRememberDictionary)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isRememberDictionary = isChecked;
                    }
                });

                // Colors

                TextView textCustomizeFontBGColor = inflate.findViewById(R.id.textCustomizeFontBGColor);
                if (AppState.get().isCustomizeBgAndColors || controller.isTextFormat()) {
                    textCustomizeFontBGColor.setText(R.string.customize_font_background_colors);
                } else {
                    textCustomizeFontBGColor.setText(R.string.customize_background_color);
                }

                final ImageView onDayColorImage = inflate.findViewById(R.id.onDayColorImage);
                final TextView textDayColor = TxtUtils.underlineTextView(inflate.findViewById(R.id.onDayColor));
                textDayColor.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        boolean isSolid = !AppState.get().isUseBGImageDay;

                        new ColorsDialog((FragmentActivity) controller.getActivity(), true, AppState.get().colorDayText, AppState.get().colorDayBg, false, isSolid, new ColorsDialogResult() {

                            @Override
                            public void onChooseColor(int colorText, int colorBg) {
                                textDayColor.setTextColor(colorText);
                                textDayColor.setBackgroundColor(colorBg);
                                TintUtil.setTintImageWithAlpha(onDayColorImage, colorText);

                                AppState.get().colorDayText = colorText;
                                AppState.get().colorDayBg = colorBg;

                                IMG.clearMemoryCache();
                                IMG.clearDiscCache();

                                if (AppState.get().isUseBGImageDay) {
                                    textDayColor.setBackgroundDrawable(MagicHelper.getBgImageDayDrawable(true));
                                }
                            }
                        });
                    }
                });

                final ImageView onNigthColorImage = inflate.findViewById(R.id.onNigthColorImage);
                final TextView textNigthColor = TxtUtils.underlineTextView(inflate.findViewById(R.id.onNigthColor));
                textNigthColor.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        boolean isSolid = !AppState.get().isUseBGImageNight;
                        new ColorsDialog((FragmentActivity) controller.getActivity(), false, AppState.get().colorNigthText, AppState.get().colorNigthBg, false, isSolid, new ColorsDialogResult() {

                            @Override
                            public void onChooseColor(int colorText, int colorBg) {
                                textNigthColor.setTextColor(colorText);
                                textNigthColor.setBackgroundColor(colorBg);
                                TintUtil.setTintImageWithAlpha(onNigthColorImage, colorText);

                                AppState.get().colorNigthText = colorText;
                                AppState.get().colorNigthBg = colorBg;

                                if (AppState.get().isUseBGImageNight) {
                                    textNigthColor.setBackgroundDrawable(MagicHelper.getBgImageNightDrawable(true));
                                }

                            }
                        });
                    }
                });

                final LinearLayout lc = inflate.findViewById(R.id.preColors);

                TintUtil.setTintImageWithAlpha(onDayColorImage, AppState.get().colorDayText);
                TintUtil.setTintImageWithAlpha(onNigthColorImage, AppState.get().colorNigthText);

                textNigthColor.setTextColor(AppState.get().colorNigthText);
                textNigthColor.setBackgroundColor(AppState.get().colorNigthBg);
                textDayColor.setTextColor(AppState.get().colorDayText);
                textDayColor.setBackgroundColor(AppState.get().colorDayBg);

                if (AppState.get().isUseBGImageDay) {
                    textDayColor.setTextColor(Color.BLACK);
                    textDayColor.setBackgroundDrawable(MagicHelper.getBgImageDayDrawable(true));

                }
                if (AppState.get().isUseBGImageNight) {
                    textNigthColor.setTextColor(Color.WHITE);
                    textNigthColor.setBackgroundDrawable(MagicHelper.getBgImageNightDrawable(true));
                }

// lc.setVisibility(controller.isTextFormat() ||
// AppState.get().isCustomizeBgAndColors ? View.VISIBLE :
// View.GONE);

                final int padding = Dips.dpToPx(3);
                final Runnable colorsLine = new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                        lc.removeAllViews();

                        for (String line : AppState.get().readColors.split(";")) {
                            if (TxtUtils.isEmpty(line)) {
                                continue;
                            }
                            String[] split = line.split(",");
                            LOG.d("Split colors", split[0], split[1], split[2]);
                            String name = split[0];
                            final int bg = Color.parseColor(split[1]);
                            final int text = Color.parseColor(split[2]);
                            final boolean isDay = split[3].equals("0");

                            BorderTextView t1 = new BorderTextView(controller.getActivity());
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30));
                            params.setMargins(padding, padding, padding, padding);
                            t1.setLayoutParams(params);
                            t1.setGravity(Gravity.CENTER);
                            t1.setBackgroundColor(bg);
                            if (controller.isTextFormat() || AppState.get().isCustomizeBgAndColors) {
                                t1.setText(name);
                                t1.setTextColor(text);
                                t1.setTypeface(null, Typeface.BOLD);
                            }

                            t1.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    if (isDay) {
                                        if (controller.isTextFormat() || AppState.get().isCustomizeBgAndColors) {
                                            AppState.get().colorDayText = text;
                                            textDayColor.setTextColor(text);
                                        }

                                        AppState.get().colorDayBg = bg;
                                        textDayColor.setBackgroundColor(bg);
                                        AppState.get().isUseBGImageDay = false;

                                    } else {
                                        if (controller.isTextFormat() || AppState.get().isCustomizeBgAndColors) {
                                            AppState.get().colorNigthText = text;
                                            textNigthColor.setTextColor(text);
                                        }

                                        AppState.get().colorNigthBg = bg;
                                        textNigthColor.setBackgroundColor(bg);
                                        AppState.get().isUseBGImageNight = false;
                                    }

                                    TintUtil.setTintImageWithAlpha(onDayColorImage, AppState.get().colorDayText);
                                    TintUtil.setTintImageWithAlpha(onNigthColorImage, AppState.get().colorNigthText);
                                }

                            });
                            lc.addView(t1);
                        }
                        // add DayBG
                        {
                            ImageView t1 = new ImageView(controller.getActivity());
                            t1.setTag(controller.getString(R.string.no_tint));
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30));
                            params.setMargins(padding, padding, padding, padding);
                            t1.setLayoutParams(params);
                            t1.setScaleType(ScaleType.FIT_XY);

                            t1.setImageDrawable(MagicHelper.getBgImageDayDrawable(false));

                            t1.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    AppState.get().colorDayText = AppState.COLOR_BLACK;
                                    AppState.get().colorDayBg = AppState.COLOR_WHITE;

                                    textDayColor.setTextColor(Color.BLACK);
                                    textDayColor.setBackgroundDrawable(MagicHelper.getBgImageDayDrawable(false));
                                    AppState.get().isUseBGImageDay = true;

                                    TintUtil.setTintImageWithAlpha(onDayColorImage, AppState.get().colorDayText);

                                }
                            });
                            lc.addView(t1, AppState.get().readColors.split(";").length / 2);
                        }

                        // add Night
                        {
                            ImageView t2 = new ImageView(controller.getActivity());
                            t2.setTag(controller.getString(R.string.no_tint));
                            LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30));
                            params2.setMargins(padding, padding, padding, padding);
                            t2.setLayoutParams(params2);
                            t2.setScaleType(ScaleType.FIT_XY);

                            t2.setImageDrawable(MagicHelper.getBgImageNightDrawable(false));

                            t2.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    AppState.get().colorNigthText = AppState.COLOR_WHITE;
                                    AppState.get().colorNigthBg = AppState.COLOR_BLACK;

                                    textNigthColor.setTextColor(Color.WHITE);
                                    textNigthColor.setBackgroundDrawable(MagicHelper.getBgImageNightDrawable(false));
                                    AppState.get().isUseBGImageNight = true;
                                    TintUtil.setTintImageWithAlpha(onNigthColorImage, AppState.get().colorNigthText);

                                }
                            });
                            lc.addView(t2);
                        }

                    }
                };
                colorsLine.run();

                TxtUtils.underlineTextView(inflate.findViewById(R.id.onDefaultColor)).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        AlertDialogs.showOkDialog(controller.getActivity(), controller.getString(R.string.restore_defaults_full), new Runnable() {

                            @Override
                            public void run() {
                                AppState.get().readColors = AppState.READ_COLORS_DEAFAUL;
                                AppState.get().isUseBGImageDay = false;
                                AppState.get().isUseBGImageNight = false;

                                AppState.get().bgImageDayTransparency = AppState.DAY_TRANSPARENCY;
                                AppState.get().bgImageDayPath = MagicHelper.IMAGE_BG_1;

                                AppState.get().bgImageNightTransparency = AppState.NIGHT_TRANSPARENCY;
                                AppState.get().bgImageNightPath = MagicHelper.IMAGE_BG_1;

                                AppState.get().isCustomizeBgAndColors = false;

                                AppState.get().colorDayText = AppState.COLOR_BLACK;
                                AppState.get().colorDayBg = AppState.COLOR_WHITE;

                                textDayColor.setTextColor(AppState.get().colorDayText);
                                textDayColor.setBackgroundColor(AppState.get().colorDayBg);

                                AppState.get().colorNigthText = AppState.COLOR_WHITE_2;
                                AppState.get().colorNigthBg = AppState.COLOR_BLACK_2;

                                textNigthColor.setTextColor(AppState.get().colorNigthText);
                                textNigthColor.setBackgroundColor(AppState.get().colorNigthBg);

                                TintUtil.setTintImageWithAlpha(onDayColorImage, AppState.get().colorDayText);
                                TintUtil.setTintImageWithAlpha(onNigthColorImage, AppState.get().colorNigthText);

                                AppState.get().statusBarColorDay = Color.parseColor(AppState.TEXT_COLOR_DAY);
                                AppState.get().statusBarColorNight = Color.parseColor(AppState.TEXT_COLOR_NIGHT);

                                colorsLine.run();
                            }
                        });

                    }
                });

                inflate.findViewById(R.id.moreReadColorSettings).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(controller.getActivity());
                        builder.setTitle(R.string.customize);

                        final LinearLayout root = new LinearLayout(controller.getActivity());
                        root.setOrientation(LinearLayout.VERTICAL);

                        for (String line : AppState.get().readColors.split(";")) {
                            if (TxtUtils.isEmpty(line)) {
                                continue;
                            }
                            final String[] split = line.split(",");
                            LOG.d("Split colors", split[0], split[1], split[2]);
                            final String name = split[0];
                            final int bg = Color.parseColor(split[1]);
                            final int text = Color.parseColor(split[2]);
                            final boolean isDay = split[3].equals("0");

                            final LinearLayout child = new LinearLayout(controller.getActivity());
                            child.setOrientation(LinearLayout.HORIZONTAL);
                            child.setTag(line);

                            final TextView t1Img = new TextView(controller.getActivity());
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Dips.dpToPx(60), Dips.dpToPx(30));
                            params.setMargins(padding, padding, padding, padding);
                            t1Img.setLayoutParams(params);
                            t1Img.setGravity(Gravity.CENTER);
                            t1Img.setBackgroundColor(bg);
                            t1Img.setText(name);
                            t1Img.setTextColor(text);
                            t1Img.setTypeface(null, Typeface.BOLD);
                            t1Img.setTag(isDay);

                            TextView t0 = new TextView(controller.getActivity());
                            t0.setEms(1);

                            TextView t00 = new TextView(controller.getActivity());
                            t00.setEms(2);

                            final TextView t2BG = new TextView(controller.getActivity());
                            t2BG.setText(TxtUtils.underline(split[1]));
                            t2BG.setEms(5);
                            t2BG.setTag(bg);

                            final TextView t3Text = new TextView(controller.getActivity());
                            t3Text.setText(TxtUtils.underline(split[2]));
                            t3Text.setEms(5);
                            t3Text.setTag(text);

                            child.addView(t0);
                            child.addView(t1Img);
                            child.addView(t00);
                            child.addView(t2BG);
                            child.addView(t3Text);

                            child.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    new ColorsDialog((FragmentActivity) controller.getActivity(), (Boolean) t1Img.getTag(), (Integer) t3Text.getTag(), (Integer) t2BG.getTag(), true, true, new ColorsDialogResult() {

                                        @Override
                                        public void onChooseColor(int colorText, int colorBg) {
                                            t1Img.setTextColor(colorText);
                                            t1Img.setBackgroundColor(colorBg);

                                            t2BG.setText(TxtUtils.underline(MagicHelper.colorToString(colorBg)));
                                            t3Text.setText(TxtUtils.underline(MagicHelper.colorToString(colorText)));

                                            t2BG.setTag(colorBg);
                                            t3Text.setTag(colorText);

                                            String line = name + "," + MagicHelper.colorToString(colorBg) + "," + MagicHelper.colorToString(colorText) + "," + split[3];
                                            child.setTag(line);

                                        }
                                    });

                                }
                            });

                            root.addView(child);

                        }

                        builder.setView(root);

                        builder.setNegativeButton(R.string.apply, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String res = "";
                                for (int i = 0; i < root.getChildCount(); i++) {
                                    View childAt = root.getChildAt(i);
                                    String line = (String) childAt.getTag();
                                    res = res + line + ";";
                                }
                                AppState.get().readColors = res;
                                LOG.d("SAVE readColors", AppState.get().readColors);
                                colorsLine.run();

                            }
                        });
                        builder.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        builder.show();

                    }
                });

                return inflate;

            }
        }.show(DragingPopup.PREF + "_preferences").setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                // LOG.d("ojectAsString2", Objects.ojectAsString(AppState.get()));

                if (initHash != Objects.appHash()) {
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    controller.restartActivity();
                }
            }
        });
        return dialog;
    }
}
