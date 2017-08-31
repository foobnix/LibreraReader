package com.foobnix.sys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.GeneralDocInterface;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.ui2.AppDB;

import android.content.Context;
import android.net.Uri;

public class GeneralDocInterfaceImpl implements GeneralDocInterface {

    @Override
    public int getPageCount(String path, String pasw, int w, int h, int fontSize) {
        try {
            pasw = TxtUtils.nullToEmpty(pasw);
            TempHolder.get().clear();
            LOG.d("getPageCount", w, h);
            return ImageExtractor.getCodecContext(path, pasw, w, h).getPageCount();
        } catch (MuPdfPasswordException e) {
            throw e;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public List<PageLink> getLinksForPage(String path, int number) {
        try {
            return ImageExtractor.getCodecContext(path, "", 0, 0).getPage(number).getPageLinks();
        } catch (Exception e) {
            LOG.e(e);
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public String getPageHTML(String path, int number) {
        try {
            CodecPage page = ImageExtractor.getCodecContext(path, "", 0, 0).getPage(number);
            if (!page.isRecycled()) {
                return page.getPageHTML();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    @Override
    public TextWord[][] getPageText(String path, int number) {
        try {
            CodecPage page = ImageExtractor.getCodecContext(path, "", 0, 0).getPage(number);
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
    public void recylePage(String path, int number) {
        try {
            CodecPage page = ImageExtractor.getCodecContext(path, "", 0, 0).getPage(number);
            page.recycle();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    public void recyleDoc(String path) {
        try {
            ImageExtractor.getCodecContext(path, "", 0, 0).recycle();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    public int getCurrentPage(String path) {
        return SettingsManager.getBookSettings(path).getCurrentPage().viewIndex;
    }

    @Override
    public void setCurrentPage(String path, int pageNumber, int pages) {
        try {
            if (pageNumber > pages) {
                pageNumber = pages;
            }
            BookSettings bookSettings = SettingsManager.getBookSettings(path);
            PageIndex page = bookSettings.getCurrentPage();
            PageIndex current = new PageIndex(pageNumber, pageNumber);
            bookSettings.currentPageChanged(page, current, pages);

            SettingsManager.storeBookSettings();
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    @Override
    public void addToRecent(Context a, Uri path) {
        AppDB.get().addRecent(path.getPath());
        // AppSharedPreferences.get().addRecent(path);
    }

    @Override
    public List<OutlineLinkWrapper> getOutline(String path, String pwd) {
        List<OutlineLinkWrapper> outline = new ArrayList<OutlineLinkWrapper>();
        try {
            CodecDocument codecContext = ImageExtractor.getCodecContext(path, "", 0, 0);
            if (codecContext == null || codecContext.getOutline() == null) {
                return outline;
            }
            for (OutlineLink ol : codecContext.getOutline()) {
                outline.add(new OutlineLinkWrapper(ol.getTitle(), ol.getLink(), ol.getLevel()));
            }
            return outline;
        } catch (Exception e) {
            LOG.e(e);
            return outline;
        }
    }

    @Override
    public String getFooterNote(String path, String input) {
        try {
            return TxtUtils.getFooterNote(input, ImageExtractor.getCodecContext(path, "", 0, 0).getFootNotes());
        } catch (Exception e) {
            LOG.e(e);
            return "";
        }
    }

    @Override
    public List<String> getMediaAttachments(String path) {
        try {
            return ImageExtractor.getCodecContext(path, "", 0, 0).getMediaAttachments();
        } catch (Exception e) {
            LOG.e(e);
            return Collections.EMPTY_LIST;
        }
    }

}
