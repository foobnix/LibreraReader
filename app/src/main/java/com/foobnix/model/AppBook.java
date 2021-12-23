package com.foobnix.model;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MyMath;
import com.foobnix.android.utils.Objects;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.events.CurrentPageListener;

public class AppBook implements CurrentPageListener {
    public static int LOCK_NONE = 0;
    public static int LOCK_YES = 1;
    public static int LOCK_NOT = 2;


    @Objects.IgnoreCalculateHashCode
    public transient String path;

    public int z = 100;//z
    public boolean sp = false;//split pages
    public boolean cp = false; //crop pages
    public boolean dp = false; //double pages normal
    public boolean dc = false; //double pages cover
    public int lk = LOCK_NONE;
    public float x; //offsetX
    public float y; //offsetY
    public int s = AppState.get().autoScrollSpeed; //speed
    public int d = 0;//delta

    public float p; //percent
    public long t;//time
    public String ln;

    @Override
    public int hashCode() {
        final String s = "" + path + z + sp + cp + dp + dc + lk + (int) (x * 100) + (int) (y * 100) + this.s + d + p + ln;
        LOG.d("hashCode-appbook", s);
        return s.hashCode();
    }

    public AppBook() {
    }

    public AppBook(final String path) {
        this.path = path;
    }

    public void updateFromAppState() {
        cp = AppSP.get().isCrop;
        dp = AppSP.get().isDouble;
        sp = AppSP.get().isCut;
        dc = AppSP.get().isDoubleCoverAlone;
        d = TempHolder.get().pageDelta;
        s = AppState.get().autoScrollSpeed;
        setLock(AppSP.get().isLocked);
    }

    public void setLock(Boolean lock) {
        if (lock == null) {
            this.lk = LOCK_NONE;
        } else {
            this.lk = lock ? LOCK_YES : LOCK_NOT;
        }
    }

    public boolean getLock(boolean isTextFormat) {
        if (lk == LOCK_NONE) {
            return isTextFormat;
        }
        return lk == LOCK_YES;
    }


    public void currentPageChanged(int page, int pages) {
        if (page <= 0 || pages <= 0) {
            //if (LOG.isEnable) {
            //    throw new RuntimeException("Error!!! " + page + " : " + pages);
            //}
            LOG.d("currentPageChanged ERROR!!!", page + " : " + pages);
            return;
        }
        this.p = MyMath.percent(page, pages);
        LOG.d("currentPageChanged", page, pages, p);
        t = System.currentTimeMillis();
    }


    public PageIndex getCurrentPage(int pages) {
        if (pages <= 0) {
            throw new RuntimeException("Error!!! " + pages);
        }
        if (this.p > 2) {//old import support
            LOG.d("AppBook-getCurrentPage old", p, pages);

        }

        int p = Math.round(pages * this.p);
        if (p > 0) {
            p = p - 1;
        }
        LOG.d("AppBook-getCurrentPage", p, this.p, pages);

        return new PageIndex(p, p);
    }

    public float getZoom() {
        return z / 100.0f;
    }

    public void setZoom(final float zoom) {
        this.z = Math.round(zoom * 100);
    }


    public static class Diff {

        private static final short D_SplitPages = 0x0001 << 1;
        private static final short D_AnimationType = 0x0001 << 3;
        private static final short D_CropPages = 0x0001 << 4;
        private static final short D_NightMode = 0x0001 << 5;

        private static final short D_Effects = D_NightMode;

        private short mask;
        private final boolean firstTime;

        public Diff(final AppBook olds, final AppBook news) {
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
