package com.foobnix.pdf.search.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.MuPdfLinks;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MessageAutoFit;
import com.foobnix.pdf.search.activity.msg.MessageCenterHorizontally;
import com.foobnix.pdf.search.activity.msg.MovePageAction;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.PointF;

public abstract class HorizontalModeController extends DocumentController {


    private int pagesCount;
    int currentPage;
    private CopyAsyncTask searchTask;
    private boolean isTextFormat = false;
    String bookPath;
    CodecDocument codeDocument;

    int imageWidth, imageHeight;
    private SharedPreferences matrixSP;

    public HorizontalModeController(Activity activity, int w, int h) {
        super(activity);
        matrixSP = activity.getSharedPreferences("matrix", Context.MODE_PRIVATE);

        LOG.d("DocumentControllerHorizontalView", "begin");
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
        LOG.d("udpateImageSize", w, h, isTextFormat);
        imageWidth = isTextFormat ? w : (int) (Math.min(Dips.screenWidth(), Dips.screenHeight()) * AppState.get().pageQuality);
        imageHeight = isTextFormat ? h : (int) (Math.max(Dips.screenWidth(), Dips.screenHeight()) * AppState.get().pageQuality);
    }

    @Override
    public int getBookHeight() {
        return imageHeight;
    }

    @Override
    public int getBookWidth() {
        return imageWidth;
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
        try {
        PageImageState.get().getMatrix().reset();
        matrixSP.edit().remove("" + bookPath.hashCode()).commit();
        } catch (Exception e) {
            LOG.e(e);
        }
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

        AppState.get().lastBookPath = bookPath;

        BookSettings bs = SettingsManager.getBookSettings(bookPath);
        if (bs != null) {
            AppState.get().isCut = bs.splitPages;
            AppState.get().isCrop = bs.cropPages;
            AppState.get().isDouble = bs.doublePages;
            AppState.get().isDoubleCoverAlone = bs.doublePagesCover;
            AppState.get().isLocked = bs.isLocked;
            TempHolder.get().pageDelta = bs.pageDelta;

            if (AppState.get().isCropPDF && !isTextFormat) {
                AppState.get().isCrop = true;
            }
        }

        BookCSS.get().detectLang(bookPath);

        if (false) {
            PageImageState.get().needAutoFit = true;
        } else {
            if (TxtUtils.isNotEmpty(bookPath) && !ExtUtils.isTextFomat(bookPath)) {
                String string = matrixSP.getString(bookPath.hashCode() + "", "");
                LOG.d("MATRIX", "READ STR", string);
                if (TxtUtils.isEmpty(string) || AppState.get().isCut || AppState.get().isCrop) {
                    PageImageState.get().needAutoFit = true;
                } else {
                    PageImageState.get().needAutoFit = false;
                }
                Matrix matrix = PageImageState.fromString(string);
                PageImageState.get().getMatrix().set(matrix);

                LOG.d("MATRIX", "READ", bookPath.hashCode() + "", PageImageState.get().getMatrixAsString());

            }
        }

        if (AppState.get().isDouble && isTextFormat) {
            imageWidth = Dips.screenWidth() / 2;
        }

        FileMetaCore.checkOrCreateMetaInfo(activity);
        LOG.d("pagesCount", "init", imageWidth, imageHeight);
        String pasw = activity.getIntent().getStringExtra(EXTRA_PASSWORD);
        pasw = TxtUtils.nullToEmpty(pasw);

        codeDocument = ImageExtractor.getNewCodecContext(getBookPath(), pasw, imageWidth, imageHeight);
        if (codeDocument != null) {
            pagesCount = codeDocument.getPageCount();
        } else {
            pagesCount = 0;
        }

        if (pagesCount == -1) {
            throw new IllegalArgumentException("Pages count = -1");
        }

        try {
            if (pagesCount > 0) {
                FileMeta meta = AppDB.get().load(bs.fileName);
                if (meta != null) {
                    meta.setPages(pagesCount);
                    AppDB.get().update(meta);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        // int charsCount = 0;
        // for (int i = 0; i <= pagesCount; i++) {
        // CodecPage page2 = codeDocument.getPage(i);
        // charsCount += page2.getCharCount();
        // page2.recycle();
        // }
        // LOG.d("total-chars", charsCount);

        AppDB.get().addRecent(bookPath);
        // getPageFromUri();

        // loadOutline(null);
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

    public int getPageFromUriSingleRun() {
        final double percent = activity.getIntent().getDoubleExtra(DocumentController.EXTRA_PERCENT, 0.0);
        int number = activity.getIntent().getIntExtra(EXTRA_PAGE, 0);
        LOG.d("_PAGE", "uri page", number);

        activity.getIntent().putExtra(EXTRA_PAGE, 0);
        activity.getIntent().putExtra(EXTRA_PERCENT, 0.0);

        if (percent > 0) {
            number = (int) (pagesCount * percent);
        }

        LOG.d("getPageFromUri", "number by percent", percent, number);

        if (number > 0) {
            currentPage = number;
        } else {
            currentPage = SettingsManager.getBookSettings(getBookPath()).getCurrentPage().viewIndex;
            // currentPage = PageUrl.realToFake(currentPage);
        }

        LOG.d("_PAGE", "LOAD currentPage", currentPage, getBookPath());
        return currentPage;
    }

    public void saveCurrentPage() {
        if (TempHolder.get().loadingCancelled) {
            LOG.d("Loading cancelled");
            return;
        }
        // int page = PageUrl.fakeToReal(currentPage);
        LOG.d("_PAGE", "saveCurrentPage", currentPage, pagesCount);
        try {
            BookSettings bs = SettingsManager.getBookSettings(getBookPath());
            bs.updateFromAppState();
            bs.currentPageChanged(new PageIndex(currentPage, currentPage), pagesCount);
            bs.save();
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public int getOpenPageNumber() {
        return currentPage;
    }

    @Override
    public PageUrl getPageUrl(int page) {
        PageUrl build = PageUrl.build(getBookPath(), page, imageWidth, imageHeight);
        build.setDoText(true);
        return build;
    }

    public abstract void onGoToPageImpl(int page);

    public abstract void notifyAdapterDataChanged();

    public abstract void showInterstialAndClose();

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

    public TextWord[][] getPageText(int number) {
        LOG.d("Get page text for page", number);
        try {
            CodecPage page = codeDocument.getPage(number);
            if (!page.isRecycled()) {
                TextWord[][] text = page.getText();
                return text;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;
    }

    @Override
    public String getTextForPage(int page) {
        try {
            CodecPage codecPage = codeDocument.getPage(page);
            if (!codecPage.isRecycled()) {
                String pageHTML = codecPage.getPageHTML();
                pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);
                return pageHTML;

            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";

    }

    @Override
    public List<PageLink> getLinksForPage(int page) {
        try {
            return codeDocument.getPage(page).getPageLinks();
        } catch (Exception e) {
            LOG.e(e);
            return Collections.EMPTY_LIST;
        }
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
        EventBus.getDefault().post(new MovePageAction(MovePageAction.ZOOM_PLUS, getCurentPage()));
    }

    @Override
    public void onZoomDec() {
        EventBus.getDefault().post(new MovePageAction(MovePageAction.ZOOM_MINUS, getCurentPage()));
    }

    @Override
    public void onZoomInOut(int x, int y) {

    }

    @Override
    public String getFootNote(String text) {
        try {
            return TxtUtils.getFooterNote(text, codeDocument.getFootNotes());
        } catch (Exception e) {
            LOG.e(e);
            return "";
        }
    }

    @Override
    public List<String> getMediaAttachments() {
        try {
            return codeDocument.getMediaAttachments();
        } catch (Exception e) {
            LOG.e(e);
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public void onScrollDown() {
    }

    @Override
    public void onScrollUp() {
    }

    private volatile boolean isClosed = false;

    @Override
    public void onCloseActivityFinal(final Runnable run) {

        stopTimer();
        TTSEngine.get().stop();
        TTSNotification.hideNotification();

        Safe.run(new Runnable() {

            @Override
            public void run() {
                isClosed = true;
                if (codeDocument != null) {
                    codeDocument.recycle();
                    codeDocument = null;
                }
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

                ImageExtractor.clearCodeDocument();
                if (run != null) {
                    run.run();
                }
            }
        });
    }

    @Override
    public void onCloseActivityAdnShowInterstial() {
        showInterstialAndClose();

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
        AppState.get().selectedText = null;
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
    public synchronized void getOutline(final com.foobnix.android.utils.ResultResponse<List<OutlineLinkWrapper>> outlineResonse, boolean forse) {
        if (codeDocument == null) {
            return;
        }
        if (outline == null) {
            outline = new ArrayList<OutlineLinkWrapper>();
            new Thread() {
                @Override
                public void run() {

                    try {
                        for (OutlineLink ol : codeDocument.getOutline()) {
                            if (TempHolder.get().loadingCancelled) {
                                return;
                            }
                            if (!codeDocument.isRecycled() && TxtUtils.isNotEmpty(ol.getTitle())) {
                                if (ol.getLink() != null && ol.getLink().startsWith("#") && !ol.getLink().startsWith("#0")) {
                                    outline.add(new OutlineLinkWrapper(ol.getTitle(), ol.getLink(), ol.getLevel(), ol.docHandle, ol.linkUri));
                                } else {
                                    int page = MuPdfLinks.getLinkPageWrapper(ol.docHandle, ol.linkUri) + 1;
                                    outline.add(new OutlineLinkWrapper(ol.getTitle(), "#" + page, ol.getLevel(), ol.docHandle, ol.linkUri));
                                }
                            }
                        }

                        // setOutline(outline);
                        if (outlineResonse != null) {
                            getActivity().runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    outlineResonse.onResultRecive(outline);
                                }
                            });

                        }
                    } catch (Exception e) {
                        LOG.e(e);
                    }

                };
            }.start();
        } else {
            outlineResonse.onResultRecive(outline);
        }

    }

    @Override
    public void recyclePage(int number) {
        if (codeDocument == null) {
            return;
        }
        try {
            CodecPage page = codeDocument.getPage(number);
            page.recycle();
        } catch (Exception e) {
            LOG.e(e);
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

                        TextWord[][] pageText = getPageText(i);
                        recyclePage(i);
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
                                    LOG.d("Contains 1", word.w);
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
                                    LOG.d("Contains 2", firstPart, word.w, text);
                                    PageImageState.get().addWord(i, firstWord);
                                    PageImageState.get().addWord(i, word);
                                    nextWorld = false;
                                    firstWord = null;
                                    firstPart = "";
                                    if (prev != i) {
                                        result.onResultRecive(i);
                                        prev = i;
                                    }

                                } else if (nextWorld && TxtUtils.isNotEmpty(word.w)) {
                                    nextWorld = false;
                                    firstWord = null;
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