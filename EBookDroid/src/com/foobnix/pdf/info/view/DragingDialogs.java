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
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.android.utils.Views;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.hypen.HyphenPattern;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.BookCSS;
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
import com.foobnix.pdf.search.activity.PageImageState;
import com.foobnix.pdf.search.activity.msg.FlippingStart;
import com.foobnix.pdf.search.activity.msg.FlippingStop;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MovePageAction;
import com.foobnix.pdf.search.menu.MenuBuilderM;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
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
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

    public static void dialogBlueLight(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {

        DragingPopup dialog = new DragingPopup(R.string.bluelight_filter, anchor, PREF_WIDTH, PREF_HEIGHT) {

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
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {
                final Activity activity = controller.getActivity();
                final View inflate = inflater.inflate(R.layout.dialog_bluelight, null, false);

                /** Blue Light Color start **/

                inflate.findViewById(R.id.blueLightLayout).setVisibility(AppState.get().isInkMode ? View.GONE : View.VISIBLE);

                final CustomColorView blueLightColor = (CustomColorView) inflate.findViewById(R.id.blueLightColor);
                TxtUtils.bold(blueLightColor.getText1());
                blueLightColor.withDefaultColors(AppState.BLUE_FILTER_DEFAULT_COLOR, Color.BLACK, Color.RED, Color.GREEN);
                blueLightColor.init(AppState.get().blueLightColor);
                blueLightColor.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        AppState.get().blueLightColor = Color.parseColor(string);
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        Keyboards.hideNavigation(controller.getActivity());
                        return false;
                    }
                });

                final CustomSeek blueLightAlpha = (CustomSeek) inflate.findViewById(R.id.blueLightAlpha);
                blueLightAlpha.init(0, 99, AppState.getInstance().blueLightAlpha);
                blueLightAlpha.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().blueLightAlpha = result;
                        blueLightAlpha.setValueText("" + AppState.getInstance().blueLightAlpha + "%");
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        return false;
                    }
                });
                blueLightAlpha.setValueText("" + AppState.getInstance().blueLightAlpha + "%");

                TextView blueLightOff = (TextView) inflate.findViewById(R.id.blueLightOff);
                TxtUtils.underlineTextView(blueLightOff);
                blueLightOff.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppState.get().blueLightAlpha = 0;
                        blueLightAlpha.reset(0);
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                    }
                });

                /** Blue Light Color end **/

                return inflate;
            }
        };
        dialog.setOnCloseListener(new Runnable() {

            @Override
            public void run() {
                AppState.get().save(controller.getActivity());

            }
        });
        dialog.show(DragingPopup.PREF + "_blueLightFilter");

    }

    public static void contrastAndBrigtness(final FrameLayout anchor, final DocumentController controller, final Runnable onRealod, final Runnable onRestart) {

        DragingPopup dialog = new DragingPopup(R.string.contrast_and_brightness, anchor, 300, 230) {

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

                TintUtil.setTintImage(onUp);
                TintUtil.setTintImage(onDonw);
                TintUtil.setTintImage(onLeft);
                TintUtil.setTintImage(onRight);
                TintUtil.setTintImage(onPlus);
                TintUtil.setTintImage(onMinus);
                TintUtil.setTintImage(onCenter);
                TintUtil.setTintImage(onCrop);

                onCrop.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppState.get().isCrop = !AppState.get().isCrop;
                        SettingsManager.getBookSettings().updateFromAppState();
                        updateUIRefresh.run();

                        if (AppState.get().isCrop) {
                            TintUtil.setTintImage(onCrop, TintUtil.COLOR_ORANGE);
                        } else {
                            TintUtil.setTintImage(onCrop);
                        }
                    }
                });

                if (AppState.get().isCrop) {
                    TintUtil.setTintImage(onCrop, TintUtil.COLOR_ORANGE);
                } else {
                    TintUtil.setTintImage(onCrop);
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

        DragingPopup dialog = new DragingPopup(R.string.text_to_speech, anchor, 300, 440) {

            @Override
            @SuppressLint("NewApi")
            public View getContentView(LayoutInflater inflater) {

                final Activity activity = controller.getActivity();
                final View view = inflater.inflate(R.layout.dialog_tts, null, false);

                final TextView ttsPage = (TextView) view.findViewById(R.id.ttsPage);
                // ttsPage.setText(activity.getString(R.string.page) + " " +
                // controller.getCurentPageFirst1());

                final TextView textEngine = (TextView) view.findViewById(R.id.ttsEngine);
                final TextView textDebug = (TextView) view.findViewById(R.id.textDebug);

                final TextView timerTime = (TextView) view.findViewById(R.id.timerTime);
                final TextView timerStart = (TextView) view.findViewById(R.id.timerStart);

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
                        ttsPage.setText(
                                TempHolder.get().timerFinishTime == 0 ? "" : controller.getString(R.string.reading_will_be_stopped) + " " + DateFormat.getTimeFormat(activity).format(TempHolder.get().timerFinishTime));
                    }
                });

                timerStart.setText(TempHolder.get().timerFinishTime == 0 ? R.string.start : R.string.cancel);
                ttsPage.setText(TempHolder.get().timerFinishTime == 0 ? "" : controller.getString(R.string.reading_will_be_stopped) + " " + DateFormat.getTimeFormat(activity).format(TempHolder.get().timerFinishTime));

                TTSEngine.get().getTTS(new OnInitListener() {

                    @Override
                    public void onInit(int status) {
                        textEngine.setText(TTSEngine.get().getCurrentEngineName());
                        TxtUtils.underlineTextView(textEngine);
                    }
                });
                textEngine.setText(TTSEngine.get().getCurrentEngineName());

                TxtUtils.underlineTextView(textEngine);

                textEngine.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PopupMenu menu = new PopupMenu(v.getContext(), v);
                        List<EngineInfo> engines = TTSEngine.get().getTTS().getEngines();
                        for (final EngineInfo eInfo : engines) {
                            final String name = TTSEngine.engineToString(eInfo);
                            menu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    textEngine.setText(name);
                                    TTSEngine.get().setTTSWithEngine(eInfo.name);
                                    TxtUtils.underlineTextView(textEngine);
                                    return false;
                                }
                            });
                        }
                        menu.show();
                    }
                });

                view.findViewById(R.id.ttsSettings).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            TTSEngine.get().stop();
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

                view.findViewById(R.id.onPlay).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), "");

                    }
                });
                view.findViewById(R.id.onPause).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        TTSEngine.get().stop();
                        TTSNotification.hideNotification();
                    }
                });

                view.findViewById(R.id.onPause).setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        textDebug.setVisibility(View.VISIBLE);
                        return true;
                    }
                });

                final SeekBar seekBarSpeed = (SeekBar) view.findViewById(R.id.seekBarSpeed);
                seekBarSpeed.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            AppState.get().ttsSpeed = (float) progress / 100;
                        }

                    }
                });
                final SeekBar seekBarPitch = (SeekBar) view.findViewById(R.id.seekBarPitch);
                seekBarPitch.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            AppState.get().ttsPitch = (float) progress / 100;
                        }
                    }
                });

                seekBarPitch.setProgress((int) (AppState.get().ttsPitch * 100));
                seekBarSpeed.setProgress((int) (AppState.get().ttsSpeed * 100));

                final AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

                final SeekBar seekVolume = (SeekBar) view.findViewById(R.id.seekVolume);
                seekVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                seekVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                seekVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                        }
                    }
                });

                TxtUtils.underlineTextView((TextView) view.findViewById(R.id.restore_defaults)).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        AlertDialogs.showOkDialog(v.getContext(), controller.getString(R.string.restore_defaults_full), new Runnable() {

                            @Override
                            public void run() {
                                seekBarPitch.setProgress(100);
                                seekBarSpeed.setProgress(100);

                                AppState.get().ttsPitch = (float) 1.0;
                                AppState.get().ttsSpeed = (float) 1.0;

                                TTSEngine.get().shutdown();
                                TTSEngine.get().getTTS();

                                textEngine.setText(TTSEngine.get().getCurrentEngineName());
                                TxtUtils.underlineTextView(textEngine);
                            }
                        });

                    }
                });
                //

                CheckBox notificationOngoing = (CheckBox) view.findViewById(R.id.notificationOngoing);
                notificationOngoing.setChecked(AppState.getInstance().notificationOngoing);
                notificationOngoing.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().notificationOngoing = isChecked;
                        TTSNotification.hideNotification();
                        TTSNotification.showLast();
                    }
                });

                view.findViewById(R.id.ttsSkeakToFile).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Toast.makeText(controller.getActivity(), R.string.please_wait, Toast.LENGTH_LONG).show();
                        TTSEngine.get().speakToFile(controller);

                    }
                });

                final TextView ttsSpeakPath = (TextView) view.findViewById(R.id.ttsSpeakPath);
                ttsSpeakPath.setText(AppState.getInstance().ttsSpeakPath);
                TxtUtils.underlineTextView(ttsSpeakPath);
                ttsSpeakPath.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        ChooserDialogFragment.chooseFolder((FragmentActivity) controller.getActivity(), AppState.getInstance().ttsSpeakPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                            @Override
                            public boolean onResultRecive(String nPath, Dialog dialog) {
                                AppState.getInstance().ttsSpeakPath = nPath;
                                ttsSpeakPath.setText(nPath);
                                TxtUtils.underlineTextView(ttsSpeakPath);
                                dialog.dismiss();
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

                final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar1);
                final TextView searchingMsg = (TextView) view.findViewById(R.id.searching);
                final GridView gridView = (GridView) view.findViewById(R.id.grid1);
                gridView.setColumnWidth(Dips.dpToPx(80));

                final BaseItemLayoutAdapter<Integer> adapter = new BaseItemLayoutAdapter<Integer>(anchor.getContext(), android.R.layout.simple_spinner_dropdown_item) {

                    @Override
                    public void populateView(View inflate, int arg1, Integer page) {
                        final TextView text = Views.text(inflate, android.R.id.text1, TxtUtils.deltaPage(page + 1));
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
                if (page == -1) {
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

        try {
            int number = Integer.parseInt(AppState.get().selectedText);
            Dialogs.showDeltaPage(anchor, controller, number, reloadUI);
            return null;
        } catch (Exception e) {
        }

        return new DragingPopup(R.string.text, anchor, 260, 400) {
            @Override
            public View getContentView(LayoutInflater inflater) {
                final View view = inflater.inflate(R.layout.dialog_selected_text, null, false);
                final LinearLayout linearLayoutColor = (LinearLayout) view.findViewById(R.id.colorsLine);
                linearLayoutColor.removeAllViews();
                List<String> colors = new ArrayList<String>(AppState.getInstance().COLORS);
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
                            TintUtil.setTintImage(image, colorInt);

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
                        highlight.setColorFilter(Color.parseColor(AppState.getInstance().annotationTextColor), Mode.SRC_ATOP);

                        Drawable underline = controller.getActivity().getResources().getDrawable(R.drawable.glyphicons_104_te_underline);
                        underline.setColorFilter(Color.parseColor(AppState.getInstance().annotationTextColor), Mode.SRC_ATOP);

                        Drawable strikeout = controller.getActivity().getResources().getDrawable(R.drawable.glyphicons_105_te_strike);
                        strikeout.setColorFilter(Color.parseColor(AppState.getInstance().annotationTextColor), Mode.SRC_ATOP);

                        menu.getMenu().add(R.string.highlight_of_text).setIcon(highlight).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().customConfigColors += "H" + AppState.getInstance().annotationTextColor + ",";
                                updateConfigRunnable.run();
                                return false;
                            }
                        });
                        menu.getMenu().add(R.string.underline_of_text).setIcon(underline).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().customConfigColors += "U" + AppState.getInstance().annotationTextColor + ",";
                                updateConfigRunnable.run();
                                return false;
                            }
                        });
                        menu.getMenu().add(R.string.strikethrough_of_text).setIcon(strikeout).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().customConfigColors += "S" + AppState.getInstance().annotationTextColor + ",";
                                updateConfigRunnable.run();
                                return false;
                            }
                        });
                        menu.show();

                        PopupHelper.initIcons(menu, Color.parseColor(AppState.getInstance().annotationTextColor));

                    }
                });

                underLine.setColorFilter(Color.parseColor(AppState.getInstance().annotationTextColor));
                strike.setColorFilter(Color.parseColor(AppState.getInstance().annotationTextColor));
                selection.setColorFilter(Color.parseColor(AppState.getInstance().annotationTextColor));

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
                            AppState.getInstance().annotationTextColor = colorName;
                            underLine.setColorFilter(Color.parseColor(colorName));
                            strike.setColorFilter(Color.parseColor(colorName));
                            selection.setColorFilter(Color.parseColor(colorName));
                        }
                    });

                }

                final EditText editText = (EditText) view.findViewById(R.id.editText);
                String selectedText = AppState.get().selectedText;

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
                        TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), editText.getText().toString().trim());
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
                onBookSearch.setVisibility(AppState.get().selectedText.contains(" ") ? View.GONE : View.VISIBLE);
                onBookSearch.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        searchMenu(anchor, controller, AppState.get().selectedText);
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

                PackageManager pm = anchor.getContext().getPackageManager();

                final List<ResolveInfo> proccessTextList = pm.queryIntentActivities(intentProccessText, 0);
                final List<ResolveInfo> searchList = pm.queryIntentActivities(intentSearch, 0);
                final List<ResolveInfo> sendList = pm.queryIntentActivities(intentSend, 0);

                final List<ResolveInfo> all = new ArrayList<ResolveInfo>();
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
                        if (app.activityInfo.packageName.toLowerCase().contains(pkgKey)) {
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

                                        if (proccessTextList.contains(app)) {
                                            intentProccessText.addCategory(Intent.CATEGORY_LAUNCHER);
                                            intentProccessText.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentProccessText.setComponent(name);

                                            intentProccessText.putExtra(Intent.EXTRA_TEXT, selecteText);
                                            intentProccessText.putExtra(Intent.EXTRA_PROCESS_TEXT, selecteText);
                                            intentProccessText.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, selecteText);

                                            controller.getActivity().startActivity(intentProccessText);
                                        } else if (searchList.contains(app)) {
                                            intentSearch.addCategory(Intent.CATEGORY_LAUNCHER);
                                            intentSearch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            intentSearch.setComponent(name);

                                            intentSearch.putExtra(SearchManager.QUERY, selecteText);
                                            intentSearch.putExtra(Intent.EXTRA_TEXT, selecteText);

                                            controller.getActivity().startActivity(intentSearch);
                                        } else if (sendList.contains(app)) {
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
                        controller.underlineText(Color.parseColor(AppState.getInstance().annotationTextColor), 2.0f, AnnotationType.UNDERLINE);
                        closeDialog();
                        controller.saveAnnotationsToFile();
                    }
                });
                view.findViewById(R.id.onStrike).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.underlineText(Color.parseColor(AppState.getInstance().annotationTextColor), 2.0f, AnnotationType.STRIKEOUT);
                        closeDialog();
                        controller.saveAnnotationsToFile();
                    }
                });
                view.findViewById(R.id.onSelection).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        controller.underlineText(Color.parseColor(AppState.getInstance().annotationTextColor), 2.0f, AnnotationType.HIGHLIGHT);
                        closeDialog();
                        controller.saveAnnotationsToFile();
                    }
                });

                if (!BookType.PDF.is(controller.getCurrentBook().getPath()) || !withAnnotation) {
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
                controller.clearSelectedText();
            }
        });

    }

    public static void thumbnailDialog(final FrameLayout anchor, final DocumentController controller) {
        new DragingPopup(R.string.go_to_page_dialog, anchor, 300, 400) {
            @Override
            public View getContentView(LayoutInflater inflater) {
                View view = inflater.inflate(R.layout.dialog_go_to_page, null, false);

                final EditText number = (EditText) view.findViewById(R.id.edit1);
                number.clearFocus();
                number.setText("" + controller.getCurentPageFirst1());
                final GridView grid = (GridView) view.findViewById(R.id.grid1);
                int dpToPx = Dips.dpToPx(AppState.get().coverSmallSize);

                if (AppState.get().isDouble && Dips.isHorizontal()) {
                    dpToPx = dpToPx * 2;
                }
                grid.setColumnWidth(dpToPx);

                final File currentBook = controller.getCurrentBook();
                if (ExtUtils.isValidFile(currentBook)) {
                    grid.setAdapter(new PageThumbnailAdapter(anchor.getContext(), controller.getPageCount(), controller.getCurentPageFirst1() - 1) {
                        @Override
                        public PageUrl getPageUrl(int page) {
                            PageUrl pageUrl = null;
                            if (controller.isTextFormat()) {
                                // pageUrl = controller.getPageUrl(page);
                            } else {
                            }
                            pageUrl = PageUrl.buildSmall(currentBook.getPath(), page);
                            return pageUrl;
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
                grid.setSelection(controller.getCurentPage() - 1);

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
                            page = Integer.valueOf(number.getText().toString());
                        } catch (NumberFormatException e) {
                            number.setText("1");
                        }
                        Keyboards.close(number);

                        if (page >= 0 && page <= controller.getPageCount()) {
                            controller.onGoToPage(page);
                        }
                        grid.setSelection(page - 1);
                    }
                });

                number.setOnEditorActionListener(new OnEditorActionListener() {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {

                            int page = 1;
                            try {
                                page = Integer.valueOf(number.getText().toString());
                            } catch (NumberFormatException e) {
                                number.setText("1");
                            }
                            if (page >= 0 && page <= controller.getPageCount()) {
                                controller.onGoToPage(page);
                            }
                            grid.setSelection(page - 1);
                            Keyboards.close(number);
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
                // ImageLoader.getInstance().clearAllTasks();
            }
        });

    }

    public static void editColorsPanel(final FrameLayout anchor, final DocumentController controller, final DrawView drawView, final boolean force) {
        drawView.setOnFinishDraw(new Runnable() {

            @Override
            public void run() {
                String annotationDrawColor = AppState.getInstance().annotationDrawColor;
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
                AppState.getInstance().annotationDrawColor = "";
                drawView.setVisibility(View.GONE);
                drawView.clear();
            };

            @Override
            public View getContentView(final LayoutInflater inflater) {
                View a = inflater.inflate(R.layout.edit_panel, null, false);
                final GridView grid = (GridView) a.findViewById(R.id.gridColors);

                if (AppState.get().editWith == AppState.EDIT_DELETE) {
                    AppState.getInstance().annotationDrawColor = AppState.get().COLORS.get(0);
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
                                    int acolor = IMG.alphaColor(AppState.get().editAlphaColor, AppState.getInstance().annotationDrawColor);
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
                                    int acolor = IMG.alphaColor(AppState.get().editAlphaColor, AppState.getInstance().annotationDrawColor);
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
                        if (color.equals(AppState.getInstance().annotationDrawColor)) {
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
                        if (view.getTag().equals(AppState.getInstance().annotationDrawColor)) {
                            AppState.getInstance().annotationDrawColor = "";
                            drawView.setVisibility(View.GONE);
                        } else {
                            AppState.getInstance().annotationDrawColor = (String) view.getTag();
                            int acolor = IMG.alphaColor(AppState.get().editAlphaColor, AppState.getInstance().annotationDrawColor);
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
        new DragingPopup(R.string.recent, anchor, PREF_WIDTH, PREF_HEIGHT) {

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

                DefaultListeners.bindAdapter(controller.getActivity(), recentAdapter);

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

        String TEXT = controller.getString(R.string.fast_bookmark) + " " + TxtUtils.LONG_DASH + " «" + controller.getString(R.string.page) + " " + page + "»";
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
                        String TEXT = controller.getString(R.string.fast_bookmark) + " " + TxtUtils.LONG_DASH + " «" + controller.getString(R.string.page) + " " + page + "»";
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

    public static void showContent(final FrameLayout anchor, final DocumentController controller) {

        final OnItemClickListener onClickContent = new OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                final OutlineLinkWrapper link = (OutlineLinkWrapper) parent.getItemAtPosition(position);
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
        new DragingPopup(anchor.getContext().getString(R.string.content_of_book), anchor, 300, 400) {

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
                                            final OutlineAdapter adapter = new OutlineAdapter(controller.getActivity(), outline, currentByPageNumber);
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

                if (BookType.FB2.is(controller.getCurrentBook().getPath())) {
                    setTitlePopupIcon(AppState.getInstance().outlineMode == AppState.OUTLINE_ONLY_HEADERS ? R.drawable.glyphicons_114_justify : R.drawable.glyphicons_114_justify_sub);
                    titlePopupMenu = new MyPopupMenu(controller.getActivity(), null);

                    List<Integer> names = Arrays.asList(R.string.headings_only, R.string.heading_and_subheadings);
                    final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_114_justify_sub);
                    final List<Integer> actions = Arrays.asList(AppState.OUTLINE_ONLY_HEADERS, AppState.OUTLINE_HEADERS_AND_SUBHEADERES);

                    for (int i = 0; i < names.size(); i++) {
                        final int index = i;
                        titlePopupMenu.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.getInstance().outlineMode = actions.get(index);
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

        new DragingPopup(anchor.getContext().getString(R.string.automatic_page_flipping), anchor, 300, 300) {

            @Override
            public View getContentView(LayoutInflater inflater) {
                View inflate = inflater.inflate(R.layout.dialog_flipping_pages, null, false);

                CheckBox isScrollAnimation = (CheckBox) inflate.findViewById(R.id.isScrollAnimation);
                isScrollAnimation.setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
                isScrollAnimation.setChecked(AppState.getInstance().isScrollAnimation);
                isScrollAnimation.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isScrollAnimation = isChecked;
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

                CheckBox isShowToolBar = (CheckBox) inflate.findViewById(R.id.isShowToolBar);
                isShowToolBar.setChecked(AppState.getInstance().isShowToolBar);
                isShowToolBar.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isShowToolBar = isChecked;
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
                    }
                });
                isShowReadingProgress.setChecked(AppState.get().isShowReadingProgress);

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
                        }
                    }
                });
                isShowChaptersOnProgress.setChecked(AppState.get().isShowChaptersOnProgress);
                isShowChaptersOnProgress.setEnabled(AppState.get().isShowReadingProgress);

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

                // status bar

                final CheckBox isShowSatusBar = (CheckBox) inflate.findViewById(R.id.isShowSatusBar);
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
                isShowSatusBar.setChecked(AppState.get().isShowToolBar);
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
                progressLineHeight.init(1, 10, AppState.get().progressLineHeight);
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
        final int appHash = AppState.get().hashCode();

        DragingPopup dialog = new DragingPopup(R.string.advanced_settings, anchor, PREF_WIDTH, PREF_HEIGHT) {

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
                View inflate = inflater.inflate(R.layout.dialog_adv_preferences, null, false);

                CheckBox isLoopAutoplay = (CheckBox) inflate.findViewById(R.id.isLoopAutoplay);
                isLoopAutoplay.setChecked(AppState.getInstance().isLoopAutoplay);
                isLoopAutoplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isLoopAutoplay = isChecked;
                    }
                });

                CheckBox isBrighrnessEnable = (CheckBox) inflate.findViewById(R.id.isBrighrnessEnable);
                isBrighrnessEnable.setChecked(AppState.getInstance().isBrighrnessEnable);
                isBrighrnessEnable.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isBrighrnessEnable = isChecked;
                    }
                });

                CheckBox isRewindEnable = (CheckBox) inflate.findViewById(R.id.isRewindEnable);
                isRewindEnable.setChecked(AppState.getInstance().isRewindEnable);
                isRewindEnable.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isRewindEnable = isChecked;
                    }
                });

                final CustomSeek mouseWheelSpeed = (CustomSeek) inflate.findViewById(R.id.seekWheelSpeed);
                mouseWheelSpeed.init(1, 200, AppState.getInstance().mouseWheelSpeed);
                mouseWheelSpeed.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.getInstance().mouseWheelSpeed = result;
                        return false;
                    }
                });

                CheckBox isScrollAnimation = (CheckBox) inflate.findViewById(R.id.isScrollAnimation);
                isScrollAnimation.setVisibility(AppState.get().isAlwaysOpenAsMagazine ? View.VISIBLE : View.GONE);
                isScrollAnimation.setChecked(AppState.getInstance().isScrollAnimation);
                isScrollAnimation.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isScrollAnimation = isChecked;
                    }
                });

                if (AppState.getInstance().isAlwaysOpenAsMagazine) {
                    inflate.findViewById(R.id.onDoubleTapLayout).setVisibility(View.GONE);
                } else {
                    // inflate.findViewById(R.id.onDoubleTapLayout).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath())
                    // ? View.GONE : View.VISIBLE);
                    inflate.findViewById(R.id.onDoubleTapLayout).setVisibility(View.VISIBLE);

                }

                CheckBox isVibration = (CheckBox) inflate.findViewById(R.id.isVibration);
                isVibration.setChecked(AppState.getInstance().isVibration);
                isVibration.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isVibration = isChecked;
                    }
                });

                CheckBox isOLED = (CheckBox) inflate.findViewById(R.id.isOLED);
                isOLED.setChecked(AppState.getInstance().isOLED);
                isOLED.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isOLED = isChecked;
                    }
                });

                CheckBox isLockPDF = (CheckBox) inflate.findViewById(R.id.isLockPDF);
                isLockPDF.setChecked(AppState.getInstance().isLockPDF);
                isLockPDF.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isLockPDF.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isLockPDF = isChecked;
                    }
                });

                CheckBox isCropPDF = (CheckBox) inflate.findViewById(R.id.isCropPDF);
                isCropPDF.setChecked(AppState.getInstance().isCropPDF);
                isCropPDF.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isCropPDF.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isCropPDF = isChecked;
                    }
                });

                CheckBox isCustomizeBgAndColors = (CheckBox) inflate.findViewById(R.id.isCustomizeBgAndColors);
                isCustomizeBgAndColors.setVisibility(controller.isTextFormat() ? View.GONE : View.VISIBLE);
                isCustomizeBgAndColors.setChecked(AppState.getInstance().isCustomizeBgAndColors);
                isCustomizeBgAndColors.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isCustomizeBgAndColors = isChecked;
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
                highlightByLetters.setChecked(AppState.getInstance().selectingByLetters);
                highlightByLetters.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().selectingByLetters = isChecked;
                    }
                });

                CheckBox isCutRTL = (CheckBox) inflate.findViewById(R.id.isCutRTL);
                isCutRTL.setChecked(AppState.getInstance().isCutRTL);
                isCutRTL.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isCutRTL = isChecked;
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
                        for (int i = 0; i <= 2f; i++) {
                            final int number = i;
                            popupMenu.getMenu().add("" + i).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.get().pagesInMemory = number;
                                    pagesInMemory.setText("" + AppState.get().pagesInMemory);
                                    TxtUtils.underlineTextView(pagesInMemory);
                                    return false;
                                }
                            });
                        }
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

                List<String> doubleTapNames = Arrays.asList(//
                        controller.getString(R.string.db_auto_scroll), //
                        controller.getString(R.string.db_auto_alignemnt), //
                        controller.getString(R.string.db_auto_center_horizontally), //
                        controller.getString(R.string.db_do_nothing), //
                        controller.getString(R.string.zoom_in_zoom_out));

                final List<Integer> doubleTapIDS = Arrays.asList(//
                        AppState.DOUBLE_CLICK_AUTOSCROLL, //
                        AppState.DOUBLE_CLICK_ADJUST_PAGE, //
                        AppState.DOUBLE_CLICK_CENTER_HORIZONTAL, //
                        AppState.DOUBLE_CLICK_NOTHING, //
                        AppState.DOUBLE_CLICK_ZOOM_IN_OUT);//

                final Spinner doubleTapSpinner = (Spinner) inflate.findViewById(R.id.doubleTapSpinner);
                doubleTapSpinner.setAdapter(new BaseItemLayoutAdapter<String>(controller.getActivity(), android.R.layout.simple_spinner_dropdown_item, doubleTapNames) {

                    @Override
                    public void populateView(View inflate, int arg1, String value) {
                        Views.text(inflate, android.R.id.text1, "" + value);
                    }
                });
                doubleTapSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        AppState.getInstance().doubleClickAction = doubleTapIDS.get(position);

                        try {
                            TextView textView = (TextView) doubleTapSpinner.getChildAt(0);
                            textView.setTextAppearance(controller.getActivity(), R.style.textLinkStyle);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                doubleTapSpinner.setSelection(doubleTapIDS.indexOf(AppState.getInstance().doubleClickAction));

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

                // image format
                final TextView imageFormat = (TextView) inflate.findViewById(R.id.imageFormat);
                // ((ViewGroup)
                // imageFormat.getParent()).setVisibility(AppState.get().isAlwaysOpenAsMagazine
                // ? View.VISIBLE : View.GONE);
                ((ViewGroup) imageFormat.getParent()).setVisibility(View.GONE);
                imageFormat.setText(AppState.get().imageFormat);
                TxtUtils.underlineTextView(imageFormat);
                imageFormat.setOnClickListener(new OnClickListener() {

                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        popupMenu.getMenu().add(AppState.PNG).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().imageFormat = AppState.PNG;
                                imageFormat.setText(AppState.get().imageFormat);
                                TxtUtils.underlineTextView(imageFormat);
                                return false;
                            }
                        });
                        popupMenu.getMenu().add(AppState.JPG).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().imageFormat = AppState.JPG;
                                imageFormat.setText(AppState.get().imageFormat);
                                TxtUtils.underlineTextView(imageFormat);
                                return false;
                            }
                        });
                        popupMenu.show();

                    }
                });

                // remind rest time
                final TextView remindRestTime = (TextView) inflate.findViewById(R.id.remindRestTime);
                final String minutesString = controller.getString(R.string.minutes).toLowerCase(Locale.US);
                remindRestTime.setText(AppState.get().remindRestTime + " " + minutesString);
                TxtUtils.underlineTextView(remindRestTime);
                remindRestTime.setOnClickListener(new OnClickListener() {

                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
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
                AppState.get().save(controller.getActivity());
                boolean one = appHash != AppState.get().hashCode();
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

        DragingPopup dialog = new DragingPopup(R.string.reading_settings, anchor, PREF_WIDTH, PREF_HEIGHT) {

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

                // hypenLang.setVisibility(isSupportHypens ? View.VISIBLE :
                // View.GONE);
                // hypenLangLabel.setVisibility(isSupportHypens ? View.VISIBLE :
                // View.GONE);

                hypenLang.setVisibility(View.GONE);
                hypenLangLabel.setVisibility(View.GONE);

                hypenLang.setText(DialogTranslateFromTo.getLanuageByCode(BookCSS.get().hypenLang));
                TxtUtils.underlineTextView(hypenLang);

                hypenLang.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                        HyphenPattern[] values = HyphenPattern.values();

                        List<String> all = new ArrayList<String>();
                        for (HyphenPattern p : values) {
                            all.add(p.lang);
                        }
                        Collections.sort(all);

                        for (final String lang : all) {

                            final String titleLang = DialogTranslateFromTo.getLanuageByCode(lang) + " (" + lang + ")";
                            popupMenu.getMenu().add(titleLang).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    BookCSS.get().hypenLang = lang;
                                    hypenLang.setText(titleLang);
                                    TxtUtils.underlineTextView(hypenLang);
                                    return false;
                                }
                            });
                        }
                        popupMenu.show();

                    }
                });
                // - hypens

                TextView customCSS = (TextView) inflate.findViewById(R.id.customCSS);
                TxtUtils.underlineTextView(customCSS);
                customCSS.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle(R.string.custom_css);
                        final EditText edit = new EditText(v.getContext());
                        edit.setMinWidth(Dips.dpToPx(1000));
                        edit.setLines(8);
                        edit.setGravity(Gravity.TOP);
                        edit.setText(BookCSS.get().customCSS);
                        builder.setView(edit);

                        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, final int id) {
                                BookCSS.get().customCSS = edit.getText().toString();
                                BookCSS.get().save(v.getContext());
                            }
                        });
                        builder.show();

                    }
                });

                final CustomSeek fontInterval = (CustomSeek) inflate.findViewById(R.id.fontInterval);
                fontInterval.init(0, 30, BookCSS.get().lineHeight);
                fontInterval.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().lineHeight = result;
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
                boolean isShow = BookType.FB2.is(controller.getCurrentBook().getPath()) || //
                BookType.HTML.is(controller.getCurrentBook().getPath()) || //
                BookType.TXT.is(controller.getCurrentBook().getPath());//

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
                marginTop.init(0, 30, BookCSS.get().marginTop);
                marginTop.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginTop = result;
                        return false;
                    }
                });

                final CustomSeek marginBottom = (CustomSeek) inflate.findViewById(R.id.marginBottom);
                marginBottom.init(0, 30, BookCSS.get().marginBottom);
                marginBottom.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginBottom = result;
                        return false;
                    }
                });

                final CustomSeek marginLeft = (CustomSeek) inflate.findViewById(R.id.marginLeft);
                marginLeft.init(0, 30, BookCSS.get().marginLeft);
                marginLeft.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginLeft = result;
                        return false;
                    }
                });

                final CustomSeek marginRight = (CustomSeek) inflate.findViewById(R.id.marginRight);
                marginRight.init(0, 30, BookCSS.get().marginRight);
                marginRight.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        BookCSS.get().marginRight = result;
                        return false;
                    }
                });

                // font folder
                final TextView fontsFolder = (TextView) inflate.findViewById(R.id.fontsFolder);
                fontsFolder.setText(TxtUtils.underline(BookCSS.get().fontFolder));
                fontsFolder.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ChooserDialogFragment.chooseFolder((FragmentActivity) controller.getActivity(), BookCSS.get().fontFolder).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                            @Override
                            public boolean onResultRecive(String nPath, Dialog dialog) {
                                File result = new File(nPath);
                                BookCSS.get().fontFolder = result.getPath();
                                fontsFolder.setText(TxtUtils.underline(BookCSS.get().fontFolder));
                                BookCSS.get().save(controller.getActivity());
                                dialog.dismiss();
                                return false;
                            }
                        });

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

                        AlertDialogs.showOkDialog(v.getContext(), controller.getString(R.string.restore_defaults_full), new Runnable() {

                            @Override
                            public void run() {
                                BookCSS.get().resetToDefault(controller.getActivity());

                                fontsFolder.setText(TxtUtils.underline(BookCSS.get().fontFolder));
                                textAlign.setText(TxtUtils.underline(alignConst.get(BookCSS.get().textAlign)));

                                fontWeight.reset(BookCSS.get().fontWeight / 100);
                                fontWeight.setValueText("" + BookCSS.get().fontWeight);

                                fontInterval.reset(BookCSS.get().lineHeight);
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
                if (initCssHash != BookCSS.get().toCssString().hashCode()) {
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

    static boolean isChangedColor = false;
    static boolean isChangedPreFormatting = false;

    public static DragingPopup preferences(final FrameLayout anchor, final DocumentController controller, final Runnable onRefresh, final Runnable updateUIRefresh) {
        final int initSP = AppState.get().fontSizeSp;
        final int cssHash = BookCSS.get().toCssString().hashCode();
        isChangedColor = false;
        isChangedPreFormatting = false;

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
                        AppState.get().isInvert = !AppState.get().isInvert;
                        controller.restartActivity();
                    }
                });
                brightness.setImageResource(!AppState.get().isInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

                final ImageView isCrop = (ImageView) inflate.findViewById(R.id.onCrop);
                // isCrop.setVisibility(controller.isTextFormat() ||
                // AppState.get().isCut ? View.GONE : View.VISIBLE);
                isCrop.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        AppState.get().isCrop = !AppState.get().isCrop;
                        SettingsManager.getBookSettings().updateFromAppState();
                        TintUtil.setTintImage(isCrop, !AppState.get().isCrop ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);
                        updateUIRefresh.run();
                    }
                });
                TintUtil.setTintImage(isCrop, !AppState.get().isCrop ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);

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
                                TintUtil.setTintImage(bookCut, !AppState.get().isCut ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);
                                SettingsManager.getBookSettings().updateFromAppState();
                                EventBus.getDefault().post(new InvalidateMessage());
                                return false;
                            }
                        });
                    }
                });
                TintUtil.setTintImage(bookCut, !AppState.get().isCut ? TintUtil.COLOR_TINT_GRAY : Color.LTGRAY);

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
                        isChangedPreFormatting = true;
                    }
                });

                isPreText.setVisibility(BookType.TXT.is(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                CheckBox isLineBreaksText = (CheckBox) inflate.findViewById(R.id.isLineBreaksText);
                isLineBreaksText.setChecked(AppState.get().isLineBreaksText);
                isLineBreaksText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppState.get().isLineBreaksText = isChecked;
                        isChangedPreFormatting = true;
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
                fontSizeSp.init(10, 70, AppState.getInstance().fontSizeSp);
                fontSizeSp.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().fontSizeSp = result;
                        return false;
                    }
                });
                fontSizeSp.setValueText("" + AppState.getInstance().fontSizeSp);

                inflate.findViewById(R.id.fontSizeLayout).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);
                inflate.findViewById(R.id.fontNameSelectionLayout).setVisibility(ExtUtils.isTextFomat(controller.getCurrentBook().getPath()) ? View.VISIBLE : View.GONE);

                final List<String> fontNames = BookCSS.get().getAllFontsFiltered();

                final TextView fontNamePreview = (TextView) inflate.findViewById(R.id.fontNamePreview);
                fontNamePreview.setTypeface(BookCSS.getTypeFaceForFont(BookCSS.get().normalFont));
                // Font choose
                final Spinner spinnerFontName = (Spinner) inflate.findViewById(R.id.spinnerFontName);
                spinnerFontName.setAdapter(new BaseItemLayoutAdapter<String>(controller.getActivity(), android.R.layout.simple_spinner_dropdown_item, fontNames) {

                    @Override
                    public void populateView(View inflate, int arg1, String value) {
                        TextView tv = (TextView) inflate.findViewById(android.R.id.text1);
                        tv.setText("" + value);
                        // tv.setTypeface(BookCSS.getTypeFaceForFont(value));
                    }
                });

                int indexOf = BookCSS.get().spinnerIndex;
                spinnerFontName.setSelection(indexOf);
                final Runnable runnable = new Runnable() {

                    @Override
                    public void run() {
                        spinnerFontName.setOnItemSelectedListener(new OnItemSelectedListener() {

                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                String font = fontNames.get(position);
                                BookCSS.get().spinnerIndex = spinnerFontName.getSelectedItemPosition();
                                BookCSS.get().resetAll(font);
                                fontNamePreview.setTypeface(BookCSS.getTypeFaceForFont(BookCSS.get().normalFont));

                                try {
                                    TextView textView = (TextView) spinnerFontName.getChildAt(0);
                                    textView.setTextAppearance(controller.getActivity(), R.style.textLinkStyle);
                                } catch (Exception e) {
                                    LOG.e(e);
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });

                        try {
                            TextView textView = (TextView) spinnerFontName.getChildAt(0);
                            textView.setTextAppearance(controller.getActivity(), R.style.textLinkStyle);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                };
                spinnerFontName.postDelayed(runnable, 100);

                final View moreFontSettings = inflate.findViewById(R.id.moreFontSettings);
                moreFontSettings.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        FontDialog.show(controller.getActivity(), new Runnable() {

                            @Override
                            public void run() {
                                spinnerFontName.setOnItemSelectedListener(null);
                                spinnerFontName.setSelection(fontNames.indexOf(BookCSS.get().normalFont));
                                spinnerFontName.postDelayed(runnable, 100);
                            }
                        });
                    }
                });

                CheckBox autoSettings = (CheckBox) inflate.findViewById(R.id.autoSettings);

                final BrigtnessDraw brigtnessDraw = (BrigtnessDraw) controller.getActivity().findViewById(R.id.brigtnessProgressView);

                final CustomSeek customBrightness = (CustomSeek) inflate.findViewById(R.id.customBrightness);
                customBrightness.init(0, 100, -1);
                customBrightness.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        float f = (float) result / 100;
                        if (f <= 0) {
                            f = 0;
                        }
                        AppState.getInstance().brightness = f;
                        DocumentController.applyBrigtness(controller.getActivity());
                        customBrightness.setValueText("" + result);
                        brigtnessDraw.showToast();
                        return false;
                    }
                });

                autoSettings.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        if (!buttonView.isPressed()) {
                            return;
                        }
                        if (isChecked) {// auto
                            final float val = DocumentController.getSystemBrigtness(controller.getActivity());
                            customBrightness.reset((int) (val * 100));
                            customBrightness.setEnabled(false);
                            AppState.getInstance().brightness = -1;
                            DocumentController.applyBrigtness(controller.getActivity());
                            brigtnessDraw.showToast(controller.getString(R.string.automatic));
                        } else {
                            customBrightness.setEnabled(true);
                        }

                        if (controller.getActivity() != null) {
                            AppState.getInstance().save(controller.getActivity());
                        }

                    }
                });
                if (AppState.getInstance().brightness < 0) {
                    autoSettings.setChecked(true);
                    customBrightness.reset((int) (100 * DocumentController.getSystemBrigtness(controller.getActivity())));
                } else {
                    autoSettings.setChecked(false);
                    customBrightness.reset((int) (100 * AppState.getInstance().brightness));
                }

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
                        updateUIRefresh.run();
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

                // orientation
                final List<String> orientations = Arrays.asList(controller.getString(R.string.automatic), controller.getString(R.string.landscape), controller.getString(R.string.portrait));
                final List<Integer> orIds = Arrays.asList(ActivityInfo.SCREEN_ORIENTATION_SENSOR, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

                final TextView screenOrientation = (TextView) inflate.findViewById(R.id.screenOrientation);
                try {
                    screenOrientation.setText(orientations.get(orIds.indexOf(AppState.getInstance().orientation)));
                } catch (Exception e) {
                    AppState.getInstance().orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                    screenOrientation.setText(orientations.get(orIds.indexOf(AppState.getInstance().orientation)));
                }

                TxtUtils.underlineTextView(screenOrientation);

                final TextView onBlueFilter = (TextView) inflate.findViewById(R.id.onBlueFilter);
                onBlueFilter.setVisibility(Dips.isEInk(controller.getActivity()) ? View.GONE : View.VISIBLE);
                TxtUtils.underlineTextView(onBlueFilter);
                onBlueFilter.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        dialogBlueLight(anchor, controller, onRefresh, updateUIRefresh);
                    }
                });

                screenOrientation.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PopupMenu menu = new PopupMenu(v.getContext(), v);
                        for (int i = 0; i < orientations.size(); i++) {
                            final int j = i;
                            final String name = orientations.get(i);
                            menu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    AppState.getInstance().orientation = orIds.get(j);
                                    screenOrientation.setText(orientations.get(j));
                                    TxtUtils.underlineTextView(screenOrientation);
                                    controller.doRotation(controller.getActivity());
                                    return false;
                                }
                            });
                        }
                        menu.show();

                    }
                });

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

                ((CheckBox) inflate.findViewById(R.id.isRememberDictionary)).setChecked(AppState.getInstance().isRememberDictionary);
                ((CheckBox) inflate.findViewById(R.id.isRememberDictionary)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        AppState.getInstance().isRememberDictionary = isChecked;
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
                                TintUtil.setTintImage(onDayColorImage, colorText);

                                AppState.get().colorDayText = colorText;
                                AppState.get().colorDayBg = colorBg;

                                ImageLoader.getInstance().clearDiskCache();
                                ImageLoader.getInstance().clearMemoryCache();
                                isChangedColor = true;

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
                                TintUtil.setTintImage(onNigthColorImage, colorText);

                                AppState.get().colorNigthText = colorText;
                                AppState.get().colorNigthBg = colorBg;
                                isChangedColor = true;

                                if (AppState.get().isUseBGImageNight) {
                                    textNigthColor.setBackgroundDrawable(MagicHelper.getBgImageNightDrawable(true));
                                }

                            }
                        });
                    }
                });

                final LinearLayout lc = (LinearLayout) inflate.findViewById(R.id.preColors);

                TintUtil.setTintImage(onDayColorImage, AppState.get().colorDayText);
                TintUtil.setTintImage(onNigthColorImage, AppState.get().colorNigthText);

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
                                    isChangedColor = true;

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
                                    isChangedColor = true;
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
                                    isChangedColor = true;
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

                        AlertDialogs.showOkDialog(v.getContext(), controller.getString(R.string.restore_defaults_full), new Runnable() {

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

                                TintUtil.setTintImage(onDayColorImage, AppState.get().colorDayText);
                                TintUtil.setTintImage(onNigthColorImage, AppState.get().colorNigthText);

                                AppState.get().statusBarColorDay = AppState.TEXT_COLOR_DAY;
                                AppState.get().statusBarColorNight = AppState.TEXT_COLOR_NIGHT;

                                isChangedColor = true;

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
                if (//
                initSP != AppState.get().fontSizeSp || //
                isChangedColor || //
                isChangedPreFormatting || //
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
