package com.foobnix.tts;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.DocumentController;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

public class TTSControlsView extends FrameLayout {

    Handler handler;
    private ImageView ttsPlayPause;
    private DocumentController controller;
    private ImageView ttsDialog;
    private View layoutMp3;
    private SeekBar seekMp3;
    private TextView seekCurrent;
    private TextView seekMax;
    private TextView trackName;
    private ImageView ttsPrevTrack;
    private ImageView ttsNextTrack;
    private int colorTint;
    Runnable update = new Runnable() {

        @Override
        public void run() {
            if (TTSEngine.get().isMp3()) {
                initMp3();
                if (TTSEngine.get().mp != null) {
                    seekCurrent.setText(TxtUtils.getMp3TimeString(TTSEngine.get().mp.getCurrentPosition()));
                    seekMax.setText(TxtUtils.getMp3TimeString(TTSEngine.get().mp.getDuration()));

                    seekMp3.setMax(TTSEngine.get().mp.getDuration());
                    seekMp3.setProgress(TTSEngine.get().mp.getCurrentPosition());

                    udateButtons();
                }

            } else {
                layoutMp3.setVisibility(View.GONE);
                trackName.setVisibility(View.GONE);
            }

            LOG.d("TtsStatus-isPlaying", TTSEngine.get().isPlaying());
            ttsPlayPause.setImageResource(TTSEngine.get().isPlaying() ? R.drawable.glyphicons_175_pause : R.drawable.glyphicons_174_play);
        }
    };
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public TTSControlsView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.tts_mp3_line, this, false);
        addView(view);

        final ImageView ttsStop = (ImageView) view.findViewById(R.id.ttsStop);
        ttsPlayPause = (ImageView) view.findViewById(R.id.ttsPlay);


        final ImageView ttsNext = (ImageView) view.findViewById(R.id.ttsNext);
        final ImageView ttsPrev = (ImageView) view.findViewById(R.id.ttsPrev);

        ttsPrevTrack = (ImageView) view.findViewById(R.id.ttsPrevTrack);
        ttsNextTrack = (ImageView) view.findViewById(R.id.ttsNextTrack);
        trackName = (TextView) view.findViewById(R.id.trackName);

        ttsDialog = (ImageView) view.findViewById(R.id.ttsDialog);
        ttsDialog.setVisibility(View.GONE);
        trackName.setVisibility(View.GONE);

        colorTint = Color.parseColor(AppState.get().isDayNotInvert ? BookCSS.get().linkColorDay : BookCSS.get().linkColorNight);
        int alpha = 220;
        TintUtil.setTintImageWithAlpha(ttsStop, colorTint, alpha);
        TintUtil.setTintImageWithAlpha(ttsPlayPause, colorTint, alpha);
        TintUtil.setTintImageWithAlpha(ttsNext, colorTint, alpha);
        TintUtil.setTintImageWithAlpha(ttsPrev, colorTint, alpha);
        TintUtil.setTintImageWithAlpha(ttsDialog, colorTint, alpha);
        TintUtil.setTintImageWithAlpha(ttsPrevTrack, colorTint, alpha);
        TintUtil.setTintImageWithAlpha(ttsNextTrack, colorTint, alpha);
        TintUtil.setTintText(trackName, colorTint);

        ttsNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_NEXT, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    next.send();
                } catch (CanceledException e) {
                    LOG.d(e);
                }

            }
        });
        ttsPrev.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_PREV, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    next.send();
                } catch (CanceledException e) {
                    LOG.d(e);
                }

            }
        });

        ttsStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_STOP_DESTROY, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    next.send();
                } catch (CanceledException e) {
                    LOG.d(e);
                }
            }
        });

        ttsPlayPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (TTSEngine.get().getEngineCount() == 0) {
                    Urls.openTTS(getContext());
                } else {
                    TTSService.playPause(context, controller);
                }
            }
        });

        ttsPlayPause.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                TTSEngine.get().pauseMp3();
                TTSEngine.get().seekTo(0);
                return true;
            }
        });

        handler = new Handler();
        seekMp3 = (SeekBar) view.findViewById(R.id.seekMp3);
        seekCurrent = (TextView) view.findViewById(R.id.seekCurrent);
        seekMax = (TextView) view.findViewById(R.id.seekMax);
        layoutMp3 = view.findViewById(R.id.layoutMp3);


        TintUtil.setDrawableTint(seekMp3.getProgressDrawable(), colorTint, alpha);

        if (Build.VERSION.SDK_INT >= 16) {
            TintUtil.setDrawableTint(seekMp3.getThumb(), colorTint, alpha);
        }
        TintUtil.setTintText(seekCurrent, colorTint);
        TintUtil.setTintText(seekMax, colorTint);

        layoutMp3.setVisibility(View.GONE);
        initMp3();

        ttsPrevTrack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String track = TTSTracks.getPrevTrack();
                if (track != null) {
                    BookCSS.get().mp3BookPath(track);
                    TTSEngine.get().loadMP3(track, true);
                    udateButtons();
                }
            }
        });

        ttsNextTrack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String track = TTSTracks.getNextTrack();
                if (track != null) {
                    BookCSS.get().mp3BookPath(track);
                    TTSEngine.get().loadMP3(track, true);
                    udateButtons();
                }
            }
        });

        if (TTSTracks.isMultyTracks()) {
            ttsPrevTrack.setVisibility(View.VISIBLE);
            ttsNextTrack.setVisibility(View.VISIBLE);
        } else {
            ttsPrevTrack.setVisibility(View.GONE);
            ttsNextTrack.setVisibility(View.GONE);

        }
        trackName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                MyPopupMenu menu = new MyPopupMenu(v);
                for (final File file : TTSTracks.getAllMp3InFolder()) {
                    menu.getMenu().add(file.getName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            TTSEngine.get().stop();
                            BookCSS.get().mp3BookPath(file.getPath());
                            TTSEngine.get().loadMP3(file.getPath(), true);
                            udateButtons();
                            return false;
                        }


                    });
                }
                menu.show();

            }
        });
        Apps.accessibilityButtonSize(ttsPlayPause);
        Apps.accessibilityButtonSize(ttsNext);
        Apps.accessibilityButtonSize(ttsPrev);
        Apps.accessibilityButtonSize(ttsNextTrack);
        Apps.accessibilityButtonSize(ttsPrevTrack);
        Apps.accessibilityButtonSize(ttsDialog);
        Apps.accessibilityButtonSize(ttsStop);

    }

    public void setDC(DocumentController dc) {
        controller = dc;
    }

    public void addOnDialogRunnable(final Runnable run) {
        ttsDialog.setVisibility(View.VISIBLE);
        ttsDialog.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                run.run();
            }
        });
    }

    public void initMp3() {
        if (TTSEngine.get().isMp3() && layoutMp3.getVisibility() == View.GONE) {
            layoutMp3.setVisibility(View.VISIBLE);
            trackName.setVisibility(View.VISIBLE);

            udateButtons();

            seekMp3.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        LOG.d("Seek-onProgressChanged", progress);
                        TTSEngine.get().mp.seekTo(progress);
                    }
                }
            });

        }
    }

    public void udateButtons() {
        trackName.setText(TTSTracks.getCurrentTrackName());

        boolean isMulty = TTSTracks.isMultyTracks();
        ttsPrevTrack.setVisibility(TxtUtils.visibleIf(isMulty));
        ttsNextTrack.setVisibility(TxtUtils.visibleIf(isMulty));

        TintUtil.setTintImageWithAlpha(ttsPrevTrack, TTSTracks.getPrevTrack() != null ? colorTint : Color.GRAY);
        TintUtil.setTintImageWithAlpha(ttsNextTrack, TTSTracks.getNextTrack() != null ? colorTint : Color.GRAY);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
        handler.removeCallbacksAndMessages(null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTTSStatus(TtsStatus status) {
        if (ttsPlayPause != null) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(update, 200);
        }
    }
    public void reset(){
        TTSEngine.get().loadMP3(BookCSS.get().mp3BookPathGet());
        update.run();

    }

}
