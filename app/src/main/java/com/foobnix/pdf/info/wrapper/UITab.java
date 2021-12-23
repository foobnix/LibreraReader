package com.foobnix.pdf.info.wrapper;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.ui2.fragment.BookmarksFragment2;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.foobnix.ui2.fragment.CloudsFragment2;
import com.foobnix.ui2.fragment.FavoritesFragment2;
import com.foobnix.ui2.fragment.OpdsFragment2;
import com.foobnix.ui2.fragment.PrefFragment2;
import com.foobnix.ui2.fragment.RecentFragment2;
import com.foobnix.ui2.fragment.SearchFragment2;
import com.foobnix.ui2.fragment.UIFragment;

import java.util.ArrayList;
import java.util.List;

public enum UITab {

    SearchFragment(0, SearchFragment2.PAIR.first, SearchFragment2.PAIR.second, SearchFragment2.class, true), //
    BrowseFragment(1, BrowseFragment2.PAIR.first, BrowseFragment2.PAIR.second, BrowseFragment2.class, true), //
    RecentFragment(2, RecentFragment2.PAIR.first, RecentFragment2.PAIR.second, RecentFragment2.class, true), //
    StarsFragment(3, FavoritesFragment2.PAIR.first, FavoritesFragment2.PAIR.second, FavoritesFragment2.class, true), //
    BookmarksFragment(4, BookmarksFragment2.PAIR.first, BookmarksFragment2.PAIR.second, BookmarksFragment2.class, true), //
    OpdsFragment(5, OpdsFragment2.PAIR.first, OpdsFragment2.PAIR.second, OpdsFragment2.class, true), //
    PrefFragment(6, PrefFragment2.PAIR.first, PrefFragment2.PAIR.second, PrefFragment2.class, true), //
    CloudsFragment(7, CloudsFragment2.PAIR.first, CloudsFragment2.PAIR.second, CloudsFragment2.class, true); //

    public int index;
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
        return SearchFragment;
    }

    public static List<UITab> getOrdered() {
        synchronized (AppState.get().tabsOrder7) {
            String input = AppState.get().tabsOrder7;
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
    }

    public static int getCurrentTabIndex(UITab tab) {
        List<UITab> ordered = getOrdered();
        int count = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).isVisible) {
                count++;
            }
            if (ordered.get(i) == tab) {
                return count;
            }
        }
        return 0;

    }

    public static boolean isShowRecent() {
        synchronized (AppState.get().tabsOrder7) {
            return AppState.get().tabsOrder7.contains(UITab.RecentFragment.index + "#1");
        }
    }

    public static boolean isShowPreferences() {
        synchronized (AppState.get().tabsOrder7) {
            return AppState.get().tabsOrder7.contains(UITab.PrefFragment.index + "#1");
        }
    }

    public static boolean isShowCloudsPreferences() {
        synchronized (AppState.get().tabsOrder7) {
            return AppState.get().tabsOrder7.contains(UITab.CloudsFragment.index + "#1");
        }
    }

    public boolean isVisible() {
        if (AppsConfig.IS_FDROID && (this == UITab.OpdsFragment || this == UITab.CloudsFragment)) {
            return false;
        }
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

}
