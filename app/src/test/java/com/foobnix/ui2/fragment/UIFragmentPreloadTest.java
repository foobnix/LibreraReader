package com.foobnix.ui2.fragment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import org.junit.Test;

public class UIFragmentPreloadTest {
    @Test public void onlyVisibleBookRowsBecomeCoverRequests() {
        FileMetaAdapter adapter = mock(FileMetaAdapter.class);
        FileMeta book = new FileMeta("content://fixture/book");
        when(adapter.getItemCount()).thenReturn(3);
        when(adapter.getItemViewType(0)).thenReturn(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_BOOKS);
        when(adapter.getItemViewType(1)).thenReturn(FileMetaAdapter.DISPLAY_TYPE_FILE);
        when(adapter.getItemViewType(2)).thenReturn(FileMetaAdapter.DISPLAY_TYPE_FILE);
        when(adapter.getItem(1)).thenReturn(book);
        when(adapter.getItem(2)).thenReturn(new FileMeta());
        assertTrue(UIFragment.coverPreloadItems(adapter, -1).isEmpty());
        assertTrue(UIFragment.coverPreloadItems(adapter, 0).isEmpty());
        assertEquals(book, UIFragment.coverPreloadItems(adapter, 1).get(0));
        assertTrue(UIFragment.coverPreloadItems(adapter, 2).isEmpty());
        assertTrue(UIFragment.coverPreloadItems(adapter, 3).isEmpty());
    }
}
