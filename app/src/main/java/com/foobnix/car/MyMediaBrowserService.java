package com.foobnix.car;

import static android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.utils.MediaConstants;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.pdf.info.IMG;
import com.foobnix.sys.ImageExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyMediaBrowserService extends MediaBrowserServiceCompat {
    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    Map<String, Bitmap> cache = new HashMap();
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "LOG_TAG");

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);




        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MediaSessionCompat.Callback() {



        });

        mediaSession.setMetadata(
                new MediaMetadataCompat.Builder()
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Song Name")
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Artist name")
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                "https://user-images.githubusercontent.com/8379914/197509105-11eaddc3-c855-4c36-9aa2-f4f9f19f907e.png")
                        .putLong(
                                MediaConstants.METADATA_KEY_IS_EXPLICIT,
                                MediaConstants.METADATA_VALUE_ATTRIBUTE_PRESENT)
                        .putLong(
                                MediaDescriptionCompat.EXTRA_DOWNLOAD_STATUS,
                                MediaDescriptionCompat.STATUS_DOWNLOADED)
                        .build());


        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());


    }

    @Override
    public void onSearch(@NonNull String query, Bundle extras, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        super.onSearch(query, extras, result);
        LOG.d("onSearch", query);
        result.detach();

        MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(
                new MediaDescriptionCompat.Builder()
                        .setTitle("Title")
                        .setSubtitle("Subtitile")
                        //.setIconUri(Uri.parse(it.getPath()))
                        .setMediaUri(Uri.parse("https://download.samplelib.com/mp3/sample-9s.mp3"))
                        .setExtras(extras)
                        .build(), FLAG_PLAYABLE);


        result.sendResult(Arrays.asList(item));


    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String s, int i, @Nullable Bundle bundle) {
        Bundle b = new Bundle();
        b.putBoolean(MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, true);
        b.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        );
        b.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        );

        return new MediaBrowserServiceCompat.BrowserRoot(MY_MEDIA_ROOT_ID, b);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        List<FileMeta> allRecent = AppData.get().getAllRecent(false);
        for (FileMeta it : allRecent) {

            Bitmap bitmap = cache.get(it.getPath());
            if (bitmap == null) {
                bitmap = ImageExtractor.messageFileBitmap("file", "demo");
            }

            Bundle extras = new Bundle();
            extras.putString(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                    "Songs");


            extras.putLong(
                    MediaConstants.METADATA_KEY_IS_EXPLICIT,
                    MediaConstants.METADATA_VALUE_ATTRIBUTE_PRESENT);
            extras.putInt(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS,
                    MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED);

            if (it.getIsRecentProgress() != null) {
                extras.putDouble(
                        MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE, it.getIsRecentProgress());
            }

            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setTitle(it.getTitle())
                            .setMediaId(it.getPath())
                            .setSubtitle(it.getAuthor())
                            .setMediaId(it.getPath())
                            //.setIconUri(Uri.parse(it.getPath()))
                            .setMediaUri(Uri.parse("https://download.samplelib.com/mp3/sample-9s.mp3"))
                            .setIconBitmap(bitmap)
                            .setExtras(extras)
                            .build(), FLAG_PLAYABLE);

            mediaItems.add(item);


            String url = IMG.toUrl(it.getPath(), ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
            Glide.with(LibreraApp.context).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    cache.put(it.getPath(), resource);
                    notifyChildrenChanged(MY_MEDIA_ROOT_ID);

                }

                @Override
                public void onLoadCleared(Drawable placeholder) {

                }

            });

        }
        result.sendResult(mediaItems);
    }


}
