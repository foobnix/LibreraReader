package com.foobnix.sys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.MuPdfLinks;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.ebookdroid.ui.viewer.ViewerActivityController;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.text.InputType;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.Toast;

public class VerticalModeController extends DocumentController {

    private final ViewerActivityController ctr;

    private static final double ZOOM_VALUE = 0.05;

    Handler handler;

    public VerticalModeController(final Activity activity, final ViewerActivityController ctr) {
        super(activity);
        this.ctr = ctr;
        AppSettings.getInstance().fullScreen = AppState.get().isFullScreen;
        handler = new Handler();
        TempHolder.get().loadingCancelled = false;
        initHandler();
    }

    @Override
    public int getBookWidth() {
        return ctr.getView().getWidth();
    }

    @Override
    public int getBookHeight() {
        return ctr.getView().getHeight();
    }

    public void saveCurrentPage() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                saveSettings();
                LOG.d("PAGE SAVED");
            }
        }, 2000);
    }

    @Override
    public void cleanImageMatrix() {
    }

    @Override
    public void onGoToPage(int page) {
        ctr.getDocumentController().goToPage(page - 1);
        saveCurrentPage();
    }

    @Override
    public int getCurentPageFirst1() {
        return getCurentPage();
    }

    @Override
    public void onGoToPage(final int page, final float offsetX, final float offsetY) {
    }

    @Override
    public float getOffsetX() {
        return ctr.getView().getScrollX();
    }

    @Override
    public PageUrl getPageUrl(int page) {
        return PageUrl.build(getCurrentBook().getPath(), page, Dips.screenWidth(), Dips.screenHeight());
    }

    @Override
    public float getOffsetY() {
        return ctr.getView().getScrollY();
    }

    @Override
    public void onCrop() {
        try {
            // AppState.get().isCrop = !AppState.get().isCrop;
            ctr.toggleCrop(AppState.get().isCrop);
        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void clearSelectedText() {
        ctr.getDocumentController().clearSelectedText();
    }

    @Override
    public void onSrollLeft() {
        // ctr.getDocumentController().getView().startPageScroll(5, 0);
        ctr.getDocumentController().getView().scrollBy(5, 0);
    }

    @Override
    public void onSrollRight() {
        // ctr.getDocumentController().getView().startPageScroll(-5, 0);
        ctr.getDocumentController().getView().scrollBy(-5, 0);
    }

    @Override
    public void onScrollUp() {
        ctr.getDocumentController().getView().scrollBy(0, -1 * AppState.get().mouseWheelSpeed);

    }

    @Override
    public void onScrollDown() {
        ctr.getDocumentController().getView().scrollBy(0, AppState.get().mouseWheelSpeed);
    }

    @Override
    public void saveSettings() {
        ctr.onPause();
    }

    @Override
    public void onNextPage(final boolean animate) {
        final int page = ctr.getDocumentModel().getCurrentDocPageIndex() + 1;
        ctr.getDocumentController().goToPage(page, animate);
        saveCurrentPage();

    }

    @Override
    public void onPrevPage(final boolean animate) {
        final int page = ctr.getDocumentModel().getCurrentDocPageIndex() - 1;
        ctr.getDocumentController().goToPage(page, animate);
        saveCurrentPage();
    }

    @Override
    public void onClickTop() {
        final int page = ctr.getDocumentModel().getCurrentDocPageIndex();
        ctr.getDocumentController().goToPage(page, (float) 0.25, 0);
        saveCurrentPage();
    }

    @Override
    public void onNextScreen(boolean animate) {
        int nextScreenScrollBy = AppState.get().nextScreenScrollBy;
        LOG.d("nextScreenScrollBy", nextScreenScrollBy, "animate", animate);
        if (animate) {
            ctr.getDocumentController().getView().startPageScroll(0, 1 * nextScreenScrollBy * getScrollValue() / 100);
        } else {
            ctr.getDocumentController().getView().scrollBy(0, 1 * nextScreenScrollBy * getScrollValue() / 100);
        }
        saveCurrentPage();
    }

    @Override
    public void onPrevScreen(boolean animate) {
        int nextScreenScrollBy = AppState.get().nextScreenScrollBy;

        if (animate) {
            ctr.getDocumentController().getView().startPageScroll(0, -1 * nextScreenScrollBy * getScrollValue() / 100);
        } else {
            ctr.getDocumentController().getView().scrollBy(0, -1 * nextScreenScrollBy * getScrollValue() / 100);
        }
        saveCurrentPage();

    }

    @Override
    public void deleteAnnotation(long pageHanderl, final int page, final int index) {
        ctr.getDecodeService().deleteAnnotation(pageHanderl, page - 1, index, new ResultResponse<List<Annotation>>() {

            @Override
            public boolean onResultRecive(List<Annotation> arg0) {
                ctr.getDocumentModel().getPageObject(page - 1).annotations = arg0;
                ctr.getDocumentController().toggleRenderingEffects();
                return false;
            }
        });

    }

    @Override
    public boolean isCropCurrentBook() {
        BookSettings bookSettings = SettingsManager.getBookSettings();
        if (bookSettings == null) {
            return false;
        }
        return bookSettings.cropPages;
    }

    @Override
    public void saveChanges(final List<PointF> points, final int color) {
        if (SettingsManager.getBookSettings().cropPages) {
            onCrop();
            return;
        }

        final float zoom = ctr.getZoomModel().getZoom();
        int scrollX = ctr.getDocumentController().getView().getScrollX();
        int scrollY = ctr.getDocumentController().getView().getScrollY();

        final Map<Integer, List<PointF>> result = new HashMap<Integer, List<PointF>>();

        final int first = ctr.getDocumentController().getFirstVisiblePage();
        final int last = ctr.getDocumentController().getLastVisiblePage() + 1;
        LOG.d("first", first, "last", last);

        for (PointF p : points) {
            RectF tapRect = new RectF(p.x, p.y, p.x, p.y);
            tapRect.offset(scrollX, scrollY);

            Iterable<Page> pages = ctr.getDocumentModel().getPages(first, last);
            for (Page page : pages) {
                RectF pbounds = page.getBounds(zoom);
                float aspect = page.getAspectRatio();
                float k = page.cpi.width / pbounds.width();

                LOG.d("PAGE #", page.index.viewIndex);
                LOG.d("PAGE wxh", page.cpi.width, page.cpi.height);
                LOG.d("PAGE dpi", page.cpi.dpi, page.cpi.dpi);
                LOG.d("pbounds p", pbounds, "zoom", zoom, "aspect", aspect);
                LOG.d("pbounds p", pbounds, "zoom", zoom, "aspect", aspect);

                if (RectF.intersects(pbounds, tapRect)) {
                    int pNumber = page.index.docIndex;
                    List<PointF> list = result.get(pNumber);
                    if (list == null) {
                        list = new ArrayList<PointF>();
                    }

                    p.x += scrollX;
                    p.y += scrollY - pbounds.top;

                    p.x = p.x * k;
                    p.y = p.y * k;

                    list.add(p);
                    result.put(pNumber, list);
                }
            }

        }

        float width = AppState.get().editLineWidth * 3;
        float alpha = AppState.get().editAlphaColor;

        ctr.getDecodeService().addAnnotation(result, color, width, alpha, new ResultResponse<Pair<Integer, List<Annotation>>>() {

            @Override
            public boolean onResultRecive(Pair<Integer, List<Annotation>> p) {
                ctr.getDocumentModel().getPageObject(p.first).annotations = p.second;
                ctr.getDocumentController().toggleRenderingEffects();
                return false;
            }

        });

    }

    @Override
    public String getTextForPage(int page) {
        String pageHTML = ctr.getDecodeService().getPageHTML(page);
        pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);
        return pageHTML;
    }

    @Override
    public List<PageLink> getLinksForPage(int page) {
        return ctr.getDecodeService().getLinksForPage(page);
    }

    @Override
    public void underlineText(int color, float width, AnnotationType type) {
        if (ctr == null || ctr.getDocumentController() == null || ctr.getDocumentModel() == null) {
            LOG.d("Can't underlineText");
            return;
        }
        final int first = ctr.getDocumentController().getFirstVisiblePage();
        final int last = ctr.getDocumentController().getLastVisiblePage();
        for (int i = first; i < last + 1; i++) {
            final Page page = ctr.getDocumentModel().getPageByDocIndex(i);
            if (page == null || page.selectedText == null) {
                continue;
            }
            List<TextWord> texts = page.selectedText;

            final ArrayList<PointF> quadPoints = new ArrayList<PointF>();

            for (TextWord text : texts) {
                RectF rect = text.getOriginal();
                quadPoints.add(new PointF(rect.left, rect.bottom));
                quadPoints.add(new PointF(rect.right, rect.bottom));
                quadPoints.add(new PointF(rect.right, rect.top));
                quadPoints.add(new PointF(rect.left, rect.top));

            }

            PointF[] array = quadPoints.toArray(new PointF[quadPoints.size()]);
            ctr.getDecodeService().underlineText(i, array, color, type, new ResultResponse<List<Annotation>>() {

                @Override
                public boolean onResultRecive(List<Annotation> arg0) {
                    page.annotations = arg0;
                    page.selectedText = new ArrayList<TextWord>();
                    ctr.getDocumentController().toggleRenderingEffects();
                    return false;
                }
            });

        }
    }

    Thread t = new Thread();

    @Override
    public void onAutoScroll() {
        if (t.isAlive()) {
            return;
        }
        t = new Thread(new Runnable() {

            @Override
            public void run() {
                while (AppState.get().isAutoScroll) {
                    boolean repeat = false;
                    if (AppState.get().isLoopAutoplay && ctr.getDocumentController().getScrollLimits().bottom == ctr.getDocumentController().getView().getScrollY()) {
                        repeat = true;
                        try {
                            Thread.sleep(3000);
                            LOG.d("Sleep 3000");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    auto();

                    if (repeat) {
                        try {
                            Thread.sleep(3000);
                            LOG.d("Sleep 3000");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        float x = AppState.get().autoScrollSpeed;

                        if (x <= 50) {
                            x = x * 2;
                        } else {
                            final int x1 = 51;
                            final int y1 = 101;

                            final int x2 = 149;
                            final int y2 = 149;
                            // x = (x - y1) * (x2 - x1) / (y2 - y1) + x1;
                            x = (x - x1) * (y2 - y1) / (x2 - x1) + y1;

                        }
                        LOG.d("TEST", "Speed after" + x);

                        Thread.sleep(Math.max(AppState.MAX_SPEED - (int) x + 5, 5));
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
    }

    public void auto() {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (AppState.get().isLoopAutoplay && ctr.getDocumentController().getScrollLimits().bottom == ctr.getDocumentController().getView().getScrollY()) {
                    LOG.d("onScrollY 01");
                    onScrollY(0);
                } else {
                    ctr.getDocumentController().getView().scrollBy(0, 1);
                }
            }
        });
    }

    @Override
    public void onScrollY(final int value) {
        final int x = ctr.getDocumentController().getView().getScrollX();
        ctr.getDocumentController().getView().scrollTo(x, value);
        LOG.d("onScrollY", value);
    }

    public int getScrollValue() {
        int value = ctr.getDocumentController().getView().getHeight() - Dips.dpToPx(4);

        View titleBar = activity.findViewById(R.id.titleBar);
        if (titleBar.getVisibility() == View.VISIBLE) {
            value = value - titleBar.getHeight();
        }
        View progress = activity.findViewById(R.id.progressDraw);
        if (progress.getVisibility() == View.VISIBLE) {
            value = value - progress.getHeight();
        }

        return value;
    }

    @Override
    public void onZoomInc() {
        final float zoom = ctr.getZoomModel().getZoom();
        ctr.getZoomModel().setZoom((float) (zoom + ZOOM_VALUE), false);
        commit();

    }

    float currentZoom, currentX, currentY, pageN;
    boolean isLocked;

    @Override
    public void onZoomInOut(int x, int y) {
        if (currentZoom == 0) {

            final float zoom = ctr.getZoomModel().getZoom();
            currentZoom = zoom;
            currentX = ctr.getDocumentController().getView().getScrollX();
            currentY = ctr.getDocumentController().getView().getScrollY();
            pageN = ctr.getDocumentController().getFirstVisiblePage();
            int w = Dips.screenWidth() / 2;// center y
            int h = Dips.screenHeight() / 2;// center x
            ctr.getDocumentController().getView().scrollBy(x - w, y - h);
            ctr.getZoomModel().setZoom(zoom + 2f, false);
            commit();
            isLocked = AppState.get().isLocked;
            AppState.get().isLocked = false;
        } else {
            ctr.getZoomModel().setZoom(currentZoom, false);
            if (pageN == ctr.getDocumentController().getFirstVisiblePage()) {
                ctr.getDocumentController().getView().scrollTo((int) currentX, (int) currentY);
                commit();
            } else {
                alignDocument();
            }

            currentZoom = 0;
            AppState.get().isLocked = isLocked;
        }
    }

    @Override
    public void alignDocument() {
        int curentPage = ctr.getDocumentModel().getCurrentDocPageIndex();

        ctr.getZoomModel().initZoom(1);
        ctr.getZoomModel().commit();

        ctr.getDocumentController().goToPage(curentPage, 0, 0);
        ctr.getDocumentController().goToPage(curentPage);

        ctr.getDocumentController().toggleRenderingEffects();
    }

    @Override
    public void centerHorizontal() {
        final float zoom = ctr.getZoomModel().getZoom();
        int viewWidth = (int) ctr.getDocumentModel().getCurrentPageObject().getBounds(zoom).width();
        int currentY = ctr.getDocumentController().getView().getScrollY();

        int dx = (viewWidth - Dips.screenWidth()) / 2;
        ctr.getDocumentController().getView().scrollTo(dx, currentY);
        LOG.d("viewWidth", viewWidth, dx);
        // commit();
    }

    @Override
    public void updateRendering() {
        ctr.getDocumentController().toggleRenderingEffects();
    }

    @Override
    public void onZoomDec() {
        final float zoom = ctr.getZoomModel().getZoom();
        ctr.getZoomModel().setZoom((float) (zoom - ZOOM_VALUE), false);
        commit();

    }

    public void commit() {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                ctr.getZoomModel().commit();
            }
        }, 2000);
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    @Override
    public void saveAnnotationsToFile() {
        if (AppState.get().isSaveAnnotatationsAutomatically) {

            String path = getCurrentBook().getAbsolutePath();
            ctr.getDecodeService().saveAnnotations(path, new Runnable() {

                @Override
                public void run() {
                }
            });
        }
    }

    @Override
    public void onCloseActivityFinal(Runnable run) {
        stopTimer();
        ctr.closeActivityFinal(run);
    }

    @Override
    public void onCloseActivityAdnShowInterstial() {
        handler.removeCallbacksAndMessages(null);
        if (ctr == null || ctr.getDecodeService() == null) {
            return;
        }
        if (ctr.getDecodeService().hasAnnotationChanges()) {
            final StringBuilder path = new StringBuilder(getCurrentBook().getAbsolutePath());

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.save_changes);

            builder.setMessage(getCurrentBook().getAbsolutePath());
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    if (!path.toString().equals(getCurrentBook().getAbsolutePath())) {
                        LOG.d("Save TO new file", path.toString());
                        File newBook = new File(path.toString());
                        newBook.delete();
                        try {
                            copy(getCurrentBook(), newBook);

                        } catch (IOException e) {
                            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    }
                    final ProgressDialog progress = ProgressDialog.show(getActivity(), null, getActivity().getString(R.string.saving_));
                    progress.setCancelable(false);
                    progress.show();
                    ctr.getDecodeService().saveAnnotations(path.toString(), new Runnable() {

                        @Override
                        public void run() {
                            LOG.d("saveAnnotations return 1");
                            progress.dismiss();
                            LOG.d("saveAnnotations return 2");
                            ctr.closeActivity(null);
                        }
                    });
                };
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    ctr.closeActivity(null);
                }
            });
            builder.setNeutralButton(R.string.rename, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PrefDialogs.selectFileDialog(activity, Arrays.asList(".pdf"), getCurrentBook(), new ResultResponse<String>() {

                        @Override
                        public boolean onResultRecive(String result) {

                            path.setLength(0);
                            path.append(result);

                            builder.setMessage(result);
                            builder.show();
                            return false;
                        }
                    });

                }
            });
            builder.show();

        } else {
            ctr.closeActivity(null);
        }
    }

    @Override
    public void onNightMode() {
        if (getCurrentBook() == null) {
            return;
        }

        if (true) {
            AppState.get().isDayNotInvert = !AppState.get().isDayNotInvert;
            saveSettings();
            restartActivity();
            return;
        }
        if (ExtUtils.isTextFomat(getCurrentBook().getPath())) {
            saveSettings();
            restartActivity();
        } else {
            ctr.toggleNightMode();
        }

    }

    @Override
    public void restartActivity() {
        try {
            ctr.getDocumentModel().recyclePages();
        } catch (Exception e) {
            LOG.e(e);
        }
        super.restartActivity();
    }

    @Override
    public void onFullScreen() {
        AppSettings.getInstance().fullScreen = !AppSettings.getInstance().fullScreen;
        activity.finish();
        activity.startActivity(activity.getIntent());
    }

    @Override
    public int getCurentPage() {
        return ctr.getDocumentModel().getCurrentViewPageIndex() + 1;
    }

    @Override
    public int getPageCount() {
        return ctr.getDocumentModel().getPageCount();
    }

    @Override
    public void doSearch(String text, ResultResponse<Integer> result) {
        ctr.doSearch(text, result);
    }

    @Override
    public void toPageDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText number = new EditText(getActivity());

        number.setInputType(InputType.TYPE_CLASS_NUMBER);
        number.setText(String.valueOf(getCurentPage()));

        builder.setView(number);
        builder.setTitle(R.string.go_to_page_dialog);
        final DialogInterface.OnClickListener onOkListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                int page = 1;
                try {
                    page = Integer.valueOf(number.getText().toString());
                } catch (final NumberFormatException e) {
                    number.setText("1");
                }
                if (page >= 0 && page <= getPageCount()) {
                    onGoToPage(page);
                    dialog.dismiss();
                }
            }
        };
        builder.setPositiveButton(R.string.go, onOkListener);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                dialog.dismiss();
            }
        });
        final AlertDialog dialog = builder.show();

        number.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        onOkListener.onClick(dialog, 0);
                        return true;
                    default:
                        break;
                    }
                }
                return false;
            }
        });
    }

    List<OutlineLinkWrapper> outline;

    @Override
    public synchronized void getOutline(final ResultResponse<List<OutlineLinkWrapper>> resultWrapper, boolean forseRealod) {
        if (outline != null) {
            resultWrapper.onResultRecive(outline);
            return;
        }
        ctr.getDocumentModel().decodeService.getOutline(new ResultResponse<List<OutlineLink>>() {

            @Override
            public boolean onResultRecive(List<OutlineLink> outlineLinks) {
                outline = new ArrayList<OutlineLinkWrapper>();
                if (outlineLinks == null) {
                    return resultWrapper.onResultRecive(null);
                }

                for (OutlineLink ol : outlineLinks) {
                    if (TempHolder.get().loadingCancelled) {
                        return false;
                    }

                    try {
                        if (!ctr.getDocumentModel().decodeService.getCodecDocument().isRecycled() && TxtUtils.isNotEmpty(ol.getTitle())) {

                            if (ol.getLink() != null && ol.getLink().startsWith("#") && !ol.getLink().startsWith("#0")) {
                                outline.add(new OutlineLinkWrapper(ol.getTitle(), ol.getLink(), ol.getLevel(), ol.docHandle, ol.linkUri));
                            } else {
                                int page = MuPdfLinks.getLinkPageWrapper(ol.docHandle, ol.linkUri) + 1;
                                outline.add(new OutlineLinkWrapper(ol.getTitle(), "#" + page, ol.getLevel(), ol.docHandle, ol.linkUri));
                            }

                        }
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }
                resultWrapper.onResultRecive(outline);
                return true;
            }
        });
    }

    @Override
    public String getFootNote(String text) {
        return ctr.getDocumentModel().decodeService.getFooterNote(text);
    }

    @Override
    public List<String> getMediaAttachments() {
        try {
            return ctr.getDocumentModel().decodeService.getAttachemnts();
        } catch (Exception e) {
            return Collections.EMPTY_LIST;

        }
    }

    public void onDestroy() {
        AppState.get().isAutoScroll = false;
    }

}
