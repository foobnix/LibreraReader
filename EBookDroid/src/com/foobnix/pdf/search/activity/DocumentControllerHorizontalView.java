package com.foobnix.pdf.search.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.GeneralDocInterface;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MessageAutoFit;
import com.foobnix.pdf.search.activity.msg.MessageCenterHorizontally;
import com.foobnix.sys.GeneralDocInterfaceImpl;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.widget.Toast;

public abstract class DocumentControllerHorizontalView extends DocumentController {
    public static final String PASSWORD_EXTRA = "password";
    public static final String PAGE = "page";
    public static final String PERCENT_EXTRA = "percent";

    private GeneralDocInterface generadDocInterface;
    private int pagesCount;
    int currentPage;
    private CopyAsyncTask searchTask;
    private boolean isTextFormat = false;
    String bookPath;

    int imageWidth, imageHeight;
    private SharedPreferences matrixSP;

    public DocumentControllerHorizontalView(Activity activity, int w, int h) {
        super(activity);
        matrixSP = activity.getSharedPreferences("matrix", Context.MODE_PRIVATE);
        LOG.d("DocumentControllerHorizontalView", "begin");
        try {
            generadDocInterface = new GeneralDocInterfaceImpl();
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(activity, "Erorr ...", Toast.LENGTH_LONG).show();
        }
        LOG.d("DocumentControllerHorizontalView", "end");

        isTextFormat = ExtUtils.isTextFomat(activity.getIntent());

        udpateImageSize(w, h);

        if (isTextFormat) {
            AppState.get().isCrop = false;
            AppState.get().isCut = false;
            AppState.get().isLocked = true;
        }
    }

    public void udpateImageSize(int w, int h) {
        imageWidth = isTextFormat ? w : (int) (Math.min(Dips.screenWidth(), Dips.screenHeight()) * AppState.get().pageQuality);
        imageHeight = isTextFormat ? h : (int) (Math.max(Dips.screenWidth(), Dips.screenHeight()) * AppState.get().pageQuality);
    }

    @Override
    public void onLinkHistory() {
        if (!getLinkHistory().isEmpty()) {
            final int last = getLinkHistory().removeLast();
            onGoToPage(last);
        }
    }

    @Override
    public float getOffsetY() {
        return getCurentPageFirst1();
    }

    @Override
    public void cleanImageMatrix() {
        matrixSP.edit().remove(bookPath).commit();
    }

    @Override
    public void saveAnnotationsToFile() {
    }

    public static String getBookPathFromActivity(Activity a) {
        String bookPath = CacheManager.getFilePathFromAttachmentIfNeed(a);
        if (TxtUtils.isEmpty(bookPath)) {
            bookPath = a.getIntent().getData().getPath();
        }
        return bookPath;
    }

    public static String getTempTitle(Activity a) {
        try {
            return getTitle(getBookPathFromActivity(a));
        } catch (Exception e) {
            LOG.e(e);
            return "";
        }
    }

    public void init(final Activity activity) {
        PageImageState.get().cleanSelectedWords();
        PageImageState.get().pagesText.clear();

        LOG.d("DocumentControllerHorizontalView", "init begin");

        bookPath = getBookPathFromActivity(activity);

        if (true) {
            PageImageState.get().needAutoFit = true;
        } else {
            if (TxtUtils.isNotEmpty(bookPath) && !ExtUtils.isTextFomat(bookPath)) {
                String string = matrixSP.getString(bookPath.hashCode() + "", "");
                LOG.d("MATRIX", "READ STR", string);
                if (TxtUtils.isNotEmpty(string)) {
                    PageImageState.get().needAutoFit = false;
                } else {
                    PageImageState.get().needAutoFit = true;
                }
                Matrix matrix = PageImageState.fromString(string);
                PageImageState.get().getMatrix().set(matrix);

                LOG.d("MATRIX", "READ", bookPath.hashCode() + "", PageImageState.get().getMatrixAsString());

            }
        }

        BookSettings bs = SettingsManager.getBookSettings(bookPath);
        if (bs != null) {
            AppState.get().isCut = bs.splitPages;
            AppState.get().isCrop = bs.cropPages;
            AppState.get().isDouble = bs.doublePages;
            AppState.get().isDoubleCoverAlone = bs.doublePagesCover;
            AppState.get().isLocked = bs.isLocked;
        }

        if (AppState.get().isDouble) {
            if (isTextFormat) {
                imageWidth = Dips.screenWidth() / 2;
            } else {
                imageWidth = (int) (Dips.screenWidth() * AppState.get().pageQuality / 2);
            }
        }

        FileMetaCore.checkOrCreateMetaInfo(activity);
        LOG.d("pagesCount", "init", imageWidth, imageHeight);
        pagesCount = generadDocInterface.getPageCount(getBookPath(), activity.getIntent().getStringExtra(PASSWORD_EXTRA), imageWidth, imageHeight, AppState.get().fontSizeSp);
        if (pagesCount == -1) {
            throw new IllegalArgumentException("Pages count = -1");
        }

        generadDocInterface.addToRecent(activity, activity.getIntent().getData());
        getPageFromUri();

        loadOutline(null);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        currentPage = page;
    }

