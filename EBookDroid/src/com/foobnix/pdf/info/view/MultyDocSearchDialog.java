package com.foobnix.pdf.info.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.BookType;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MultyDocSearchDialog {

    public static class Model {
        static Model inst = new Model();
        public String path = Environment.getExternalStorageDirectory().getPath();
        public String text;
        public volatile boolean isSearcingRun = false;

        public String currentDoc;
        public int currentPage;
        public int currentPagesCount;

        public List<Pair<String, Integer>> res = new ArrayList<Pair<String, Integer>>();

        public static Model get() {
            return inst;
        }
    }

    public static void show(FragmentActivity c) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Doc Search");
        builder.setView(getDialogView(c));
        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
            }
        });
        builder.show();
    }

    public static View getDialogView(final FragmentActivity c) {
        View inflate = LayoutInflater.from(c).inflate(R.layout.dialog_multy_search, null, false);

        final EditText editPath = (EditText) inflate.findViewById(R.id.editPath);
        final Button buttonPath = (Button) inflate.findViewById(R.id.buttonPath);

        final EditText editSearchText = (EditText) inflate.findViewById(R.id.editSearchText);
        final Button searchStart = (Button) inflate.findViewById(R.id.searchStart);
        final Button searchStop = (Button) inflate.findViewById(R.id.searchStop);

        final TextView infoView1 = (TextView) inflate.findViewById(R.id.infoView1);
        final ListView listView = (ListView) inflate.findViewById(R.id.listView);

        final BaseItemLayoutAdapter adapter = new BaseItemLayoutAdapter<Pair<String, Integer>>(c, android.R.layout.simple_list_item_1, Model.get().res) {

            @Override
            public void populateView(View layout, int position, final Pair<String, Integer> item) {

                final File file = new File(item.first);

                TextView t = (TextView) layout.findViewById(android.R.id.text1);
                t.setText(file.getName() + " [" + (item.second == -1 ? "not found" : (item.second + 1)) + "]");

                t.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ExtUtils.showDocument(c, file, item.second + 1);
                    }
                });
            }

        };

        listView.setAdapter(adapter);

        buttonPath.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ChooserDialogFragment.chooseFolder(c, Model.get().path).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                    @Override
                    public boolean onResultRecive(String nPath, Dialog dialog) {
                        Model.get().path = nPath;
                        editPath.setText(Model.get().path);
                        dialog.dismiss();

                        return false;
                    }
                });

            }
        });
        editPath.setText(Model.get().path);

        final Handler updater2 = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                adapter.notifyDataSetChanged();
            }

        };

        final Handler updater1 = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                infoView1.setText(Model.get().currentPage + "/" + Model.get().currentPagesCount + " " + Model.get().currentDoc);
            }

        };

        searchStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Model.get().text = editSearchText.getText().toString().trim();
                if (TxtUtils.isEmpty(Model.get().text)) {
                    Toast.makeText(c, "Empty Criteria", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Model.get().isSearcingRun) {
                    Toast.makeText(c, "Searching is running", Toast.LENGTH_SHORT).show();
                    return;
                }
                Model.get().isSearcingRun = true;
                new MyTask(updater1, updater2).execute();

                infoView1.setText("");

                Model.get().res.clear();
                adapter.notifyDataSetChanged();
            }
        });

        searchStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Model.get().isSearcingRun = false;
            }
        });
        infoView1.setText("");

        Model.get().res.clear();
        adapter.notifyDataSetChanged();

        return inflate;

    }

    static class MyTask extends AsyncTask {

        private Handler updater1, updater2;

        public MyTask(Handler h1, Handler h2) {
            this.updater1 = h1;
            this.updater2 = h2;

        }

        @Override
        protected Object doInBackground(Object... params) {

            Model.get().res.clear();

            File[] list = new File(Model.get().path).listFiles();
            for (File file : list) {
                if (!Model.get().isSearcingRun) {
                    return -1;
                }
                if (file.getName().toLowerCase(Locale.US).endsWith(".pdf")) {
                    int page = -1;
                    try {
                        page = searchInThePDF(file.getPath(), Model.get().text, updater1);
                    } catch (Exception e) {
                        page = -2;
                    }
                    Model.get().res.add(new Pair<String, Integer>(file.getPath(), page));
                    updater2.sendEmptyMessage(0);
                }
            }
            updater2.sendEmptyMessage(0);
            Model.get().isSearcingRun = false;
            return null;
        }

    };

    public static int searchInThePDF(String path, String text, final Handler update1) {
        text = text.toLowerCase(Locale.US);
        CodecContext ctx = BookType.getCodecContextByPath(path);
        CodecDocument openDocument = null;
        CacheZipUtils.cacheLock.lock();
        try {
            // String zipPath = CacheZipUtils.extracIfNeed(path).unZipPath;
            openDocument = ctx.openDocument(path, "");
        } finally {
            CacheZipUtils.cacheLock.unlock();
        }

        int pageCount = openDocument.getPageCount();
        Model.get().currentPagesCount = pageCount;
        Model.get().currentDoc = new File(path).getName();

        for (int i = 0; i < pageCount; i++) {
            if (!Model.get().isSearcingRun) {
                return -1;
            }
            CodecPage page = openDocument.getPage(i);
            List<TextWord> findText = Page.findText(text, page.getText());
            page.recycle();

            Model.get().currentPage = i;
            update1.sendEmptyMessage(0);

            if (!findText.isEmpty()) {
                ctx.recycle();
                return i;
            }
        }

        ctx.recycle();
        return -1;
    }

}
