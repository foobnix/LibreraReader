package com.foobnix;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.BookType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class OpenerActivity extends Activity {

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
        AppProfile.init(this);

        if (getIntent() == null || getIntent().getData() == null) {
            Toast.makeText(this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            finish();
        }

        LOG.d("OpenerActivity", getIntent());
        LOG.d("OpenerActivity Data", getIntent().getData());

        LOG.d("OpenerActivity Path", getDataPath());
        LOG.d("OpenerActivity Scheme", getIntent().getScheme());
        LOG.d("OpenerActivity Mime", getIntent().getType());
        LOG.d("OpenerActivity ConentName", getContentName(this));


        String path = getDataPath();
        File file = new File(path);
        if (!file.isFile()) {
            try {
                file = new File(Environment.getExternalStorageDirectory(), path.substring(path.indexOf("/", 1)));
                LOG.d("OpenerActivity find file", file.getPath());
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        if (!file.isFile()) {


            try {
                BookType bookType = BookType.getByMimeType(getIntent().getType());

                String name1 = getContentName(this);
                String name2 = getDataPath();
                String name3 = bookType != null ? bookType.getExt() : null;

                LOG.d("OpenerActivity ==============");
                LOG.d("OpenerActivity getContentName", name1);
                LOG.d("OpenerActivity getPath", name2);
                LOG.d("OpenerActivity getByMimeType", name3);

                String ext = "";
                if (BookType.isSupportedExtByPath(name2)) {
                    ext = ExtUtils.getFileExtension(name2);
                } else if (BookType.isSupportedExtByPath(name1)) {
                    ext = ExtUtils.getFileExtension(name1);
                } else if (name3 != null) {
                    ext = name3;
                }

                LOG.d("OpenerActivity final ext", ext);


                if (ext == null) {
                    Toast.makeText(this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                    finish();
                }


                String name = getDataPath().hashCode() + "." + ext;

                LOG.d("OpenerActivity", "cache", name);

                if (!CacheZipUtils.CACHE_RECENT.exists()) {
                    CacheZipUtils.CACHE_RECENT.mkdirs();
                }

                file = new File(CacheZipUtils.CACHE_RECENT, name);
                if (!file.isFile()) {
                    FileOutputStream out = new FileOutputStream(file);
                    InputStream inputStream = getContentResolver().openInputStream(getIntent().getData());
                    IOUtils.copyClose(inputStream, out);

                    LOG.d("OpenerActivity", "creatae cache file", file.getPath());
                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        FileMeta meta = FileMetaCore.createMetaIfNeed(file.getPath(), false);
        ExtUtils.openFile(this, meta);
        LOG.d("OpenerActivity", "open file", meta.getPath());
    }

    private String getDataPath() {
        if (getIntent().getData() == null) {
            return "";
        }

        String path = getIntent().getData().getPath();

        return path;
    }

    public static String getContentName(Activity a) {
        try {
            Cursor cursor = a.getContentResolver().query(a.getIntent().getData(), new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
            cursor.moveToFirst();
            int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (nameIndex >= 0) {
                final String string = cursor.getString(nameIndex);
                cursor.close();
                return string;

            }
            cursor.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