    @Override
    public int getCurentPageFirst1() {
        return currentPage + 1;
    }

    public int getPageFromUri() {
        final double percent = activity.getIntent().getDoubleExtra(PERCENT_EXTRA, 0);
        int number = activity.getIntent().getIntExtra(PAGE, 0);
        LOG.d("_PAGE", "uri page", number);

        if (percent > 0) {
            number = (int) (pagesCount * percent);
        }

        LOG.d("getPageFromUri", "number by percent", percent, number);

        if (number > 0) {
            currentPage = number;
        } else {
            currentPage = generadDocInterface.getCurrentPage(getBookPath());
            currentPage = PageUrl.realToFake(currentPage);
        }

        LOG.d("_PAGE", "LOAD currentPage", currentPage, getBookPath());
        return currentPage;
    }

    public void saveCurrentPage() {
        int page = PageUrl.fakeToReal(currentPage);
        int pages = pagesCount;
        LOG.d("_PAGE", "saveCurrentPage", page, pages);
        generadDocInterface.setCurrentPage(getBookPath(), page, pages);

        BookSettings bs = SettingsManager.getBookSettings();
        if (bs != null) {
            bs.updateFromAppState();
        }
        SettingsManager.storeBookSettings();

    }

    public int getOpenPageNumber() {
        return currentPage;
    }

    @Override
    public PageUrl getPageUrl(int page) {
        return PageUrl.build(getBookPath(), page, imageWidth, imageHeight);
    }

    public abstract void onGoToPageImpl(int page);

    public abstract void notifyAdapterDataChanged();

    @Override
    public void onGoToPage(int page) {
        if (page <= getPageCount()) {
            onGoToPageImpl(page - 1);
        }
    }

    @Override
    public void onSrollLeft() {
        throw new RuntimeException("Not Implemented");
    }

    public TextWord[][] getPageText(int page) {
        LOG.d("Get page text for page", page);
        return generadDocInterface.getPageText(getBookPath(), page);
    }

