package com.foobnix.sys;

import java.io.File;

import org.ebookdroid.core.codec.CodecDocument;

import com.foobnix.android.utils.Dips;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.MainTabs2;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import dalvik.system.DexClassLoader;

public class BitmapActivity extends Activity {

    String path1 = "/storage/emulated/0/Books/Books/ninepatch-sources.zip.pdf";

    String path2 = "/mnt/sdcard/test.djvu";
    String path3 = "/mnt/sdcard/error1.pdf";
    private ImageExtractor instance;
    ClassLoader libClassLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("PDF Document");
        if (true) {
            // Intent intent = new Intent();
            // intent.setAction(android.content.Intent.ACTION_VIEW);
            // intent.setDataAndType(Uri.fromFile(new File(path1)),
            // "application/pdf");
            // startActivity(intent);
            ExtUtils.showDocument(this, new File(path1));
            return;
        }

        PackageManager pm = getPackageManager();
        String libSrcPath = null;
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)) {
            if (app.packageName.equals("com.foobnix.pdf.x86")) {
                libSrcPath = app.sourceDir;
            }
        }
        try {
            libClassLoader = new DexClassLoader(libSrcPath, getDir("dex", 0).getAbsolutePath(), null, getClassLoader());
            Class<?> clazz = libClassLoader.loadClass("com.foobnix.pdf.x86.X86Loader");
            clazz.getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        IMG.init(getApplicationContext());

        ImageLoader.getInstance().clearDiscCache();
        ImageLoader.getInstance().clearMemoryCache();

        instance = ImageExtractor.getInstance(this);
        instance.getNewCodecContext(path1, "", 100, 200);
        instance.getNewCodecContext(path2, "", 100, 200);
        instance.getNewCodecContext(path1, "", 100, 200);
        instance.getNewCodecContext(path1, "", 100, 200);
        instance.getNewCodecContext(path2, "", 100, 200);

        GridView grid = new GridView(this);
        grid.setNumColumns(3);
        try {
            // grid.setAdapter(new MyAdapter1(path3));
        } catch (Exception e) {
            Log.e("TEST", "ERROR", e);
            TextView t = new TextView(this);
            t.setText("exeption 2");
            setContentView(t);
            return;
        }

        setContentView(grid);

        Handler h = new Handler();
        h.postDelayed(new Runnable() {

            @Override
            public void run() {
                startActivity(new Intent(BitmapActivity.this, MainTabs2.class));
            }
        }, 5000);

        File exportFile = new File(Environment.getExternalStorageDirectory(), "test.json");
        ExportSettingsManager.getInstance(this).exportAll(exportFile);
        ExportSettingsManager.getInstance(this).importAll(exportFile);

    }

    class MyAdapter1 extends BaseAdapter {

        private LayoutInflater inflater;
        private String pageTxt;
        private int width;
        private CodecDocument codecDocument;
        private String path;

        MyAdapter1(String path) {
            this.path = path;
            inflater = LayoutInflater.from(getApplicationContext());
            pageTxt = getString(R.string.page);
            width = Dips.dpToPx(100);
            codecDocument = instance.getNewCodecContext(path, "", 100, 200);
        }

        @Override
        public int getCount() {
            return codecDocument.getPageCount();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_page, parent, false);
            }
            ImageView img = (ImageView) view.findViewById(R.id.image1);

            String url = IMG.toUrl(path, position, width);

            ImageLoader.getInstance().displayImage(url, img, IMG.displayCacheMemoryDisc);

            TextView txt = (TextView) view.findViewById(R.id.text1);
            txt.setText("" + (position + 1));

            txt.setVisibility(View.VISIBLE);
            img.setVisibility(View.VISIBLE);

            return view;
        }
    }

}
