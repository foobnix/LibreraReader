package org.ebookdroid.common.settings.books;

import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.events.CurrentPageListener;

import java.io.File;

public class BookSettings implements CurrentPageListener {

    public String path;
    public int z = 100;//z

    public boolean sp = false;//split pages
    public boolean cp = false; //crop pages
    public boolean dp = false; //double pages
    public boolean dc = false; //double pages cover
    public boolean l = false; //locked

    public float x; //offsetX
    public float y; //offsetY

    public int s = AppState.get().autoScrollSpeed; //speed

    public int n; //pages
    public int d = 0;//delta
    public float p; //percent

    public File getCacheFile(){
        //return new File(AppsConfig.SYNC_FOLDER, new File(path).getName()+"_"+ path.hashCode()+".json");
        return new File(AppsConfig.SYNC_FOLDER, "book-"+new File(path).getName()+".json");
    }

    public BookSettings(){

    }
    public BookSettings(final String path) {
        this.path = path;
    }

    public void updateFromAppState() {
        cp = AppState.get().isCrop;
        dp = AppState.get().isDouble;
        sp = AppState.get().isCut;
        dc = AppState.get().isDoubleCoverAlone;
        l = AppState.get().isLocked;
        d = TempHolder.get().pageDelta;
    }

    public void save() {
        SettingsManager.db.storeBookSettings(this);
    }

    public void currentPageChanged(final int page, int pages) {
        this.n = pages;
        this.p = (float) page / pages;
    }

    public void currentPageChanged(final int page) {
        this.p = (float) page / n;
    }

    public PageIndex getCurrentPage() {
        int p = Math.round(n * this.p);
        return new PageIndex(p, p);
    }

    public float getZoom() {
        return z / 100.0f;
    }

    public void setZoom(final float zoom) {
        this.z = Math.round(zoom * 100);
    }

    public void setN(int n) {
        this.n = n;
    }

    public static class Diff {

        private static final short D_SplitPages = 0x0001 << 1;
        private static final short D_AnimationType = 0x0001 << 3;
        private static final short D_CropPages = 0x0001 << 4;
        private static final short D_NightMode = 0x0001 << 5;

        private static final short D_Effects = D_NightMode;

        private short mask;
        private final boolean firstTime;

        public Diff(final BookSettings olds, final BookSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = (short) 0xFFFF;
            } else if (news != null) {
                if (olds.sp != news.sp) {
                    mask |= D_SplitPages;
                }
                if (olds.cp != news.cp) {
                    mask |= D_CropPages;
                }

            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isSplitPagesChanged() {
            return 0 != (mask & D_SplitPages);
        }

        public boolean isAnimationTypeChanged() {
            return 0 != (mask & D_AnimationType);
        }

        public boolean isCropPagesChanged() {
            return 0 != (mask & D_CropPages);
        }

        public boolean isEffectsChanged() {
            return 0 != (mask & (D_Effects));
        }
    }
}
