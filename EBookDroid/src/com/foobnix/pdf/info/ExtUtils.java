package com.foobnix.pdf.info;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ebookdroid.BookType;
import org.ebookdroid.LibreraApp;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.json.JSONObject;
import org.mozilla.universalchardet.UniversalDetector;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Views;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.wrapper.AppBookmark;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.HorizontalModeController;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.zipmanager.ZipDialog;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.EnvironmentCompat;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ExtUtils {

    private static final String IMAGE_PNG_BASE64 = "image/png;base64,";
    private static final String IMAGE_JPEG_BASE64 = "image/jpeg;base64,";
    public static final String REFLOW_EPUB = "-reflow.epub";
    public static final String REFLOW_HTML = "-reflow.html";
    private static final String IMAGE_BEGIN = "<image-begin>";
    private static final String IMAGE_END = "<image-end>";

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
        browseExts.addAll(Arrays.asList(AppState.OTHER_BOOK_MEDIA));

        mimeCache.put(".tpz", "application/x-topaz-ebook");
        mimeCache.put(".azw1", "application/x-topaz-ebook");

        mimeCache.put(".pgn", " application/x-chess-pgn");

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

    public static void openFile(Activity a, FileMeta meta) {
        File file = new File(meta.getPath());

        if (ExtUtils.isExteralSD(meta.getPath())) {
            CacheZipUtils.removeFiles(CacheZipUtils.ATTACHMENTS_CACHE_DIR.listFiles());
            Uri uri = Uri.parse(meta.getPath());
            file = new File(CacheZipUtils.ATTACHMENTS_CACHE_DIR, meta.getTitle());
            if (!file.exists()) {
                try {
                    InputStream inputStream = a.getContentResolver().openInputStream(uri);
                    if (inputStream == null) {
                        Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CacheZipUtils.copyFile(inputStream, file);
                    LOG.d("Create-file", file.getPath(), file.length());
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
        }

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

    public static boolean isExteralSD(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("content:/");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getExtSDDisplayName(Context c, String path) {
        String id;
        Uri uri = Uri.parse(path);
        try {
            if (DocumentsContract.isDocumentUri(c, uri)) {
                id = DocumentsContract.getDocumentId(uri);
            } else {
                id = DocumentsContract.getTreeDocumentId(uri);
            }
        } catch (Exception e) {
            LOG.e(e);
            return path;
        }
        return id;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Uri getChildUri(Context c, Uri uri) {
        if (DocumentsContract.isDocumentUri(c, uri)) {
            return DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri));
        } else {
            return DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
        }
    }

    public static boolean isMediaContent(String path) {
        if (TxtUtils.isEmpty(path)) {
            return false;
        }
        path = path.trim().toLowerCase(Locale.US);

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
        return !BookType.isSupportedExtByPath(file.getPath());
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
            String name = file.getName().toLowerCase(Locale.US);
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
            String name = file.getName().toLowerCase(Locale.US);
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
            String name = file.getName().toLowerCase(Locale.US);
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
    private static java.text.DateFormat timeFormat;

    public static void init(Context c) {
        context = c;

        dateFormat = DateFormat.getDateFormat(c);
        timeFormat = DateFormat.getTimeFormat(c);
        updateSearchExts();
    }

    public static String getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    public static String getFileExtension(String name) {
        LOG.d("getFileExtension 1", name);
        if (name == null) {
            return "";
        }
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        if (!name.contains(".")) {
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
        if (Clouds.isCloud(file.getPath())) {
            return true;
        }

        if (file != null && file.isFile()) {
            return true;
        }
        if (c != null) {
            Toast.makeText(c, c.getString(R.string.file_not_found) + " " + file.getPath(), Toast.LENGTH_LONG).show();
        }
        return false;

    }

    public static boolean doifFileExists(Context c, String path) {
        if (Clouds.isCloud(path)) {
            return true;
        }
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
        return BookType.ZIP.is(path) || BookType.EPUB.is(path) || BookType.FB2.is(path) || BookType.TXT.is(path) || BookType.RTF.is(path) || BookType.HTML.is(path) || BookType.MHT.is(path) || BookType.MOBI.is(path);
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
        return BookType.TXT.is(path) || BookType.RTF.is(path) || BookType.HTML.is(path) || BookType.MHT.is(path) || BookType.PDF.is(path) || BookType.DJVU.is(path) || BookType.CBZ.is(path);
    }

    public static String getDateTimeFormat(File file) {
        return dateFormat.format(file.lastModified()) + " " + timeFormat.format(file.lastModified());
    }

    public static String getDateFormat(File file) {
        return dateFormat.format(file.lastModified());
    }

    public static String getDateFormat(long datetime) {
        return dateFormat.format(datetime);
    }

    public static String readableFileSize(long size) {
        if (true) {
            return Formatter.formatFileSize(LibreraApp.context, size).replace(" ", "");
        }
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
        if (Clouds.isCloud(file.getPath())) {
            return true;
        }
        return file != null && file.isFile();
    }

    public static boolean isValidFile(final String path) {
        return path != null && isValidFile(new File(path));
    }

    public static boolean isValidFile(final Uri uri) {
        LOG.d("getScheme()", uri.getScheme(), uri, uri.getPath());
        return uri != null && ("content".equals(uri.getScheme()) || isValidFile(uri.getPath()));
    }

    public static boolean showDocument(final Context c, final File file) {
        return showDocument(c, file, -1);
    }

    public static boolean showDocument(final Context c, final File file, final int page) {

        ImageLoader.getInstance().clearAllTasks();

        if (AppState.get().isRememberMode) {
            showDocumentWithoutDialog(c, file, page);
            return true;
        }

        View view = LayoutInflater.from(c).inflate(R.layout.choose_mode_dialog, null, false);

        final TextView vertical = (TextView) view.findViewById(R.id.vertical);
        final TextView horizontal = (TextView) view.findViewById(R.id.horizontal);
        final TextView music = (TextView) view.findViewById(R.id.music);

        final EditText verticalEdit = (EditText) view.findViewById(R.id.verticalEdit);
        final EditText horizontalEdit = (EditText) view.findViewById(R.id.horizontalEdit);
        final EditText musicEdit = (EditText) view.findViewById(R.id.musicEdit);

        verticalEdit.setText(AppState.get().nameVerticalMode);
        horizontalEdit.setText(AppState.get().nameHorizontalMode);
        musicEdit.setText(AppState.get().nameMusicianMode);

        vertical.setText(AppState.get().nameVerticalMode);
        horizontal.setText(AppState.get().nameHorizontalMode);
        music.setText(AppState.get().nameMusicianMode);

        Views.gone(verticalEdit, horizontalEdit, musicEdit);

        final TextView editNames = (TextView) view.findViewById(R.id.editNames);
        TxtUtils.underlineTextView(editNames);

        editNames.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                AppState.get().nameVerticalMode = c.getString(R.string.mode_vertical);
                AppState.get().nameHorizontalMode = c.getString(R.string.mode_horizontally);
                AppState.get().nameMusicianMode = c.getString(R.string.mode_musician);

                verticalEdit.setText(AppState.get().nameVerticalMode);
                horizontalEdit.setText(AppState.get().nameHorizontalMode);
                musicEdit.setText(AppState.get().nameMusicianMode);

                vertical.setText(AppState.get().nameVerticalMode);
                horizontal.setText(AppState.get().nameHorizontalMode);
                music.setText(AppState.get().nameMusicianMode);

                AppState.get().save(c);

                return true;
            }
        });

        editNames.setOnClickListener(new View.OnClickListener() {
            boolean isEdit = true;

            @Override
            public void onClick(View v) {

                String vText = verticalEdit.getText().toString().trim();
                String hText = horizontalEdit.getText().toString().trim();
                String mText = musicEdit.getText().toString().trim();

                if (TxtUtils.isEmpty(vText)) {
                    verticalEdit.setSelected(true);
                    Toast.makeText(c, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TxtUtils.isEmpty(hText)) {
                    horizontalEdit.setSelected(true);
                    Toast.makeText(c, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TxtUtils.isEmpty(mText)) {
                    musicEdit.setSelected(true);
                    Toast.makeText(c, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isEdit) { // edit
                    editNames.setText(R.string.save);
                    Views.visible(verticalEdit, horizontalEdit, musicEdit);
                    Views.gone(vertical, horizontal, music);

                    AppState.get().save(c);

                } else { // text view
                    editNames.setText(R.string.edit_names);
                    Views.visible(vertical, horizontal, music);
                    Views.gone(verticalEdit, horizontalEdit, musicEdit);
                }

                AppState.get().nameVerticalMode = vText;
                AppState.get().nameHorizontalMode = hText;
                AppState.get().nameMusicianMode = mText;

                Keyboards.close(v);

                verticalEdit.setText(AppState.get().nameVerticalMode);
                horizontalEdit.setText(AppState.get().nameHorizontalMode);
                musicEdit.setText(AppState.get().nameMusicianMode);

                vertical.setText(AppState.get().nameVerticalMode);
                horizontal.setText(AppState.get().nameHorizontalMode);
                music.setText(AppState.get().nameMusicianMode);

                TxtUtils.underlineTextView(editNames);
                isEdit = !isEdit;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(R.string.select_the_reading_mode);
        builder.setView(view);
        builder.setCancelable(true);
        final AlertDialog dialog = builder.show();

        vertical.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                AppState.get().isAlwaysOpenAsMagazine = false;
                AppState.get().isMusicianMode = false;
                showDocumentWithoutDialog(c, file, page);
            }
        });
        horizontal.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                AppState.get().isAlwaysOpenAsMagazine = true;
                AppState.get().isMusicianMode = false;
                showDocumentWithoutDialog(c, file, page);
            }
        });

        music.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                AppState.get().isAlwaysOpenAsMagazine = false;
                AppState.get().isMusicianMode = true;
                showDocumentWithoutDialog(c, file, page);
            }
        });

        if (Dips.isEInk(c)) {
            view.findViewById(R.id.music).setVisibility(View.GONE);
        }
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBoxRemember);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppState.get().isRememberMode = isChecked;
            }
        });

        return true;

    }

    public static void showDocumentWithoutDialog(final Context c, final File file, final int page) {
        showDocument(c, Uri.fromFile(file), page);
    }

    public static boolean showDocument(final Activity c, final Uri uri) {
        String filePath = CacheManager.getFilePathFromAttachmentIfNeed(c);
        if (TxtUtils.isEmpty(filePath) && uri != null && uri.getPath() != null) {
            filePath = uri.getPath();
        }
        // MetaCache.get().getOrCreateByPath(filePath);
        return showDocument(c, new File(filePath), -1);
    }

    public static void showDocument(final Context c, final Uri uri, final int page) {
        Safe.run(new Runnable() {

            @Override
            public void run() {
                showDocumentInner(c, uri, page);
            }
        });

    }

    public static void showDocumentInner(final Context c, final Uri uri, final int page) {
        if (!isValidFile(uri)) {
            Toast.makeText(c, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        LOG.d("showDocument", uri.getPath());

        if (AppState.get().isAlwaysOpenAsMagazine) {
            openHorizontalView(c, uri, page - 1);
            return;
        }

        final Intent intent = new Intent(c, VerticalViewActivity.class);
        try {
            intent.putExtra(PasswordDialog.EXTRA_APP_PASSWORD, ((Activity) c).getIntent().getStringExtra(PasswordDialog.EXTRA_APP_PASSWORD));
        } catch (Exception e) {
            LOG.e(e);
        }
        intent.setData(uri);

        if (page > 0) {
            intent.putExtra(DocumentController.EXTRA_PAGE, page);

        }
        c.startActivity(intent);
        // FileMetaDB.get().addRecent(uri.getPath());
    }

    private static void openHorizontalView(final Context c, final Uri uri, final int page) {
        if (uri == null) {
            Toast.makeText(c, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        if (!isValidFile(uri)) {
            Toast.makeText(c, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }

        final Intent intent = new Intent(c, HorizontalViewActivity.class);
        intent.setData(uri);
        try {
            intent.putExtra(PasswordDialog.EXTRA_APP_PASSWORD, ((Activity) c).getIntent().getStringExtra(PasswordDialog.EXTRA_APP_PASSWORD));
        } catch (Exception e) {
            LOG.e(e);
        }

        if (page > 0) {
            intent.putExtra(DocumentController.EXTRA_PAGE, page);
        }
        c.startActivity(intent);

        // FileMetaDB.get().addRecent(file.getPath());

        return;

    }

    public static Intent createOpenFileIntent(Context context, File file) {
        String extension = extensionFromName(file.getName());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType == null) {
            // If android doesn't know extension we can check our own list.
            mimeType = getMimeType(file);
        }

        Intent openIntent = new Intent();
        openIntent.setAction(android.content.Intent.ACTION_VIEW);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // openIntent.setDataAndType(getUriProvider(context, file), mimeType);
        openIntent.setDataAndType(getUriProvider(context, file), mimeType);
        // LOG.d("getUriProvider2", getUriProvider(context, file));
        // LOG.d("getUriProvider2", Uri.fromFile(file));

        // 1. Check if there is a default app opener for this type of content.
        final PackageManager packageManager = context.getPackageManager();
        ResolveInfo defaultAppInfo = packageManager.resolveActivity(openIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (defaultAppInfo != null && defaultAppInfo.activityInfo != null && defaultAppInfo.activityInfo.name != null && !defaultAppInfo.activityInfo.name.endsWith("ResolverActivity")) {
            return openIntent;
        }

        // 2. Retrieve all apps for our intent. If there are no apps - return usual
        // already created intent.
        List<Intent> targetedOpenIntents = new ArrayList<Intent>();
        List<ResolveInfo> appInfoList = packageManager.queryIntentActivities(openIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (appInfoList.isEmpty()) {
            return openIntent;
        }

        // 3. Sort in alphabetical order, filter itself and create intent with the rest
        // of the apps.
        Collections.sort(appInfoList, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo first, ResolveInfo second) {
                String firstName = packageManager.getApplicationLabel(first.activityInfo.applicationInfo).toString();
                String secondName = packageManager.getApplicationLabel(second.activityInfo.applicationInfo).toString();
                return firstName.compareToIgnoreCase(secondName);
            }
        });
        for (ResolveInfo appInfo : appInfoList) {
            String packageName = appInfo.activityInfo.packageName;
            if (packageName.equals(context.getPackageName())) {
                continue;
            }

            Intent targetedOpenIntent = new Intent(android.content.Intent.ACTION_VIEW);
            targetedOpenIntent.setDataAndType(getUriProvider(context, file), mimeType);
            targetedOpenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            targetedOpenIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            targetedOpenIntent.setPackage(packageName);

            targetedOpenIntents.add(targetedOpenIntent);
        }
        Intent remove = targetedOpenIntents.remove(targetedOpenIntents.size() - 1);
        Intent createChooser = Intent.createChooser(remove, context.getString(R.string.select));
        Intent chooserIntent = createChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedOpenIntents.toArray(new Parcelable[] {}));

        return chooserIntent;
    }

    public static String extensionFromName(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');

        // If extension not present or empty
        if (dotPosition == -1 || dotPosition == fileName.length() - 1) {
            return "";
        } else {
            return fileName.substring(dotPosition + 1).toLowerCase(Locale.getDefault());
        }
    }

    public static void openWith(final Context a, final File file) {
        try {
            a.startActivity(createOpenFileIntent(a, file));
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(a, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static Uri getUriProvider(Context a, File file) {
        Uri uriForFile = null;
        // if (Apps.getTargetSdkVersion(a) >= 24) {
        // if (Apps.getTargetSdkVersion(a) >= 24) {
        if (Build.VERSION.SDK_INT >= 24) {
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
            intent.putExtra(Intent.EXTRA_SUBJECT, "");
            intent.putExtra(Intent.EXTRA_TEXT, "");

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(Intent.createChooser(intent, a.getString(R.string.send_file_to)));
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(a, "" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static String getMimeType(File file) {
        String name = file.getName();
        return getMimeType(name);
    }

    public static String getMimeType(String name) {
        String mime = "";
        try {
            name = name.toLowerCase(Locale.US);
            String ext = getFileExtension(name);

            String mimeType = mimeCache.get("." + ext);
            if (mimeType != null) {
                mime = mimeType;
            } else {
                BookType codecType = BookType.getByUri(name);
                mime = codecType.getFirstMimeTime();
            }
        } catch (Exception e) {
            mime = "application/" + ExtUtils.getFileExtension(name);
        }
        LOG.d("getMimeType", mime);
        return mime;
    }

    public static void sharePage(final Activity a, final File file, int page, String pageUrl) {
        try {
            if (AppState.get().fileToDelete != null) {
                new File(AppState.get().fileToDelete).delete();
            }

            if (TxtUtils.isEmpty(pageUrl)) {
                pageUrl = IMG.toUrlWithContext(file.getPath(), page, (int) (Dips.screenWidth() * 1.5));
            }

            Bitmap imageBitmap = ImageLoader.getInstance().loadImageSync(pageUrl, IMG.ExportOptions);

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
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_STREAM, getUriProvider(a, oFile));
            shareIntent.setType("image/jpeg");
            a.startActivity(Intent.createChooser(shareIntent, a.getString(R.string.send_snapshot_of_the_page)));

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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

        a.startActivity(Intent.createChooser(intent, a.getString(R.string.export_bookmarks)));
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getAllExportString(a, AppSharedPreferences.get()));
        a.startActivity(Intent.createChooser(intent, a.getString(R.string.export_bookmarks)));
    }

    public static void openPDFInTextReflow(final Activity a, final File file, final int page, final DocumentController dc) {
        if (ExtUtils.isNotValidFile(file)) {
            Toast.makeText(a, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        new AsyncTask() {
            AlertDialog dialog;

            Handler handler;

            @Override
            protected void onPreExecute() {
                TempHolder.get().isConverting = true;

                final AlertDialog.Builder builder = new AlertDialog.Builder(a);
                View view = LayoutInflater.from(a).inflate(R.layout.dialog_loading_book, null, false);
                final TextView text = (TextView) view.findViewById(R.id.text1);

                handler = new Handler() {
                    @Override
                    public void handleMessage(android.os.Message msg) {
                        text.setText(a.getString(R.string.please_wait) + " " + msg.what + "/100%");
                    };
                };

                ImageView image = (ImageView) view.findViewById(R.id.onCancel);
                TintUtil.setTintImageWithAlpha(image);
                image.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        LOG.d("loadingBook Cancel");
                        TempHolder.get().isConverting = false;
                        dialog.dismiss();
                    }
                });

                builder.setView(view);
                builder.setCancelable(false);

                dialog = builder.show();
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            };

            @Override
            protected Object doInBackground(Object... params) {
                try {
                    return openPDFInTextReflowAsync(a, file, handler);
                } catch (RuntimeException e) {
                    LOG.e(e);
                    return null;
                }
            };

            @Override
            protected void onPostExecute(final Object result) {
                if (dialog != null) {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }
                if (!TempHolder.get().isConverting) {
                    return;
                }
                if (result != null) {
                    Runnable run = new Runnable() {

                        @Override
                        public void run() {
                            if (a instanceof VerticalViewActivity) {
                                AppState.get().isAlwaysOpenAsMagazine = false;
                                AppState.get().isMusicianMode = false;
                                showDocumentWithoutDialog(a, (File) result, page);

                            } else if (a instanceof HorizontalViewActivity) {
                                AppState.get().isAlwaysOpenAsMagazine = true;
                                AppState.get().isMusicianMode = false;
                                showDocumentWithoutDialog(a, (File) result, page);
                            } else {
                                showDocument(a, (File) result);
                            }
                        }
                    };
                    if (dc != null) {
                        dc.onCloseActivityFinal(run);
                    } else {
                        Safe.run(run);
                    }

                }
            };

        }.execute();
    }

    public static File openPDFInTextReflowAsync(Activity a, final File file, Handler dialog) {
        try {
            File bookTempRoot = new File(AppState.get().downlodsPath, "temp-dir-" + file.getName());
            if (!bookTempRoot.exists()) {
                bookTempRoot.mkdirs();
            } else {
                CacheZipUtils.removeFiles(bookTempRoot.listFiles());
            }

            String pwd = "";
            try {
                pwd = a.getIntent().getStringExtra(HorizontalModeController.EXTRA_PASSWORD);
                if (pwd == null) {
                    pwd = "";
                }
            } catch (Exception e) {
                LOG.e(e);
            }

            CodecDocument doc = BookType.getCodecContextByPath(file.getPath()).openDocument(file.getPath(), pwd);

            List<OutlineLink> outline = doc.getOutline();

            final File fileReflowHtml = new File(bookTempRoot, "temp" + REFLOW_HTML);
            try {
                FileWriter fout = new FileWriter(fileReflowHtml);
                BufferedWriter out = new BufferedWriter(fout);
                out.write("<html>");
                out.write("<head><meta charset=\"utf-8\"/></head>");
                out.write("<body>");

                int pages = doc.getPageCount();

                int imgCount = 0;
                for (int i = 0; i < pages; i++) {
                    LOG.d("Extract page", i);
                    CodecPage pageCodec = doc.getPage(i);
                    String html = pageCodec.getPageHTMLWithImages();

                    out.write("<a id=\"" + i + "\"></a>");

                    html = TxtUtils.replaceEndLine(html);

                    int startImage = html.indexOf(IMAGE_BEGIN);
                    while (startImage >= 0) {
                        if (!TempHolder.get().isConverting) {
                            CacheZipUtils.removeFiles(bookTempRoot.listFiles());
                            bookTempRoot.delete();
                            break;
                        }
                        imgCount++;
                        LOG.d("Extract image", imgCount);
                        int endImage = html.indexOf(IMAGE_END, startImage);

                        String mime = html.substring(startImage + IMAGE_BEGIN.length(), endImage);
                        String format;
                        if (mime.startsWith(IMAGE_JPEG_BASE64)) {
                            format = ".jpg";
                            mime = mime.replace(IMAGE_JPEG_BASE64, "");
                        } else if (mime.startsWith(IMAGE_PNG_BASE64)) {
                            format = ".png";
                            mime = mime.replace(IMAGE_PNG_BASE64, "");
                        } else {
                            format = ".none";
                        }

                        // FileOutputStream mimeOut = new FileOutputStream(new File(bookTempRoot, "mime"
                        // + imgCount + ".mime"));
                        // mimeOut.write(mime.getBytes());
                        // mimeOut.close();

                        byte[] decode = Base64.decode(mime, Base64.DEFAULT);

                        String imageName = imgCount + format;

                        LOG.d("Extract-mime", mime.substring(mime.length() - 10, mime.length()));

                        FileOutputStream imgStream = new FileOutputStream(new File(bookTempRoot, imageName));
                        imgStream.write(decode);
                        imgStream.close();

                        html = html.substring(0, startImage) + "<img src=\"" + imageName + "\"/>" + html.substring(endImage + IMAGE_END.length());
                        startImage = html.indexOf(IMAGE_BEGIN);
                        LOG.d("startImage", startImage);
                    }

                    // out.write(TextUtils.htmlEncode(html));
                    // html = html.replace("< ", "&lt; ");
                    // html = html.replace("> ", "&gt; ");
                    // html = html.replace("&", "&amp;");

                    out.write(html);
                    pageCodec.recycle();
                    LOG.d("Extract page end1", i);
                    dialog.sendEmptyMessage(((i + 1) * 100) / pages);

                    if (!TempHolder.get().isConverting) {
                        CacheZipUtils.removeFiles(bookTempRoot.listFiles());
                        bookTempRoot.delete();
                        break;
                    }

                }
                doc.recycle();

                out.write("</body></html>");
                out.flush();
                out.close();
                fout.close();
            } catch (Exception e) {
                LOG.e(e);
                return null;
            }

            File epubOutpub = new File(AppState.get().downlodsPath, file.getName() + REFLOW_EPUB);
            if (epubOutpub.isFile()) {
                epubOutpub.delete();
            }

            FileMeta meta = AppDB.get().getOrCreate(file.getPath());

            Fb2Extractor.convertFolderToEpub(bookTempRoot, epubOutpub, meta.getAuthor(), meta.getTitle(), outline);

            CacheZipUtils.removeFiles(bookTempRoot.listFiles());
            bookTempRoot.delete();

            if (!TempHolder.get().isConverting) {
                epubOutpub.delete();
                LOG.d("Delete temp file", fileReflowHtml.getPath());
            }

            LOG.d("openPDFInTextReflow", fileReflowHtml.getPath());
            return epubOutpub;
        } catch (RuntimeException e) {
            LOG.e(e);
            return null;
        }

    }

    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
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

    public static String getSDPath() {
        String[] strPath = { //
                "/storage/sdcard1", //
                "/storage/extsdcard", //
                "/storage/extSdCard", //
                "/storage/sdcard0/external_sdcard", //
                "/storage/external_SD", //
                "/storage/ext_sd", //
                "/storage/removable/sdcard1", //

                "/data/sdext", //
                "/data/sdext2", //
                "/data/sdext3", //
                "/data/sdext4", //

                "/removable/microsd", //
                "/Removable/MicroSD", //
                "/emmc", //
                "/sdcard/sd", //

                "/mnt/extsdcard", //
                "/mnt/sdcard/external_sd", //
                "/mnt/external_sd", //
                "/mnt/emmc", //
                "/mnt/media_rw/sdcard1", //
                "/mnt/sdcard/bpemmctest", //
                "/mnt/sdcard/_ExternalSD", //
                "/mnt/sdcard-ext", //
                "/mnt/Removable/MicroSD", //
                "/mnt/external1", //
                "/mnt/extsd", //
                "/mnt/usb_storage", //
                "/mnt/extSdCard", //
                "/mnt/UsbDriveA", //
                "/mnt/UsbDriveB"//
        };

        for (String value : strPath) {
            File f = new File(value);
            if (f.exists() && f.isDirectory() && f.canRead()) {
                return value;
            }
        }
        return null;
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

    public static String determineHtmlEncoding(InputStream fis, InputStream fis2) {

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis));
            String line;

            List<String> es = Arrays.asList("encoding=\"", "charset=\"", "charset=");
            int count = 0;
            while ((line = bufferedReader.readLine()) != null) {
                count++;
                if (line.contains("<script")) {
                    continue;
                }
                for (String e : es) {
                    line = line.toLowerCase(Locale.US);
                    if (line.contains(e)) {
                        bufferedReader.close();
                        int index = line.indexOf(e) + e.length();
                        String encoding = line.substring(index, line.indexOf("\"", index));
                        LOG.d("extract-encoding-html", encoding);
                        bufferedReader.close();
                        LOG.d("determineHtmlEncoding", encoding);
                        fis2.close();
                        return encoding;
                    }
                }

                if (count >= 100) {
                    break;
                }

            }
            bufferedReader.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        String encdogin = determineEncodingAuto(fis2);
        LOG.d("determineHtmlEncoding auto", encdogin);

        return encdogin;

    }

    private static String determineEncodingAuto(InputStream fis) {
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

    public static String determineTxtEncoding(InputStream fis) {
        String encoding = null;
        try {
            UniversalDetector detector = new UniversalDetector(null);

            int nread;
            byte[] buf = new byte[2024];
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
