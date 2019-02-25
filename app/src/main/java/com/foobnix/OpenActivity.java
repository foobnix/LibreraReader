package com.foobnix;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.BookType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class OpenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || getIntent().getData() == null) {
            Toast.makeText(this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            finish();
        }

        LOG.d("OpenActivity", getIntent());
        LOG.d("OpenActivity Data", getIntent().getData());
        LOG.d("OpenActivity Path", getIntent().getData().getPath());
        LOG.d("OpenActivity Scheme", getIntent().getScheme());
        LOG.d("OpenActivity Mime", getIntent().getType());

        String path = getIntent().getData().getPath();
        if (new File(path).isFile()) {

            FileMeta meta = FileMetaCore.createMetaIfNeed(path, false);
            ExtUtils.openFile(this, meta);
        } else {
            try {

                BookType mime = BookType.getByMimeType(getIntent().getType());

                if(mime.getExt()==null){
                    Toast.makeText(this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                    finish();
                }

                String name = getIntent().getData().getPath().hashCode() + "." + mime.getExt();

                LOG.d("OpenActivity", "cache", name);

                if (!CacheZipUtils.CACHE_OPENER.exists()) {
                    CacheZipUtils.CACHE_OPENER.mkdirs();
                }

                File file = new File(CacheZipUtils.CACHE_OPENER, name);
                if (!file.isFile()) {
                    FileOutputStream out = new FileOutputStream(file);
                    InputStream inputStream = getContentResolver().openInputStream(getIntent().getData());
                    IOUtils.copy(inputStream, out);
                    LOG.d("OpenActivity", "creatae cache file", file.getPath());
                }

                FileMeta meta = FileMetaCore.createMetaIfNeed(file.getPath(), false);
                ExtUtils.openFile(this, meta);
                LOG.d("OpenActivity", "open file", meta.getPath());


            } catch (Exception e) {
                LOG.e(e);
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
