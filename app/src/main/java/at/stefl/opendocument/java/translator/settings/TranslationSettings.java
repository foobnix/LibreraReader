package at.stefl.opendocument.java.translator.settings;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.opendocument.java.util.FileCache;

public class TranslationSettings {

	private FileCache cache;
	private ImageStoreMode imageStoreMode;

	private boolean splitPages;

	private Vector2i maxTableDimension;
	private int maxRowRepetition;

	private boolean backTranslateable;

	public TranslationSettings() {
		this.maxTableDimension = new Vector2i(Integer.MAX_VALUE);
		this.maxRowRepetition = -1;
	}

	public TranslationSettings(TranslationSettings settings) {
		this.cache = settings.cache;
		this.imageStoreMode = settings.imageStoreMode;
		this.splitPages = settings.splitPages;
		this.maxTableDimension = settings.maxTableDimension;
		this.maxRowRepetition = settings.maxRowRepetition;
		this.backTranslateable = settings.backTranslateable;
	}

	public FileCache getCache() {
		return cache;
	}

	public ImageStoreMode getImageStoreMode() {
		return imageStoreMode;
	}

	public boolean isSplitPages() {
		return splitPages;
	}

	public Vector2i getMaxTableDimension() {
		return maxTableDimension;
	}

	public int getMaxRowRepetition() {
		return maxRowRepetition;
	}

	public boolean hasMaxRowRepetition() {
		return maxRowRepetition != -1;
	}

	public boolean isBackTranslateable() {
		return backTranslateable;
	}

	public void setCache(FileCache cache) {
		this.cache = cache;
	}

	public void setImageStoreMode(ImageStoreMode imageStoreMode) {
		this.imageStoreMode = imageStoreMode;
	}

	public void setSplitPages(boolean splitPages) {
		this.splitPages = splitPages;
	}

	public void setMaxTableDimension(Vector2i maxTableDimension) {
		this.maxTableDimension = maxTableDimension;
	}

	public void setMaxRowRepetition(int maxRowRepetition) {
		this.maxRowRepetition = maxRowRepetition;
	}

	public void setBackTranslateable(boolean backTranslateable) {
		this.backTranslateable = backTranslateable;
	}

}