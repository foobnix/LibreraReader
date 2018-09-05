package org.ebookdroid.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageHolder;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason;
import org.emdev.utils.CompareUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.MathUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.Colors;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;

import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;

public class DecodeServiceBase implements DecodeService {

    static final AtomicLong TASK_ID_SEQ = new AtomicLong();

    final CodecContext codecContext;

    final ExecutorRunnable executor = new ExecutorRunnable();

    final AtomicBoolean isRecycled = new AtomicBoolean();

    final AtomicReference<ViewState> viewState = new AtomicReference<ViewState>();

    private CodecDocument codecDocument;

    private Map<Integer, CodecPageHolder> pages = new LinkedHashMap<Integer, CodecPageHolder>() {

        private static final long serialVersionUID = -8845124816503128098L;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<Integer, CodecPageHolder> eldest) {
            if (this.size() > getCacheSize()) {
                final CodecPageHolder value = eldest != null ? eldest.getValue() : null;
                if (value != null) {
                    if (value.isInvalid(-1)) {
                        return true;
                    } else {
                        boolean recycled = value.recycle(-1, false);
                        return recycled;
                    }
                }
            }
            return false;
        }

    };

    private IView view;

    public DecodeServiceBase(final CodecContext codecContext, IView view) {
        this.codecContext = codecContext;
        this.view = view;
    }

    @Override
    public int getPixelFormat() {
        final Config cfg = getBitmapConfig();
        switch (cfg) {
        case ALPHA_8:
            return PixelFormat.A_8;
        case ARGB_4444:
            return PixelFormat.RGBA_4444;
        case RGB_565:
            return PixelFormat.RGB_565;
        case ARGB_8888:
            return PixelFormat.RGBA_8888;
        default:
            return PixelFormat.RGB_565;
        }
    }

    @Override
    public boolean hasAnnotationChanges() {
        return codecDocument != null ? codecDocument.hasChanges() : false;
    }

    @Override
    public void saveAnnotations(final String path, final Runnable response) {

        executor.addAny(new Task(0) {

            @Override
            public void run() {
                LOG.d("saveAnnotations Begin");
                if (hasAnnotationChanges()) {
                    codecDocument.saveAnnotations(path);
                } else {
                    LOG.d("NO Annotations for save!!!");
                }
                LOG.d("saveAnnotations DONE");
                response.run();
            }
        });
    }

    @Override
    public Config getBitmapConfig() {
        return this.codecContext.getBitmapConfig();
    }

    @Override
    public void open(final String fileName, final String password) {
        ImageExtractor.clearCodeDocument();
        codecDocument = codecContext.openDocument(fileName, password);
        ImageExtractor.init(codecDocument, fileName);

    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        return codecDocument != null ? codecDocument.getUnifiedPageInfo() : null;
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex) {
        return codecDocument != null ? codecDocument.getPageInfo(pageIndex) : null;
    }

    @Override
    public void updateViewState(final ViewState viewState) {
        this.viewState.set(viewState);
    }

    @Override
    public void addAnnotation(Map<Integer, List<PointF>> points, int color, float width, float alpha, ResultResponse<Pair<Integer, List<Annotation>>> result) {
        final AddAnnotationTask anTask = new AddAnnotationTask(points, color, width, alpha, result);
        executor.addAny(anTask);
    }

    @Override
    public void decodePage(final ViewState viewState, final PageTreeNode node) {
        if (isRecycled.get()) {
            return;
        }

        final DecodeTask decodeTask = new DecodeTask(viewState, node);
        updateViewState(viewState);
        executor.add(decodeTask);
    }

    @Override
    public void stopDecoding(final PageTreeNode node, final String reason) {
        executor.stopDecoding(null, node, reason);
    }

    @Override
    public void updateAnnotation(int page, float[] color, PointF[][] points, float width, float alpha) {
        if (points != null) {
            getPage(page).addAnnotation(color, points, width, alpha);
        }

    }

    @Override
    public void deleteAnnotation(final long pageHandle, final int page, final int index, final ResultResponse<List<Annotation>> response) {
        executor.addAny(new Task(0) {

            @Override
            public void run() {
                codecDocument.deleteAnnotation(pageHandle, index);
                pages.clear();
                response.onResultRecive(getPage(page).getAnnotations());
            }
        });

    }

    @Override
    public void searchText(final String text, final Page[] pages, final ResultResponse<Integer> response, final Runnable finish) {
        Thread t = new Thread() {
            @Override
            public void run() {
                for (Page page : pages) {

                    if (!TempHolder.isSeaching) {
                        response.onResultRecive(Integer.MAX_VALUE);
                        finish.run();
                        return;
                    }

                    if (page.index.docIndex > 1) {
                        response.onResultRecive(page.index.docIndex * -1);
                    }

                    if (isRecycled.get()) {
                        TempHolder.isSeaching = false;
                        return;
                    }
                    if (page.texts == null) {
                        final CodecPage page2 = codecDocument.getPage(page.index.docIndex);
                        page.texts = page2.getText();
                        if (!page2.isRecycled()) {
                            executor.addAny(new Task(0) {

                                @Override
                                public void run() {
                                    page2.recycle();
                                }
                            });
                        }
                    }

                    page.selectedText = new ArrayList<TextWord>();
                    List<TextWord> findText = page.findText(text);
                    if (findText != null && !findText.isEmpty()) {
                        page.selectedText = findText;
                        response.onResultRecive(page.index.docIndex);
                        LOG.d("Find on page1", page.index.docIndex, text);
                    }
                }
                response.onResultRecive(-1);
                finish.run();
                TempHolder.isSeaching = false;
            };

        };
        t.start();
    }

    @Override
    public void underlineText(final int page, final PointF[] points, final int color, final AnnotationType type, final ResultResponse<List<Annotation>> callback) {
        executor.addAny(new Task(0) {

            @Override
            public void run() {
                getPage(page).addMarkupAnnotation(points, type, Colors.toMupdfColor(color));
                pages.clear();
                callback.onResultRecive(getPage(page).getAnnotations());
            }
        });

    }

    void performDecode(final DecodeTask task) {
        if (executor.isTaskDead(task)) {
            return;
        }

        CodecPageHolder holder = null;
        CodecPage vuPage = null;
        Rect r = null;
        RectF croppedPageBounds = null;

        // TempHolder.lock.lock();
        try {
            holder = getPageHolder(task.id, task.pageNumber);
            vuPage = holder.getPage(task.id);
            if (executor.isTaskDead(task)) {
                return;
            }

            croppedPageBounds = checkCropping(task, vuPage);
            if (executor.isTaskDead(task)) {
                return;
            }

            r = getScaledSize(task.node, task.viewState.zoom, croppedPageBounds, vuPage);

            final RectF actualSliceBounds = task.node.croppedBounds != null ? task.node.croppedBounds : task.node.pageSliceBounds;

            // TempHolder.lock.lock();
            final BitmapRef bitmap = vuPage.renderBitmap(r.width(), r.height(), actualSliceBounds);
            // TempHolder.lock.unlock();

            if (executor.isTaskDead(task)) {
                BitmapManager.release(bitmap);
                return;
            }
            // TempHolder.lock.lock();
            if (task.node.page.links == null) {
                task.node.page.links = vuPage.getPageLinks();
                if (LengthUtils.isNotEmpty(task.node.page.links)) {
                }
            }

            if (task.node.page.annotations == null) {
                task.node.page.annotations = vuPage.getAnnotations();
            }

            if (task.node.page.texts == null) {
                task.node.page.texts = vuPage.getText();
            }
            // TempHolder.lock.unlock();

            finishDecoding(task, vuPage, bitmap, r, croppedPageBounds);
            // test
            // vuPage.recycle();

        } catch (final OutOfMemoryError ex) {
            for (int i = 0; i <= AppSettings.getInstance().pagesInMemory; i++) {
                getPages().put(Integer.MAX_VALUE - i, null);
            }
            getPages().clear();
            if (vuPage != null) {
                vuPage.recycle();
            }

            BitmapManager.clear("DecodeService OutOfMemoryError: ");

            abortDecoding(task, null, null);
        } catch (final Throwable th) {
            th.printStackTrace();
            abortDecoding(task, vuPage, null);
        } finally {
            // TempHolder.lock.unlock();
            if (holder != null) {
                holder.unlock();
            }

        }
    }

    RectF checkCropping(final DecodeTask task, final CodecPage vuPage) {
        // Checks if cropping setting is not set
        if (task.viewState.book == null || !task.viewState.book.cropPages) {
            return null;
        }
        // Checks if page has been cropped before
        if (task.node.croppedBounds != null) {
            // Page size is actuall now
            return null;
        }

        RectF croppedPageBounds = null;

        // Checks if page root node has been cropped before
        final PageTreeNode root = task.node.page.nodes.root;
        if (root.croppedBounds == null) {
            final Rect rootRect = new Rect(0, 0, Math.min(PageCropper.MAX_WIDTH, vuPage.getWidth()), Math.min(PageCropper.MAX_HEIGHT, vuPage.getHeight()));
            // final Rect rootRect = new Rect(0, 0, rootRect.width(), rootRect.height());

            // TempHolder.lock.lock();
            LOG.d("DJVU1-1", rootRect.width(), rootRect.height());
            final BitmapRef rootBitmap = vuPage.renderBitmapSimple(rootRect.width(), rootRect.height(), new RectF(0, 0, 1f, 1f));
            // TempHolder.lock.unlock();

            LOG.d("DJVU1-1a", vuPage.getWidth(), vuPage.getHeight());
            LOG.d("DJVU1-1b", rootBitmap.getBitmap().getWidth(), rootBitmap.getBitmap().getHeight());

            root.croppedBounds = PageCropper.getCropBounds(rootBitmap.getBitmap(), rootRect, new RectF(0, 0, 1f, 1f));
            LOG.d("DJVU1-2", root.croppedBounds.width(), root.croppedBounds.height());

            BitmapManager.release(rootBitmap);

            final ViewState viewState = task.viewState;
            final float pageWidth = vuPage.getWidth() * root.croppedBounds.width();
            final float pageHeight = vuPage.getHeight() * root.croppedBounds.height();

            LOG.d("DJVU1-3", pageWidth, pageHeight);

            final PageIndex currentPage = viewState.book.getCurrentPage();
            final float offsetX = viewState.book.offsetX;
            final float offsetY = viewState.book.offsetY;

            root.page.setAspectRatio(pageWidth, pageHeight);
            viewState.ctrl.invalidatePageSizes(InvalidateSizeReason.PAGE_LOADED, task.node.page);

            croppedPageBounds = root.page.getBounds(task.viewState.zoom);

            task.node.page.base.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    viewState.ctrl.goToPage(currentPage.viewIndex, offsetX, offsetY);
                }
            });

        }

        if (task.node != root) {
            task.node.croppedBounds = PageTreeNode.evaluateCroppedPageSliceBounds(task.node.pageSliceBounds, task.node.parent);
        }

        return croppedPageBounds;
    }

    Rect getScaledSize(final PageTreeNode node, final float zoom, final RectF croppedPageBounds, final CodecPage vuPage) {
        final RectF pageBounds = MathUtils.zoom(croppedPageBounds != null ? croppedPageBounds : node.page.bounds, zoom);
        final RectF r = Page.getTargetRect(node.page.type, pageBounds, node.pageSliceBounds);
        return new Rect(0, 0, (int) r.width(), (int) r.height());
    }

    void finishDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final BitmapRef bitmap, final Rect bitmapBounds, final RectF croppedPageBounds) {
        stopDecoding(currentDecodeTask.node, "complete");
        updateImage(currentDecodeTask, page, bitmap, bitmapBounds, croppedPageBounds);
    }

    void abortDecoding(final DecodeTask currentDecodeTask, final CodecPage page, final BitmapRef bitmap) {
        stopDecoding(currentDecodeTask.node, "failed");
        updateImage(currentDecodeTask, page, bitmap, null, null);
    }

    CodecPage getPage(final int pageIndex) {
        return getPageHolder(-2, pageIndex).getPage(-2);
    }

    private synchronized CodecPageHolder getPageHolder(final long taskId, final int pageIndex) {
        for (final Iterator<Map.Entry<Integer, CodecPageHolder>> i = getPages().entrySet().iterator(); i.hasNext();) {
            final Map.Entry<Integer, CodecPageHolder> entry = i.next();
            final int index = entry.getKey();
            final CodecPageHolder ref = entry.getValue();
            if (ref.isInvalid(-1)) {
                i.remove();
            }
        }

        CodecPageHolder holder = getPages().get(pageIndex);
        if (holder == null) {
            holder = new CodecPageHolder(codecDocument, pageIndex);
            getPages().put(pageIndex, holder);
        }

        // Preventing problem inside the MuPDF
        if (!codecContext.isParallelPageAccessAvailable()) {
            // holder.getPage(taskId);
            // TODO TEST!!!
        }
        return holder;
    }

    void updateImage(final DecodeTask currentDecodeTask, final CodecPage page, final BitmapRef bitmap, final Rect bitmapBounds, final RectF croppedPageBounds) {
        currentDecodeTask.node.decodeComplete(page, bitmap, bitmapBounds, croppedPageBounds);
    }

    @Override
    public int getPageCount() {
        return codecDocument != null ? codecDocument.getPageCount(view.getWidth(), view.getHeight(), AppState.get().fontSizeSp) : 0;
    }

    @Override
    public void getOutline(final ResultResponse<List<OutlineLink>> response) {
        if (true) {

            new Thread() {
                @Override
                public void run() {
                    if (codecDocument == null) {
                        response.onResultRecive(null);
                        return;
                    }
                    response.onResultRecive(codecDocument.getOutline());
                }
            }.start();

            return;
        }
        executor.addAny(new Task(0) {

            @Override
            public void run() {
                if (codecDocument == null) {
                    response.onResultRecive(null);
                    return;
                }
                response.onResultRecive(codecDocument.getOutline());
            }
        });
    }

    @Override
    public void recycle() {
        // TempHolder.codecDocument = null;
        // TempHolder.path = null;
        if (isRecycled.compareAndSet(false, true)) {
            executor.recycle();
        }
    }

    protected int getCacheSize() {
        final ViewState vs = viewState.get();
        int minSize = 1;
        if (vs != null) {
            minSize = vs.pages.lastVisible - vs.pages.firstVisible + 1;
        }
        int pagesInMemory = AppSettings.getInstance().pagesInMemory;
        return pagesInMemory == 0 ? 1 : Math.max(minSize, pagesInMemory);
    }

    class ExecutorRunnable implements Runnable {

        final Map<PageTreeNode, DecodeTask> decodingTasks = new IdentityHashMap<PageTreeNode, DecodeTask>();

        final List<Task> tasks = new ArrayList<Task>();
        final AtomicBoolean run = new AtomicBoolean(true);

        ExecutorRunnable() {
            Thread t = new Thread(this);
            t.setPriority(AppSettings.getInstance().decodingThreadPriority);
            t.start();
        }

        @Override
        public void run() {
            try {
                while (run.get()) {
                    final Runnable r = nextTask();
                    if (r != null) {
                        BitmapManager.release();
                        r.run();
                    }
                }

            } catch (final Throwable th) {
                th.printStackTrace();
            } finally {
                BitmapManager.release();
            }
        }

        Runnable nextTask() {
            // TempHolder.lock.lock();
            try {
                if (!tasks.isEmpty()) {
                    final TaskComparator comp = new TaskComparator(viewState.get());
                    Task candidate = null;
                    int cindex = 0;

                    int index = 0;
                    while (index < tasks.size() && candidate == null) {
                        candidate = tasks.get(index);
                        cindex = index;
                        index++;
                    }
                    if (candidate == null) {
                        tasks.clear();
                    } else {
                        while (index < tasks.size()) {
                            final Task next = tasks.get(index);
                            if (next != null && comp.compare(next, candidate) < 0) {
                                candidate = next;
                                cindex = index;
                            }
                            index++;
                        }
                        tasks.set(cindex, null);
                    }
                    return candidate;
                }
            } catch (Exception e) {
                LOG.e(e);
            } finally {
                // TempHolder.lock.unlock();
            }
            synchronized (run) {
                try {
                    run.wait(60000);
                } catch (final InterruptedException ex) {
                    Thread.interrupted();
                }
            }
            return null;
        }

        public void addAny(final Task task) {

            // TempHolder.lock.lock();
            try {
                boolean added = false;
                for (int index = 0; index < tasks.size(); index++) {
                    if (null == tasks.get(index)) {
                        tasks.set(index, task);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    tasks.add(task);
                }

                synchronized (run) {
                    run.notifyAll();
                }
            } catch (Exception e) {
                LOG.e(e);
            } finally {
                // TempHolder.lock.unlock();
            }
        }

        public void add(final DecodeTask task) {

            // TempHolder.lock.lock();
            try {
                final DecodeTask running = decodingTasks.get(task.node);
                if (running != null && running.equals(task) && !isTaskDead(running)) {
                    return;
                }

                decodingTasks.put(task.node, task);

                boolean added = false;
                for (int index = 0; index < tasks.size(); index++) {
                    if (null == tasks.get(index)) {
                        tasks.set(index, task);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    tasks.add(task);
                }

                synchronized (run) {
                    run.notifyAll();
                }

                if (running != null) {
                    stopDecoding(running, null, "canceled by new one");
                }
            } catch (Exception e) {
                LOG.e(e);
            } finally {
                // TempHolder.lock.unlock();
            }
        }

        public void stopDecoding(final DecodeTask task, final PageTreeNode node, final String reason) {
            // TempHolder.lock.lock();
            try {
                final DecodeTask removed = task == null ? decodingTasks.remove(node) : task;

                if (removed != null) {
                    removed.cancelled.set(true);
                    for (int i = 0; i < tasks.size(); i++) {
                        if (removed == tasks.get(i)) {
                            tasks.set(i, null);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.e(e);
            } finally {
                // TempHolder.lock.unlock();
            }
        }

        public boolean isTaskDead(final DecodeTask task) {
            return task.cancelled.get();
        }

        public void recycle() {
            // TempHolder.lock.lock();
            try {
                for (final DecodeTask task : decodingTasks.values()) {
                    stopDecoding(task, null, "recycling");
                }

                // tasks.add(new ShutdownTask());
                shutdownInner();

                synchronized (run) {
                    run.notifyAll();
                }
            } catch (Exception e) {
                LOG.e(e);
            } finally {
                // TempHolder.lock.unlock();
            }
        }

        void shutdown() {
            Safe.run(new Runnable() {

                @Override
                public void run() {
                    shutdownInner();
                }
            });
        }

        private void shutdownInner() {

            LOG.d("Begin shutdown 1");

            for (final CodecPageHolder ref : getPages().values()) {
                ref.recycle(-3, true);
            }

            LOG.d("Begin shutdown 2");

            getPages().clear();

            LOG.d("Begin shutdown 3");
            if (getCodecDocument() != null) {
                getCodecDocument().recycle();
                codecDocument = null;
            }
            LOG.d("Begin shutdown 4");
            codecContext.recycle();
            LOG.d("Begin shutdown 5");
            run.set(false);
            LOG.d("Begin shutdown 6");

            ImageExtractor.clearCodeDocument();

        }

    }

    class TaskComparator implements Comparator<Task> {

        final PageTreeNodeComparator cmp;

        public TaskComparator(final ViewState viewState) {
            cmp = viewState != null ? new PageTreeNodeComparator(viewState) : null;
        }

        @Override
        public int compare(final Task r1, final Task r2) {
            if (r1.priority < r2.priority) {
                return -1;
            }
            if (r2.priority < r1.priority) {
                return +1;
            }

            if (r1 instanceof DecodeTask && r2 instanceof DecodeTask) {
                final DecodeTask t1 = (DecodeTask) r1;
                final DecodeTask t2 = (DecodeTask) r2;

                if (cmp != null) {
                    return cmp.compare(t1.node, t2.node);
                }
                return 0;
            }

            return CompareUtils.compare(r1.id, r2.id);
        }

    }

    abstract class Task implements Runnable {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final AtomicBoolean cancelled = new AtomicBoolean();
        final int priority;

        Task(final int priority) {
            this.priority = priority;
        }

    }

    class ShutdownTask extends Task {

        public ShutdownTask() {
            super(0);
        }

        @Override
        public void run() {
            executor.shutdown();
        }
    }

    class AddAnnotationTask extends Task {

        private Map<Integer, List<PointF>> points;
        private int color;
        private ResultResponse<Pair<Integer, List<Annotation>>> onResult;
        private float width;
        private float alpha;

        public AddAnnotationTask(Map<Integer, List<PointF>> points, int color, float width, float alpha, ResultResponse<Pair<Integer, List<Annotation>>> result) {
            super(1);
            this.points = points;
            this.width = width;
            this.alpha = alpha;
            this.onResult = result;
            this.color = color;

        }

        @Override
        public void run() {
            Set<Integer> keySet = points.keySet();
            for (int docIndex : keySet) {
                List<PointF> good = points.get(docIndex);
                if (good == null) {
                    continue;
                }
                PointF[][] path = new PointF[1][good.size()];
                path[0] = good.toArray(new PointF[good.size()]);

                updateAnnotation(docIndex, Colors.toMupdfColor(color), path, width, alpha);
                pages.clear();

                onResult.onResultRecive(new Pair<Integer, List<Annotation>>(docIndex, getPage(docIndex).getAnnotations()));
            }
        }
    }

    class DecodeTask extends Task {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final AtomicBoolean cancelled = new AtomicBoolean();

        final PageTreeNode node;
        final ViewState viewState;
        final int pageNumber;

        DecodeTask(final ViewState viewState, final PageTreeNode node) {
            super(2);
            this.pageNumber = node.page.index.docIndex;
            this.viewState = viewState;
            this.node = node;
        }

        @Override
        public void run() {
            performDecode(this);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DecodeTask) {
                final DecodeTask that = (DecodeTask) obj;
                return this.pageNumber == that.pageNumber && this.viewState.viewRect.width() == that.viewState.viewRect.width() && this.viewState.zoom == that.viewState.zoom;
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder("DecodeTask");
            buf.append("[");

            buf.append("id").append("=").append(id);
            buf.append(", ");
            buf.append("target").append("=").append(node);
            buf.append(", ");
            buf.append("width").append("=").append((int) viewState.viewRect.width());
            buf.append(", ");
            buf.append("zoom").append("=").append(viewState.zoom);

            buf.append("]");
            return buf.toString();
        }

    }

    @Override
    public boolean isPageSizeCacheable() {
        return codecContext.isPageSizeCacheable();
    }

    @Override
    public Map<Integer, CodecPageHolder> getPages() {
        return pages;
    }

    @Override
    public void processTextForPages(Page[] pages) {
        for (Page page : pages) {
            if (isRecycled.get()) {
                return;
            }
            if (page.texts == null) {
                CodecPage page2 = codecDocument.getPage(page.index.docIndex);
                page.texts = page2.getText();
                page2.recycle();
            }
        }
    }

    @Override
    public List<PageLink> getLinksForPage(int page) {
        if (codecDocument == null || codecDocument.getPage(page) == null) {
            return null;
        }
        return codecDocument.getPage(page).getPageLinks();
    }

    @Override
    public TextWord[][] getTextForPage(int page) {
        if (codecDocument == null || codecDocument.getPage(page) == null) {
            return null;
        }
        return codecDocument.getPage(page).getText();
    }

    @Override
    public String getPageHTML(int page) {
        if (codecDocument == null || codecDocument.getPage(page) == null) {
            return null;
        }
        return codecDocument.getPage(page).getPageHTML();
    }

    @Override
    public String getFooterNote(String input) {
        if (codecDocument == null || codecDocument.getFootNotes() == null) {
            return "";
        }
        return TxtUtils.getFooterNote(input, codecDocument.getFootNotes());
    }

    @Override
    public List<String> getAttachemnts() {
        return codecDocument.getMediaAttachments();
    }

    @Override
    public CodecDocument getCodecDocument() {
        return codecDocument;
    }

}
