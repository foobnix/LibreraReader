package org.ebookdroid.ui.viewer;

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;

import android.app.Activity;
import android.content.Context;

import org.emdev.ui.actions.IActionController;

import com.foobnix.sys.VerticalModeController;

public interface IActivityController extends IActionController<VerticalViewActivity> {

    Context getContext();

    Activity getActivity();

    DecodeService getDecodeService();

    DocumentModel getDocumentModel();

    IView getView();

    IViewController getDocumentController();

    IActionController<?> getActionController();

    ZoomModel getZoomModel();

    DecodingProgressModel getDecodingProgressModel();

    ViewState jumpToPage(int viewIndex, float offsetX, float offsetY, boolean addToHistory);

    VerticalModeController getListener();

}
