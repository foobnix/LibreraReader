package com.foobnix.pdf.search.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.PointF;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Intents;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MessageAutoFit;
import com.foobnix.pdf.search.activity.msg.MessageCenterHorizontally;
import com.foobnix.pdf.search.activity.msg.MessagePageXY;
import com.foobnix.pdf.search.activity.msg.MovePageAction;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.core.PageSearcher;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.MuPdfLinks;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

        isTextFormat = ExtUtils.isTextFomat(activity.getIntent());
        udpateImageSize(isTextFormat, w, h);

        matrixSP = activity.getSharedPreferences("matrix", Context.MODE_PRIVATE);

        PageImageState.get().cleanSelectedWords();
        PageImageState.get().pagesText.clear();

        AppSP.get().isSmartReflow = false;

        if (isTextFormat) {
            AppSP.get().isCrop = false;
            AppSP.get().isCut = false;
            AppSP.get().isLocked = true;
        }

        bookPath = getBookPathFromActivity(activity);
        AppSP.get().lastBookPath = bookPath;

        AppBook bs = SettingsManager.getBookSettings(bookPath);

        if (bs != null) {
            AppSP.get().isCut = bs.sp;
            AppSP.get().isCrop = bs.cp;
            AppSP.get().isDouble = bs.dp;
            AppSP.get().isDoubleCoverAlone = bs.dc;
            AppSP.get().isLocked = bs.getLock(isTextFormat);
            TempHolder.get().pageDelta = bs.d;

            if (AppState.get().isCropPDF && !isTextFormat) {
                AppSP.get().isCrop = true;
            }
        }
        if(AppState.get().alwaysTwoPages){
            AppSP.get().isDouble = true;
            AppSP.get().isCut = false;
            AppSP.get().isDoubleCoverAlone = false;
            AppSP.get().isSmartReflow = false;
        }

        FileMetaCore.checkOrCreateMetaInfo(activity);
        BookCSS.get().detectLang(bookPath);


        String pasw = activity.getIntent().getStringExtra(EXTRA_PASSWORD);
        pasw = TxtUtils.nullToEmpty(pasw);

        if (AppSP.get().isDouble && isTextFormat) {
            imageWidth = Dips.screenWidth() / 2;
        }

        codeDocument = ImageExtractor.getNewCodecContext(bookPath, pasw, imageWidth, imageHeight);
        if (codeDocument != null) {
            pagesCount = codeDocument.getPageCount(imageWidth, imageHeight, BookCSS.get().fontSizeSp);
        } else {
            pagesCount = 0;
        }

        try {
            if (pagesCount > 0) {
                FileMeta meta = AppDB.get().load(bookPath);
                if (meta != null) {
                    meta.setPages(pagesCount);
                    AppDB.get().update(meta);
                    LOG.d("update openDocument.getPageCount()", bookPath, pagesCount);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        if (pagesCount == -1) {
            throw new IllegalArgumentException("Pages count = -1");
        }


        AppDB.get().addRecent(bookPath);

        float percent = Intents.getFloatAndClear(activity.getIntent(), DocumentController.EXTRA_PERCENT);


        if (percent > 0.0f) {
            currentPage = Math.round(pagesCount * percent) - 1;
        } else if (pagesCount > 0) {
            currentPage = bs.getCurrentPage(getPageCount()).viewIndex;
        }

        if (false) {
            PageImageState.get().needAutoFit = true;
        } else {
            if (TxtUtils.isNotEmpty(bookPath) && !ExtUtils.isTextFomat(bookPath)) {
                String string = matrixSP.getString(bookPath.hashCode() + "", "");
                LOG.d("MATRIX", "READ STR", string);
                if (TxtUtils.isEmpty(string) || AppSP.get().isCut || AppSP.get().isCrop) {
                    PageImageState.get().needAutoFit = true;
                } else {
                    PageImageState.get().needAutoFit = false;
                }
                Matrix matrix = PageImageState.fromString(string);
                PageImageState.get().getMatrix().set(matrix);

                LOG.d("MATRIX", "READ", bookPath.hashCode() + "", PageImageState.get().getMatrixAsString());

            }
        }


    }

    @Override
    public void updateRendering() {

    }

    @Override
    public void onScrollYPercent(float value) {
        int page2 = Math.round(value * getPageCount());
        onGoToPage(page2);
    }

    public void udpateImageSize(boolean isTextFormat, int w, int h) {
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
        return a.getIntent().getData().getPath();
    }

    public static String getTempTitle(Activity a) {
        try {
            return getTitle(getBookPathFromActivity(a));
        } catch (Exception e) {
            LOG.e(e);
            return "";
        }
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
    public synchronized String getTextForPage(int page) {
        try {
            CodecPage codecPage = codeDocument.getPage(page);
            if (!codecPage.isRecycled()) {
                String pageHTML = codecPage.getPageHTML();
                codecPage.recycle();
                pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);
                pageHTML = pageHTML.replace(TxtUtils.TTS_PAUSE, " ");
                pageHTML = pageHTML.replace(TxtUtils.NON_BREAKE_SPACE, " ");
                return pageHTML;

            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    @Override
    public String getPageHtml() {
        try {
            CodecPage codecPage = codeDocument.getPage(getCurentPageFirst1() - 1);
            if (!codecPage.isRecycled()) {
                String pageHTML = codecPage.getPageHTML();
                pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);
                pageHTML = pageHTML.replace(TxtUtils.TTS_PAUSE, TxtUtils.TTS_PAUSE_VIEW);

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

                //saveCurrentPage();
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
        EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
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
            new Thread("@T getOutlineH") {
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

                }

                ;
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
                    int firstWordIndex = 0;

                    PageSearcher pageSearcher = new PageSearcher();
                    pageSearcher.setTextForSearch(text);
                    pageSearcher.setListener(new PageSearcher.OnWordSearched() {
                        @Override
                        public void onSearch(TextWord word, Object data) {
                            if (!(data instanceof Integer))
                                return;
                            Integer pageNumber = (Integer) data;
                            LOG.d("Find on page_", pageNumber, text, word);
                            List<TextWord> selectedWords = PageImageState.get().getSelectedWords(pageNumber);
                            if (selectedWords == null || selectedWords.size() <= 0) {
                                result.onResultRecive(pageNumber);
                                LOG.d("Find on page", pageNumber, text);
                            }
                            if (selectedWords == null || !selectedWords.contains(word)) {
                                PageImageState.get().addWord(pageNumber, word);
                            }
                        }
                    });

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
                                    firstWordIndex = i;
                                    firstPart = word.w.replace("-", "");
                                } else if (nextWorld && (firstPart + word.w.toLowerCase(Locale.US)).contains(text)) {
                                    LOG.d("Contains 2", firstPart, word.w, text);
                                    PageImageState.get().addWord(firstWordIndex, firstWord);
                                    PageImageState.get().addWord(i, word);
                                    nextWorld = false;
                                    firstWord = null;
                                    firstPart = "";
                                    if (prev != firstWordIndex) {
                                        result.onResultRecive(firstWordIndex);
                                        prev = firstWordIndex;
                                    }
                                    if (prev != i) {
                                        result.onResultRecive(i);
                                        prev = i;
                                    }

                                } else if (nextWorld && TxtUtils.isNotEmpty(word.w)) {
                                    nextWorld = false;
                                    firstWord = null;
                                }
                                pageSearcher.addWord(new PageSearcher.WordData(word, i));
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
            }

            ;

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