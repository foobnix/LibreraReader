package org.ebookdroid.common.settings.listeners;

import org.ebookdroid.common.settings.books.BookSettings;

public interface IBookSettingsChangeListener {

    void onBookSettingsChanged(BookSettings oldSettings, BookSettings newSettings, BookSettings.Diff diff);

}
