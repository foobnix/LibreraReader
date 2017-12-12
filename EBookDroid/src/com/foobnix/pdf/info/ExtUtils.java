package com.foobnix.pdf.info;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ebookdroid.BookType;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.ui.viewer.ViewerActivity;
import org.json.JSONObject;
import org.mozilla.universalchardet.UniversalDetector;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.wrapper.AppBookmark;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.zipmanager.ZipDialog;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.EnvironmentCompat;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class ExtUtils {

    public static final String REFLOW_FB2 = "-reflow.fb2";

    public static ExecutorService ES = Executors.newFixedThreadPool(4);

    public final static List<String> otherExts = Arrays.asList(AppState.OTHER_BOOK_EXT);
    public final static List<String> lirbeExt = Arrays.asList(AppState.LIBRE_EXT);
    public final static List<String> imageExts = Arrays.asList(".png", ".jpg", ".jpeg", ".gif");
    public final static List<String> imageMimes = Arrays.asList("image/png", "image/jpg", "image/jpeg", "image/gif");
    public final static List<String> archiveExts = Arrays.asList(AppState.OTHER_ARCH_EXT);
    public final static List<String> browseExts = BookType.getAllSupportedExtensions();
    public static Map<String, String> mimeCache = new HashMap<String, String>();
    static {
        browseExts.addAll(otherExts);
        browseExts.addAll(archiveExts);
        browseExts.addAll(imageExts);
        browseExts.addAll(lirbeExt);
        browseExts.add(".json");
        browseExts.addAll(BookCSS.fontExts);

        mimeCache.put(".jpeg", "image/jpeg");
        mimeCache.put(".jpg", "image/jpeg");
        mimeCache.put(".png", "image/png");

        mimeCache.put(".chm", "application/x-chm");
        mimeCache.put(".xps", "application/vnd.ms-xpsdocument");
        mimeCache.put(".chm", "application/x-chm");
        mimeCache.put(".lit", "application/x-ms-reader");

        mimeCache.put(".doc", "application/msword");
        mimeCache.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        mimeCache.put(".ppt", "application/vnd.ms-powerpoint");
        mimeCache.put(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        mimeCache.put(".odt", "application/vnd.oasis.opendocument.text");
        mimeCache.put(".odp", "application/vnd.oasis.opendocument.presentation");

        mimeCache.put(".gz", "application/x-gzip");
        mimeCache.put(".zip", "application/x-compressed-zip");
        mimeCache.put(".rar", "application/x-rar-compressed");

        mimeCache.put(".cbr", "application/x-cbr");
        mimeCache.put(".cbt", "application/x-cbr");
        mimeCache.put(".cb7", "application/x-cbr");

        mimeCache.put(".mp3", "audio/mpeg");
        mimeCache.put(".mp4", "audio/mp4");
        mimeCache.put(".wav", "audio/vnd.wav");
        mimeCache.put(".ogg", "audio/ogg");
        mimeCache.put(".m4a", "audio/m4a");

        mimeCache.put(".m3u8", "application/x-mpegURL");
        mimeCache.put(".ts", "video/MP2T");

        mimeCache.put(".flv", "video/x-flv");
        mimeCache.put(".mp4", "video/mp4");
        mimeCache.put(".m4v", "video/x-m4v");
        mimeCache.put(".3gp", "video/3gpp");
        mimeCache.put(".mov", "video/quicktime");
        mimeCache.put(".avi", "video/x-msvideo");
        mimeCache.put(".wmv", "video/x-ms-wmv");
        mimeCache.put(".mp4", "video/mp4");
        mimeCache.put(".webm", "video/webm");
    }

    static List<String> audio = Arrays.asList(".mp3", ".mp4", ".wav", ".ogg", ".m4a");
    static List<String> video = Arrays.asList(".webm", ".m3u8", ".ts", ".flv", ".mp4", ".3gp", ".mov", ".avi", ".wmv", ".mp4", ".m4v");

    public static void openFile(Activity a, File file) {
        if (ExtUtils.doifFileExists(a, file)) {

            if (ExtUtils.isZip(file)) {

                if (CacheZipUtils.isSingleAndSupportEntryFile(file).first) {
                    ExtUtils.showDocument(a, file);
                } else {
                    ZipDialog.show(a, Uri.fromFile(file), null);
                }
            } else if (ExtUtils.isNotSupportedFile(file)) {
                ExtUtils.openWith(a, file);
            } else {
                ExtUtils.showDocument(a, file);
            }
        }
    }

    public static boolean isMediaContent(String path) {
        if (TxtUtils.isEmpty(path)) {
            return false;
        }
        path = path.trim().toLowerCase();

        for (String ext : audio) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        for (String ext : video) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static String upperCaseFirst(String text) {
        if (text.length() >= 1) {
            text = text.trim();
            text = text.substring(0, 1).toUpperCase() + text.substring(1);
        }
        return text;
    }

    public static boolean isNotSupportedFile(File file) {
        return !BookType.isSupportedExtByPath(file.getPath()) && (isLibreFile(file) || isImageFile(file) || isOtherFile(file) || isFileArchive(file));
    }

    public static boolean isImageOrEpub(File file) {
        return ExtUtils.isImageFile(file) || ExtUtils.isFileArchive(file) || BookType.EPUB.is(file.getPath());
    }

    public static boolean isNoTextLayerForamt(String name) {
        return BookType.DJVU.is(name) || BookType.CBR.is(name) || BookType.CBZ.is(name) || BookType.TIFF.is(name);
    }

    public static String getMimeTypeByUri(Uri uri) {
        String mimeType = null;

        try {
            if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT) && context != null) {
                ContentResolver cr = context.getContentResolver();
                mimeType = cr.getType(uri);
            }
            if (mimeType == null) {
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        return mimeType;
    }

    public static boolean isImageFile(File file) {
        if (file != null && file.isFile()) {
            return isImagePath(file.getName());
        }
        return false;
    }

    public static boolean isImagePath(String path) {
        if (path == null) {
            return false;
        }
        String name = path.toLowerCase(Locale.US);
        for (String ext : imageExts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImageMime(String mime) {
        if (mime == null) {
            return false;
        }
        mime = mime.toLowerCase(Locale.US);
        for (String ext : imageMimes) {
            if (ext.equals(mime)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLibreFile(File file) {
        if (file != null && file.isFile()) {
            String name = file.getName().toLowerCase();
            for (String ext : lirbeExt) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isOtherFile(File file) {
        if (file != null && file.isFile()) {
            String name = file.getName().toLowerCase();
            for (String ext : otherExts) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;

    }

    public static boolean isFileArchive(String name) {
        if (name == null) {
            return false;
        }
        name = name.toLowerCase(Locale.US);
        for (String ext : archiveExts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFileArchive(File file) {
        if (file != null && file.isFile()) {
            String name = file.getName().toLowerCase();
            for (String ext : archiveExts) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isFontFile(String name) {
        name = name.toLowerCase(Locale.US);
        for (String ext : BookCSS.fontExts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> seachExts = new ArrayList<String>();

    private static java.text.DateFormat dateFormat;

    public static void init(Context c) {
        context = c;

        dateFormat = DateFormat.getDateFormat(c);
        updateSearchExts();
    }

    public static String getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    public static String getFileExtension(String name) {
        if (name == null) {
            return "";
        }
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getFileNameWithoutExt(String name) {
        if (!name.contains(".")) {
            return name;
        }
        return name.substring(0, name.lastIndexOf("."));
    }

    public static String getFileName(String name) {
        if (!name.contains("/")) {
            return name;
        }
        try {
            return name.substring(name.lastIndexOf("/") + 1);
        } catch (Exception e) {
            return name;
        }
    }

    public static void updateSearchExts() {
        List<String> result = new ArrayList<String>();
        seachExts.clear();

        if (AppState.get().supportPDF) {
            result.add(".pdf");
        }
        if (AppState.get().supportXPS) {
            result.add(".xps");
        }

        if (AppState.get().supportEPUB) {
            result.add(".epub");
        }

        if (AppState.get().supportDJVU) {
            result.add(".djvu");
        }
        if (AppState.get().supportFB2) {
            result.add(".fb2");
            if (!AppState.get().supportZIP) {
                result.add(".fb2.zip");
            }
        }
        if (AppState.get().supportTXT) {
            result.add(".txt");
            result.add(".html");
            result.add(".xhtml");
            if (!AppState.get().supportZIP) {
                result.add(".txt.zip");
            }
        }
        if (AppState.get().supportRTF) {
            result.add(".rtf");
            if (!AppState.get().supportZIP) {
                result.add(".rtf.zip");
            }
        }
        if (AppState.get().supportMOBI) {
            result.add(".mobi");
            result.add(".azw");
            result.add(".azw3");
        }
        if (AppState.get().supportCBZ) {
            result.add(".cbz");
            result.add(".cbr");
        }
        if (AppState.get().supportZIP) {
            result.addAll(archiveExts);
        }
        if (AppState.get().supportOther) {
            result.addAll(otherExts);
            result.addAll(lirbeExt);
        }

        for (String ext : result) {
            seachExts.add(ext);
            // seachExts.add(ext.toUpperCase(Locale.US));
        }

    }

    public static FileFilter getFileFilter() {
        return filter;
    }

    private static Context context;

    public static boolean doifFileExists(Context c, File file) {
        if (file != null && file.isFile()) {
            return true;
        }
        if (c != null) {
            Toast.makeText(c, c.getString(R.string.file_not_found) + file.getPath(), Toast.LENGTH_LONG).show();
        }
        return false;

    }

    public static boolean doifFileExists(Context c, String path) {
        return doifFileExists(c, new File(path));
    }

    public static boolean isTextFomat(Intent intent) {
        if (intent == null || intent.getData() == null || intent.getData().getPath() == null) {
            LOG.d("isTextFomat", "intent or data or path is null");
            return false;
        }
        return isTextFomat(intent.getData().getPath());
    }

    public static synchronized boolean isTextFomat(String path) {
        if (path == null) {
            return false;
        }
        return BookType.ZIP.is(path) || BookType.EPUB.is(path) || BookType.FB2.is(path) || BookType.TXT.is(path) || BookType.RTF.is(path) || BookType.HTML.is(path) || BookType.MOBI.is(path);
    }

    public static synchronized boolean isZip(File path) {
        return isZip(path.getPath());
    }

    public static synchronized boolean isZip(String path) {
        if (path == null) {
            return false;
        }
        return path.toLowerCase(Locale.US).endsWith(".zip");
    }

    public static synchronized boolean isNoMetaFomat(String path) {
        if (path == null) {
            return false;
        }
        return BookType.TXT.is(path) || BookType.RTF.is(path) || BookType.HTML.is(path) || BookType.PDF.is(path) || BookType.DJVU.is(path) || BookType.CBZ.is(path);
    }

    public static String getDateFormat(File file) {
        return dateFormat.format(file.lastModified());
    }

    public static String readableFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0").format(size / Math.pow(1024, digitGroups)) + "" + units[digitGroups];
    }

    private static FileFilter filter = new FileFilter() {
        @Override
        public boolean accept(final File pathname) {
            for (final String s : browseExts) {
                if (pathname.getName().endsWith(s)) {
                    return true;
                }
            }
            return pathname.isDirectory();
        }
    };

    public static boolean isNotValidFile(final File file) {
        return !isValidFile(file);
    }

    public static boolean isValidFile(final File file) {
        return file != null && file.isFile();
    }

    public static boolean isValidFile(final String path) {
        return path != null && isValidFile(new File(path));
    }

    public static boolean isValidFile(final Uri uri) {
        return uri != null && isValidFile(uri.getPath());
    }

    public static boolean showDocument(final Context c, final File file) {
        return showDocument(c, file, -1);
    }

    public static boolean showDocument(final Context c, final File file, final int page) {
        if (AppState.getInstance().isRememberMode) {
            showDocumentWithoutDialog(c, file, page);
            return true;
        }

        View view = LayoutInflater.from(c).inflate(R.layout.choose_mode_dialog, null, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(R.string.choose_);
        builder.setView(view);
        builder.setCancelable(true);
        final AlertDialog dialog = builder.show();

        view.findViewById(R.id.advanced).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.getInstance().isAlwaysOpenAsMagazine = false;
                AppState.getInstance().isMusicianMode = false;
                showDocumentWithoutDialog(c, file, page);
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.simple).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.getInstance().isAlwaysOpenAsMagazine = true;
                AppState.getInstance().isMusicianMode = false;
                showDocumentWithoutDialog(c, file, page);
                dialog.dismiss();
            }
        });

        view.findViewById(R.id.music).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.getInstance().isAlwaysOpenAsMagazine = false;
                AppState.getInstance().isMusicianMode = true;
                showDocumentWithoutDialog(c, file, page);
                dialog.dismiss();
            }
        });
        if (AppState.get().isInkMode) {
            view.findViewById(R.id.music).setVisibility(View.GONE);
        }
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBoxRemember);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppState.getInstance().isRememberMode = isChecked;
            }
        });

        return true;

    }

    public static boolean showDocumentWithoutDialog(final Context c, final File file, final int page) {
        return showDocument(c, Uri.fromFile(file), page);
    }

    public static boolean showDocument(final Activity c, final Uri uri) {
        String filePath = CacheManager.getFilePathFromAttachmentIfNeed(c);
        if (TxtUtils.isEmpty(filePath) && uri != null && uri.getPath() != null) {
            filePath = uri.getPath();
        }
        // MetaCache.get().getOrCreateByPath(filePath);
        return showDocument(c, new File(filePath), -1);
    }

    public static boolean showDocument(final Context c, final Uri uri, final int page) {
        if (!isValidFile(uri)) {
            Toast.makeText(c, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return false;
        }
        LOG.d("showDocument", uri.getPath());

        if (AppState.getInstance().isAlwaysOpenAsMagazine) {
            openHorizontalView(c, new File(uri.getPath()), page - 1);
            return true;
        }

        final Intent intent = new Intent(c, ViewerActivity.class);
        intent.setData(uri);

        if (page > 0) {
            intent.putExtra("page", page);
        }

        c.startActivity(intent);
        // FileMetaDB.get().addRecent(uri.getPath());

        return true;
    }

    private static void openHorizontalView(final Context c, final File file, final int page) {
        if (file == null) {
            Toast.makeText(c, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        if (!isValidFile(file.getPath())) {
            Toast.makeText(c, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }

        final Intent intent = new Intent(c, HorizontalViewActivity.class);
        intent.setData(Uri.fromFile(file));

        if (page > 0) {
            intent.putExtra("page", page);
        }
        c.startActivity(intent);

        // FileMetaDB.get().addRecent(file.getPath());

        return;

    }

    public static void openWith(final Context a, final File file) {
        try {
            if (!isValidFile(file)) {
                Toast.makeText(a, R.string.file_not_found, Toast.LENGTH_LONG).show();
                return;
            }

            final Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(getUriProvider(a, file), getMimeType(file));
            a.startActivity(Intent.createChooser(intent, a.getString(R.string.open_with)));
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(a, "" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static Uri getUriProvider(Context a, File file) {
        Uri uriForFile = null;
        if (Apps.getTargetSdkVersion(a) >= 24) {
            uriForFile = FileProvider.getUriForFile(a, Apps.getPackageName(a) + ".provider", file);
        } else {
            uriForFile = Uri.fromFile(file);
        }
        LOG.d("getUriProvider", uriForFile);
        return uriForFile;
    }

    public static void sendFileTo(final Activity a, final File file) {
        if (!isValidFile(file)) {
            Toast.makeText(a, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            final Intent intent = new Intent(Intent.ACTION_SEND);

            intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "" });
            intent.setType(getMimeType(file));
            intent.putExtra(Intent.EXTRA_STREAM, getUriProvider(a, file));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_SUBJECT, "");
            intent.putExtra(Intent.EXTRA_TEXT, "" + file.getName() + "\n\n" + AppsConfig.APP_NAME);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            a.startActivity(Intent.createChooser(intent, a.getString(R.string.send_file_to)));
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(a, "" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    public static String getMimeType(File file) {
        String mime = "";
        try {
            String name = file.getName().toLowerCase();
            String ext = getFileExtension(name);

            String mimeType = mimeCache.get("." + ext);
            if (mimeType != null) {
                mime = mimeType;
            } else {
                BookType codecType = BookType.getByUri(name);
                mime = codecType.getFirstMimeTime();
            }
        } catch (Exception e) {
            mime = "application/" + ExtUtils.getFileExtension(file);
        }
        LOG.d("getMimeType", mime);
        return mime;
    }

    public static void sharePage(final Activity a, final File file, int page) {
        try {
            if (AppState.get().fileToDelete != null) {
                new File(AppState.get().fileToDelete).delete();
            }
            String url = IMG.toUrlWithContext(file.getPath(), page, (int) (Dips.screenWidth() * 1.5));
            Bitmap imageBitmap = ImageLoader.getInstance().loadImageSync(url, IMG.ExportOptions);

            String title = file.getName() + "." + (page + 1) + ".jpg";

            File oFile = new File(CacheZipUtils.CACHE_UN_ZIP_DIR, title);
            oFile.getParentFile().mkdirs();
            String pathofBmp = oFile.getPath();

            FileOutputStream out = new FileOutputStream(oFile);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

            AppState.get().fileToDelete = pathofBmp;
            AppState.get().save(a);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent.putExtra(Intent.EXTRA_STREAM, getUriProvider(a, oFile));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.setType("image/jpeg");
            a.startActivity(shareIntent);

        } catch (Exception e) {
            Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
            LOG.e(e);
        }
    }

    public static void sendBookmarksTo(final Activity a, final File file) {
        if (!isValidFile(file)) {
            Toast.makeText(a, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }

        final List<AppBookmark> bookmarksByBook = AppSharedPreferences.get().getBookmarksByBook(file);

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("text/plain");

        if (bookmarksByBook != null && !bookmarksByBook.isEmpty()) {
            final StringBuilder result = new StringBuilder();
            result.append(a.getString(R.string.bookmarks) + "\n\n");
            result.append(file.getName() + "\n");
            for (final AppBookmark book : bookmarksByBook) {
                result.append(String.format("%s. %s \n", book.getPage(), book.getText()));
            }
            intent.putExtra(Intent.EXTRA_TEXT, result.toString());
        }

        a.startActivity(intent);
    }

    public static void exportAllBookmarksToFile(final FragmentActivity a) {
        String sampleName = "Bookmarks-All-" + ExportSettingsManager.getInstance(a).getSampleJsonConfigName(a, ".TXT.txt");

        ChooserDialogFragment.createFile(a, sampleName).setOnSelectListener(new ResultResponse2<String, Dialog>() {
            @Override
            public boolean onResultRecive(String nPath, Dialog dialog) {
                File toFile = new File(nPath);
                LOG.d("exportAllBookmarksToFile 1", toFile);
                if (toFile == null || toFile.getName().trim().length() == 0) {
                    Toast.makeText(a, "Invalid File name", Toast.LENGTH_LONG).show();
                    return false;
                }
                try {
                    LOG.d("exportAllBookmarksToFile 2", toFile);
                    FileWriter writer = new FileWriter(toFile);
                    writer.write(getAllExportString(a, AppSharedPreferences.get()));
                    writer.flush();
                    writer.close();
                    Toast.makeText(a, R.string.success, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    LOG.e(e);
                    Toast.makeText(a, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
                return false;
            }
        });

    }

    public static void importAllBookmarksFromJson(final FragmentActivity a, final Runnable onSuccess) {
        String sampleName = "Bookmarks-All-" + ExportSettingsManager.getInstance(a).getSampleJsonConfigName(a, ".JSON.txt");
        ChooserDialogFragment.chooseFile(a, sampleName).setOnSelectListener(new ResultResponse2<String, Dialog>() {
            @Override
            public boolean onResultRecive(String nPath, Dialog dialog) {
                File toFile = new File(nPath);
                LOG.d("exportAllBookmarksToFile", toFile);
                if (toFile == null || !toFile.isFile() || toFile.getName().trim().length() == 0) {
                    Toast.makeText(a, "Invalid File name " + toFile.getName(), Toast.LENGTH_LONG).show();
                    return false;
                }
                try {
                    String json = new Scanner(toFile).useDelimiter("\\A").next();

                    JSONObject jsonObject = new JSONObject(json);
                    if (jsonObject.has(ExportSettingsManager.PREFIX_BOOKMARKS_PREFERENCES)) {
                        jsonObject = jsonObject.getJSONObject(ExportSettingsManager.PREFIX_BOOKMARKS_PREFERENCES);
                    }

                    ExportSettingsManager.importFromJSon(jsonObject, AppSharedPreferences.get().getBookmarkPreferences());
                    Toast.makeText(a, R.string.success, Toast.LENGTH_LONG).show();
                    onSuccess.run();
                } catch (Exception e) {
                    LOG.e(e);
                    Toast.makeText(a, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
                return false;
            }
        });

    }

    public static void exportAllBookmarksToJson(final FragmentActivity a) {
        String sampleName = "Bookmarks-All-" + ExportSettingsManager.getInstance(a).getSampleJsonConfigName(a, ".JSON.txt");

        ChooserDialogFragment.chooseFile(a, sampleName).setOnSelectListener(new ResultResponse2<String, Dialog>() {
            @Override
            public boolean onResultRecive(String nPath, Dialog dialog) {
                File toFile = new File(nPath);
                if (toFile == null || toFile.getName().trim().length() == 0) {
                    Toast.makeText(a, "Invalid File name", Toast.LENGTH_LONG).show();
                    return false;
                }

                try {
                    JSONObject result = ExportSettingsManager.exportToJSon("bookmarks", AppSharedPreferences.get().getBookmarkPreferences(), AppSharedPreferences.RECENT_);
                    FileWriter writer = new FileWriter(toFile);
                    writer.write(result.toString(2));
                    writer.flush();
                    writer.close();
                    Toast.makeText(a, R.string.success, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    LOG.e(e);
                    Toast.makeText(a, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
                return false;
            }
        });

    }

    public static String getAllExportString(final Activity a, AppSharedPreferences viewerPreferences) {
        final StringBuilder out = new StringBuilder();
        Map<String, List<AppBookmark>> bookmarks = viewerPreferences.getBookmarksMap();

        out.append(a.getString(R.string.bookmarks) + "\n");
        out.append("\n");

        for (String path : bookmarks.keySet()) {
            List<AppBookmark> list = bookmarks.get(path);
            Collections.sort(list, AppSharedPreferences.COMPARE_BY_PAGE);
            File file = new File(path);
            if (file.isFile()) {
                path = file.getName();
            }
            out.append(path + "\n");
            out.append("\n");
            for (AppBookmark item : list) {
                out.append(String.format("%s. %s \n", item.getPage(), item.getText()));
            }
            out.append("\n");

        }
        return out.toString();
    }

    public static void exportAllBookmarksToGmail(Activity a) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getAllExportString(a, AppSharedPreferences.get()));
        a.startActivity(intent);
    }

    public static void openPDFInTextReflow(final Activity a, final File file, final int page) {
        if (ExtUtils.isNotValidFile(file)) {
            Toast.makeText(a, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        new AsyncTask() {
            ProgressDialog dialog;

            Handler handler;

            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(a);
                dialog.setMessage(a.getString(R.string.msg_loading));
                dialog.setCancelable(false);
                handler = new Handler() {
                    @Override
                    public void handleMessage(android.os.Message msg) {
                        dialog.setMessage(a.getString(R.string.msg_loading) + " " + msg.what + "/100%");

                    };
                };
                dialog.show();
            };

            @Override
            protected Object doInBackground(Object... params) {
                return openPDFInTextReflowAsync(a, file, handler);
            };

            @Override
            protected void onPostExecute(Object result) {
                if (dialog != null) {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }
                if (result != null) {
                    if (a instanceof ViewerActivity) {
                        AppState.getInstance().isAlwaysOpenAsMagazine = false;
                        AppState.getInstance().isMusicianMode = false;
                        showDocumentWithoutDialog(a, (File) result, page);

                    } else if (a instanceof HorizontalViewActivity) {
                        AppState.getInstance().isAlwaysOpenAsMagazine = true;
                        AppState.getInstance().isMusicianMode = false;
                        showDocumentWithoutDialog(a, (File) result, page);
                    } else {
                        showDocument(a, (File) result);
                    }
                }
            };

        }.execute();
    }

    public static File openPDFInTextReflowAsync(Activity a, File file, Handler dialog) {
        try {
            File LIRBI_DOWNLOAD_DIR = new File(AppState.get().downlodsPath);
            if (!LIRBI_DOWNLOAD_DIR.exists()) {
                LIRBI_DOWNLOAD_DIR.mkdirs();
            }

            CodecDocument doc = BookType.getCodecContextByPath(file.getPath()).openDocument(file.getPath(), "");

            final File filefb2 = new File(LIRBI_DOWNLOAD_DIR, file.getName() + REFLOW_FB2);
            try {
                FileWriter fout = new FileWriter(filefb2);
                BufferedWriter out = new BufferedWriter(fout);
                out.write("<html>");

                out.write("<title-info>");
                out.write("<author><first-name>" + AppsConfig.APP_NAME + "</first-name></autor><br/>");
                out.write("<book-title><h1>" + file.getName() + "</h1></book-title>");
                out.write("</title-info>");

                out.write("<body>");

                int pages = doc.getPageCount();

                for (int i = 0; i < pages; i++) {
                    LOG.d("Extract page", i);
                    CodecPage pageCodec = doc.getPage(i);
                    String html = pageCodec.getPageHTML();
                    out.write(html);
                    pageCodec.recycle();
                    LOG.d("Extract page end1", i);
                    dialog.sendEmptyMessage(((i + 1) * 100) / pages);

                }
                out.write("</body></html>");
                out.flush();
                out.close();
                fout.close();
            } catch (Exception e) {
                LOG.e(e);
                return null;
            }
            LOG.d("openPDFInTextReflow", filefb2.getPath());
            return filefb2;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }

    }

    public static List<String> getExternalStorageDirectories1(Context c) {

        File[] list = ContextCompat.getExternalFilesDirs(c, null);
        List<String> res = new ArrayList<String>();
        for (File f : list) {
            if (f != null && !f.getPath().endsWith("files")) {
                res.add(f.getPath());
            }
        }

        return res;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static List<String> getExternalStorageDirectories(Context c) {

        List<String> results = new ArrayList<String>();
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                File[] externalDirs = ContextCompat.getExternalFilesDirs(c, null);

                for (File file : externalDirs) {
                    String path = file.getPath().split("/Android")[0];

                    boolean addPath = false;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        addPath = Environment.isExternalStorageRemovable(file);
                    } else {
                        addPath = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file));
                    }

                    if (addPath) {
                        results.add(path);
                    }
                }
            }

            if (results.isEmpty()) {
                String output = "";
                try {
                    final Process process = new ProcessBuilder().command("mount | grep /dev/block/vold").redirectErrorStream(true).start();
                    process.waitFor();
                    final InputStream is = process.getInputStream();
                    final byte[] buffer = new byte[1024];
                    while (is.read(buffer) != -1) {
                        output = output + new String(buffer);
                    }
                    is.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                if (!output.trim().isEmpty()) {
                    String devicePoints[] = output.split("\n");
                    for (String voldPoint : devicePoints) {
                        results.add(voldPoint.split(" ")[2]);
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0; i < results.size(); i++) {
                    if (!results.get(i).toLowerCase(Locale.US).matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
                        results.remove(i--);
                    }
                }
            } else {
                for (int i = 0; i < results.size(); i++) {
                    if (!results.get(i).toLowerCase(Locale.US).contains("ext") && !results.get(i).toLowerCase(Locale.US).contains("sdcard")) {
                        results.remove(i--);
                    }
                }
            }

        } catch (Exception e) {
            LOG.e(e);
        }

        return results;
    }

    public static String determineEncoding(InputStream fis) {
        String encoding = null;
        try {
            UniversalDetector detector = new UniversalDetector(null);

            int nread;
            byte[] buf = new byte[1024];
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();

            encoding = detector.getDetectedCharset();
            detector.reset();
            fis.close();

            LOG.d("File Encoding", encoding);

        } catch (Exception e) {
            LOG.e(e);
        }
        return encoding == null ? "UTF-8" : encoding;
    }

}
