package com.foobnix.pdf.info.wrapper;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.fragment.BookmarksFragment2;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.foobnix.ui2.fragment.PrefFragment2;
import com.foobnix.ui2.fragment.RecentFragment2;
import com.foobnix.ui2.fragment.SearchFragment2;
import com.foobnix.ui2.fragment.UIFragment;

public enum UITab {

    SearchFragment2(0, R.string.library, R.drawable.glyphicons_2_book_open, SearchFragment2.class, true), //
    BrowseFragment2(1, R.string.folders, R.drawable.glyphicons_145_folder_open, BrowseFragment2.class, true), //
    RecentFragment2(2, R.string.recent, R.drawable.glyphicons_72_book, RecentFragment2.class, true), //
    BookmarksFragment2(3, R.string.bookmarks, R.drawable.glyphicons_73_bookmark, BookmarksFragment2.class, true), //
    PrefFragment2(4, R.string.preferences, R.drawable.glyphicons_281_settings, PrefFragment2.class, true);//

    private int index;
    private int name;
    private int icon;
    private Class<? extends UIFragment> clazz;
    private boolean isVisible;

    private UITab(int index, int name, int icon, Class<? extends UIFragment> clazz, boolean isVisible) {
        this.index = index;
        this.name = name;
        this.icon = icon;
        this.clazz = clazz;
        this.isVisible = isVisible;
    }

    public int getIndex() {
        return index;
    }

    public int getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }

    public Class<? extends UIFragment> getClazz() {
        return clazz;
    }

    public static UITab getByIndex(int index) {
        for (UITab tab : values()) {
            if (tab.index == index) {
                return tab;
            }
        }
        return SearchFragment2;
    }

    public static List<UITab> getOrdered(String input) {
        LOG.d("getOrdered", input);
        List<UITab> list = new ArrayList<UITab>();
        for (String pair : input.split(",")) {
            String[] tab = pair.split("#");
            int id = Integer.valueOf(tab[0]);
            boolean isVisible = tab[1].equals("1");
            UITab byIndex = getByIndex(id);
            byIndex.setVisible(isVisible);
            list.add(byIndex);
        }
        return list;
    }

    public static int getCurrentTabIndex(UITab tab) {
        List<UITab> ordered = getOrdered(AppState.get().tabsOrder);
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i) == tab) {
                return i;
            }
        }
        return 0;
    }

    public static boolean isShowRecent() {
        return AppState.get().tabsOrder.contains("2#1");
    }

    public static boolean isShowPreferences() {
        return AppState.get().tabsOrder.contains("4#1");
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

}
