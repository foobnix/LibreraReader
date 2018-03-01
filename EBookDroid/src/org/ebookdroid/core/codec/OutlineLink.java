package org.ebookdroid.core.codec;

import com.foobnix.android.utils.LOG;

public class OutlineLink implements CharSequence {

    private String title;
    private int level;
    private final String link;
    public long docHandle;
    public String linkUri;

    public OutlineLink(final String title, final String link, final int level, long docHandle, String linkUri) {
        this.title = title;
        this.link = link;
        this.level = level;
        this.docHandle = docHandle;
        this.linkUri = linkUri;
        LOG.d("OutlineLink", title, link, level, docHandle, linkUri);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.CharSequence#charAt(int)
     */
    @Override
    public char charAt(final int index) {
        return title.charAt(index);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.CharSequence#length()
     */
    @Override
    public int length() {
        return title.length();
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        return title.subSequence(start, end);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public int getLevel() {
        return level;
    }

    public String getLink() {
        return link;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
