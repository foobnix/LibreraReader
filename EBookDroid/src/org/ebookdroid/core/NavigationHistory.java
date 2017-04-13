package org.ebookdroid.core;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.viewer.IActivityController;

import java.util.LinkedList;

public class NavigationHistory {

    private final IActivityController base;

    private final LinkedList<Position> history = new LinkedList<NavigationHistory.Position>();

    public NavigationHistory(final IActivityController base) {
        this.base = base;
    }

    public void update() {
        history.addFirst(new Position(SettingsManager.getBookSettings()));
    }

    public boolean goBack() {
        final Position position = history.isEmpty() ? null : history.removeFirst();
        if (position != null) {
            base.getDocumentController().goToPage(position.index.viewIndex, position.offsetX, position.offsetY);
            return true;
        }
        return false;
    }

    private static final class Position {

        final PageIndex index;

        final float offsetX;

        final float offsetY;

        Position(final BookSettings bs) {
            this.index = bs.currentPage;
            this.offsetX = SettingsManager.getBookSettings().offsetX;
            this.offsetY = SettingsManager.getBookSettings().offsetY;
        }
    }
}
