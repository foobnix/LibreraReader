package com.foobnix.pdf;

import android.content.Context;
import android.net.Uri;

import com.foobnix.pdf.info.model.OutlineLinkWrapper;

import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.List;

public interface GeneralDocInterface {

    public int getPageCount(String path, String pasw, int w, int h, int fontSize);

    public int getCurrentPage(String path);


    public void addToRecent(Context a, Uri path);

    public List<OutlineLinkWrapper> getOutline(String path, String pwd);

    public TextWord[][] getPageText(String path, int number);

    public String getPageHTML(String path, int number);

    public void recylePage(String path, int number);

    public List<PageLink> getLinksForPage(String path, int number);

    public String getFooterNote(String path, String input);

    public List<String> getMediaAttachments(String path);

    public void recyleDoc(String path);
}
