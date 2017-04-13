package org.ebookdroid.core.codec;

public class OutlineLink implements CharSequence {

    private String title;
    private int level;
	private final String link;

	public OutlineLink(final String title, final String link, final int level) {
		this.title = title;
		this.link = link;
		this.level = level;
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
