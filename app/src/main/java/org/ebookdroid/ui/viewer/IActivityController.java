package org.ebookdroid.ui.viewer;

import android.app.Activity;
import android.content.Context;

import com.foobnix.sys.VerticalModeController;

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.emdev.ui.actions.IActionController;

public interface IActivityController extends IActionController<VerticalViewActivity> {

    Context getContext();

    Activity getActivity();

    DecodeService getDecodeService();

    DocumentModel getDocumentModel();

    IView getView();

    IViewController getDocumentController();

    IActionController<?> getActionController();

    ZoomModel getZoomModel();


    ViewState jumpToPage(int viewIndex, float offsetX, float offsetY, boolean addToHistory);

    VerticalModeController getListener();

}