    @Override
    public String getTextForPage(int page) {
        String pageHTML = generadDocInterface.getPageHTML(getBookPath(), page);
        pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);
        return pageHTML;
    }

    @Override
    public List<PageLink> getLinksForPage(int page) {
        return generadDocInterface.getLinksForPage(getBookPath(), page);
    }

    @Override
    public void onSrollRight() {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void onNextPage(boolean animate) {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void onPrevPage(boolean animate) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void onNextScreen(boolean animate) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isCropCurrentBook() {
        return false;
    }

    @Override
    public void onPrevScreen(boolean animate) {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void onZoomInc() {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void onZoomInOut(int x, int y) {

    }

    @Override
    public String getFootNote(String text) {
        return generadDocInterface.getFooterNote(getBookPath(), text);
    }

    @Override
    public List<String> getMediaAttachments() {
        return generadDocInterface.getMediaAttachments(getBookPath());
    }

    @Override
    public void onScrollDown() {
    }

    @Override
    public void onScrollUp() {
    }

    @Override
    public void onZoomDec() {
        throw new RuntimeException("Not Implemented");

    }

    private volatile boolean isClosed = false;

    @Override
    public void onCloseActivity() {
        isClosed = true;
        try {
            if (!ExtUtils.isTextFomat(bookPath)) {
                matrixSP.edit().putString(bookPath.hashCode() + "", PageImageState.get().getMatrixAsString()).commit();
                LOG.d("MATRIX", "SAVE", bookPath.hashCode() + "", PageImageState.get().getMatrixAsString());
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        saveCurrentPage();
        LOG.d("_PAGE", "SAVE", getCurentPage());
        final Intent i = new Intent();
        i.putExtra("page", getCurentPage());
        activity.setResult(Activity.RESULT_OK, i);
        activity.finish();

        // generadDocInterface.recyleDoc(bookPath);
        TempHolder.get().clear();
    }

    @Override
    public void onNightMode() {
    }

    @Override
    public void onCrop() {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void onFullScreen() {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public int getCurentPage() {
        LOG.d("_PAGE", "getCurentPage", currentPage);
        return currentPage;
    }

    @Override
    public int getPageCount() {
        return PageUrl.realToFake(pagesCount);
    }

    @Override
    public void onScrollY(int value) {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void onAutoScroll() {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void clearSelectedText() {
        PageImageState.get().cleanSelectedWords();
        EventBus.getDefault().post(new InvalidateMessage());
    }

    @Override
    public void saveChanges(List<PointF> points, int color) {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void deleteAnnotation(long pageHander, int page, int index) {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void underlineText(int color, float width, AnnotationType type) {
        // TODO Auto-generated method stub

    }

    @Override
    public void getOutline(com.foobnix.android.utils.ResultResponse<List<OutlineLinkWrapper>> outline) {
        List<OutlineLinkWrapper> outlineRes = generadDocInterface.getOutline(getCurrentBook().getPath(), "");
        setOutline(outlineRes);
        if (outline != null) {
            outline.onResultRecive(outlineRes);
        }
    }

    @Override
    public void doSearch(final String text, final com.foobnix.android.utils.ResultResponse<Integer> result) {
        if (searchTask != null && searchTask.getStatus() != CopyAsyncTask.Status.FINISHED) {
            return;
        }

        searchTask = new CopyAsyncTask() {

            @Override
            protected Object doInBackground(Object... params) {
                try {
                    PageImageState.get().cleanSelectedWords();
                    String textLowCase = text.toLowerCase(Locale.US);
                    String bookPath = getBookPath();
                    int prev = -1;

                    boolean nextWorld = false;
                    String firstPart = "";
                    TextWord firstWord = null;

                    for (int i = 0; i < getPageCount(); i++) {
                        if (!TempHolder.isSeaching) {
                            result.onResultRecive(Integer.MAX_VALUE);
                            return null;
                        }

                        if (isClosed) {
                            TempHolder.isSeaching = false;
                            return null;
                        }
                        if (i > 1) {
                            result.onResultRecive(i * -1);
                        }

                        TextWord[][] pageText = generadDocInterface.getPageText(bookPath, i);
                        generadDocInterface.recylePage(bookPath, i);
                        if (pageText == null) {
                            continue;
                        }
                        int index = 0;
                        List<TextWord> find = new ArrayList<TextWord>();
                        for (TextWord[] line : pageText) {
                            find.clear();
                            index = 0;
                            for (TextWord word : line) {
                                if (AppState.get().selectingByLetters) {
                                    String it = String.valueOf(textLowCase.charAt(index));
                                    if (word.w.toLowerCase(Locale.US).equals(it)) {
                                        index++;
                                        find.add(word);
                                    } else {
                                        index = 0;
                                        find.clear();
                                    }

                                    if (index == text.length()) {
                                        index = 0;
                                        if (prev != i) {
                                            result.onResultRecive(i);
                                            prev = i;
                                        }
                                        for (TextWord t : find) {
                                            PageImageState.get().addWord(i, t);
                                        }
                                    }

                                } else if (word.w.toLowerCase(Locale.US).contains(textLowCase)) {
                                    if (prev != i) {
                                        result.onResultRecive(i);
                                        prev = i;
                                    }
                                    PageImageState.get().addWord(i, word);
                                } else if (word.w.length() >= 3 && word.w.endsWith("-")) {
                                    nextWorld = true;
                                    firstWord = word;
                                    firstPart = word.w.replace("-", "");
                                } else if (nextWorld && (firstPart + word.w.toLowerCase(Locale.US)).contains(text)) {
                                    PageImageState.get().addWord(i, firstWord);
                                    PageImageState.get().addWord(i, word);
                                    nextWorld = false;
                                    firstWord = null;
                                    firstPart = "";
                                    if (prev != i) {
                                        result.onResultRecive(i);
                                        prev = i;
                                    }

                                }
                            }
                        }

                    }
                    result.onResultRecive(-1);
                } catch (Exception e) {
                    result.onResultRecive(-1);
                }
                TempHolder.isSeaching = false;
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                EventBus.getDefault().post(new InvalidateMessage());
            };

        }.execute();

    }

    public String getBookPath() {
        return bookPath;
    }

    @Override
    public File getCurrentBook() {
        return new File(getBookPath());
    }

    @Override
    public String getTitle() {
        return getTitle(getBookPath());
    }

    public static String getTitle(String path) {
        if (ExtUtils.isTextFomat(path)) {
            return AppDB.get().getOrCreate(path).getTitle();
        }
        return new File(path).getName();
    }

    @Override
    public void alignDocument() {
        PageImageState.get().isAutoFit = true;
        EventBus.getDefault().post(new MessageAutoFit(getCurentPage()));
    }

    @Override
    public void centerHorizontal() {
        PageImageState.get().isAutoFit = true;
        EventBus.getDefault().post(new MessageCenterHorizontally(getCurentPage()));
    }

}