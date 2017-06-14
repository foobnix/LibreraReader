package com.foobnix.pdf.search.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.SettingsManager;
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
import com.foobnix.pdf.info.TTSModule;
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
    private static final String PERCENT_EXTRA = "percent";

    private GeneralDocInterface generadDocInterface;
    private int pagesCount;
    int currentPage;
    private CopyAsyncTask searchTask;
    private boolean isTextFormat = false;
    String bookPath;

    int imageWidth, imageHeight;
    private SharedPreferences matrixSP;

    public DocumentControllerHorizontalView(Activity activity) {
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
        imageWidth = isTextFormat ? Dips.screenWidth() : (int) (Dips.screenWidth() * AppState.get().pageQuality);
        imageHeight = isTextFormat ? Dips.screenHeight() : (int) (Dips.screenHeight() * AppState.get().pageQuality);

        if (isTextFormat) {
            AppState.get().isCrop = false;
            AppState.get().isCut = false;
            AppState.get().isLocked = true;
        }
        TTSModule.getInstanceInit(activity, this);
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

    public void init(final Activity activity) {
        PageImageState.get().cleanSelectedWords();
        PageImageState.get().pagesText.clear();

        LOG.d("DocumentControllerHorizontalView", "init begin");

        bookPath = CacheManager.getFilePathFromAttachmentIfNeed(activity);
        if (TxtUtils.isEmpty(bookPath)) {
            bookPath = activity.getIntent().getData().getPath();
        }

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

            AppState.get().isCut = SettingsManager.getBookSettings(bookPath).splitPages;
            AppState.get().isCrop = SettingsManager.getBookSettings(bookPath).cropPages;
        }

        FileMetaCore.checkOrCreateMetaInfo(activity);

        pagesCount = generadDocInterface.getPageCount(getBookPath(), activity.getIntent().getStringExtra(PASSWORD_EXTRA), imageWidth, imageHeight, AppState.get().fontSizeSp);
        generadDocInterface.addToRecent(activity, activity.getIntent().getData());
        getPageFromUri();

        loadOutline();
    }

    public int getCurrentPage() {
        return currentPage;
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

        if (number > 0) {
            currentPage = number;
        } else {
            currentPage = generadDocInterface.getCurrentPage(getBookPath());
        }
        LOG.d("_PAGE", "LOAD currentPage", currentPage, getBookPath());
        return currentPage;
    }

    public void saveCurrentPage() {
        LOG.d("_PAGE", "Save current page", currentPage);
        generadDocInterface.setCurrentPage(getBookPath(), currentPage, pagesCount);

    }

    public int getOpenPageNumber() {
        return currentPage;
    }

    public String getCurrentPagePath() {
        return getPagePath(currentPage);
    }

    @Override
    public String getPagePath(int page) {
        PageUrl url = new PageUrl();
        url.setPath(getBookPath());
        url.setPage(AppState.get().isCut ? page / 2 : page);
        url.setWidth(AppState.get().isCut ? (int) (imageWidth * 1.5) : imageWidth);
        url.setHeight(AppState.get().isCut ? (int) (imageHeight * 1.5) : imageHeight);
        url.setInvert(!AppState.get().isInvert);
        url.setCrop(AppState.get().isCrop);
        url.setRotate(AppState.get().rotate);
        url.setCutp(AppState.get().cutP);

        if (AppState.get().isCut) {
            if (AppState.get().isCutRTL) {
                url.setNumber(page % 2 == 0 ? 2 : 1);
            } else {
                url.setNumber(page % 2 == 0 ? 1 : 2);
            }
        }

        return url.toString();
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
        pageHTML = TTSModule.replaceHTML(pageHTML);
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
        if (!ExtUtils.isTextFomat(bookPath)) {
            matrixSP.edit().putString(bookPath.hashCode() + "", PageImageState.get().getMatrixAsString()).commit();
            LOG.d("MATRIX", "SAVE", bookPath.hashCode() + "", PageImageState.get().getMatrixAsString());
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
        if (AppState.get().isCut) {
            return pagesCount * 2;
        }
        return pagesCount;
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
        if (AppState.get().isCut && outlineRes != null) {
            for (OutlineLinkWrapper item : outlineRes) {
                if (item != null) {
                    item.targetPage = item.targetPage * 2 - 1;
                }
            }

        }
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
                    for (int i = 0; i < getPageCount(); i++) {
                        if (!TempHolder.isSeaching) {
                            result.onResultRecive(0);
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
        if (ExtUtils.isTextFomat(getBookPath())) {
            return AppDB.get().getOrCreate(getBookPath()).getTitle();
        }
        return getCurrentBook().getName();
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