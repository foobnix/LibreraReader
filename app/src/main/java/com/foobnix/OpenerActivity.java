package com.foobnix;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import com.foobnix.android.utils.Cursors;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.MyContextWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class OpenerActivity extends Activity {
    public static String TAG = "OpenerActivity";

    public static String findFileInDownloads(Context context, String name, String id) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = MediaStore.Files.getContentUri("external");

            String[] projection = {
                    MediaStore.Files.FileColumns.DATA
            };

            String selection = MediaStore.Files.FileColumns._ID + " = ? AND " +
                    MediaStore.Files.FileColumns.DISPLAY_NAME + " = ?";

            String[] selectionArgs = new String[]{id, name};

            try (Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int filePathId = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    String filePath = cursor.getString(filePathId);
                    LOG.d(TAG, "FileFinder", "File Path: " + filePath);
                    return filePath;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppState.get().isDayNotInvert) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }

        super.onCreate(savedInstanceState);


        if (!Android6.canWrite(this)) {
            Android6.checkPermissions(this, true);
            return;
        }


        if (getIntent() == null) {
            Toast.makeText(this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Uri uri = getIntent().getData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                if (uri == null && getIntent().getClipData() != null) {
                    getIntent().setData(getIntent().getClipData().getItemAt(0).getUri());
                }
            } catch (Exception e) {
                LOG.e(e);
            }

        }
        if (uri == null) {
            Toast.makeText(this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        LOG.d(TAG, getIntent());
        LOG.d(TAG, " Uri", uri);
        LOG.d(TAG, " dataPath", getDataPath());
        LOG.d(TAG, " Scheme", getIntent().getScheme());
        LOG.d(TAG, " Mime", getIntent().getType());

        File file = new File("");

        if ("file".equals(uri.getScheme())) {
            LOG.d(TAG, "Find file in file");
            file = new File(uri.getPath());
        }

        String name = getCursorValue(MediaStore.MediaColumns.DISPLAY_NAME);
        String id = uri.getLastPathSegment();
        String size = getCursorValue(MediaStore.MediaColumns.SIZE);
        String dataPath = getDataPath();

        LOG.d(TAG, "id", id, "name", name, "size", size);

        if (!file.isFile()) {
            LOG.d(TAG, "Find file in getLastPathSegment");
            if (id != null) {
                String utfID = Uri.decode(id).replace("file://", "");
                LOG.d(TAG, "utfID", utfID);
                file = new File(utfID);
                LOG.d(TAG, "File:", file.getPath());
            }
        }


        if (!file.isFile()) {
            LOG.d(TAG, "Find file in getDataPath");
            file = new File(dataPath);
            LOG.d(TAG, "File:", file.getPath());
        }
        if (!file.isFile()) {
            LOG.d(TAG, "Find file in getDataPath /indexOf 1");
            int beginIndex = dataPath.indexOf("/", 1);
            if (beginIndex > 0) {
                file = new File(dataPath.substring(beginIndex));
            }
            LOG.d(TAG, "File:", file.getPath());
        }
        if (!file.isFile()) {
            LOG.d(TAG, "Find file in getDataPath /indexOf 2");
            int beginIndex = dataPath.indexOf("/", 1);
            if (beginIndex > 0) {
                file = new File(dataPath.substring(beginIndex));
            }
            LOG.d(TAG, "File:", file.getPath());
        }

        if (!file.isFile()) {
            LOG.d(TAG, "Find file in Telegram");
            if (dataPath.startsWith("/media/Android/data/org.telegram.messenger/files/")) {
                file = new File(AppProfile.DOWNLOADS_DIR, "Telegram/" + name);
            }
            LOG.d(TAG, "File:", file.getPath());
        }

        if (!file.isFile()) {
            LOG.d(TAG, "Find file in all Downloads by name and id", name, id);
            String documentID = id.replaceAll("\\D", "");
            LOG.d(TAG, "documentID", documentID);
            String fileInDownlaods = findFileInDownloads(this, name, documentID);
            if (fileInDownlaods != null) {
                LOG.d(TAG, "findFileInDownloads", fileInDownlaods);
                file = new File(fileInDownlaods);
            }
            LOG.d(TAG, "File:", file.getPath());
        }
//        if (!file.isFile()) {
//            LOG.d(TAG, "Find file in [Downloads]");
//            if (TxtUtils.isEmpty(ExtUtils.getFileExtension(name))) {
//                String extByMimeType = ExtUtils.getExtByMimeType(getIntent().getType());
//                LOG.d("extByMimeType", extByMimeType, getIntent().getType());
//                name = name + "." + extByMimeType;
//            }
//            name = TxtUtils.fixFileName(name);
//            file = new File(AppProfile.DOWNLOADS_DIR, name);
//        }

        if (!file.isFile()) {
            LOG.d(TAG, "Find file in [Librera/Downloads]");
            AppProfile.downloadBookFolder.mkdirs();
            LOG.d(TAG, "Find file in [Downloads]");
            if (TxtUtils.isEmpty(ExtUtils.getFileExtension(name))) {
                String extByMimeType = ExtUtils.getExtByMimeType(getIntent().getType());
                LOG.d("extByMimeType", extByMimeType, getIntent().getType());
                name = name + "." + extByMimeType;
            }
            name = TxtUtils.fixFileName(name);
            file = new File(AppProfile.downloadBookFolder, name);

            if (size != null && file.length() != Long.parseLong(size)) {
                file.delete();
                LOG.d(TAG, "Delete old file");
            }
            LOG.d(TAG, "File:", file.getPath());
        }

        if (!file.isFile()) {
            LOG.d(TAG, "Create file in [Librera/Downloads]");
            try {
                FileOutputStream out = new FileOutputStream(file);
                InputStream inputStream = getContentResolver().openInputStream(uri);
                IOUtils.copyClose(inputStream, out);
            } catch (Exception e) {
                LOG.e(e);
            }
            LOG.d(TAG, "File:", file.getPath());
        }

        if (file == null || !file.canRead() || !file.isFile()) {
            Toast.makeText(this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        LOG.d(TAG, "Open file", file);
        ExtUtils.openFile(this, new FileMeta(file.getPath()));
    }

    public String getCursorValue(String id) {
        try {
            return Cursors.getValue(this, id);
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }


    private String getDataPath() {
        if (getIntent() == null) {
            return "";
        }
        if (getIntent().getData() == null) {
            return "";
        }

        String path = getIntent().getData().getPath();
        if (path == null) {
            return "";
        }

        return path;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
