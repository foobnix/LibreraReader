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
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.MyContextWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class OpenerActivity extends Activity {
    public static String TAG = "OpenerActivity";

    public static String findFileInDownloads(Context context, String name, String id) {
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
        }


        LOG.d(TAG, getIntent());
        LOG.d(TAG, " Data", uri);
        LOG.d(TAG, " Path", getDataPath());
        LOG.d(TAG, " Scheme", getIntent().getScheme());
        LOG.d(TAG, " Mime", getIntent().getType());

        File file = null;
        if ("content".equals(uri.getScheme())) {
            String id = uri.getLastPathSegment();
            String utfID = Uri.decode(id).replace("file://", "");
            LOG.d(TAG, "utfID", utfID);
            file = new File(utfID);
            if (!file.isFile()) {

                String name = getCursorValue(MediaStore.MediaColumns.DISPLAY_NAME);

                LOG.d(TAG, "id", id, "name", name);

                String documentID = id.replaceAll("\\D", "");

                LOG.d(TAG, "documentID", documentID);

                String fileInDownlaods = findFileInDownloads(this, name, documentID);


                if (fileInDownlaods != null) {
                    LOG.d(TAG, "findFileInDownloads", fileInDownlaods);
                    file = new File(fileInDownlaods);
                } else {

                    if (TxtUtils.isEmpty(ExtUtils.getFileExtension(name))) {
                        String extByMimeType = ExtUtils.getExtByMimeType(getIntent().getType());
                        LOG.d("extByMimeType", extByMimeType, getIntent().getType());
                        name = name + "." + extByMimeType;
                    }
                    name = TxtUtils.fixFileName(name);

                    file = new File(AppProfile.DOWNLOADS_DIR, name);

                    if (!file.isFile()) {
                        AppProfile.downloadBookFolder.mkdirs();
                        file = new File(AppProfile.downloadBookFolder, name);

                        if (!file.isFile()) {
                            LOG.d(TAG, "create file", file.getPath());
                            try {
                                FileOutputStream out = new FileOutputStream(file);
                                InputStream inputStream = getContentResolver().openInputStream(uri);
                                IOUtils.copyClose(inputStream, out);
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }
                    }
                }
            }
        } else if ("file".equals(uri.getScheme())) {
            file = new File(uri.getPath());
        }

        if (file == null || !file.isFile()) {
            Toast.makeText(this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        LOG.d(TAG, "file", file);
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
        if (getIntent().getData() == null) {
            return "";
        }

        String path = getIntent().getData().getPath();

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
