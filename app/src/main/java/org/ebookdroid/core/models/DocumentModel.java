package org.ebookdroid.core.models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.Bitmaps;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.PageCacheFile;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;
import org.ebookdroid.core.DecodeServiceStub;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.utils.CompareUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.listeners.ListenerProxy;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;

public class DocumentModel extends ListenerProxy {

    public final DecodeService decodeService;

    protected PageIndex currentIndex = PageIndex.FIRST;

    private static final Page[] EMPTY_PAGES = {};

    private final CodecContext context;

    private Page[] pages = EMPTY_PAGES;

    public DocumentModel(final BookType activityType, IView view) {
        super(CurrentPageListener.class);
        LOG.d("Document activityType Type", activityType);
        if (activityType != null) {
            try {
                context = BookType.getCodecContextByType(activityType);
                LOG.d("Document context Type", context);
                decodeService = new DecodeServiceBase(context, view);
            } catch (final Throwable th) {
                throw new RuntimeException(th);
            }
        } else {
            context = null;
            decodeService = new DecodeServiceStub();
        }
    }

    public void open(String fileName, String password) {
        if (!ExtUtils.isValidFile(fileName)) {
            throw new IllegalArgumentException("Invalid file:" + fileName);
        }
        decodeService.open(fileName, password);
    }

    public Page[] getPages() {
        return pages;
    }

    public Iterable<Page> getPages(final int start) {
        return new PageIterator(start, pages.length);
    }

    public Iterable<Page> getPages(final int start, final int end) {
        return new PageIterator(start, Math.min(end, pages.length));
    }

    public int getPageCount() {
        return LengthUtils.length(pages);
    }

    public boolean recycle() {
        decodeService.recycle();
        recyclePages();
        return pages == EMPTY_PAGES;
    }

    public void recyclePages() {
        if (LengthUtils.isNotEmpty(pages)) {
            final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
            for (final Page page : pages) {
                page.recycle(bitmapsToRecycle);
            }
            BitmapManager.release(bitmapsToRecycle);
            BitmapManager.release();
        }
        pages = EMPTY_PAGES;
    }

    public Page getPageObject(final int viewIndex) {
        return pages != null && 0 <= viewIndex && viewIndex < pages.length ? pages[viewIndex] : null;
    }

    public Page getPageByDocIndex(final int docIndex) {
        for (Page page : pages) {
            if (page.index.docIndex == docIndex) {
                return page;
            }
        }
        return null;
    }

    /**
     * Gets the current page object.
     * 
     * @return the current page object
     */
    public Page getCurrentPageObject() {
        return getPageObject(this.currentIndex.viewIndex);
    }

    /**
     * Gets the last page object.
     * 
     * @return the last page object
     */
    public Page getLastPageObject() {
        return getPageObject(pages.length - 1);
    }

    public void setCurrentPageIndex(final PageIndex newIndex, int pages) {
        if (!CompareUtils.equals(currentIndex, newIndex)) {

            this.currentIndex = newIndex;

            this.<CurrentPageListener>getListener().currentPageChanged(newIndex, pages);
        }
    }

    public PageIndex getCurrentIndex() {
        return this.currentIndex;
    }

    public int getCurrentViewPageIndex() {
        return this.currentIndex.viewIndex;
    }

    public int getCurrentDocPageIndex() {
        // return this.currentIndex.docIndex;
        return this.currentIndex.viewIndex;
    }

    public double getPercentRead() {
        return (currentIndex.viewIndex + 0.0001) / getPageCount();
    }

    public void setCurrentPageByFirstVisible(final int firstVisiblePage, int pages) {
        final Page page = getPageObject(firstVisiblePage);
        if (page != null) {
            setCurrentPageIndex(page.index, pages);
        }
    }

    public void initPages(final IActivityController base, final IProgressIndicator task) {
        recyclePages();

        final BookSettings bs = SettingsManager.getBookSettings();

        if (base == null || bs == null || context == null || decodeService == null) {
            return;
        }

        final IView view = base.getView();

        final CodecPageInfo defCpi = new CodecPageInfo();
        defCpi.width = (view.getWidth());
        defCpi.height = (view.getHeight());

        LOG.d("initPages", defCpi.width, defCpi.height);

        int viewIndex = 0;

        try {
            final ArrayList<Page> list = new ArrayList<Page>();
            final CodecPageInfo[] infos = retrievePagesInfo(base, bs, task);

            for (int docIndex = 0; docIndex < infos.length; docIndex++) {
                if (TempHolder.get().loadingCancelled) {
                    return;
                }
                if (!AppState.get().isCut) {
                    CodecPageInfo cpi = infos[docIndex] != null ? infos[docIndex] : defCpi;

                    final Page page = new Page(base, new PageIndex(docIndex, viewIndex++), PageType.FULL_PAGE, cpi);
                    list.add(page);

                } else {
                    final Page page1 = new Page(base, new PageIndex(docIndex, viewIndex++), PageType.LEFT_PAGE, infos[docIndex]);
                    final Page page2 = new Page(base, new PageIndex(docIndex, viewIndex++), PageType.RIGHT_PAGE, infos[docIndex]);

                    if (AppState.get().isCutRTL) {
                        list.add(page2);
                        list.add(page1);
                    } else {
                        list.add(page1);
                        list.add(page2);
                    }
                }
            }
            pages = list.toArray(new Page[list.size()]);
        } finally {
        }
    }

    private CodecPageInfo[] retrievePagesInfo(final IActivityController base, final BookSettings bs, final IProgressIndicator task) {
        int pagesCount = base.getDecodeService().getPageCount();
        final PageCacheFile pagesFile = CacheManager.getPageFile(bs.fileName, pagesCount);

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

        if (decodeService.isPageSizeCacheable() && pagesFile.exists()) {
            final CodecPageInfo[] infos = pagesFile.load();
            if (infos != null && infos.length == decodeService.getPageCount()) {
                return infos;
            }
        }

        final CodecPageInfo[] infos = new CodecPageInfo[decodeService.getPageCount()];
        final CodecPageInfo unified = decodeService.getUnifiedPageInfo();

        for (int i = 0; i < infos.length; i++) {
            if (TempHolder.get().loadingCancelled) {
                return null;
            }
            infos[i] = unified != null ? unified : decodeService.getPageInfo(i);
        }

        if (decodeService.isPageSizeCacheable()) {
            pagesFile.save(infos);
        }
        return infos;
    }

    private final class PageIterator implements Iterable<Page>, Iterator<Page> {

        private final int end;
        private int index;

        private PageIterator(final int start, final int end) {
            this.index = start;
            this.end = end;
        }

        @Override
        public boolean hasNext() {
            return 0 <= index && index < end;
        }

        @Override
        public Page next() {
            return hasNext() ? pages[index++] : null;
        }

        @Override
        public void remove() {
        }

        @Override
        public Iterator<Page> iterator() {
            return this;
        }
    }
}
