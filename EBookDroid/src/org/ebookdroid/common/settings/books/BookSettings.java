package org.ebookdroid.common.settings.books;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.events.CurrentPageListener;
import org.json.JSONException;
import org.json.JSONObject;

import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.TempHolder;

public class BookSettings implements CurrentPageListener {

    public String fileName;
    public PageIndex currentPage;
    public int zoom = 100;

    public boolean splitPages = false;
    public DocumentViewMode viewMode = DocumentViewMode.VERTICALL_SCROLL;
    public boolean cropPages = false;
    public boolean doublePages = false;
    public boolean doublePagesCover = false;
    public Boolean isLocked = null;

    public float offsetX;
    public float offsetY;

    public int speed = AppState.get().autoScrollSpeed;

    public boolean autoLevels;
    private int pages;
    public int pageDelta = 0;


    public BookSettings(final String fileName) {
        this.fileName = fileName;
        this.currentPage = PageIndex.FIRST;
        if (isLocked == null) {
            isLocked = ExtUtils.isTextFomat(fileName);
        }
    }

    public boolean isTextFormat() {
        return ExtUtils.isTextFomat(fileName);
    }

    public void updateFromAppState() {
        cropPages = AppState.get().isCrop;
        doublePages = AppState.get().isDouble;
        splitPages = AppState.get().isCut;
        doublePagesCover = AppState.get().isDoubleCoverAlone;
        isLocked = AppState.get().isLocked;
        pageDelta = TempHolder.get().pageDelta;
    }

    public void save() {
        SettingsManager.db.storeBookSettings(this);
        //

    }

    BookSettings(final JSONObject object) throws JSONException {
        this.fileName = object.getString("fileName");
        this.currentPage = new PageIndex(object.getJSONObject("currentPage"));
        this.zoom = object.optInt("zoom", zoom);
        this.offsetX = (float) object.optDouble("offsetX", offsetX);
        this.offsetY = (float) object.optDouble("offsetY", offsetY);
        this.autoLevels = object.optBoolean("autoLevels", autoLevels);
        this.cropPages = object.optBoolean("cropPages", cropPages);
        this.doublePages = object.optBoolean("doublePages", doublePages);
        this.doublePagesCover = object.optBoolean("doublePagesCover", doublePagesCover);
        this.isLocked = object.optBoolean("isLocked", true);
        this.splitPages = object.optBoolean("splitPages", splitPages);
        this.speed = object.optInt("speed", speed);
        this.pages = object.optInt("pages", pages);
        this.pageDelta = object.optInt("pageDelta", pageDelta);
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject obj = new JSONObject();
        obj.put("fileName", fileName);
        obj.put("currentPage", currentPage != null ? currentPage.toJSON() : null);
        obj.put("zoom", zoom);
        obj.put("offsetX", offsetX);
        obj.put("offsetY", offsetY);
        obj.put("autoLevels", autoLevels);
        obj.put("cropPages", cropPages);
        obj.put("doublePages", doublePages);
        obj.put("isLocked", isLocked);
        obj.put("doublePagesCover", doublePagesCover);
        obj.put("splitPages", splitPages);
        obj.put("speed", speed);
        obj.put("pages", pages);
        obj.put("pageDelta", pageDelta);
        return obj;
    }

    @Override
    public void currentPageChanged(final PageIndex newIndex, int pages) {
        this.currentPage = newIndex;
        this.pages = pages;
    }

    public void currentPageChanged(final int page) {
        this.currentPage = new PageIndex(page, page);
    }

    public PageIndex getCurrentPage() {
        return currentPage;
    }

    public float getZoom() {
        return zoom / 100.0f;
    }

    public void setZoom(final float zoom) {
        this.zoom = Math.round(zoom * 100);
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public static class Diff {

        private static final short D_SplitPages = 0x0001 << 1;
        private static final short D_PageAlign = 0x0001 << 2;
        private static final short D_AnimationType = 0x0001 << 3;
        private static final short D_CropPages = 0x0001 << 4;
        private static final short D_NightMode = 0x0001 << 5;
        private static final short D_AutoLevels = 0x0001 << 6;

        private static final short D_Effects = D_NightMode | D_AutoLevels;

        private short mask;
        private final boolean firstTime;

        public Diff(final BookSettings olds, final BookSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = (short) 0xFFFF;
            } else if (news != null) {
                if (olds.splitPages != news.splitPages) {
                    mask |= D_SplitPages;
                }
                if (olds.cropPages != news.cropPages) {
                    mask |= D_CropPages;
                }
                if (olds.autoLevels != news.autoLevels) {
                    mask |= D_AutoLevels;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isSplitPagesChanged() {
            return 0 != (mask & D_SplitPages);
        }

        public boolean isPageAlignChanged() {
            return 0 != (mask & D_PageAlign);
        }

        public boolean isAnimationTypeChanged() {
            return 0 != (mask & D_AnimationType);
        }

        public boolean isCropPagesChanged() {
            return 0 != (mask & D_CropPages);
        }

        public boolean isNightModeChanged() {
            return 0 != (mask & D_NightMode);
        }

        public boolean isAutoLevelsChanged() {
            return 0 != (mask & D_AutoLevels);
        }

        public boolean isEffectsChanged() {
            return 0 != (mask & (D_Effects));
        }
    }
}
