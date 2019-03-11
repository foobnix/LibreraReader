package org.ebookdroid.common.settings.listeners;

import com.foobnix.model.AppBook;

public interface IBookSettingsChangeListener {

    void onBookSettingsChanged(AppBook oldSettings, AppBook newSettings, AppBook.Diff diff);

}
