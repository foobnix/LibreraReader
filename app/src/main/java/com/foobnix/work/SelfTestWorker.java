package com.foobnix.work;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppData;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.common.settings.SettingsManager;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Engines;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.droids.mupdf.codec.MuPdfLinks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SelfTestWorker extends MessageWorker {

    public SelfTestWorker(
            @NonNull
            Context context,
            @NonNull
            WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static final String WORKER_NAME = "SelfTestWorker";

    // Where a run stands, kept across the worker being stopped (WorkManager stops a worker
    // after ten minutes and starts it again later) and across the app dying on a book
    private static final String PREFS = "SelfTestWorker";
    private static final String KEY_FRESH = "fresh";
    private static final String KEY_LOG = "log";
    private static final String KEY_NEXT = "next";
    // The book to go on from, by its path: the library is listed again on a resume and may
    // have changed - a scan still going adds books - so a place by number alone can drift
    private static final String KEY_NEXT_PATH = "next_path";
    private static final String KEY_OPENING = "opening";
    private static final String KEY_ERRORS = "errors";
    private static final String KEY_BEGIN = "begin";

    // What a run does with each book, chosen in the dialog. The first ones only read the book;
    // the last ones write to the library and are off unless asked for.
    private static final String[] OPTION_KEYS = {"opt_render", "opt_text", "opt_outline", "opt_annotations",
            "opt_recent", "opt_favorite", "opt_bookmarks", "opt_progress", "opt_all_pages"};
    private static final String[] OPTION_NAMES = {"Render first 2 pages", "Read text, links, HTML of first 2 pages",
            "Read outline (contents)", "Read PDF annotations",
            "Add every book to Recent", "Add every book to Favorites", "Add 5 bookmarks to every book",
            "Save reading progress (last page)", "Render EVERY page (slow, finds more crashes)"};
    private static final boolean[] OPTION_DEFAULTS = {true, true, true, true, false, false, false, false, false};
    private static final int OPT_ALL_PAGES = 8;

    // The engines to open books with: an AppState engine constant, or -1 for the reader's
    // setting. MuPDF by default, as checking a new MuPDF is what the test is mostly for.
    private static final String KEY_PDF_ENGINE = "pdf_engine";
    private static final String KEY_EPUB_ENGINE = "epub_engine";
    private static final int[] PDF_ENGINES = {-1, AppState.PDF_ENGINE_MUPDF, AppState.PDF_ENGINE_PDFIUM};
    private static final String[] PDF_ENGINE_NAMES = {"As in settings", "MuPDF", "PDFium"};
    private static final int[] EPUB_ENGINES = {-1, AppState.EPUB_ENGINE_MUPDF, AppState.EPUB_ENGINE_FOLIATE};
    private static final String[] EPUB_ENGINE_NAMES = {"As in settings", "MuPDF", "HTML+JS (foliate)"};
    // Books the app went down on, for the summary at the end
    private static final String KEY_CRASHED = "crashed";
    // Per format counts, kept across resumes: {"pdf":[tested, ok, zeroPages, errors, crashes], ...}
    private static final String KEY_STATS = "stats";
    private static final String KEY_SKIPPED = "skipped";
    // The summary of the last finished run, waiting to be shown in the library
    private static final String KEY_SUMMARY = "summary";
    private static final String KEY_SUMMARY_PENDING = "summary_pending";
    // The books that failed, one per line: "<what>\t<path>", to be opened from the summary
    private static final String KEY_PROBLEMS = "problems";
    // How many books of the library to test, from the top; 0 for all of them
    private static final String KEY_LIMIT = "limit";
    private static final int[] LIMITS = {50, 100, 0};
    private static final String[] LIMIT_NAMES = {"50", "100", "All"};
    public static final String ACTION_FINISHED = "com.foobnix.work.SelfTestWorker.FINISHED";
    private static final int TESTED = 0, OK = 1, ZERO = 2, ERRORS = 3, CRASHES = 4;
    // A book that takes longer than this is given up on: a MuPDF call cannot be stopped, and
    // it holds the document lock, so the run starts again in a new process after it
    private static final long HANG_MS = 3 * 60 * 1000L;
    // After an abort, how long the book is given to let go before the app is restarted
    private static final long ABORT_GRACE_MS = 30 * 1000L;
    // The document being tested now, for the watchdog to abort
    private volatile CodecDocument current;
    private volatile boolean aborted;

    private static boolean option(SharedPreferences sp, int i) {
        return sp.getBoolean(OPTION_KEYS[i], OPTION_DEFAULTS[i]);
    }

    /** The self test's own dialog: engines, what to test, then Start. Internal, so in English only. */
    public static void showDialog(final android.app.Activity a) {
        final SharedPreferences sp = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final int pad = Dips.dpToPx(20);
        final android.widget.LinearLayout root = new android.widget.LinearLayout(a);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(pad, Dips.dpToPx(8), pad, 0);

        final android.widget.RadioGroup pdf = engineGroup(a, root, "PDF engine", PDF_ENGINES, PDF_ENGINE_NAMES,
                sp.getInt(KEY_PDF_ENGINE, AppState.PDF_ENGINE_MUPDF), 0);
        final android.widget.RadioGroup epub = engineGroup(a, root, "EPUB, FB2, MOBI... engine", EPUB_ENGINES, EPUB_ENGINE_NAMES,
                sp.getInt(KEY_EPUB_ENGINE, AppState.EPUB_ENGINE_MUPDF), 0);

        final android.widget.RadioGroup limit = engineGroup(a, root, "Books to test, from the top of the library",
                LIMITS, LIMIT_NAMES, sp.getInt(KEY_LIMIT, 0), LIMITS.length - 1);
        limit.setOrientation(android.widget.RadioGroup.HORIZONTAL);

        final android.widget.TextView checksTitle = new android.widget.TextView(a);
        checksTitle.setText("Checks");
        checksTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        checksTitle.setPadding(0, Dips.dpToPx(12), 0, 0);
        root.addView(checksTitle);
        final android.widget.CheckBox[] boxes = new android.widget.CheckBox[OPTION_KEYS.length];
        for (int i = 0; i < OPTION_KEYS.length; i++) {
            boxes[i] = new android.widget.CheckBox(a);
            boxes[i].setText(OPTION_NAMES[i]);
            boxes[i].setChecked(option(sp, i));
            root.addView(boxes[i]);
        }
        // The runs before, newest first: a tap shows the result and the books that failed
        final List<File> logs = previousLogs();
        if (!logs.isEmpty()) {
            final android.widget.TextView prevTitle = new android.widget.TextView(a);
            prevTitle.setText("Previous results");
            prevTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            prevTitle.setPadding(0, Dips.dpToPx(12), 0, 0);
            root.addView(prevTitle);
            for (int i = 0; i < logs.size() && i < 15; i++) {
                final File log = logs.get(i);
                final Object[] read = readLog(log);
                final android.widget.TextView row = new android.widget.TextView(a);
                row.setText(logTitle(log) + "\n" + logEngines(log) + read[2]);
                row.setTextColor(0xFF2196F3);
                row.setPadding(0, Dips.dpToPx(8), 0, Dips.dpToPx(8));
                row.setOnClickListener(v -> {
                    @SuppressWarnings("unchecked") final List<String[]> problems = (List<String[]>) read[1];
                    showResult(a, logTitle(log), (String) read[0], problems);
                });
                root.addView(row);
            }
        }
        final android.widget.ScrollView scroll = new android.widget.ScrollView(a);
        scroll.addView(root);

        new android.app.AlertDialog.Builder(a)
                .setTitle("Self test: open every book of the library")
                .setView(scroll)
                .setPositiveButton("Start", (dialog, which) -> {
                    SharedPreferences.Editor edit = sp.edit();
                    for (int i = 0; i < boxes.length; i++) {
                        edit.putBoolean(OPTION_KEYS[i], boxes[i].isChecked());
                    }
                    edit.putInt(KEY_LIMIT, LIMITS[limit.getCheckedRadioButtonId()]);
                    edit.putInt(KEY_PDF_ENGINE, PDF_ENGINES[pdf.getCheckedRadioButtonId()]);
                    edit.putInt(KEY_EPUB_ENGINE, EPUB_ENGINES[epub.getCheckedRadioButtonId()]);
                    edit.commit();
                    run(a);
                    android.widget.Toast.makeText(a, "Self test started, log: " + AppProfile.syncTestFolder,
                                                  android.widget.Toast.LENGTH_LONG).show();
                })
                .setNeutralButton("Stop", (dialog, which) -> {
                    androidx.work.WorkManager.getInstance(a).cancelUniqueWork(WORKER_NAME);
                    sp.edit().putBoolean(KEY_FRESH, true).commit();
                    android.widget.Toast.makeText(a, "Self test stopped", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String formatOf(String path) {
        final String name = path.toLowerCase(java.util.Locale.US);
        final int dot = name.lastIndexOf('.');
        return dot >= 0 && dot > name.lastIndexOf('/') ? name.substring(dot + 1) : "?";
    }

    /**
     * Gives up on a book that takes too long: logged and counted, the run's place moved past
     * it, and the app's process ended - WorkManager starts the run again from there.
     */
    private Thread watchdog(final SharedPreferences sp, final BufferedWriter out, final String path, final String next) {
        aborted = false;
        final Thread tested = Thread.currentThread();
        final Thread t = new Thread(() -> {
            try {
                Thread.sleep(HANG_MS);
            } catch (InterruptedException e) {
                return;
            }
            // Where the book is stuck, for the log: the top of the test's own stack
            try {
                final StringBuilder where = new StringBuilder("Stuck in:");
                final StackTraceElement[] stack = tested.getStackTrace();
                for (int i = 0; i < stack.length && i < 12; i++) {
                    where.append("\n    ").append(stack[i]);
                }
                writeLine(out, where.toString());
            } catch (Exception e) {
                LOG.e(e);
            }
            // First asked to stop: MuPDF stops between operators and drawn objects
            final CodecDocument doc = current;
            if (doc != null) {
                aborted = true;
                doc.abort();
                try {
                    Thread.sleep(ABORT_GRACE_MS);
                } catch (InterruptedException e) {
                    return;
                }
            }
            // Still not done - one image being decoded is not stopped - so the app goes
            try {
                writeLine(out, "HANG: no result in " + (HANG_MS + ABORT_GRACE_MS) / 1000 + " s, restarting the app to go on");
                count(sp, path, ERRORS);
                problem(sp, "HANG (over " + HANG_MS / 60000 + " min)", path);
                sp.edit()
                  .remove(KEY_OPENING)
                  .putInt(KEY_NEXT, sp.getInt(KEY_NEXT, 0) + 1)
                  .putString(KEY_NEXT_PATH, next)
                  .putInt(KEY_ERRORS, sp.getInt(KEY_ERRORS, 0) + 1)
                  .commit();
                restartProcess(out);
            } catch (Exception e) {
                LOG.e(e);
            }
        }, "SelfTestWatchdog");
        t.setDaemon(true);
        t.start();
        return t;
    }

    private void restartProcess(BufferedWriter out) {
        try {
            out.flush();
        } catch (IOException e) {
            LOG.e(e);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private static int indexOf(List<FileMeta> all, String path) {
        if (path == null) {
            return -1;
        }
        for (int i = 0; i < all.size(); i++) {
            if (path.equals(all.get(i).getPath())) {
                return i;
            }
        }
        return -1;
    }

    /** Whether a run is going, to keep the screen on in the library meanwhile. */
    public static androidx.lifecycle.LiveData<List<androidx.work.WorkInfo>> watch(Context c) {
        return androidx.work.WorkManager.getInstance(c).getWorkInfosForUniqueWorkLiveData(WORKER_NAME);
    }

    private static void problem(SharedPreferences sp, String what, String path) {
        sp.edit().putString(KEY_PROBLEMS, sp.getString(KEY_PROBLEMS, "") + what + "\t" + path + "\n").commit();
    }

    private static void count(SharedPreferences sp, String path, int what) {
        try {
            final org.json.JSONObject stats = new org.json.JSONObject(sp.getString(KEY_STATS, "{}"));
            final String format = formatOf(path);
            org.json.JSONArray row = stats.optJSONArray(format);
            if (row == null) {
                row = new org.json.JSONArray("[0,0,0,0,0]");
            }
            if (what != CRASHES) {
                row.put(TESTED, row.getInt(TESTED) + 1);
            }
            row.put(what, row.getInt(what) + 1);
            stats.put(format, row);
            sp.edit().putString(KEY_STATS, stats.toString()).commit();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /** The run in numbers: per format, then in all. */
    private static String summary(SharedPreferences sp, int count, long seconds, File log) {
        final StringBuilder b = new StringBuilder();
        int[] total = new int[5];
        try {
            final org.json.JSONObject stats = new org.json.JSONObject(sp.getString(KEY_STATS, "{}"));
            final List<String> formats = new java.util.ArrayList<>();
            for (java.util.Iterator<String> it = stats.keys(); it.hasNext(); ) {
                formats.add(it.next());
            }
            java.util.Collections.sort(formats);
            for (String format : formats) {
                final org.json.JSONArray row = stats.getJSONArray(format);
                b.append(format.toUpperCase(java.util.Locale.US)).append(": ").append(row.getInt(TESTED)).append(" books");
                b.append(" (OK ").append(row.getInt(OK));
                if (row.getInt(ZERO) > 0) {
                    b.append(", 0 pages ").append(row.getInt(ZERO));
                }
                if (row.getInt(ERRORS) > 0) {
                    b.append(", errors ").append(row.getInt(ERRORS));
                }
                if (row.getInt(CRASHES) > 0) {
                    b.append(", CRASHES ").append(row.getInt(CRASHES));
                }
                b.append(")\n");
                for (int i = 0; i < total.length; i++) {
                    total[i] += row.getInt(i);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        b.append("\nLibrary: ").append(count).append(", tested ").append(total[TESTED])
         .append(", skipped ").append(sp.getInt(KEY_SKIPPED, 0)).append("\n");
        b.append("OK: ").append(total[OK]).append("\n");
        b.append("0 pages: ").append(total[ZERO]).append("\n");
        b.append("Errors: ").append(total[ERRORS]).append("\n");
        b.append("Crashes: ").append(total[CRASHES]).append("\n");
        final String crashed = sp.getString(KEY_CRASHED, "");
        if (TxtUtils.isNotEmpty(crashed)) {
            for (String line : crashed.split("\n")) {
                if (TxtUtils.isNotEmpty(line.trim())) {
                    b.append(line.trim()).append("\n");
                }
            }
        }
        b.append("Time: ").append(seconds / 60).append(" min ").append(seconds % 60).append(" s\n");
        b.append("Log: ").append(log.getPath());
        return b.toString();
    }

    /** The summary of a finished run, shown once, in the library. */
    public static void showSummaryIfReady(final android.app.Activity a) {
        final SharedPreferences sp = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!sp.getBoolean(KEY_SUMMARY_PENDING, false) || a.isFinishing()) {
            return;
        }
        sp.edit().putBoolean(KEY_SUMMARY_PENDING, false).commit();
        showSummary(a);
    }

    /**
     * The last run's numbers, and under them every book that failed: a tap opens it in the
     * reader, to see for oneself what goes wrong.
     */
    public static void showSummary(final android.app.Activity a) {
        final SharedPreferences sp = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final List<String[]> problems = new java.util.ArrayList<>();
        for (String line : sp.getString(KEY_PROBLEMS, "").split("\n")) {
            final int tab = line.indexOf('\t');
            if (tab > 0) {
                problems.add(new String[]{line.substring(0, tab).trim(), line.substring(tab + 1).trim()});
            }
        }
        showResult(a, "Self test result", sp.getString(KEY_SUMMARY, "No finished run yet"), problems);
    }

    /** A result: its numbers, and the books that failed, each opened in the reader by a tap. */
    private static void showResult(final android.app.Activity a, String title, String text, final List<String[]> problems) {
        final android.widget.TextView header = new android.widget.TextView(a);
        final int pad = Dips.dpToPx(20);
        header.setPadding(pad, Dips.dpToPx(8), pad, Dips.dpToPx(8));
        header.setTextIsSelectable(true);
        header.setText(text + (problems.isEmpty() ? "" : "\n\nTap a book to open it:"));

        final android.widget.ListView list = new android.widget.ListView(a);
        list.addHeaderView(header, null, false);
        final List<String> rows = new java.util.ArrayList<>();
        for (String[] p : problems) {
            rows.add(p[0] + "\n" + new File(p[1]).getName());
        }
        list.setAdapter(new android.widget.ArrayAdapter<>(a, android.R.layout.simple_list_item_1, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            final int index = position - list.getHeaderViewsCount();
            if (index >= 0 && index < problems.size()) {
                ExtUtils.openFile(a, new FileMeta(problems.get(index)[1]));
            }
        });
        new android.app.AlertDialog.Builder(a)
                .setTitle(title)
                .setView(list)
                .setPositiveButton("Close", null)
                .show();
    }

    /** The logs of earlier runs, newest first. */
    private static List<File> previousLogs() {
        final List<File> logs = new java.util.ArrayList<>();
        final File[] files = AppProfile.syncTestFolder == null ? null : AppProfile.syncTestFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".txt")) {
                    logs.add(f);
                }
            }
        }
        java.util.Collections.sort(logs, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
        return logs;
    }

    /**
     * What a log says, read back: the summary of a run that finished, or the numbers so far
     * of one that did not, and every book that failed. Logs of older versions read the same.
     */
    private static Object[] readLog(File log) {
        final List<String[]> problems = new java.util.ArrayList<>();
        final StringBuilder summary = new StringBuilder();
        int ok = 0, failed = 0, skipped = 0, crashes = 0;
        String lastPath = null, stopped = null;
        boolean inSummary = false;
        try (java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(log))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (inSummary) {
                    summary.append(line).append("\n");
                    continue;
                }
                if (line.equals("[Summary]")) {
                    inSummary = true;
                } else if (line.matches("^\\d+:/.*")) {
                    lastPath = line.substring(line.indexOf(':') + 1);
                } else if (line.startsWith("OK")) {
                    ok++;
                } else if (line.equals("Skip") || line.startsWith("Password")) {
                    skipped++;
                } else if (line.startsWith("Error") && lastPath != null) {
                    failed++;
                    final String what = line.replaceAll(" nativeMB=\\d+$", "");
                    problems.add(new String[]{what.startsWith("Error: no pages") ? "0 pages" : what, lastPath});
                } else if (line.startsWith("Crashed: ")) {
                    crashes++;
                    problems.add(new String[]{"CRASH", line.substring("Crashed: ".length())});
                } else if (line.startsWith("[Stopped")) {
                    stopped = line;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        final String text;
        final String brief;
        if (summary.length() > 0) {
            text = summary.toString().trim();
            brief = "OK " + ok + ", failed " + failed + ", crashes " + crashes;
        } else {
            text = "Not finished" + (stopped != null ? " " + stopped : "") + "\n\nOK: " + ok + "\nFailed: " + failed
                    + "\nCrashes: " + crashes + "\nSkipped: " + skipped + "\nLog: " + log.getPath();
            brief = "not finished, OK " + ok + ", failed " + failed + ", crashes " + crashes;
        }
        return new Object[]{text, problems, brief};
    }

    /** "2026-11-20 09:37, 10.1.7, MuPDF 1.28.6" from a log's name and time. */
    private static String logTitle(File log) {
        final String when = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                .format(new java.util.Date(log.lastModified()));
        // <app>_<version>_<mupdf>t+_<millis>.txt
        final String[] parts = log.getName().replace(".txt", "").split("_");
        if (parts.length >= 4) {
            return when + ", " + parts[parts.length - 3] + ", MuPDF " + parts[parts.length - 2].replace("t+", "");
        }
        return when + ", " + log.getName();
    }

    private static String logEngines(File log) {
        try (java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(log))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("PDF engine: ")) {
                    final String[] engines = line.substring("PDF engine: ".length()).split(", EPUB engine: ");
                    return engines.length == 2 ? "PDF: " + engines[0] + ", EPUB: " + engines[1] + "\n" : "";
                }
                if (line.startsWith("[Options]") || line.startsWith("OK") || line.startsWith("Error")) {
                    break;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    /** A titled row of radio buttons, one per value; a button's id is its index. */
    private static android.widget.RadioGroup engineGroup(Context c, android.widget.LinearLayout root, String title,
                                                         int[] engines, String[] names, int selected, int fallback) {
        final android.widget.TextView label = new android.widget.TextView(c);
        label.setText(title);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, Dips.dpToPx(12), 0, 0);
        root.addView(label);
        final android.widget.RadioGroup group = new android.widget.RadioGroup(c);
        group.setOrientation(android.widget.RadioGroup.VERTICAL);
        for (int i = 0; i < engines.length; i++) {
            final android.widget.RadioButton button = new android.widget.RadioButton(c);
            button.setId(i);
            button.setText(names[i]);
            group.addView(button);
            if (engines[i] == selected) {
                group.check(i);
            }
        }
        // A value stored that is no longer offered (a count typed in before)
        if (group.getCheckedRadioButtonId() == -1) {
            group.check(fallback);
        }
        root.addView(group);
        return group;
    }

    /**
     * Opens every book of the library in turn and writes what it finds - pages, outline,
     * links, text of the first pages - to a log in the test folder. The library is only
     * changed where the dialog asked for it. Started again, the run before is given up and a
     * new one begins.
     */
    public static void run(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_FRESH, true).commit();
        // A run restarted after a book took the app down goes on soon after, not after a
        // backoff that doubles each time
        androidx.work.OneTimeWorkRequest request =
                new androidx.work.OneTimeWorkRequest.Builder(SelfTestWorker.class)
                        .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 10, java.util.concurrent.TimeUnit.SECONDS)
                        .build();
        androidx.work.WorkManager.getInstance(context)
                                 .enqueueUniqueWork(WORKER_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request);
    }

    @Override
    public boolean doWorkInner() throws IOException {
        TempHolder.get().loadingCancelled.set(false);
        AppProfile.init(getApplicationContext());
        AppProfile.syncTestFolder.mkdirs();
        final SharedPreferences sp = getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        List<FileMeta>
                all =
                AppDB.get().searchBy("", AppDB.SORT_BY.getByID(AppState.get().sortBy), AppState.get().isSortAsc);

        int w = Dips.screenWidth();
        int h = Dips.screenHeight();
        int s = BookCSS.get().fontSizeSp;
        final int limit = sp.getInt(KEY_LIMIT, 0);
        int count = limit > 0 ? Math.min(limit, all.size()) : all.size();

        final boolean fresh = sp.getBoolean(KEY_FRESH, true) || TxtUtils.isEmpty(sp.getString(KEY_LOG, null));
        File logFile;
        int start;
        int errors;
        long begin;
        if (fresh) {
            logFile = AppData.getTestFileName();
            logFile.delete();
            start = 0;
            errors = 0;
            begin = System.currentTimeMillis();
            sp.edit()
              .putBoolean(KEY_FRESH, false)
              .putString(KEY_LOG, logFile.getPath())
              .putInt(KEY_NEXT, 0)
              .remove(KEY_NEXT_PATH)
              .putInt(KEY_ERRORS, 0)
              .putLong(KEY_BEGIN, begin)
              .remove(KEY_OPENING)
              .remove(KEY_CRASHED)
              .remove(KEY_STATS)
              .remove(KEY_PROBLEMS)
              .putInt(KEY_SKIPPED, 0)
              .commit();
        } else {
            logFile = new File(sp.getString(KEY_LOG, ""));
            start = sp.getInt(KEY_NEXT, 0);
            final int found = indexOf(all, sp.getString(KEY_NEXT_PATH, null));
            if (found >= 0) {
                start = found;
            }
            errors = sp.getInt(KEY_ERRORS, 0);
            begin = sp.getLong(KEY_BEGIN, System.currentTimeMillis());
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(logFile, !fresh));
        // The run goes on with the screen off; the library keeps the screen on while it is open
        final android.os.PowerManager.WakeLock wakeLock = ((android.os.PowerManager) getApplicationContext()
                .getSystemService(Context.POWER_SERVICE)).newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "Librera:SelfTest");
        wakeLock.acquire(60 * 60 * 1000L);
        // The engines chosen in the dialog, for this thread only
        Engines.TEST_ENGINES.set(new int[]{sp.getInt(KEY_PDF_ENGINE, AppState.PDF_ENGINE_MUPDF),
                sp.getInt(KEY_EPUB_ENGINE, AppState.EPUB_ENGINE_MUPDF)});
        try {
            if (fresh) {
                writeHeader(out, count);
                // As the engines are asked for on this thread, with the dialog's choice in force
                writeLine(out, "PDF engine: " + (AppState.get().getPdfEngine() == AppState.PDF_ENGINE_PDFIUM ? "PDFium" : "MuPDF")
                        + ", EPUB engine: " + (AppState.get().getEpubEngine() == AppState.EPUB_ENGINE_FOLIATE ? "foliate" : "MuPDF"));
                writeLine(out, "[Options]");
                for (int i = 0; i < OPTION_KEYS.length; i++) {
                    writeLine(out, (option(sp, i) ? "[x] " : "[ ] ") + OPTION_NAMES[i]);
                }
            } else {
                writeLine(out, "[Resumed at " + (start + 1) + "/" + count + "]");
                // The book being opened when the app went down: counted, and not opened again
                final String crashed = sp.getString(KEY_OPENING, null);
                if (crashed != null && com.foobnix.librerax.BookOpenGuard.lastRunEndedNormally(getApplicationContext())) {
                    // The app was updated, stopped or closed while this book was open, not
                    // brought down by it: the book is tested again
                    writeLine(out, "Interrupted (app closed or updated), again: " + crashed);
                    sp.edit().remove(KEY_OPENING).commit();
                } else if (crashed != null) {
                    writeLine(out, "Crashed: " + crashed);
                    errors++;
                    count(sp, crashed, CRASHES);
                    problem(sp, "CRASH", crashed);
                    final int at = indexOf(all, crashed);
                    start = (at >= 0 ? at : start) + 1;
                    final String list = sp.getString(KEY_CRASHED, "");
                    sp.edit()
                      .remove(KEY_OPENING)
                      .putInt(KEY_NEXT, start)
                      .putString(KEY_NEXT_PATH, start < count ? all.get(start).getPath() : null)
                      .putInt(KEY_ERRORS, errors)
                      .putString(KEY_CRASHED, list + crashed + "\n")
                      .commit();
                }
            }

            sendNotifyAll();

            for (int n = start; n < count; n++) {
                if (isStopped()) {
                    writeLine(out, "[Stopped at " + (n + 1) + "/" + count + "]");
                    return false;
                }
                final FileMeta item = all.get(n);
                final String path = item.getPath();
                writeLine(out, (n + 1) + ":" + path);

                if (TxtUtils.isEmpty(path)
                        || (ExtUtils.isZip(path) && !CacheZipUtils.isSingleAndSupportEntry(path).first)) {
                    writeLine(out, "Skip");
                    sp.edit()
                      .putInt(KEY_NEXT, n + 1)
                      .putString(KEY_NEXT_PATH, n + 1 < count ? all.get(n + 1).getPath() : null)
                      .putInt(KEY_SKIPPED, sp.getInt(KEY_SKIPPED, 0) + 1)
                      .commit();
                    continue;
                }
                sendTextMessage((n + 1) + "/" + count);
                sp.edit().putString(KEY_OPENING, path).putInt(KEY_NEXT, n).putString(KEY_NEXT_PATH, path).commit();

                final String next = n + 1 < count ? all.get(n + 1).getPath() : null;
                final Thread watchdog = watchdog(sp, out, path, next);
                String test;
                try {
                    test = testBook(sp, path, w, h, s);
                } finally {
                    watchdog.interrupt();
                    current = null;
                }
                if (aborted) {
                    test = "Error: HANG, aborted after " + HANG_MS / 1000 + " s";
                }
                // Native heap once the book is let go of: growing from book to book is a leak
                final String result = test + " nativeMB=" + (android.os.Debug.getNativeHeapAllocatedSize() >> 20);
                if (result.startsWith("Error")) {
                    errors++;
                    sendNotifyAll();
                }
                if (result.startsWith("Password")) {
                    sp.edit().putInt(KEY_SKIPPED, sp.getInt(KEY_SKIPPED, 0) + 1).commit();
                } else {
                    count(sp, path, result.startsWith("OK") ? OK : result.startsWith("Error: no pages") ? ZERO : ERRORS);
                }
                if (!result.startsWith("OK") && !result.startsWith("Password")) {
                    problem(sp, result.startsWith("Error: no pages") ? "0 pages"
                            : result.startsWith("Error: HANG") ? "HANG (aborted)" : result.replaceAll(" nativeMB=\\d+$", ""), path);
                }
                writeLine(out, result);
                sp.edit()
                  .remove(KEY_OPENING)
                  .putInt(KEY_NEXT, n + 1)
                  .putString(KEY_NEXT_PATH, n + 1 < count ? all.get(n + 1).getPath() : null)
                  .putInt(KEY_ERRORS, errors)
                  .commit();
                // Native memory is not given back after running out of it: the run goes on in
                // a new process, with its place kept
                if (result.contains("OutOfMemoryError")) {
                    writeLine(out, "[Out of memory: restarting the app to go on]");
                    restartProcess(out);
                }
            }

            final String summary = summary(sp, count, (System.currentTimeMillis() - begin) / 1000L, logFile);
            writeLine(out, "[Summary]");
            writeLine(out, summary);
            sp.edit().putBoolean(KEY_FRESH, true).putString(KEY_SUMMARY, summary).putBoolean(KEY_SUMMARY_PENDING, true).commit();
            // The library, if it is open, shows it straight away
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(new android.content.Intent(ACTION_FINISHED));
        } finally {
            Engines.TEST_ENGINES.remove();
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            out.close();
        }

        sendNotifyAll();
        return true;
    }

    private void writeHeader(BufferedWriter out, int count) throws IOException {
        writeLine(out, "ApplicationName: " + Apps.getApplicationName(getApplicationContext()));
        writeLine(out, "VersionName: " + Apps.getVersionName(getApplicationContext()));
        writeLine(out, "PackageName: " + Apps.getPackageName(getApplicationContext()));
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
    }

    /**
     * Opens one book, reads its outline and the first two pages - picture, text, links, PDF
     * annotations - and lets go of all of it again. Returns "OK ..." or "Error ..." for the log.
     */
    private String testBook(SharedPreferences sp, String path, int w, int h, int s) {
        final boolean render = option(sp, 0), text = option(sp, 1), outline = option(sp, 2), annotations = option(sp, 3);
        final long t0 = System.currentTimeMillis();
        CodecDocument codecDocument = null;
        try {
            CodecContext codecContex = BookType.getCodecContextByPath(path);
            codecDocument = TempHolder.get().loadingCancelled.ignoredWhile(() -> codecContex.openDocument(path, ""));
            current = codecDocument;
            if (codecDocument == null) {
                return "Error: not opened";
            }
            int pageCount = codecDocument.getPageCount(w, h, s);
            if (pageCount == 0) {
                return "Error: no pages";
            }
            int outlineItems = 0;
            for (OutlineLink ol : outline ? codecDocument.getOutline() : new java.util.ArrayList<OutlineLink>()) {
                if (TempHolder.get().loadingCancelled.get() || isStopped() || codecDocument.isRecycled()) {
                    break;
                }
                if (TxtUtils.isNotEmpty(ol.getTitle())) {
                    // As the contents panel reads it: "#<page>" as it is, anything else asked of MuPDF
                    if (!(ol.getLink() != null && ol.getLink().startsWith("#") && !ol.getLink().startsWith("#0"))) {
                        MuPdfLinks.getLinkPageWrapper(ol.docHandle, ol.linkUri);
                    }
                    outlineItems++;
                }
            }

            final int pages = option(sp, OPT_ALL_PAGES) ? pageCount : Math.min(2, pageCount);
            for (int pageNumber = 0; pageNumber < pages; pageNumber++) {
                if (isStopped() || aborted) {
                    break;
                }
                CodecPage page = codecDocument.getPage(pageNumber);
                // Stopped by the watchdog: each step would only run the same page again
                if (aborted) {
                    page.recycle();
                    break;
                }
                try {
                    if (render || pages > 2) {
                        BitmapRef bitmapRef = page.renderBitmap(w, h, new RectF(0, 0, 1f, 1f), false);
                        if (bitmapRef == null || bitmapRef.getBitmap() == null) {
                            return "Error: page " + (pageNumber + 1) + " not drawn";
                        }
                        bitmapRef.getBitmap().recycle();
                    }
                    if (text && pageNumber < 2 && !aborted) {
                        page.getText();
                        page.getPageLinks();
                        page.getPageHTML();
                    }
                    if (annotations && pageNumber < 2 && !aborted && BookType.PDF.is(path)) {
                        page.getAnnotations();
                    }
                } finally {
                    if (!page.isRecycled()) {
                        page.recycle();
                    }
                }
            }
            // What the dialog asked to be written to the library
            if (option(sp, 7)) {
                AppBook bs = SettingsManager.getBookSettings(path);
                bs.currentPageChanged(pageCount - 1, pageCount + 1);
                SharedBooks.save(bs);
            }
            if (option(sp, 4)) {
                AppData.get().addRecent(new SimpleMeta(path));
            }
            if (option(sp, 5)) {
                AppData.get().addFavorite(new SimpleMeta(path));
            }
            if (option(sp, 6)) {
                for (int i = 0; i < 5; i++) {
                    BookmarksData.get().add(new AppBookmark(path, i + " : " + path, i / 10f));
                }
            }
            return "OK pages=" + pageCount + " outline=" + outlineItems + " ms=" + (System.currentTimeMillis() - t0);
        } catch (Throwable e) {
            // A book behind a password is not a failure of the engine
            if (e.getClass().getSimpleName().contains("PasswordRequired")) {
                return "Password";
            }
            LOG.e(e, "Error:" + path);
            return "Error: " + e;
        } finally {
            // DjVu documents are left open here, as the test always did
            if (codecDocument != null && !codecDocument.isRecycled() && !BookType.DJVU.is(path)) {
                try {
                    codecDocument.recycle();
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
        }
    }

    public void writeLine(BufferedWriter out, String line) throws IOException {
        LOG.d("Self-test", line);
        out.write(line);
        out.newLine();
        out.flush();
    }
}
