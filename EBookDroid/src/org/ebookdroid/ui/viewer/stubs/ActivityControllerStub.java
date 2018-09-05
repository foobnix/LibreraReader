package org.ebookdroid.ui.viewer.stubs;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;
import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.IActionController;

import com.foobnix.sys.VerticalModeController;

import android.app.Activity;
import android.content.Context;

public class ActivityControllerStub extends ActionController<VerticalViewActivity> implements IActivityController {

    public static final ActivityControllerStub STUB = new ActivityControllerStub();

    public static final DocumentModel DM_STUB = new DocumentModel(null, null);

    public static final ZoomModel ZM_STUB = new ZoomModel();


    private ActivityControllerStub() {
        super(null, null);
    }

    @Override
    public Context getContext() {
        return LibreraApp.context;
    }

    @Override
    public Activity getActivity() {
        return null;
    }

    @Override
    public DecodeService getDecodeService() {
        return null;
    }

    @Override
    public DocumentModel getDocumentModel() {
        return DM_STUB;
    }

    @Override
    public IView getView() {
        return ViewStub.STUB;
    }

    @Override
    public IViewController getDocumentController() {
        return ViewContollerStub.STUB;
    }

    @Override
    public IActionController<?> getActionController() {
        return null;
    }

    @Override
    public ZoomModel getZoomModel() {
        return ZM_STUB;
    }

    @Override
    public DecodingProgressModel getDecodingProgressModel() {
        return null;
    }

    @Override
    public ViewState jumpToPage(final int viewIndex, final float offsetX, final float offsetY, boolean history) {
        return null;
    }

    @Override
    public VerticalModeController getListener() {
        return null;
    }

}
