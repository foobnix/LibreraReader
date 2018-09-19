package com.foobnix.pdf.info.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ebookdroid.BookType;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.StringResponse;
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
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.android.utils.Views;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.hypen.HyphenPattern;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FontExtractor;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
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
import com.foobnix.pdf.info.wrapper.AppBookmark;
import com.foobnix.pdf.info.wrapper.AppState;
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
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.view.inputmethod.EditorInfo;
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
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class DragingDialogs {

    public final static int PREF_WIDTH = 330;
    public final static int PREF_HEIGHT = 560;

    public static final String EDIT_COLORS_PANEL = "editColorsPanel";

    public static void samble(final FrameLayout anchor, final DocumentController controller) {

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
                AppState.get().save(controller.getActivity());

            }
        });
        dialog.show("Sample");

    }

    public static void customCropDialog(final FrameLayout anchor, final DocumentController controller, final Runnable onCropChange) {

        DragingPopup dialog = new DragingPopup(R.string.crop_white_borders, anchor, 360, 400) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final Activity activity = controller.getActivity();
                final View inflate = inflater.inflate(R.layout.dialog_custom_crop, null, false);

                // Margins
                final CustomSeek marginTop = (CustomSeek) inflate.findViewById(R.id.marginTop);
                final CustomSeek marginBottom = (CustomSeek) inflate.findViewById(R.id.marginBottom);

                int max = 30;
                marginTop.init(0, max, AppState.get().cropTop, "%");
                marginTop.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().cropTop = result;
                        onCropChange.run();
                        if (AppState.get().isCropSymetry) {
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
                        if (AppState.get().isCropSymetry) {
                            marginTop.reset(result);
                        }
                        return false;
                    }
                });

                final CustomSeek marginLeft = (CustomSeek) inflate.findViewById(R.id.marginLeft);
                final CustomSeek marginRight = (CustomSeek) inflate.findViewById(R.id.marginRight);

                marginLeft.init(0, max, AppState.get().cropLeft, "%");
                marginLeft.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().cropLeft = result;
                        onCropChange.run();
                        if (AppState.get().isCropSymetry) {
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
                        if (AppState.get().isCropSymetry) {
                            marginLeft.reset(result);
                        }
                        return false;
                    }
                });

                CheckBox isCropSymetry = (CheckBox) inflate.findViewById(R.id.isCropSymetry);
                isCropSymetry.setChecked(AppState.get().isCropSymetry);
                isCropSymetry.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isCropSymetry = isChecked;
                        if (isChecked) {
                            marginBottom.reset(marginTop.getCurrentValue());
                            marginRight.reset(marginLeft.getCurrentValue());
                        }
                    }
                });

                final CheckBox isEnableCrop = (CheckBox) inflate.findViewById(R.id.isEnableCrop);
                isEnableCrop.setChecked(AppState.get().isCrop);
                isEnableCrop.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isEnableCrop) {
                        AppState.get().isCrop = isEnableCrop;
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

                        AppState.get().isCrop = true;
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

        DragingPopup dialog = new DragingPopup(controller.getString(R.string.page_position), anchor, 280, 250) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final Activity activity = controller.getActivity();
                final View view = inflater.inflate(R.layout.dialog_move_manually, null, false);
                ImageView onUp = (ImageView) view.findViewById(R.id.onUp);
                ImageView onDonw = (ImageView) view.findViewById(R.id.onDown);
                ImageView onLeft = (ImageView) view.findViewById(R.id.onLeft);
                ImageView onRight = (ImageView) view.findViewById(R.id.onRight);
                ImageView onPlus = (ImageView) view.findViewById(R.id.onPlus);
                ImageView onMinus = (ImageView) view.findViewById(R.id.onMinus);
                ImageView onCenter = (ImageView) view.findViewById(R.id.onCenter);
                final ImageView onCrop = (ImageView) view.findViewById(R.id.onCrop);

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
                        AppState.get().isCrop = !AppState.get().isCrop;
                        SettingsManager.getBookSettings().updateFromAppState();
                        updateUIRefresh.run();

                        if (AppState.get().isCrop) {
                            TintUtil.setTintImageWithAlpha(onCrop, TintUtil.COLOR_ORANGE);
                        } else {
                            TintUtil.setTintImageWithAlpha(onCrop, AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);
                        }
                    }
                });

                if (AppState.get().isCrop) {
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
                AppState.get().save(controller.getActivity());

            }

        });
        dialog.show("MovePage");

    }

    public static void textToSpeachDialog(final FrameLayout anchor, final DocumentController controller) {
        textToSpeachDialog(anchor, controller, "");
    }

    public static void textToSpeachDialog(final FrameLayout anchor, final DocumentController controller, final String textToRead) {

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

                final TextView ttsPage = (TextView) view.findViewById(R.id.ttsPage);

                final TextView textEngine = (TextView) view.findViewById(R.id.ttsEngine);

                final TextView timerTime = (TextView) view.findViewById(R.id.timerTime);
                final TextView timerStart = (TextView) view.findViewById(R.id.timerStart);

                TTSControlsView tts = (TTSControlsView) view.findViewById(R.id.ttsActive);
                tts.setDC(controller);

                TextView ttsSkeakToFile = (TextView) view.findViewById(R.id.ttsSkeakToFile);

                final TextView ttsLang = (TextView) view.findViewById(R.id.ttsLang);
                // TxtUtils.underlineTextView(ttsLang);

                final TextView ttsPauseDuration = (TextView) view.findViewById(R.id.ttsPauseDuration);
                ttsPauseDuration.setText("" + AppState.get().ttsPauseDuration + " ms");
                TxtUtils.underlineTextView(ttsPauseDuration);

                ttsPauseDuration.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        for (int i = 0; i <= 750; i += 50) {
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
                        popupMenu.show();

                    }
                });

                ttsLang.setVisibility(TxtUtils.visibleIf(Build.VERSION.SDK_INT >= 21));

                timerTime.setText(AppState.get().ttsTimer + " " + controller.getString(R.string.minutes).toLowerCase(Locale.US));
                timerTime.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        for (int i = 15; i <= 300; i += 15) {
                            final int number = i;
                            popupMenu.getMenu().add("" + i).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().ttsTimer = number;
                                    timerTime.setText(AppState.get().ttsTimer + " " + controller.getString(R.string.minutes).toLowerCase(Locale.US));
                                    TxtUtils.underlineTextView(timerTime);
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
                            TempHolder.get().timerFinishTime = System.currentTimeMillis() + AppState.get().ttsTimer * 60 * 1000;
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

                TTSEngine.get().getTTS(new OnInitListener() {

                    @Override
                    public void onInit(int status) {
                        textEngine.setText(TTSEngine.get().getCurrentEngineName());
                        ttsLang.setText(TTSEngine.get().getCurrentLang());

                    }
                });

                controller.runTimer(3000, new Runnable() {

                    @Override
                    public void run() {
                        textEngine.setText(TTSEngine.get().getCurrentEngineName());
                        ttsLang.setText(TTSEngine.get().getCurrentLang());
                    }
                });

                textEngine.setText(TTSEngine.get().getCurrentEngineName());
                ttsLang.setText(TTSEngine.get().getCurrentLang());

                TxtUtils.underlineTextView((TextView) view.findViewById(R.id.ttsSettings)).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            TTSEngine.get().stop();
                            TTSEngine.get().stopDestroy();

                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (Build.VERSION.SDK_INT >= 14) {
                                intent.setAction("com.android.settings.TTS_SETTINGS");
                            } else {
                                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TextToSpeechSettings"));
                            }
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                            LOG.e(e);
                        }
                    }
                });

                final CustomSeek seekBarSpeed = (CustomSeek) view.findViewById(R.id.seekBarSpeed);
                seekBarSpeed.init(0, 600, (int) AppState.get().ttsSpeed * 100);
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
                for (final float i:values) {
                    menu.getMenu().add(String.format("%s x", i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            seekBarSpeed.reset((int)(i * 100));
                            seekBarSpeed.sendProgressChanged();
                            return false;
                        }
                    });
                }

                seekBarSpeed.addMyPopupMenu(menu);
                TxtUtils.setLinkTextColor(seekBarSpeed.getTitleText());

                final CustomSeek seekBarPitch = (CustomSeek) view.findViewById(R.id.seekBarPitch);
                seekBarPitch.init(0, 200, (int) AppState.get().ttsPitch * 100);
                seekBarPitch.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        TTSEngine.get().stop();
                        AppState.get().ttsPitch = (float) result / 100;
                        return false;
                    }
                });

                final AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

                final CustomSeek seekVolume = (CustomSeek) view.findViewById(R.id.seekVolume);
                seekVolume.init(0, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                seekVolume.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, result, 0);
                        return false;
                    }
                });

                TxtUtils.underlineTextView((TextView) view.findViewById(R.id.restore_defaults)).setOnClickListener(new OnClickListener() {

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
                            }
                        });

                    }
                });
                //

                CheckBox stopReadingOnCall = (CheckBox) view.findViewById(R.id.stopReadingOnCall);
                stopReadingOnCall.setChecked(AppState.get().stopReadingOnCall);
                stopReadingOnCall.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().stopReadingOnCall = isChecked;
                    }
                });

                CheckBox isFastBookmarkByTTS = (CheckBox) view.findViewById(R.id.isFastBookmarkByTTS);
                isFastBookmarkByTTS.setChecked(AppState.get().isFastBookmarkByTTS);
                isFastBookmarkByTTS.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isFastBookmarkByTTS = isChecked;
                    }
                });

                ttsSkeakToFile.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(controller.getActivity());
                        dialog.setTitle(R.string.speak_into_file_wav_);

                        View inflate = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_tts_wav, null, false);
                        final TextView ttsSpeakPath = (TextView) inflate.findViewById(R.id.ttsSpeakPath);
                        final TextView progressText = (TextView) inflate.findViewById(R.id.progressText);
                        final ProgressBar progressBar1 = (ProgressBar) inflate.findViewById(R.id.progressBarTTS);
                        final Button start = (Button) inflate.findViewById(R.id.start);
                        final Button stop = (Button) inflate.findViewById(R.id.stop);

                        progressBar1.setVisibility(View.GONE);
                        progressText.setText("");

                        ttsSpeakPath.setText(Html.fromHtml("<u>" + AppState.get().ttsSpeakPath + "/<b>" + controller.getCurrentBook().getName() + "</b></u>"));
                        ttsSpeakPath.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                ChooserDialogFragment.chooseFolder((FragmentActivity) controller.getActivity(), AppState.get().ttsSpeakPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                                    @Override
                                    public boolean onResultRecive(String nPath, Dialog dialog) {
                                        AppState.get().ttsSpeakPath = nPath;
                                        ttsSpeakPath.setText(Html.fromHtml("<u>" + AppState.get().ttsSpeakPath + "/<b>" + controller.getCurrentBook().getName() + "</b></u>"));
                                        dialog.dismiss();
                                        return false;
                                    }
                                });

                            }
                        });
                        final ResultResponse<String> info = new ResultResponse<String>() {
                            @Override
                            public boolean onResultRecive(final String result) {
                                controller.getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        progressText.setText(result);
                                    }
                                });
                                return false;
                            }
                        };

                        stop.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                TempHolder.isRecordTTS = false;
                                progressBar1.setVisibility(View.GONE);
                            }
                        });

                        start.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                if (!TempHolder.isRecordTTS) {
                                    TempHolder.isRecordTTS = true;
                                    progressBar1.setVisibility(View.VISIBLE);
                                    TTSEngine.get().speakToFile(controller, info);
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
                AppState.get().save(controller.getActivity());

            }
        });
        dialog.show("TTS");

    }

    public static void searchMenu(final FrameLayout anchor, final DocumentController controller, final String text) {
        DragingPopup dialog = new DragingPopup(R.string.search, anchor, 250, 150) {
            @Override
            public View getContentView(LayoutInflater inflater) {
                final View view = inflater.inflate(R.layout.search_dialog, null, false);

                final EditText searchEdit = (EditText) view.findViewById(R.id.edit1);
                searchEdit.setText(text);

                final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBarSearch);
                final TextView searchingMsg = (TextView) view.findViewById(R.id.searching);
                final GridView gridView = (GridView) view.findViewById(R.id.grid1);
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

                ImageView onClear = (ImageView) view.findViewById(R.id.imageClear);
                onClear.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        boolean isRun = TempHolder.isSeaching;
                        TempHolder.isSeaching = false;
                        if (!isRun) {
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
                if (TxtUtils.isNotEmpty(text)) {
                    onSearch.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            onSearch.performClick();
                        }
                    }, 250);

                }

                final String searchingString = anchor.getContext().getString(R.string.searching_please_wait_);
                final int count = controller.getPageCount();

                final Handler hMessage = new Handler() {
                    @Override
                    public void handleMessage(android.os.Message msg) {
                        int pageNumber = msg.what;
                        LOG.d("Receive page", pageNumber);
                        progressBar.setVisibility(View.GONE);
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

                    };
                };

                onSearch.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (TempHolder.isSeaching) {
                            return;
                        }
                        String searchString = searchEdit.getText().toString().trim();
                        if (searchString.length() < 2) {
                            return;
                        }
                        TempHolder.isSeaching = true;

                        searchingMsg.setText(R.string.searching_please_wait_);
                        searchingMsg.setVisibility(View.VISIBLE);

                        progressBar.setVisibility(View.VISIBLE);
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

                TextView goTo = (TextView) inflate.findViewById(R.id.goTo);
                TextView footerNumber = (TextView) inflate.findViewById(R.id.footerNumber);
                TextView goBack = (TextView) inflate.findViewById(R.id.goBack);
                TextView text = (TextView) inflate.findViewById(R.id.text);
                float size = Math.max(14, AppState.get().fontSizeSp * 0.85f);
                text.setTextSize(size);
                LOG.d("FONT SIZE", size);

                footerNumber.setText(TxtUtils.getFooterNoteNumber(selectedText));

                goTo.setText(controller.getString(R.string.go_to_page_dialog) + " " + page);
                text.setText(controller.getFootNote(selectedText));
                if (page == -1 || page == 0 || AppState.get().isDouble) {
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
                        if (controller.getLinkHistory().contains(offsetY)) {
                            controller.getLinkHistory().remove(offsetY);
                        }
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

    public static DragingPopup selectTextMenu(final FrameLayout anchor, final DocumentController controller, final boolean withAnnotation, final Runnable reloadUI) {

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
                final LinearLayout linearLayoutColor = (LinearLayout) view.findViewById(R.id.colorsLine);
                linearLayoutColor.removeAllViews();
                List<String> colors = new ArrayList<String>(AppState.get().COLORS);
                colors.remove(0);
                colors.remove(0);

                final ImageView underLine = (ImageView) view.findViewById(R.id.onUnderline);
                final ImageView strike = (ImageView) view.findViewById(R.id.onStrike);
                final ImageView selection = (ImageView) view.findViewById(R.id.onSelection);
                final ImageView onAddCustom = (ImageView) view.findViewById(R.id.onAddCustom);

                final LinearLayout customsLayout = (LinearLayout) view.findViewById(R.id.customsLayout);

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
                                image.setImageResource(R.drawable.glyphicons_607_te_background);
                            } else if (line.startsWith("U")) {
                                image.setImageResource(R.drawable.glyphicons_104_te_underline);
                            } else if (line.startsWith("S")) {
                                image.setImageResource(R.drawable.glyphicons_105_te_strike);
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

                        Drawable highlight = controller.getActivity().getResources().getDrawable(R.drawable.glyphicons_607_te_background);
                        highlight.setColorFilter(Color.parseColor(AppState.get().annotationTextColor), Mode.SRC_ATOP);

                        Drawable underline = controller.getActivity().getResources().getDrawable(R.drawable.glyphicons_104_te_underline);
                        underline.setColorFilter(Color.parseColor(AppState.get().annotationTextColor), Mode.SRC_ATOP);

                        Drawable strikeout = controller.getActivity().getResources().getDrawable(R.drawable.glyphicons_105_te_strike);
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

                final EditText editText = (EditText) view.findViewById(R.id.editText);
                final String selectedText = AppState.get().selectedText;
                // AppState.get().selectedText = null;

                editText.setText(selectedText);

                final View onTranslate = view.findViewById(R.id.onTranslate);
                onTranslate.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        anchor.removeAllViews();

                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                        final Map<String, String> providers = AppState.getDictionaries(editText.getText().toString().trim());

                        for (final String name : providers.keySet()) {

                            popupMenu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    Urls.open(anchor.getContext(), providers.get(name).trim());
                                    return false;
                                }
                            });
                        }

                        popupMenu.show();
                    }
                });

                view.findViewById(R.id.onAddToBookmark).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        closeDialog();
                        ListBoxHelper.showAddDialog(controller, null, null, editText.getText().toString().trim());
                    }
                });

                view.findViewById(R.id.readTTS).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        TTSEngine.get().stop();
                        TTSEngine.get().speek(editText.getText().toString().trim());
                    }
                });
                view.findViewById(R.id.readTTSNext).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        TTSEngine.get().stop();

                        TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), editText.getText().toString().trim(), controller.getBookWidth(), controller.getBookHeight(), AppState.get().fontSizeSp, controller.getTitle());
                    }
                });

                view.findViewById(R.id.onShare).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        closeDialog();
                        final Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        String txt = "\"" + editText.getText().toString().trim() + "\" (" + controller.getBookFileMetaName() + ")";
                        intent.putExtra(Intent.EXTRA_TEXT, txt);
                        controller.getActivity().startActivity(Intent.createChooser(intent, controller.getString(R.string.share)));

                    }
                });

                view.findViewById(R.id.onCopy).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Context c = anchor.getContext();
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(editText.getText().toString().trim());
                        } else {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", editText.getText().toString().trim());
                            clipboard.setPrimaryClip(clip);
                        }
                        closeDialog();
                    }
                });

                view.findViewById(R.id.onGoogle).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        closeDialog();
                        Urls.open(anchor.getContext(), "http://www.google.com/search?q=" + editText.getText().toString().trim());
                    }
                });

                TextView onBookSearch = (TextView) view.findViewById(R.id.onBookSearch);
                // onBookSearch.setText(controller.getString(R.string.search_in_the_book)
                // + " \"" + AppState.get().selectedText + "\"");
                onBookSearch.setVisibility(selectedText.contains(" ") ? View.GONE : View.VISIBLE);
                onBookSearch.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        searchMenu(anchor, controller, selectedText);
                    }
                });

                LinearLayout dictLayout = (LinearLayout) view.findViewById(R.id.dictionaryLine);
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

                final List<ResolveInfo> proccessTextList = pm.queryIntentActivities(intentProccessText, 0);
                final List<ResolveInfo> searchList = pm.queryIntentActivities(intentSearch, 0);
                final List<ResolveInfo> sendList = pm.queryIntentActivities(intentSend, 0);
                final List<ResolveInfo> customList = pm.queryIntentActivities(intentCustom, 0);

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
                        if (app.activityInfo.packageName.toLowerCase(Locale.US).contains(pkgKey)) {
                            if (cache.contains(app.activityInfo.name)) {
                                continue;
                            }
                            cache.add(app.activityInfo.name);

                            LOG.d("Add APP", app.activityInfo.name);
                            try {
                                ImageView image = new ImageView(anchor.getContext());
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

                                            intentCustom.addCategory(Intent.CATEGORY_LAUNCHER);
                                            intentCustom.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentCustom.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                            intentCustom.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                            intentCustom.setComponent(name);

                                            intentCustom.putExtra("EXTRA_QUERY", selecteText);
                                            DictsHelper.updateExtraGoldenDict(intentCustom);

                                            LOG.d("intentCustom", intentCustom, intentCustom.getExtras());

                                            controller.getActivity().startActivity(intentCustom);
                                            // controller.getActivity().overridePendingTransition(0, 0);

                                        } else if (proccessTextList.contains(app)) {
                                            LOG.d("dict-intent", "proccessTextList");

                                            intentProccessText.addCategory(Intent.CATEGORY_LAUNCHER);
                                            intentProccessText.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentProccessText.setComponent(name);

                                            intentProccessText.putExtra(Intent.EXTRA_TEXT, selecteText);
                                            intentProccessText.putExtra(Intent.EXTRA_PROCESS_TEXT, selecteText);
                                            intentProccessText.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, selecteText);

                                            controller.getActivity().startActivity(intentProccessText);
                                        } else if (searchList.contains(app)) {
                                            LOG.d("dict-intent", "searchList");
                                            intentSearch.addCategory(Intent.CATEGORY_LAUNCHER);
                                            intentSearch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentSearch.setComponent(name);

                                            intentSearch.putExtra(SearchManager.QUERY, selecteText);
                                            intentSearch.putExtra(Intent.EXTRA_TEXT, selecteText);

                                            controller.getActivity().startActivity(intentSearch);
                                        } else if (sendList.contains(app)) {
                                            LOG.d("dict-intent", "sendList");
                                            intentSend.addCategory(Intent.CATEGORY_LAUNCHER);
                                            intentSend.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentSend.setComponent(name);

                                            intentSend.putExtra(Intent.EXTRA_TEXT, selecteText);
                                            controller.getActivity().startActivity(intentSend);
                                        }
                                        sp.edit().putString("last", app.activityInfo.name).commit();

                                    }
                                });
                                if (app.activityInfo.name.equals(lastID)) {
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

                view.findViewById(R.id.onUnderline).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.underlineText(Color.parseColor(AppState.get().annotationTextColor), 2.0f, AnnotationType.UNDERLINE);
                        closeDialog();
                        controller.saveAnnotationsToFile();
                    }
                });
                view.findViewById(R.id.onStrike).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.underlineText(Color.parseColor(AppState.get().annotationTextColor), 2.0f, AnnotationType.STRIKEOUT);
                        closeDialog();
                        controller.saveAnnotationsToFile();
                    }
                });
                view.findViewById(R.id.onSelection).setOnClickListener(new View.OnClickListener() {

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
                // controller.clearSelectedText();
                AppState.get().selectedText = null;
            }
        });

    }

    public static DragingPopup thumbnailDialog(final FrameLayout anchor, final DocumentController controller) {
        DragingPopup popup = new DragingPopup(R.string.go_to_page_dialog, anchor, 300, 400) {
            View searchLayout;

            @Override
            public void beforeCreate() {
                setTitlePopupIcon(R.drawable.glyphicons_518_option_vertical);
                titlePopupMenu = new MyPopupMenu(anchor.getContext(), anchor);
                titlePopupMenu.getMenu().add(R.string.hide_search_bar).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.get().isShowSearchBar = false;
                        searchLayout.setVisibility(View.GONE);
                        return false;
                    }
                });
                titlePopupMenu.getMenu().add(R.string.show_search_bar).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.get().isShowSearchBar = true;
                        searchLayout.setVisibility(View.VISIBLE);
                        return false;
                    }
                });

            };

            @Override
            public View getContentView(LayoutInflater inflater) {
                View view = inflater.inflate(R.layout.dialog_go_to_page, null, false);
                searchLayout = view.findViewById(R.id.searchLayout);
                Views.visible(searchLayout, AppState.get().isShowSearchBar);

                final EditText number = (EditText) view.findViewById(R.id.edit1);
                number.clearFocus();
                number.setText("" + controller.getCurentPageFirst1());
                final GridView grid = (GridView) view.findViewById(R.id.grid1);
                int dpToPx = Dips.dpToPx(AppState.get().coverSmallSize);

                if (AppState.get().isDouble && !controller.isTextFormat()) {
                    dpToPx = dpToPx * 2;
                }
                grid.setColumnWidth(dpToPx);

                final File currentBook = controller.getCurrentBook();
                if (ExtUtils.isValidFile(currentBook)) {
                    grid.setAdapter(new PageThumbnailAdapter(anchor.getContext(), controller.getPageCount(), controller.getCurentPageFirst1() - 1) {
                        @Override
                        public PageUrl getPageUrl(int page) {
                            return PageUrl.buildSmall(currentBook.getPath(), page);
                        };
                    });
                }

                grid.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        controller.onGoToPage(position + 1);
                        ((PageThumbnailAdapter) grid.getAdapter()).setCurrentPage(position);
                        ((PageThumbnailAdapter) grid.getAdapter()).notifyDataSetChanged();
                    }

                });
                grid.setOnItemLongClickListener(new OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        Vibro.vibrate();
                        if (grid.isFastScrollEnabled()) {
                            grid.setFastScrollEnabled(false);
                            grid.setFastScrollAlwaysVisible(false);
                        } else {
                            grid.setFastScrollEnabled(true);
                            grid.setFastScrollAlwaysVisible(false);
                        }
                        return true;
                    }
                });

                grid.setSelection(controller.getCurentPage() - 1);

                grid.setOnScrollListener(new OnScrollListener() {

                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {

                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        LOG.d("onScroll", firstVisibleItem, Math.abs(firstVisibleItem - controller.getCurentPage()));
                        if (firstVisibleItem < 3 || Math.abs(firstVisibleItem - controller.getCurentPage()) < 20) {
                            // searchLayout.setVisibility(View.VISIBLE);
                        } else {
                            // searchLayout.setVisibility(View.GONE);
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

                onSearch.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        int page = 1;
                        try {
                            String txt = number.getText().toString();

                            if (txt.contains("%") || txt.contains(".") || txt.contains(",")) {
                                txt = txt.replace("%", "").replace(",", ".");
                                float parseFloat = Float.parseFloat(txt);
                                if (parseFloat > 100) {
                                    Toast.makeText(controller.getActivity(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                                }
                                page = (int) (controller.getPageCount() * parseFloat) / 100;
                                page = page + 1;
                            } else {
                                page = Integer.valueOf(txt);
                            }

                        } catch (Exception e) {
                            LOG.e(e);
                            number.setText("1");
                        }

                        if (page >= 0 && page <= controller.getPageCount()) {
                            controller.onGoToPage(page);
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

        }.show("thumbnailDialog", false, true).setOnCloseListener(new Runnable() {

            @Override
            public void run() {
            }

        });
        return popup;

    }

    public static void editColorsPanel(final FrameLayout anchor, final DocumentController controller, final DrawView drawView, final boolean force) {
        drawView.setOnFinishDraw(new Runnable() {

            @Override
            public void run() {
                String annotationDrawColor = AppState.get().annotationDrawColor;
                if (TxtUtils.isEmpty(annotationDrawColor)) {
                    annotationDrawColor = AppState.get().COLORS.get(0);
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
            };

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View a = inflater.inflate(R.layout.edit_panel, null, false);
                final GridView grid = (GridView) a.findViewById(R.id.gridColors);

                if (AppState.get().editWith == AppState.EDIT_DELETE) {
                    AppState.get().annotationDrawColor = AppState.get().COLORS.get(0);
                }

                final BaseItemAdapter<String> adapter = new BaseItemAdapter<String>(AppState.get().COLORS) {

                    @Override
                    public View getView(int pos, View arg1, ViewGroup arg2, String color) {
                        View view = null;
                        if (pos == 0) {
                            view = inflater.inflate(R.layout.item_color_cut, arg2, false);
                        } else if (pos == 1) {
                            view = inflater.inflate(R.layout.item_color_spinner, arg2, false);
                            final Spinner spinner = (Spinner) view.findViewById(R.id.spinner1);
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
                            final Spinner spinner = (Spinner) view.findViewById(R.id.spinner1);
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
        new DragingPopup(R.string.recent_favorites_tags, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public View getContentView(final LayoutInflater inflater) {
                RecyclerView recyclerView = new RecyclerView(controller.getActivity());
                FileMetaAdapter recentAdapter = new FileMetaAdapter();
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(controller.getActivity());
                recyclerView.setLayoutManager(mLayoutManager);
                recentAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST);
                recyclerView.setAdapter(recentAdapter);

                List<FileMeta> all = AppDB.get().getAllRecentWithProgress();
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
        Vibro.vibrate();
        List<AppBookmark> objects = AppSharedPreferences.get().getBookmarksByBook(controller.getCurrentBook());
        int page = PageUrl.fakeToReal(controller.getCurentPageFirst1());

        for (AppBookmark all : objects) {
            if (all.getPage() == page) {
                Toast.makeText(controller.getActivity(), R.string.bookmark_for_this_page_already_exists, Toast.LENGTH_LONG).show();
                return;
            }
        }

        final AppBookmark bookmark = new AppBookmark(controller.getCurrentBook().getPath(), controller.getString(R.string.fast_bookmark), page, controller.getTitle());
        AppSharedPreferences.get().addBookMark(bookmark);

        String TEXT = controller.getString(R.string.fast_bookmark) + " " + TxtUtils.LONG_DASH1 + " " + controller.getString(R.string.page) + " " + page + "";
        Toast.makeText(controller.getActivity(), TEXT, Toast.LENGTH_SHORT).show();

    }

    public static void addBookmarks(final FrameLayout anchor, final DocumentController controller, final Runnable onRefeshUI) {
        final List<AppBookmark> objects = new ArrayList<AppBookmark>();
        final BookmarksAdapter bookmarksAdapter = new BookmarksAdapter(anchor.getContext(), objects, true);

        final View.OnClickListener onAdd = new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                ListBoxHelper.showAddDialog(controller, objects, bookmarksAdapter, "");

            }
        };

        final OnItemClickListener onItem = new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {

                final AppBookmark appBookmark = objects.get(position);
                int page = PageUrl.realToFake(appBookmark.getPage());

                if (page != controller.getCurentPageFirst1()) {
                    final Integer offsetY = Integer.valueOf((int) controller.getOffsetY());
                    LOG.d("onItemClick: Bookmark", offsetY);
                    controller.getLinkHistory().clear();
                    controller.getLinkHistory().add(offsetY);
                }

                controller.onGoToPage(page);

                onRefeshUI.run();
            }
        };

        final OnItemLongClickListener onBooksLong = new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                ListBoxHelper.showEditDeleteDialog(objects.get(position), controller, bookmarksAdapter, objects);
                return true;
            }

        };

        new DragingPopup(R.string.bookmarks, anchor, 300, 400) {

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View a = inflater.inflate(R.layout.dialog_bookmarks, null, false);
                final ListView contentList = (ListView) a.findViewById(R.id.contentList);
                contentList.setDivider(new ColorDrawable(Color.TRANSPARENT));
                contentList.setVerticalScrollBarEnabled(false);
                contentList.setAdapter(bookmarksAdapter);
                contentList.setOnItemClickListener(onItem);
                contentList.setOnItemLongClickListener(onBooksLong);
                a.findViewById(R.id.addBookmark).setOnClickListener(onAdd);

                final View.OnClickListener onAddPAge = new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        int page = PageUrl.fakeToReal(controller.getCurentPageFirst1());

                        for (AppBookmark all : objects) {
                            if (all.getPage() == page) {
                                Toast.makeText(controller.getActivity(), R.string.bookmark_for_this_page_already_exists, Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        final AppBookmark bookmark = new AppBookmark(controller.getCurrentBook().getPath(), controller.getString(R.string.fast_bookmark), page, controller.getTitle());
                        AppSharedPreferences.get().addBookMark(bookmark);

                        objects.clear();
                        objects.addAll(AppSharedPreferences.get().getBookmarksByBook(controller.getCurrentBook()));
                        bookmarksAdapter.notifyDataSetChanged();

                        closeDialog();
                        String TEXT = controller.getString(R.string.fast_bookmark) + " " + TxtUtils.LONG_DASH1 + " " + controller.getString(R.string.page) + " " + page + "";
                        Toast.makeText(controller.getActivity(), TEXT, Toast.LENGTH_SHORT).show();

                    }
                };

                a.findViewById(R.id.addPageBookmark).setOnClickListener(onAddPAge);

                objects.clear();
                objects.addAll(AppSharedPreferences.get().getBookmarksByBook(controller.getCurrentBook()));
                bookmarksAdapter.notifyDataSetChanged();

                return a;
            }
        }.show("addBookmarks", false, true);
    }

    public static DragingPopup showContent(final FrameLayout anchor, final DocumentController controller) {

        final OnItemClickListener onClickContent = new OnItemClickListener() {

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

                    }
                    return;
                }

            }
        };
        DragingPopup dragingPopup = new DragingPopup(anchor.getContext().getString(R.string.content_of_book), anchor, 300, 400) {

            @Override
            public View getContentView(LayoutInflater inflater) {
                View view = inflater.inflate(R.layout.dialog_recent_books, null, false);

                LinearLayout attachemnts = (LinearLayout) view.findViewById(R.id.mediaAttachments);
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
                                        dialog = ProgressDialog.show(controller.getActivity(), "", controller.getString(R.string.msg_loading));
                                    };

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
                                                ExtUtils.openWith(anchor.getContext(), aPath);
                                            } else {
                                                Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                                            }
                                        } catch (Exception e) {
                                            LOG.e(e);
                                        }

                                    };
                                }.execute();

                            }
                        });
                    }
                } else {
                    view.findViewById(R.id.mediaAttachmentsScroll).setVisibility(View.GONE);
                }

                final ListView contentList = (ListView) view.findViewById(R.id.contentList);
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
                                            OutlineLinkWrapper currentByPageNumber = OutlineHelper.getCurrentByPageNumber(outline, controller.getCurentPageFirst1());
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

                if (false && BookType.FB2.is(controller.getCurrentBook().getPath())) {
                    setTitlePopupIcon(AppState.get().outlineMode == AppState.OUTLINE_ONLY_HEADERS ? R.drawable.glyphicons_114_justify : R.drawable.glyphicons_114_justify_sub);
                    titlePopupMenu = new MyPopupMenu(controller.getActivity(), null);

                    List<Integer> names = Arrays.asList(R.string.headings_only, R.string.heading_and_subheadings);
                    final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_114_justify_sub);
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
        return dragingPopup;

    }

    public static void sliceDialog(final FrameLayout anchor, final DocumentController controller, final Runnable onRefreshDoc, final ResultResponse<Integer> onMoveCut) {

        new DragingPopup(anchor.getContext().getString(R.string.split_pages_in_two), anchor, 300, 200) {
            SeekBar seek;
            EditText editPercent;
            Runnable updateUI = new Runnable() {

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
                editPercent = (EditText) view.findViewById(R.id.editPercent);

                seek = (SeekBar) view.findViewById(R.id.seekBar);
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

                Button buttonCancel = (Button) view.findViewById(R.id.buttonCancel);
                buttonCancel.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PageImageState.get().isShowCuttingLine = false;
                        AppState.get().isCut = false;
                        BookSettings bookSettings = SettingsManager.getBookSettings(controller.getCurrentBook().getPath());
                        boolean wasSplit = bookSettings.splitPages;
                        bookSettings.splitPages = false;
                        onRefreshDoc.run();
                        closeDialog();
                        if (wasSplit) {
                            controller.onGoToPage(controller.getCurentPage() / 2 + 1);
                        }
                    }
                });

                Button buttonApply = (Button) view.findViewById(R.id.buttonApply);

                buttonApply.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PageImageState.get().isShowCuttingLine = false;
                        AppState.get().isCut = true;
                        AppState.get().isCrop = false;
                        boolean init = SettingsManager.getBookSettings().splitPages;
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

        new DragingPopup(anchor.getContext().getString(R.string.automatic_page_flipping), anchor, 300, 380) {

            @Override
            public View getContentView(LayoutInflater inflater) {
                View inflate = inflater.inflate(R.layout.dialog_flipping_pages, null, false);

                CheckBox isScrollAnimation = (CheckBox) inflate.findViewById(R.id.isScrollAnimation);
                isScrollAnimation.setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
                isScrollAnimation.setChecked(AppState.get().isScrollAnimation);
                isScrollAnimation.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isScrollAnimation = isChecked;
                    }
                });

                CheckBox isLoopAutoplay = (CheckBox) inflate.findViewById(R.id.isLoopAutoplay);
                isLoopAutoplay.setChecked(AppState.get().isLoopAutoplay);
                isLoopAutoplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isLoopAutoplay = isChecked;
                    }
                });

                CheckBox isShowToolBar = (CheckBox) inflate.findViewById(R.id.isShowToolBar);
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

                final CustomSeek flippingInterval = (CustomSeek) inflate.findViewById(R.id.flippingInterval);
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

                final CheckBox isShowReadingProgress = (CheckBox) inflate.findViewById(R.id.isShowReadingProgress);
                final CheckBox isShowChaptersOnProgress = (CheckBox) inflate.findViewById(R.id.isShowChaptersOnProgress);
                final CheckBox isShowSubChaptersOnProgress = (CheckBox) inflate.findViewById(R.id.isShowSubChaptersOnProgress);

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

                final EditText musicText = (EditText) inflate.findViewById(R.id.musicText);
                musicText.setText(AppState.get().musicText);
                ((View) musicText.getParent()).setVisibility(AppState.get().isMusicianMode ? View.VISIBLE : View.GONE);
                inflate.findViewById(R.id.musicTextOk).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppState.get().musicText = musicText.getText().toString();
                        if (onRefresh != null) {
                            onRefresh.run();
                        }

                    }
                });

                CheckBox isRewindEnable = (CheckBox) inflate.findViewById(R.id.isRewindEnable);
                isRewindEnable.setChecked(AppState.get().isRewindEnable);
                isRewindEnable.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isRewindEnable = isChecked;
                    }
                });
                //

                final CheckBox isShowSatusBar = (CheckBox) inflate.findViewById(R.id.isShowSatusBar);
                isShowSatusBar.setChecked(AppState.get().isShowToolBar);
                isShowSatusBar.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowToolBar = isChecked;
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                final CheckBox isShowTime = (CheckBox) inflate.findViewById(R.id.isShowTime);
                isShowTime.setChecked(AppState.get().isShowTime);
                isShowTime.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowTime = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        if (isChecked) {
                            isShowSatusBar.setChecked(true);
                        }
                    }
                });

                final CheckBox isShowBattery = (CheckBox) inflate.findViewById(R.id.isShowBattery);
                isShowBattery.setChecked(AppState.get().isShowBattery);
                isShowBattery.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isShowBattery = isChecked;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        if (isChecked) {
                            isShowSatusBar.setChecked(true);
                        }
                    }
                });

                // status bar

                final List<Integer> modeIds = Arrays.asList(//
                        AppState.READING_PROGRESS_NUMBERS, //
                        AppState.READING_PROGRESS_PERCENT, //
                        AppState.READING_PROGRESS_PERCENT_NUMBERS//
                );//

                final List<String> modeStrings = Arrays.asList(//
                        controller.getString(R.string.number), //
                        controller.getString(R.string.percent), //
                        controller.getString(R.string.percent_and_number)//
                );//

                final TextView readingProgress = (TextView) inflate.findViewById(R.id.readingProgress);
                readingProgress.setText(modeStrings.get(modeIds.indexOf(AppState.get().readingProgress)));
                TxtUtils.underlineTextView(readingProgress);

                readingProgress.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final MyPopupMenu popupMenu = new MyPopupMenu(v.getContext(), v);
                        for (int i = 0; i < modeStrings.size(); i++) {
                            final int j = i;
                            popupMenu.getMenu().add(modeStrings.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().readingProgress = modeIds.get(j);
                                    readingProgress.setText(modeStrings.get(modeIds.indexOf(AppState.get().readingProgress)));
                                    TxtUtils.underlineTextView(readingProgress);
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

                final CustomSeek statusBarTextSize = (CustomSeek) inflate.findViewById(R.id.statusBarTextSize);
                statusBarTextSize.setTitleTextWidth(Dips.dpToPx(100));

                statusBarTextSize.init(5, 30, controller.isEasyMode() ? AppState.get().statusBarTextSizeEasy : AppState.get().statusBarTextSizeAdv);
                statusBarTextSize.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        isShowSatusBar.setChecked(true);
                        if (controller.isEasyMode()) {
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

                final CustomSeek progressLineHeight = (CustomSeek) inflate.findViewById(R.id.progressLineHeight);
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

                final CustomColorView statusBarColorDay = (CustomColorView) inflate.findViewById(R.id.statusBarColorDay);
                statusBarColorDay.withDefaultColors(AppState.TEXT_COLOR_DAY, AppState.get().tintColor);
                statusBarColorDay.init(AppState.get().statusBarColorDay);
                statusBarColorDay.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        isShowSatusBar.setChecked(true);
                        AppState.get().statusBarColorDay = Color.parseColor(string);
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        Keyboards.hideNavigation(controller.getActivity());
                        return false;
                    }
                });

                final CustomColorView statusBarColorNight = (CustomColorView) inflate.findViewById(R.id.statusBarColorNight);
                statusBarColorNight.withDefaultColors(AppState.TEXT_COLOR_NIGHT, AppState.get().tintColor);
                statusBarColorNight.init(AppState.get().statusBarColorNight);
                statusBarColorNight.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        isShowSatusBar.setChecked(true);
                        AppState.get().statusBarColorNight = Color.parseColor(string);
                        AppState.get().isEditMode = false;
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        Keyboards.hideNavigation(controller.getActivity());
                        return false;
                    }
                });

                statusBarColorDay.getText1().getLayoutParams().width = Dips.dpToPx(150);
                statusBarColorNight.getText1().getLayoutParams().width = Dips.dpToPx(150);

                return inflate;
            }
        };

        dialog.show(DragingPopup.PREF + "_statusBarSettings");

        return dialog;
    }

    public static DragingPopup performanceSettings(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {
        AppState.get().saveIn(controller.getActivity());
        final int cssHash = BookCSS.get().toCssString().hashCode();
        final int appHash = Objects.hashCode(AppState.get());

        DragingPopup dialog = new DragingPopup(R.string.advanced_settings, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public void beforeCreate() {
                titleAction = controller.getString(R.string.preferences);
                titleRunnable = new Runnable() {

                    @Override
                    public void run() {
                        if (appHash != Objects.hashCode(AppState.get())) {
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

                CheckBox isLoopAutoplay = (CheckBox) inflate.findViewById(R.id.isLoopAutoplay);
                isLoopAutoplay.setChecked(AppState.get().isLoopAutoplay);
                // isLoopAutoplay.setVisibility(AppState.get().isAlwaysOpenAsMagazine ?
                // View.GONE : View.VISIBLE);
                isLoopAutoplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isLoopAutoplay = isChecked;
                    }
                });

                CheckBox isScrollSpeedByVolumeKeys = (CheckBox) inflate.findViewById(R.id.isScrollSpeedByVolumeKeys);
                isScrollSpeedByVolumeKeys.setChecked(AppState.get().isScrollSpeedByVolumeKeys);
                isScrollSpeedByVolumeKeys.setVisibility(View.GONE);
                isScrollSpeedByVolumeKeys.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isScrollSpeedByVolumeKeys = isChecked;
                    }
                });

                CheckBox isBrighrnessEnable = (CheckBox) inflate.findViewById(R.id.isBrighrnessEnable);
                isBrighrnessEnable.setChecked(AppState.get().isBrighrnessEnable);
                isBrighrnessEnable.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isBrighrnessEnable = isChecked;
                    }
                });

                CheckBox isShowLongBackDialog = (CheckBox) inflate.findViewById(R.id.isShowLongBackDialog);
                isShowLongBackDialog.setChecked(AppState.get().isShowLongBackDialog);
                isShowLongBackDialog.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isShowLongBackDialog = isChecked;
                    }
                });

                CheckBox isAllowTextSelection = (CheckBox) inflate.findViewById(R.id.isAllowTextSelection);
                isAllowTextSelection.setChecked(AppState.get().isAllowTextSelection);
                isAllowTextSelection.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isAllowTextSelection = isChecked;
                        if (isChecked) {
                            TempHolder.get().isAllowTextSelectionFirstTime = true;
                        }
                    }
                });

                CheckBox isZoomInOutWithVolueKeys = (CheckBox) inflate.findViewById(R.id.isZoomInOutWithVolueKeys);
                isZoomInOutWithVolueKeys.setChecked(AppState.get().isZoomInOutWithVolueKeys);
                isZoomInOutWithVolueKeys.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isZoomInOutWithVolueKeys = isChecked;
                    }
                });

                final CustomSeek mouseWheelSpeed = (CustomSeek) inflate.findViewById(R.id.seekWheelSpeed);
                mouseWheelSpeed.getTitleText().setSingleLine(false);
                mouseWheelSpeed.init(1, 200, AppState.get().mouseWheelSpeed);
                mouseWheelSpeed.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().mouseWheelSpeed = result;
                        return false;
                    }
                });

                CheckBox isScrollAnimation = (CheckBox) inflate.findViewById(R.id.isScrollAnimation);
                isScrollAnimation.setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
                isScrollAnimation.setChecked(AppState.get().isScrollAnimation);
                isScrollAnimation.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isScrollAnimation = isChecked;
                    }
                });

                CheckBox isDisableSwipe = (CheckBox) inflate.findViewById(R.id.isEnableVerticalSwipe);
                isDisableSwipe.setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
                isDisableSwipe.setChecked(AppState.get().isEnableVerticalSwipe);
                isDisableSwipe.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isEnableVerticalSwipe = isChecked;
                    }
                });

                final ImageView isSwipeGestureReverse = (ImageView) inflate.findViewById(R.id.isSwipeGestureReverse);
                isSwipeGestureReverse.setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
                isSwipeGestureReverse.setImageResource(AppState.get().isSwipeGestureReverse ? R.drawable.glyphicons_214_arrow_up : R.drawable.glyphicons_21_arrow_down);
                isSwipeGestureReverse.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppState.get().isSwipeGestureReverse = !AppState.get().isSwipeGestureReverse;
                        isSwipeGestureReverse.setImageResource(AppState.get().isSwipeGestureReverse ? R.drawable.glyphicons_214_arrow_up : R.drawable.glyphicons_21_arrow_down);
                    }
                });

                CheckBox isEnableHorizontalSwipe = (CheckBox) inflate.findViewById(R.id.isEnableHorizontalSwipe);
                isEnableHorizontalSwipe.setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
                isEnableHorizontalSwipe.setChecked(AppState.get().isEnableHorizontalSwipe);
                isEnableHorizontalSwipe.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isEnableHorizontalSwipe = isChecked;
                    }
                });

                CheckBox isVibration = (CheckBox) inflate.findViewById(R.id.isVibration);
                isVibration.setChecked(AppState.get().isVibration);
                isVibration.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isVibration = isChecked;
                    }
                });

                CheckBox isOLED = (CheckBox) inflate.findViewById(R.id.isOLED);
                isOLED.setChecked(AppState.get().isOLED);
                isOLED.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isOLED = isChecked;
                    }
                });

                CheckBox isLockPDF = (CheckBox) inflate.findViewById(R.id.isLockPDF);
                isLockPDF.setChecked(AppState.get().isLockPDF);
                isLockPDF.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isLockPDF.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isLockPDF = isChecked;
                    }
                });

                CheckBox isCropPDF = (CheckBox) inflate.findViewById(R.id.isCropPDF);
                isCropPDF.setChecked(AppState.get().isCropPDF);
                isCropPDF.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isCropPDF.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isCropPDF = isChecked;
                    }
                });

                CheckBox isCustomizeBgAndColors = (CheckBox) inflate.findViewById(R.id.isCustomizeBgAndColors);
                isCustomizeBgAndColors.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isCustomizeBgAndColors.setChecked(AppState.get().isCustomizeBgAndColors);
                isCustomizeBgAndColors.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isCustomizeBgAndColors = isChecked;
                    }
                });
                final CheckBox isReplaceWhite = (CheckBox) inflate.findViewById(R.id.isReplaceWhite);
                isReplaceWhite.setChecked(AppState.get().isReplaceWhite);
                isReplaceWhite.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);

                isReplaceWhite.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isReplaceWhite = isChecked;
                    }
                });

                CheckBox isIgnoreAnnotatations = (CheckBox) inflate.findViewById(R.id.isIgnoreAnnotatations);
                isIgnoreAnnotatations.setChecked(AppState.get().isIgnoreAnnotatations);
                isIgnoreAnnotatations.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isIgnoreAnnotatations = isChecked;

                    }
                });

                isIgnoreAnnotatations.setVisibility(!AppState.get().isAlwaysOpenAsMagazine && BookType.PDF.is(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                CheckBox isSaveAnnotatationsAutomatically = (CheckBox) inflate.findViewById(R.id.isSaveAnnotatationsAutomatically);
                isSaveAnnotatationsAutomatically.setChecked(AppState.get().isSaveAnnotatationsAutomatically);
                isSaveAnnotatationsAutomatically.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isSaveAnnotatationsAutomatically = isChecked;

                    }
                });

                isSaveAnnotatationsAutomatically.setVisibility(!AppState.get().isAlwaysOpenAsMagazine && BookType.PDF.is(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                CheckBox highlightByLetters = (CheckBox) inflate.findViewById(R.id.highlightByLetters);
                highlightByLetters.setChecked(AppState.get().selectingByLetters);
                highlightByLetters.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().selectingByLetters = isChecked;
                    }
                });

                CheckBox isCutRTL = (CheckBox) inflate.findViewById(R.id.isCutRTL);
                isCutRTL.setChecked(AppState.get().isCutRTL);
                isCutRTL.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isCutRTL = isChecked;
                    }
                });

                final TextView pageQuality = (TextView) inflate.findViewById(R.id.pageQuality);
                ((ViewGroup) pageQuality.getParent()).setVisibility(AppState.get().isAlwaysOpenAsMagazine && !ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);
                pageQuality.setText((int) (AppState.get().pageQuality * 100) + "%");
                TxtUtils.underlineTextView(pageQuality);
                pageQuality.setOnClickListener(new OnClickListener() {

                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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

                final TextView pagesInMemory = (TextView) inflate.findViewById(R.id.pagesInMemory);
                ((ViewGroup) pagesInMemory.getParent()).setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);

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
                final TextView inactivityTime = (TextView) inflate.findViewById(R.id.inactivityTime);
                inactivityTime.setText("" + AppState.get().inactivityTime);
                TxtUtils.underlineTextView(inactivityTime);
                inactivityTime.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        for (int i = 1; i <= 10; i++) {
                            final int number = i;
                            popupMenu.getMenu().add("" + i).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().inactivityTime = number;
                                    inactivityTime.setText("" + AppState.get().inactivityTime);
                                    TxtUtils.underlineTextView(inactivityTime);
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });

                ///

                final TextView rotate = (TextView) inflate.findViewById(R.id.rotate);
                ((ViewGroup) rotate.getParent()).setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);

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

                final TextView tapzoneSize = (TextView) inflate.findViewById(R.id.tapzoneSize);
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

                // double tap

                final List<String> doubleTapNames = Arrays.asList(//
                        controller.getString(R.string.db_auto_scroll), //
                        controller.getString(R.string.db_auto_alignemnt), //
                        controller.getString(R.string.db_auto_center_horizontally), //
                        controller.getString(R.string.zoom_in_zoom_out), //
                        controller.getString(R.string.close_book), //
                        controller.getString(R.string.close_book_and_application), //
                        controller.getString(R.string.hide_app), //
                        controller.getString(R.string.db_do_nothing) //

                );

                final List<Integer> doubleTapIDS = Arrays.asList(//
                        AppState.DOUBLE_CLICK_AUTOSCROLL, //
                        AppState.DOUBLE_CLICK_ADJUST_PAGE, //
                        AppState.DOUBLE_CLICK_CENTER_HORIZONTAL, //
                        AppState.DOUBLE_CLICK_ZOOM_IN_OUT, //
                        AppState.DOUBLE_CLICK_CLOSE_BOOK, //
                        AppState.DOUBLE_CLICK_CLOSE_BOOK_AND_APP, //
                        AppState.DOUBLE_CLICK_CLOSE_HIDE_APP, //
                        AppState.DOUBLE_CLICK_NOTHING //
                );//
                final TextView doubleClickAction1 = (TextView) inflate.findViewById(R.id.doubleTapValue);
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

                final TextView tapzoneCustomize = (TextView) inflate.findViewById(R.id.tapzoneCustomize);
                TxtUtils.underlineTextView(tapzoneCustomize);
                tapzoneCustomize.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        TapZoneDialog.show(controller.getActivity());
                    }
                });

                final TextView allocatedMemorySize = (TextView) inflate.findViewById(R.id.allocatedMemorySize);
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
                final TextView remindRestTime = (TextView) inflate.findViewById(R.id.remindRestTime);
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

                final TextView rotateViewPager = (TextView) inflate.findViewById(R.id.rotateViewPager);
                ((ViewGroup) rotateViewPager.getParent()).setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
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
                final TextView rtlText = (TextView) inflate.findViewById(R.id.rtlText);
                ((ViewGroup) rtlText.getParent()).setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
                if (AppState.get().isRTL) {
                    rtlText.setText(R.string.right_to_left);
                } else {
                    rtlText.setText(R.string.left_to_rigth);
                }
                TxtUtils.underlineTextView(rtlText);
                rtlText.setOnClickListener(new OnClickListener() {

                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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

                return inflate;
            }
        };
        dialog.show(DragingPopup.PREF + "_performanceSettings");
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                boolean one = appHash != Objects.hashCode(AppState.get());
                boolean two = controller.isTextFormat() && cssHash != BookCSS.get().toCssString().hashCode();
                if (one || two) {
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    controller.restartActivity();
                }
            }
        });
        return dialog;
    };

    public static DragingPopup moreBookSettings(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {
        final int initCssHash = BookCSS.get().toCssString().hashCode();
        final int initAppHash = Objects.hashCode(AppState.get());

        DragingPopup dialog = new DragingPopup(R.string.reading_settings, anchor, PREF_WIDTH, PREF_HEIGHT) {

            @Override
            public void beforeCreate() {
                titleAction = controller.getString(R.string.preferences);
                titleRunnable = new Runnable() {

                    @Override
                    public void run() {
                        if (initCssHash != BookCSS.get().toCssString().hashCode()) {
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

                final CustomSeek fontWeight = (CustomSeek) inflate.findViewById(R.id.fontWeight);
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
                        controller.getString(R.string.document_styles), //
                        controller.getString(R.string.user_styles));

                final TextView docStyle = (TextView) inflate.findViewById(R.id.documentStyle);

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

                // end styles

                // hypens
                boolean isSupportHypens = controller.isTextFormat();

                CheckBox isAutoHypens = (CheckBox) inflate.findViewById(R.id.isAutoHypens);
                isAutoHypens.setVisibility(isSupportHypens ? View.VISIBLE : View.GONE);

                isAutoHypens.setChecked(BookCSS.get().isAutoHypens);
                isAutoHypens.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        BookCSS.get().isAutoHypens = isChecked;
                    }
                });

                final TextView hypenLangLabel = (TextView) inflate.findViewById(R.id.hypenLangLabel);

                final TextView hypenLang = (TextView) inflate.findViewById(R.id.hypenLang);

                hypenLang.setVisibility(isSupportHypens ? View.VISIBLE : View.GONE);
                hypenLangLabel.setVisibility(isSupportHypens ? View.VISIBLE : View.GONE);

                // hypenLang.setVisibility(View.GONE);
                // hypenLangLabel.setVisibility(View.GONE);

                hypenLang.setText(DialogTranslateFromTo.getLanuageByCode(BookCSS.get().hypenLang));
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
                                    BookCSS.get().hypenLang = code;
                                    hypenLang.setText(titleLang);
                                    TxtUtils.underlineTextView(hypenLang);
                                    FileMeta load = AppDB.get().load(controller.getCurrentBook().getPath());
                                    if (load != null) {
                                        load.setLang(code);
                                        AppDB.get().update(load);
                                    }
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });
                // - hypens
                //
                CheckBox isAccurateFontSize = (CheckBox) inflate.findViewById(R.id.isAccurateFontSize);
                isAccurateFontSize.setVisibility(controller.isTextFormat() ? View.VISIBLE : View.GONE);
                isAccurateFontSize.setChecked(AppState.get().isAccurateFontSize);
                isAccurateFontSize.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.get().isAccurateFontSize = isChecked;
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
                                BookCSS.get().save(v.getContext());
                            }
                        });
                        builder.show();

                    }
                });

                final CustomSeek lineHeight = (CustomSeek) inflate.findViewById(R.id.lineHeight);
                lineHeight.init(0, 30, BookCSS.get().lineHeight);
                lineHeight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().lineHeight = result;
                        return false;
                    }
                });

                final CustomSeek paragraphHeight = (CustomSeek) inflate.findViewById(R.id.paragraphHeight);
                paragraphHeight.init(0, 20, BookCSS.get().paragraphHeight);
                paragraphHeight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().paragraphHeight = result;
                        return false;
                    }
                });

                final CustomSeek fontParagraph = (CustomSeek) inflate.findViewById(R.id.fontParagraph);
                fontParagraph.init(0, 30, BookCSS.get().textIndent);
                fontParagraph.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().textIndent = result;
                        return false;
                    }
                });

                final CustomSeek emptyLine = (CustomSeek) inflate.findViewById(R.id.emptyLine);
                boolean isShow = BookType.FB2.is(controller.getCurrentBook().getPath());// || //
                // BookType.HTML.is(controller.getCurrentBook().getPath()) || //
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

                final CustomSeek marginTop = (CustomSeek) inflate.findViewById(R.id.marginTop);
                int maxMargin = Dips.isLargeOrXLargeScreen() ? 200 : 30;
                marginTop.init(0, maxMargin, BookCSS.get().marginTop);
                marginTop.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginTop = result;
                        return false;
                    }
                });

                final CustomSeek marginBottom = (CustomSeek) inflate.findViewById(R.id.marginBottom);
                marginBottom.init(0, maxMargin, BookCSS.get().marginBottom);
                marginBottom.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginBottom = result;
                        return false;
                    }
                });

                final CustomSeek marginLeft = (CustomSeek) inflate.findViewById(R.id.marginLeft);
                marginLeft.init(0, maxMargin, BookCSS.get().marginLeft);
                marginLeft.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginLeft = result;
                        return false;
                    }
                });

                final CustomSeek marginRight = (CustomSeek) inflate.findViewById(R.id.marginRight);
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
                final TextView fontsFolder = (TextView) inflate.findViewById(R.id.fontsFolder);
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
                                BookCSS.get().save(controller.getActivity());
                                dialog.dismiss();
                                return false;
                            }
                        });

                    }

                });

                final View downloadFonts = inflate.findViewById(R.id.downloadFonts);
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
                final TextView textAlign = (TextView) inflate.findViewById(R.id.textAlign);
                textAlign.setText(TxtUtils.underline(alignConst.get(BookCSS.get().textAlign)));
                textAlign.setOnClickListener(new OnClickListener() {

                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public void onClick(View v) {
                        if (Build.VERSION.SDK_INT <= 10) {
                            BookCSS.get().textAlign += 1;
                            if (BookCSS.get().textAlign == 4) {
                                BookCSS.get().textAlign = 0;
                            }
                            textAlign.setText(TxtUtils.underline(alignConst.get(BookCSS.get().textAlign)));
                            return;
                        }

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
                final CustomColorView linkColorDay = (CustomColorView) inflate.findViewById(R.id.linkColorDay);
                linkColorDay.withDefaultColors(Color.parseColor(BookCSS.LINK_COLOR_DAY), Color.parseColor(BookCSS.LINK_COLOR_UNIVERSAL));
                linkColorDay.init(Color.parseColor(BookCSS.get().linkColorDay));
                linkColorDay.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        BookCSS.get().linkColorDay = string;
                        return false;
                    }
                });

                final CustomColorView linkColorNight = (CustomColorView) inflate.findViewById(R.id.linkColorNight);
                linkColorNight.withDefaultColors(Color.parseColor(BookCSS.LINK_COLOR_NIGHT), Color.parseColor(BookCSS.LINK_COLOR_UNIVERSAL));
                linkColorNight.init(Color.parseColor(BookCSS.get().linkColorNight));
                linkColorNight.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        BookCSS.get().linkColorNight = string;
                        return false;
                    }
                });
                linkColorDay.getText1().getLayoutParams().width = Dips.dpToPx(150);
                linkColorNight.getText1().getLayoutParams().width = Dips.dpToPx(150);

                TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.onResetStyles)).setOnClickListener(new OnClickListener() {

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

                                lineHeight.reset(BookCSS.get().lineHeight);
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
                if (initCssHash != BookCSS.get().toCssString().hashCode() || initAppHash != Objects.hashCode(AppState.get())) {
                    AppState.get().save(controller.getActivity());
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
        final int cssHash = BookCSS.get().toCssString().hashCode();
        final int appHash = Objects.hashCode(AppState.get());

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

                inflate.findViewById(R.id.allBGConfig).setVisibility(Dips.isEInk(inflate.getContext()) ? View.GONE : View.VISIBLE);

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
                        if (Build.VERSION.SDK_INT <= 10) {
                            Toast.makeText(anchor.getContext(), R.string.this_function_will_works_in_modern_android, Toast.LENGTH_SHORT).show();
                            return;
                        }
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

                ImageView brightness = (ImageView) inflate.findViewById(R.id.onBrightness);
                brightness.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        AppState.get().isDayNotInvert = !AppState.get().isDayNotInvert;
                        controller.restartActivity();
                    }
                });
                brightness.setImageResource(!AppState.get().isDayNotInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

                final ImageView isCrop = (ImageView) inflate.findViewById(R.id.onCrop);
                // isCrop.setVisibility(controller.isTextFormat() ||
                // AppState.get().isCut ? View.GONE : View.VISIBLE);
                isCrop.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        AppState.get().isCrop = !AppState.get().isCrop;
                        SettingsManager.getBookSettings().updateFromAppState();
                        TintUtil.setTintImageWithAlpha(isCrop, !AppState.get().isCrop ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);
                        updateUIRefresh.run();
                    }
                });
                TintUtil.setTintImageWithAlpha(isCrop, !AppState.get().isCrop ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);

                final ImageView bookCut = (ImageView) inflate.findViewById(R.id.bookCut);
                // bookCut.setVisibility(controller.isTextFormat() ? View.GONE :
                // View.VISIBLE);
                bookCut.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        closeDialog();
                        DragingDialogs.sliceDialog(anchor, controller, updateUIRefresh, new ResultResponse<Integer>() {

                            @Override
                            public boolean onResultRecive(Integer result) {
                                TintUtil.setTintImageWithAlpha(bookCut, !AppState.get().isCut ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);
                                SettingsManager.getBookSettings().updateFromAppState();
                                EventBus.getDefault().post(new InvalidateMessage());
                                return false;
                            }
                        });
                    }
                });
                TintUtil.setTintImageWithAlpha(bookCut, !AppState.get().isCut ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);

                inflate.findViewById(R.id.onFullScreen).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        AppState.get().isFullScreen = !AppState.get().isFullScreen;
                        DocumentController.chooseFullScreen(controller.getActivity(), AppState.get().isFullScreen);
                        if (controller.isTextFormat()) {
                            if (onRefresh != null) {
                                onRefresh.run();
                            }
                            controller.restartActivity();
                        }
                    }
                });
                View tts = inflate.findViewById(R.id.onTTS);
                tts.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        closeDialog();
                        DragingDialogs.textToSpeachDialog(anchor, controller);
                    }
                });

                final ImageView pin = (ImageView) inflate.findViewById(R.id.onPin);
                pin.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        AppState.get().isShowToolBar = !AppState.get().isShowToolBar;
                        pin.setImageResource(AppState.get().isShowToolBar ? R.drawable.glyphicons_336_pushpin : R.drawable.glyphicons_200_ban);
                        if (onRefresh != null) {
                            onRefresh.run();
                        }

                    }
                });
                pin.setImageResource(AppState.get().isShowToolBar ? R.drawable.glyphicons_336_pushpin : R.drawable.glyphicons_200_ban);

                // TOP panel end

                CheckBox isPreText = (CheckBox) inflate.findViewById(R.id.isPreText);
                isPreText.setChecked(AppState.get().isPreText);
                isPreText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isPreText = isChecked;
                    }
                });

                isPreText.setVisibility(BookType.TXT.is(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                CheckBox isLineBreaksText = (CheckBox) inflate.findViewById(R.id.isLineBreaksText);
                isLineBreaksText.setChecked(AppState.get().isLineBreaksText);
                isLineBreaksText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isLineBreaksText = isChecked;
                    }
                });

                isLineBreaksText.setVisibility(BookType.TXT.is(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                //

                TextView moreSettings = (TextView) inflate.findViewById(R.id.moreSettings);
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

                TextView performanceSettings = TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.performanceSettigns));
                performanceSettings.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        performanceSettings(anchor, controller, onRefresh, updateUIRefresh);
                    }
                });

                TextView statusBarSettings = TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.statusBarSettings));
                statusBarSettings.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        statusBarSettings(anchor, controller, onRefresh, updateUIRefresh);
                    }
                });

                final CustomSeek fontSizeSp = (CustomSeek) inflate.findViewById(R.id.fontSizeSp);
                fontSizeSp.init(10, 70, AppState.get().fontSizeSp);
                fontSizeSp.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().fontSizeSp = result;
                        return false;
                    }
                });
                fontSizeSp.setValueText("" + AppState.get().fontSizeSp);

                inflate.findViewById(R.id.fontSizeLayout).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);
                inflate.findViewById(R.id.fontNameSelectionLayout).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                final TextView textFontName = (TextView) inflate.findViewById(R.id.textFontName);
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
                CheckBox isCropBorders = (CheckBox) inflate.findViewById(R.id.isCropBorders);
                isCropBorders.setChecked(controller.isCropCurrentBook());
                isCropBorders.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        controller.onCrop();

                    }
                });

                // volume
                final CheckBox isReverseKyes = (CheckBox) inflate.findViewById(R.id.isReverseKyes);
                isReverseKyes.setChecked(AppState.get().isReverseKeys);
                isReverseKyes.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isReverseKeys = isChecked;
                    }
                });

                isReverseKyes.setEnabled(AppState.get().isUseVolumeKeys ? true : false);

                CheckBox isUseVolumeKeys = (CheckBox) inflate.findViewById(R.id.isUseVolumeKeys);
                isUseVolumeKeys.setChecked(AppState.get().isUseVolumeKeys);
                isUseVolumeKeys.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isUseVolumeKeys = isChecked;
                        isReverseKyes.setEnabled(AppState.get().isUseVolumeKeys ? true : false);
                    }
                });

                // orientation begin

                final TextView screenOrientation = (TextView) inflate.findViewById(R.id.screenOrientation);
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

                final TextView selectedDictionaly = (TextView) inflate.findViewById(R.id.selectedDictionaly);
                selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
                selectedDictionaly.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        DialogTranslateFromTo.show(controller.getActivity(), new Runnable() {

                            @Override
                            public void run() {
                                selectedDictionaly.setText(DialogTranslateFromTo.getSelectedDictionaryUnderline());
                            }
                        });
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

                TextView textCustomizeFontBGColor = (TextView) inflate.findViewById(R.id.textCustomizeFontBGColor);
                if (AppState.get().isCustomizeBgAndColors || controller.isTextFormat()) {
                    textCustomizeFontBGColor.setText(R.string.customize_font_background_colors);
                } else {
                    textCustomizeFontBGColor.setText(R.string.customize_background_color);
                }

                final ImageView onDayColorImage = (ImageView) inflate.findViewById(R.id.onDayColorImage);
                final TextView textDayColor = TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.onDayColor));
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

                                ImageLoader.getInstance().clearDiskCache();
                                ImageLoader.getInstance().clearMemoryCache();

                                if (AppState.get().isUseBGImageDay) {
                                    textDayColor.setBackgroundDrawable(MagicHelper.getBgImageDayDrawable(true));
                                }
                            }
                        });
                    }
                });

                final ImageView onNigthColorImage = (ImageView) inflate.findViewById(R.id.onNigthColorImage);
                final TextView textNigthColor = TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.onNigthColor));
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

                final LinearLayout lc = (LinearLayout) inflate.findViewById(R.id.preColors);

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

                TxtUtils.underlineTextView((TextView) inflate.findViewById(R.id.onDefaultColor)).setOnClickListener(new OnClickListener() {

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

                                textDayColor.setTextColor(AppState.COLOR_BLACK);
                                textDayColor.setBackgroundColor(AppState.COLOR_WHITE);

                                AppState.get().colorNigthText = AppState.COLOR_WHITE;
                                AppState.get().colorNigthBg = AppState.COLOR_BLACK;

                                textNigthColor.setTextColor(AppState.COLOR_WHITE);
                                textNigthColor.setBackgroundColor(AppState.COLOR_BLACK);

                                TintUtil.setTintImageWithAlpha(onDayColorImage, AppState.get().colorDayText);
                                TintUtil.setTintImageWithAlpha(onNigthColorImage, AppState.get().colorNigthText);

                                AppState.get().statusBarColorDay = AppState.TEXT_COLOR_DAY;
                                AppState.get().statusBarColorNight = AppState.TEXT_COLOR_NIGHT;

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

                if (//
                appHash != Objects.hashCode(AppState.get()) || //
                (controller.isTextFormat() && cssHash != BookCSS.get().toCssString().hashCode())) {
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
