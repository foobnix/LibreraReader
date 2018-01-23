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
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MultyDocSearchDialog {

    private static TextView infoView1;

    public static class Model {
        static Model inst = new Model();
        public String path = Environment.getExternalStorageDirectory().getPath();
        public String text;
        public volatile boolean isSearcingRun = false;

        public volatile String currentDoc;
        public volatile int currentPage;
        public volatile int currentPagesCount;

        public List<Pair<String, Integer>> res = new ArrayList<Pair<String, Integer>>();

        public static Model get() {
            return inst;
        }
    }

    public static void show(FragmentActivity c) {
        Model.get().path = AppState.get().dirLastPath == null ? Environment.getExternalStorageDirectory().getPath() : AppState.get().dirLastPath;
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(R.string.search_for_text_in_documents);
        builder.setView(getDialogView(c));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                Model.get().isSearcingRun = false;
            }
        });
        builder.show();
    }

    public static View getDialogView(final FragmentActivity c) {
        View inflate = LayoutInflater.from(c).inflate(R.layout.dialog_multy_search, null, false);

        inflate.setKeepScreenOn(true);

        final EditText editPath = (EditText) inflate.findViewById(R.id.editPath);
        final Button buttonPath = (Button) inflate.findViewById(R.id.buttonPath);

        final EditText editSearchText = (EditText) inflate.findViewById(R.id.editSearchText);
        final Button searchStart = (Button) inflate.findViewById(R.id.searchStart);
        final Button searchStop = (Button) inflate.findViewById(R.id.searchStop);

        infoView1 = (TextView) inflate.findViewById(R.id.infoView1);
        final ListView listView = (ListView) inflate.findViewById(R.id.listView);

        final CheckBox searchInTheSubfolders = (CheckBox) inflate.findViewById(R.id.searchInTheSubfolders);

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
                        AppState.get().dirLastPath = nPath;
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
                infoView1.setText((Model.get().currentPage + 1) + "/" + Model.get().currentPagesCount + " " + Model.get().currentDoc);
            }

        };

        searchStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Model.get().text = editSearchText.getText().toString().trim();
                if (TxtUtils.isEmpty(Model.get().text)) {
                    Toast.makeText(c, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Model.get().text.contains(" ")) {
                    Toast.makeText(c, R.string.you_can_search_only_one_word, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Model.get().isSearcingRun) {
                    Toast.makeText(c, R.string.searching_please_wait_, Toast.LENGTH_SHORT).show();
                    return;
                }
                Model.get().isSearcingRun = true;
                new MyTask(updater1, updater2, c, searchInTheSubfolders.isChecked()).execute();

                infoView1.setText("");

                Model.get().res.clear();
                adapter.notifyDataSetChanged();

                Keyboards.close(editSearchText);

            }
        });

        editSearchText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchStart.performClick();
                    handled = true;
                }
                return handled;
            }
        });

        editSearchText.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    searchStart.performClick();
                    return true;
                }
                return false;
            }
        });

        searchStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Model.get().isSearcingRun = false;
                infoView1.setText("");
            }
        });
        infoView1.setText("");

        Model.get().res.clear();
        adapter.notifyDataSetChanged();

        return inflate;

    }

    static class MyTask extends AsyncTask {

        private Handler updater1, updater2;
        private FragmentActivity a;
        private boolean isReqursiveSearch;

        public MyTask(Handler h1, Handler h2, FragmentActivity a, boolean isReqursiveSearch) {
            this.updater1 = h1;
            this.updater2 = h2;
            this.a = a;
            this.isReqursiveSearch = isReqursiveSearch;

        }

        @Override
        protected Object doInBackground(Object... params) {

            Model.get().res.clear();

            final List<FileMeta> allFilesMeta = new ArrayList<FileMeta>();

            a.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    infoView1.setText(R.string.searching_please_wait_);
                }
            });

            if (isReqursiveSearch) {
                SearchCore.search(allFilesMeta, new File(Model.get().path), ExtUtils.seachExts);
            } else {
                SearchCore.searchSimple(allFilesMeta, new File(Model.get().path), ExtUtils.seachExts);
            }

            for (FileMeta fileMeta : allFilesMeta) {
                final String filePath = fileMeta.getPath();
                if (!Model.get().isSearcingRun) {
                    return -1;
                }
                final int page = searchInThePDF(filePath, Model.get().text, updater1);

                if (page != -1) {
                    a.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Model.get().res.add(new Pair<String, Integer>(filePath, page));
                        }
                    });

                    updater2.sendEmptyMessage(0);
                }
            }
            updater2.sendEmptyMessage(0);
            Model.get().isSearcingRun = false;

            a.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    infoView1.setText(R.string.search_completed);
                }
            });

            return null;
        }

    };

    public static int searchInThePDF(String path, String text, final Handler update1) {
        try {
            text = text.toLowerCase(Locale.US);
            CodecContext ctx = BookType.getCodecContextByPath(path);
            CodecDocument openDocument = null;
            CacheZipUtils.cacheLock.lock();
            try {
                String zipPath = CacheZipUtils.extracIfNeed(path, CacheDir.ZipApp).unZipPath;
                openDocument = ctx.openDocument(zipPath, "");
                LOG.d("searchInThePDF", openDocument, zipPath);
            } finally {
                CacheZipUtils.cacheLock.unlock();
            }

            if (!Model.get().isSearcingRun) {
                openDocument.recycle();
                ctx.recycle();
                return -1;
            }

            int pageCount = openDocument.getPageCount();
            Model.get().currentPagesCount = pageCount;
            Model.get().currentDoc = new File(path).getName();

            LOG.d("searchInThePDF", "pageCount", Model.get().currentPagesCount, Model.get().currentDoc);
            int emptyCount = 0;
            for (int i = 0; i < pageCount; i++) {
                if (!Model.get().isSearcingRun) {
                    openDocument.recycle();
                    ctx.recycle();
                    return -1;
                }
                CodecPage page = openDocument.getPage(i);

                TextWord[][] textPage = page.getText();
                LOG.d("searchInThePDF", "getText", textPage != null ? textPage.length : "null");
                if (textPage == null || textPage.length <= 1) {
                    emptyCount++;
                    if (emptyCount >= 5) {
                        LOG.d("searchInThePDF", "Page is empty", emptyCount);

                        break;
                    }
                }

                List<TextWord> findText = Page.findText(text, textPage);
                page.recycle();
                page = null;

                Model.get().currentPage = i;
                update1.sendEmptyMessage(0);

                if (!findText.isEmpty()) {
                    openDocument.recycle();
                    ctx.recycle();
                    findText = null;
                    openDocument = null;
                    ctx = null;
                    return i;
                }
                findText = null;

            }
            openDocument.recycle();
            ctx.recycle();
            openDocument = null;
            ctx = null;
            System.gc();
        } catch (Exception e) {
            LOG.e(e);
        }
        return -1;
    }

}
