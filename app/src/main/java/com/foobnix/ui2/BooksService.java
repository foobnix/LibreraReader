package com.foobnix.ui2;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.ext.EbookMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.search.activity.msg.MessageSync;
import com.foobnix.pdf.search.activity.msg.MessageSyncFinish;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSNotification;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class BooksService extends IntentService {
    public static String TAG = "BooksService";
    public static String INTENT_NAME = "BooksServiceIntent";
    public static String ACTION_SEARCH_ALL = "ACTION_SEARCH_ALL";
    public static String ACTION_REMOVE_DELETED = "ACTION_REMOVE_DELETED";
    public static String ACTION_SYNC_DROPBOX = "ACTION_SYNC_DROPBOX";
    public static String ACTION_RUN_SELF_TEST = "ACTION_RUN_SELF_TEST";
    public static String ACTION_RUN_SYNCRONICATION = "ACTION_RUN_SYNCRONICATION";
    public static String RESULT_SYNC_FINISH = "RESULT_SYNC_FINISH";
    public static String RESULT_SEARCH_FINISH = "RESULT_SEARCH_FINISH";
    public static String RESULT_BUILD_LIBRARY = "RESULT_BUILD_LIBRARY";
    public static String RESULT_SEARCH_COUNT = "RESULT_SEARCH_COUNT";
    public static String RESULT_NOTIFY_ALL = "RESULT_NOTIFY_ALL";

    public static String RESULT_SEARCH_MESSAGE_TXT = "RESULT_SEARCH_MESSAGE_TXT";

    public static volatile boolean isRunning = false;
    Handler handler;
    boolean isStartForeground = false;
    Runnable timer2 = new Runnable() {

        @Override
        public void run() {
            LOG.d("timer2");
            sendBuildingLibrary();
            handler.postDelayed(timer2, 250);
        }
    };
    private List<FileMeta> itemsMeta = new LinkedList<FileMeta>();
    Runnable timer = new Runnable() {

        @Override
        public void run() {
            LOG.d("timer 2");
            sendProggressMessage();
            handler.postDelayed(timer, 250);
        }
    };

    public BooksService() {
        super("BooksService");
        handler = new Handler();
        LOG.d("BooksService", "Create");
    }

    public static void sendFinishMessage(Context c) {
        Intent intent = new Intent(INTENT_NAME).putExtra(Intent.EXTRA_TEXT, RESULT_SEARCH_FINISH);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

    public static void startForeground(Activity a, String action) {
        final Intent intent = new Intent(a, BooksService.class).setAction(action);
        a.startService(intent);

//        if (Build.VERSION.SDK_INT >= 26) {
//            a.startForegroundService(intent);
//        } else {
//            a.startService(intent);
//
//        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStartForeground = false;
        LOG.d("BooksService", "onDestroy");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //startMyForeground();
    }

    public void startMyForeground() {
        if (!isStartForeground) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                Notification notification = new NotificationCompat.Builder(this, TTSNotification.DEFAULT) //
                        .setSmallIcon(R.drawable.glyphicons_761_sync) //
                        .setContentTitle(Apps.getApplicationName(this)) //
                        .setContentText(getString(R.string.please_wait_books_are_being_processed_)).setPriority(NotificationCompat.PRIORITY_DEFAULT)//
                        .build();

                startForeground(TTSNotification.NOT_ID_2, notification);
            }
            AppProfile.init(this);
            isStartForeground = true;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //startMyForeground();


        if (intent == null) {
            return;
        }

        try {
            sendProggressMessage();

            if (isRunning) {
                LOG.d(TAG, "BooksService", "Is-running");
                return;
            }

            isRunning = true;
            LOG.d(TAG, "BooksService", "Action", intent.getAction());

            //TESET


            if (ACTION_RUN_SYNCRONICATION.equals(intent.getAction())) {
                if (AppSP.get().isEnableSync) {


                    AppProfile.save(this);


                    try {
                        EventBus.getDefault().post(new MessageSync(MessageSync.STATE_VISIBLE));
                        AppSP.get().syncTimeStatus = MessageSync.STATE_VISIBLE;
                        GFile.sycnronizeAll(this);

                        AppSP.get().syncTime = System.currentTimeMillis();
                        AppSP.get().syncTimeStatus = MessageSync.STATE_SUCCESS;
                        EventBus.getDefault().post(new MessageSync(MessageSync.STATE_SUCCESS));
                    } catch (UserRecoverableAuthIOException e) {
                        GFile.logout(this);
                        AppSP.get().syncTimeStatus = MessageSync.STATE_FAILE;
                        EventBus.getDefault().post(new MessageSync(MessageSync.STATE_FAILE));
                    } catch (Exception e) {
                        AppSP.get().syncTimeStatus = MessageSync.STATE_FAILE;
                        EventBus.getDefault().post(new MessageSync(MessageSync.STATE_FAILE));
                        LOG.e(e);
                    }

                    if (GFile.isNeedUpdate) {
                        LOG.d("GFILE-isNeedUpdate", GFile.isNeedUpdate);
                        TempHolder.get().listHash++;
                        EventBus.getDefault().post(new UpdateAllFragments());
                    }

                }

            }


            if (ACTION_REMOVE_DELETED.equals(intent.getAction())) {
                List<FileMeta> all = AppDB.get().getAll();

                for (FileMeta meta : all) {
                    if (meta == null) {
                        continue;
                    }

                    if (Clouds.isCloud(meta.getPath())) {
                        continue;
                    }

                    File bookFile = new File(meta.getPath());
                    if (ExtUtils.isMounted(bookFile)) {
                        if (!bookFile.exists()) {
                            AppDB.get().delete(meta);
                            LOG.d("BooksService Delete-setIsSearchBook", meta.getPath());
                        }
                    }

                }

                List<FileMeta> localMeta = new LinkedList<FileMeta>();
                if (JsonDB.isEmpty(BookCSS.get().searchPathsJson)) {
                    sendFinishMessage();
                    return;
                }

                for (final String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
                    if (path != null && path.trim().length() > 0) {
                        final File root = new File(path);
                        if (root.isDirectory()) {
                            LOG.d(TAG, "Search in " + root.getPath());
                            SearchCore.search(localMeta, root, ExtUtils.seachExts);
                        }
                    }
                }


                for (FileMeta meta : localMeta) {
                    if (!all.contains(meta)) {
                        FileMetaCore.createMetaIfNeedSafe(meta.getPath(), true);
                        LOG.d("BooksService add book", meta.getPath());
                    }
                }


                List<FileMeta> allNone = AppDB.get().getAllByState(FileMetaCore.STATE_NONE);
                for (FileMeta m : allNone) {
                    LOG.d("BooksService-createMetaIfNeedSafe-service", m.getTitle(), m.getPath(), m.getTitle());
                    FileMetaCore.createMetaIfNeedSafe(m.getPath(), false);
                }

                Clouds.get().syncronizeGet();

            } else if (ACTION_SEARCH_ALL.equals(intent.getAction())) {
                LOG.d(ACTION_SEARCH_ALL);
                //TempHolder.listHash++;
                //AppDB.get().getDao().detachAll();

                AppProfile.init(this);

                ImageExtractor.clearErrors();
                IMG.clearDiscCache();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        IMG.clearMemoryCache();
                    }
                });


                AppDB.get().deleteAllData();
                itemsMeta.clear();

                handler.post(timer);


                for (final String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
                    if (path != null && path.trim().length() > 0) {
                        final File root = new File(path);
                        if (root.isDirectory()) {
                            LOG.d("Search in: " + root.getPath());
                            SearchCore.search(itemsMeta, root, ExtUtils.seachExts);
                        }
                    }
                }


                for (FileMeta meta : itemsMeta) {
                    meta.setIsSearchBook(true);
                }

                final List<SimpleMeta> allExcluded = AppData.get().getAllExcluded();

                if (TxtUtils.isListNotEmpty(allExcluded)) {
                    for (FileMeta meta : itemsMeta) {
                        if (allExcluded.contains(SimpleMeta.SyncSimpleMeta(meta.getPath()))) {
                            meta.setIsSearchBook(false);
                        }
                    }
                }

                final List<FileMeta> allSyncBooks = AppData.get().getAllSyncBooks();
                if (TxtUtils.isListNotEmpty(allSyncBooks)) {
                    for (FileMeta meta : itemsMeta) {
                        for (FileMeta sync : allSyncBooks) {
                            if (meta.getTitle().equals(sync.getTitle()) && !meta.getPath().equals(sync.getPath())) {
                                meta.setIsSearchBook(false);
                                LOG.d(TAG, "remove-dublicate", meta.getPath());
                            }
                        }

                    }
                }


                itemsMeta.addAll(AppData.get().getAllFavoriteFiles(false));
                itemsMeta.addAll(AppData.get().getAllFavoriteFolders());


                AppDB.get().saveAll(itemsMeta);

                handler.removeCallbacks(timer);

                sendFinishMessage();

                handler.post(timer2);

                for (FileMeta meta : itemsMeta) {
                    File file = new File(meta.getPath());
                    FileMetaCore.get().upadteBasicMeta(meta, file);
                }

                AppDB.get().updateAll(itemsMeta);
                sendFinishMessage();


                for (FileMeta meta : itemsMeta) {
                    //if(FileMetaCore.isSafeToExtactBook(meta.getPath())) {
                    EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(meta.getPath(), CacheDir.ZipService, true);
                    FileMetaCore.get().udpateFullMeta(meta, ebookMeta);
                    //}
                }

                SharedBooks.updateProgress(itemsMeta, true, -1);
                AppDB.get().updateAll(itemsMeta);


                itemsMeta.clear();

                handler.removeCallbacks(timer2);
                sendFinishMessage();
                CacheDir.ZipService.removeCacheContent();

                Clouds.get().syncronizeGet();

                TagData.restoreTags();


                List<FileMeta> allNone = AppDB.get().getAllByState(FileMetaCore.STATE_NONE);
                for (FileMeta m : allNone) {
                    LOG.d("BooksService-createMetaIfNeedSafe-service", m.getTitle(), m.getPath(), m.getTitle());
                    FileMetaCore.createMetaIfNeedSafe(m.getPath(), false);
                }

                updateBookAnnotations();


            } else if (ACTION_SYNC_DROPBOX.equals(intent.getAction())) {
                Clouds.get().syncronizeGet();

            } else if (ACTION_RUN_SELF_TEST.equals(intent.getAction())) {


                try {
                    AppProfile.syncTestFolder.mkdirs();
                    File logFile = AppData.getTestFileName();
                    logFile.delete();

                    BufferedWriter out = new BufferedWriter(new FileWriter(logFile));


                    List<FileMeta> all = AppDB.get().searchBy("", AppDB.SORT_BY.getByID(AppState.get().sortBy), AppState.get().isSortAsc);

                    int w = Dips.screenWidth();
                    int h = Dips.screenHeight();
                    int s = BookCSS.get().fontSizeSp;
                    int count = all.size();
                    int n = 0;
                    int errors = 0;

                    writeLine(out, "ApplicationName: " + Apps.getApplicationName(this));
                    writeLine(out, "VersionName: " + Apps.getVersionName(this));
                    writeLine(out, "PackageName: " + Apps.getPackageName(this));
                    writeLine(out, "os.arch: " + System.getProperty("os.arch"));
                    writeLine(out, "MUPDF_VERSION: " + AppsConfig.MUPDF_FZ_VERSION);
                    writeLine(out, "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
                    writeLine(out, "Height x Width: " + Dips.screenHeight() + "x" + Dips.screenWidth());
                    out.newLine();
                    writeLine(out, "Build.MANUFACTURER: " + Build.MANUFACTURER);
                    writeLine(out, "Build.PRODUCT: " + Build.PRODUCT);
                    writeLine(out, "Build.DEVICE: " + Build.DEVICE);
                    writeLine(out, "Build.BRAND: " + Build.BRAND);
                    writeLine(out, "Build.MODEL: " + Build.MODEL);
                    out.newLine();
                    writeLine(out, "[CSS]");
                    writeLine(out, BookCSS.get().toCssString().replace("}", "}\n"));
                    writeLine(out, "[BookCSS]");
                    writeLine(out, Objects.toJSONString(BookCSS.get()).replace(",", ",\n"));
                    writeLine(out, "[AppState]");
                    writeLine(out, Objects.toJSONString(AppState.get()).replace(",", ",\n"));


                    writeLine(out, "Books: " + count);


                    sendNotifyAll();

                    for (FileMeta item : all) {
                        n++;

                        writeLine(out, item.getPath());

                        if (TxtUtils.isEmpty(item.getPath())) {
                            writeLine(out, "Skip");
                            continue;
                        }

                        if (ExtUtils.isZip(item.getPath()) && !CacheZipUtils.isSingleAndSupportEntry(item.getPath()).first) {
                            writeLine(out, "Skip");
                            continue;
                        }
                        sendTextMessage("Test: " + n + "/" + count);
                        try {
                            CodecContext codecContex = BookType.getCodecContextByPath(item.getPath());
                            CodecDocument codecDocument = codecContex.openDocument(item.getPath(), "");
                            int pageCount = codecDocument.getPageCount(w, h, s);
                            if (pageCount == 0) {
                                codecDocument.recycle();
                                writeLine(out, "Error");
                                errors++;
                                sendNotifyAll();
                                continue;
                            }
                            CodecPage page = codecDocument.getPage(pageCount / 2);
                            RectF rectF = new RectF(0, 0, 1f, 1f);
                            BitmapRef bitmapRef = page.renderBitmap(w, h, rectF, false);
                            bitmapRef.getBitmap().recycle();
                            page.getText();
                            page.getPageLinks();
                            page.getPageHTML();

                            if (!page.isRecycled()) {
                                page.recycle();
                            }

                            if (!BookType.DJVU.is(item.getPath())) {
                                codecDocument.recycle();
                            }
                        } catch (Exception e) {
                            writeLine(out, "Error");
                            errors++;
                            sendNotifyAll();
                        }

                    }
                    writeLine(out, "Errors: " + errors);
                    writeLine(out, "Finish");
                    out.close();
                } catch (Exception e) {
                    LOG.e(e);
                }
                sendNotifyAll();
            }


        } finally {
            sendFinishMessage();
            isRunning = false;

        }
        //stopSelf();
    }

    public void writeLine(BufferedWriter out, String line) throws IOException {
        LOG.d("Self-test", line);
        out.write(line);
        out.newLine();
        out.flush();
    }

    public void updateBookAnnotations() {

        if (AppState.get().isDisplayAnnotation) {
            sendBuildingLibrary();
            LOG.d("updateBookAnnotations begin");
            List<FileMeta> itemsMeta = AppDB.get().getAll();
            for (FileMeta meta : itemsMeta) {
                if (TxtUtils.isEmpty(meta.getAnnotation())) {
                    String bookOverview = FileMetaCore.getBookOverview(meta.getPath());
                    meta.setAnnotation(bookOverview);
                }
            }
            AppDB.get().updateAll(itemsMeta);
            sendFinishMessage();
            LOG.d("updateBookAnnotations end");
        }

    }

    private void sendFinishMessage() {
        try {
            //AppDB.get().getDao().detachAll();
        } catch (Exception e) {
            LOG.e(e);
        }

        sendFinishMessage(this);
        EventBus.getDefault().post(new MessageSyncFinish());
    }

    private void sendTextMessage(String text) {
        Intent itent = new Intent(INTENT_NAME).putExtra(Intent.EXTRA_TEXT, RESULT_SEARCH_MESSAGE_TXT).putExtra("TEXT", text);
        LocalBroadcastManager.getInstance(this).sendBroadcast(itent);
    }

    private void sendNotifyAll() {
        Intent itent = new Intent(INTENT_NAME).putExtra(Intent.EXTRA_TEXT, RESULT_NOTIFY_ALL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(itent);
    }

    private void sendProggressMessage() {
        Intent itent = new Intent(INTENT_NAME).putExtra(Intent.EXTRA_TEXT, RESULT_SEARCH_COUNT).putExtra("android.intent.extra.INDEX", itemsMeta.size());
        LocalBroadcastManager.getInstance(this).sendBroadcast(itent);
    }

    private void sendBuildingLibrary() {
        Intent itent = new Intent(INTENT_NAME).putExtra(Intent.EXTRA_TEXT, RESULT_BUILD_LIBRARY);
        LocalBroadcastManager.getInstance(this).sendBroadcast(itent);
    }

}
