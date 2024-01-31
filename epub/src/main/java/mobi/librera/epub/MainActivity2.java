package mobi.librera.epub;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Rating;
import androidx.media3.common.SimpleBasePlayer;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionResult;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.List;

public class MainActivity2 extends Activity {

    AudioFocusRequest audioFocusRequest;
    AudioManager audioManager;
    int NOTIFICATION_CHANNEL = 123;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder nothingPlayingNotification = new NotificationCompat.Builder(this, "NOTIFICATION_CHANNEL")
                .setSmallIcon(R.drawable.media3_icon_circular_play)
                .setShowWhen(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                //.setContentIntent(remoteStartPendingIntent)
                .setContentTitle("Title")
                .setContentText("Text");

        MediaSessionCompat mediaSession = new MediaSessionCompat(this, "MyMediaSession");

        // Set the callback for media buttons and transport controls
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                super.onCommand(command, extras, cb);
            }

            @Override
            public void onPlay() {
                super.onPlay();
                Toast.makeText(MainActivity2.this,"onPlay",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPause() {
                super.onPause();
                Toast.makeText(MainActivity2.this,"onPause",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Toast.makeText(MainActivity2.this,"onSkipToNext",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                Toast.makeText(MainActivity2.this,"onSkipToPrevious",Toast.LENGTH_SHORT).show();
            }
        });
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE).build();

        mediaSession.setPlaybackState(playbackState);


        NotificationChannel channel = new NotificationChannel("NOTIFICATION_CHANNEL",
                this.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setShowBadge(false);

        notificationManager.createNotificationChannel(channel);

        MediaMetadataCompat metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "123")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "METADATA_KEY_DISPLAY_TITLE")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "METADATA_KEY_TITLE")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "METADATA_KEY_DISPLAY_SUBTITLE")
                .build();

        mediaSession.setMetadata(metadataBuilder);


        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "NOTIFICATION_CHANNEL")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.media3_icon_circular_play)
                .setShowWhen(false)
                .setOngoing(true)
                // .setContentIntent(remoteStartPendingIntent)
                .setContentTitle("sdf")
                .setContentText("asfds")
                //.addAction(R.drawable.media3_icon_circular_play, this.getString(R.string.previous), skippreviousPI)
                //.addAction(R.drawable.ic_round_fast_rewind_24, this.getString(R.string.rewind), rewindPI)
                //.addAction(playPauseIcon, this.getString(R.string.play), playPausePI)
                //.addAction(R.drawable.ic_round_fast_forward_24, this.getString(R.string.fast_forward), fastforwardPI)
                //.addAction(R.drawable.ic_round_skip_next_24, this.getString(R.string.next), skipnextPI)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                );


        TextView hello = new TextView(this);
        hello.setText("hello:" + mediaSession.getSessionToken());


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        MediaControllerCompat mcc = new MediaControllerCompat(MainActivity2.this,mediaSession.getSessionToken());

        Button play = new Button(this);
        play.setText("Play");
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notificationManager.notify(NOTIFICATION_CHANNEL, notificationBuilder.build());
                requestAudioFocus();
                mediaSession.setActive(true);
                mcc.getTransportControls().play();
            }
        });

        Button pause = new Button(this);
        pause.setText("pause");
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abandonAudioFocus();
                mediaSession.setActive(false);
                notificationManager.cancel(NOTIFICATION_CHANNEL);
                mcc.getTransportControls().pause();
            }
        });
        Button next = new Button(this);
        next.setText("next");
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAudioFocus();
                mediaSession.setActive(true);
                notificationManager.notify(NOTIFICATION_CHANNEL, notificationBuilder.build());


              mcc.getTransportControls().skipToNext();

            }
        });

        Button release = new Button(this);
        release.setText("pause");
        release.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaSession.release();
            }
        });

        layout.addView(hello);
        layout.addView(play);
        layout.addView(pause);
        layout.addView(next);

        setContentView(layout);

    }

    private void requestAudioFocus() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                        @Override
                        public void onAudioFocusChange(int focusChange) {

                        }
                    })
                    .build();

            int result = audioManager.requestAudioFocus(audioFocusRequest);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Audio focus granted, start playback
            } else {
                // Audio focus request failed
            }
        } else {
            // For older Android versions without AudioFocusRequest
            int result = audioManager.requestAudioFocus(
                    new AudioManager.OnAudioFocusChangeListener() {
                        @Override
                        public void onAudioFocusChange(int focusChange) {
                            // Handle audio focus changes
                            // ...
                        }
                    },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Audio focus granted, start playback
            } else {
                // Audio focus request failed
            }
        }
    }

    private void abandonAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }
}
