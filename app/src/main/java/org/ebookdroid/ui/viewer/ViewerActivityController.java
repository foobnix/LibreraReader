package org.ebookdroid.ui.viewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Intents;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.DocumentWrapperUI;
import com.foobnix.pdf.search.activity.HorizontalModeController;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.TempHolder;
import com.foobnix.sys.VerticalModeController;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.BookType;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.events.DecodingProgressListener;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.ebookdroid.ui.viewer.stubs.ActivityControllerStub;
import org.ebookdroid.ui.viewer.stubs.ViewContollerStub;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.EditableValue.PasswordEditable;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.tasks.BaseAsyncTask;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ViewerActivityController extends ActionController<VerticalViewActivity> implements IActivityController, DecodingProgressListener, CurrentPageListener, IBookSettingsChangeListener {


    private final AtomicReference<IViewController> ctrl = new AtomicReference<IViewController>(ViewContollerStub.STUB);

    private ZoomModel zoomModel;


    private DocumentModel documentModel;

    private BookType codecType;

    private final Intent intent;

    private int loadingCount = 0;

    private String m_fileName;
    private String title;

    private DocumentWrapperUI wrapperControlls;

    private VerticalModeController controller;

    VerticalViewActivity viewerActivity;

    /**
     * Instantiates a new base viewer activity.
     */
    public ViewerActivityController(final VerticalViewActivity activity) {
        super(activity);
        this.viewerActivity = activity;
        this.intent = activity.getIntent();
        SettingsManager.addListener(this);

        controller = new VerticalModeController(activity, this);
        wrapperControlls = new DocumentWrapperUI(controller);
        LOG.d("ViewerActivityController create");
    }

    @Override
    public VerticalModeController getListener() {
        return controller;
    }

    public void beforeCreate(final VerticalViewActivity activity) {
        if (getManagedComponent() != activity) {
            setManagedComponent(activity);
        }
    }

    Runnable onBookLoaded;

    public void onBookLoaded(Runnable onBookLoaded) {
        this.onBookLoaded = onBookLoaded;
    }

    public void afterCreate(VerticalViewActivity a) {
        final VerticalViewActivity activity = getManagedComponent();

        DocumentController.chooseFullScreen(activity, AppState.get().fullScreenMode);

        if (++loadingCount == 1) {
            documentModel = ActivityControllerStub.DM_STUB;

            if (intent == null || intent.getData() == null) {
                return;
            }

            File file = new File(intent.getData().getPath());
            m_fileName = intent.getData().getPath();
            codecType = BookType.getByUri(m_fileName);

            FileMeta meta = FileMetaCore.createMetaIfNeed(m_fileName, false);
            title = meta.getTitle();
            if (TxtUtils.isEmpty(title)) {
                title = ExtUtils.getFileName(m_fileName);
            }
            LOG.d("Book-title", title);


            if (codecType == null) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), Apps.getApplicationName(getActivity()) + " " + getActivity().getString(R.string.application_cannot_open_the_book), Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
                return;
            }

            LOG.d("codecType last", codecType);
            documentModel = new DocumentModel(codecType, getView());
            documentModel.addListener(ViewerActivityController.this);

            final Uri uri = Uri.fromFile(file);
            controller.setCurrentBook(file);
            wrapperControlls.hideShowEditIcon();


            controller.addRecent(uri);
            SettingsManager.getBookSettings(uri.getPath());

            final AppBook.Diff diff = new AppBook.Diff(null, SettingsManager.getBookSettings());
            onBookSettingsChanged(null, SettingsManager.getBookSettings(), diff);

            if (intent.hasExtra("id2")) {
                wrapperControlls.showSelectTextMenu();
            }

            wrapperControlls.setTitle(title);
        }
        wrapperControlls.updateUI();

    }


    public void afterPostCreate() {

        if (loadingCount == 1 && documentModel != ActivityControllerStub.DM_STUB) {
            String stringExtra = intent.getStringExtra(DocumentController.EXTRA_PASSWORD);
            if (stringExtra == null) {
                stringExtra = "";
            }
            startDecoding(m_fileName, stringExtra);
        }

    }

    public int pageCount;

    public void startDecoding(final String fileName, final String password) {
        getManagedComponent().view.getView().post(new BookLoadTask(fileName, password, new Runnable() {

            @Override
            public void run() {

                intent.putExtra(HorizontalModeController.EXTRA_PASSWORD, password);


                if (onBookLoaded != null) {
                    onBookLoaded.run();
                }


                pageCount = controller.getPageCount();
                float percent = Intents.getFloatAndClear(intent, DocumentController.EXTRA_PERCENT);

                if (percent > 0f) {
                    LOG.d("startDecoding-onGoToPage", percent, pageCount);
                    controller.onGoToPage(Math.round(pageCount * percent));

                }

                controller.loadOutline(new ResultResponse<List<OutlineLinkWrapper>>() {

                    @Override
                    public boolean onResultRecive(List<OutlineLinkWrapper> result) {
                        wrapperControlls.showOutline(result, controller.getPageCount());

                        return false;
                    }
                });








            }
        }));
    }

    public void onPause() {
        if (wrapperControlls != null) {
            wrapperControlls.onPause();
        }
    }

    public void onDestroy() {
        if (wrapperControlls != null) {
            wrapperControlls.onDestroy();
        }
        LOG.d("ViewerActivityController onDestroy");
    }

    public void beforeDestroy() {
        final boolean finishing = getManagedComponent().isFinishing();
        if (finishing) {
            getManagedComponent().view.onDestroy();
            if (documentModel != null) {
                documentModel.recycle();
            }
            SettingsManager.removeListener(this);
        }
        LOG.d("ViewerActivityController beforeDestroy");

    }

    public void afterDestroy(boolean finishing) {
        getDocumentController().onDestroy();
        LOG.d("ViewerActivityController afterDestroy");
    }

    public void askPassword(final String fileName, final int promtId) {
        final EditText input = new EditText(getManagedComponent());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getManagedComponent());
        dialog.setTitle(R.string.enter_password);
        dialog.setView(input);
        dialog.setCancelable(false);
        dialog.setNegativeButton(R.string.cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                controller.onCloseActivityAdnShowInterstial();
            }
        });
        dialog.setPositiveButton(R.string.open_file, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String txt = input.getText().toString();
                if (TxtUtils.isNotEmpty(txt)) {
                    dialog.dismiss();
                    startDecoding(fileName, input.getText().toString());
                } else {
                    controller.onCloseActivityAdnShowInterstial();
                }
            }
        });
        dialog.show();

        //
    }

    public void showErrorDlg(final int msgId, final Object... args) {
        Toast.makeText(getManagedComponent(), msgId, Toast.LENGTH_SHORT).show();
    }

    public void redecodingWithPassword(final ActionEx action) {
        final PasswordEditable value = action.getParameter("input");
        final String password = value.getPassword();
        final String fileName = action.getParameter("path");

        intent.putExtra(DocumentController.EXTRA_PASSWORD, password);
        startDecoding(fileName, password);
    }

    protected IViewController switchDocumentController(final AppBook bs) {
        if (bs != null) {
            try {
                final IViewController newDc = DocumentViewMode.VERTICALL_SCROLL.create(this);
                if (newDc != null) {
                    final IViewController oldDc = ctrl.getAndSet(newDc);
                    getZoomModel().removeListener(oldDc);
                    getZoomModel().addListener(newDc);
                    return ctrl.get();
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void decodingProgressChanged(final int currentlyDecoding) {
    }

    @Override
    public void currentPageChanged(final int page, int pages) {
        final int pageCount = documentModel.getPageCount();
        String pageText = "";
        if (pageCount > 0) {
            pageText = (page + 1) + "/" + pageCount;
        }

        wrapperControlls.updateUI();


        wrapperControlls.setTitle(title);
        controller.setTitle(title);

    }

    public void createWrapper(Activity a) {
        try {
            String file = a.getIntent().getData().getPath();

            AppSP.get().lastBookPath = file;


            LOG.d("createWrapper", file);
            if (ExtUtils.isTextFomat(file)) {
                AppSP.get().isLocked = true;
            } else {
                if (AppState.get().isLockPDF) {
                    AppSP.get().isLocked = true;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        wrapperControlls.initUI(a);
    }

    public void onResume() {
        if (controller != null) {
            controller.onResume();
        }
        if (wrapperControlls != null) {
            wrapperControlls.onResume();
        }
        AppSP.get().lastClosedActivity = VerticalViewActivity.class.getSimpleName();
        LOG.d("lasta save", AppSP.get().lastClosedActivity);
    }

    public void onConfigChanged() {
        wrapperControlls.onConfigChanged();
    }


    @Override
    public ViewState jumpToPage(final int viewIndex, final float offsetX, final float offsetY, boolean addToHistory) {
        // getDocumentController().goToPage(viewIndex, x, y);
        ViewState goToPage;
        if (addToHistory) {
            int curY = getDocumentController().getView().getScrollY();
            goToPage = getDocumentController().goToPage(viewIndex);
            controller.getLinkHistory().add(curY);
            wrapperControlls.showHideHistory();
        } else {
            // getDocumentController().goToPage(viewIndex, x, y);
            goToPage = getDocumentController().goToPage(viewIndex);
        }
        return goToPage;
    }

    public final void doSearch(final String text, final ResultResponse<Integer> result) {
        getDecodeService().searchText(text, documentModel.getPages(), result, new Runnable() {

            @Override
            public void run() {
                getView().redrawView();
            }
        });
    }

    public void showDialog(final ActionEx action) {
        final Integer dialogId = action.getParameter("dialogId");
        getManagedComponent().showDialog(dialogId);
    }

    public void toggleNightMode() {
        getDocumentController().toggleRenderingEffects();
        currentPageChanged(documentModel.getCurrentIndex().docIndex, getDocumentController().getBase().getDocumentModel().getPageCount());
    }

    public void toggleCrop(boolean isCrop) {
        getDocumentController().toggleRenderingEffects();

        final IViewController newDc = switchDocumentController(SettingsManager.getBookSettings());
        newDc.init(null);
        newDc.show();

        currentPageChanged(documentModel.getCurrentIndex().docIndex, getDocumentController().getBase().getDocumentModel().getPageCount());

    }

    /**
     * Gets the z model.
     *
     * @return the z model
     */
    @Override
    public ZoomModel getZoomModel() {
        if (zoomModel == null) {
            zoomModel = new ZoomModel();
        }
        return zoomModel;
    }

    @Override
    public DecodeService getDecodeService() {
        return documentModel != null ? documentModel.decodeService : null;
    }

    /**
     * Gets the decoding progress model.
     *
     * @return the decoding progress model
     */

    @Override
    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    @Override
    public IViewController getDocumentController() {
        return ctrl.get();
    }

    @Override
    public Context getContext() {
        return getManagedComponent();
    }

    @Override
    public IView getView() {
        return getManagedComponent().view;
    }

    @Override
    public Activity getActivity() {
        return getManagedComponent();
    }

    @Override
    public IActionController<?> getActionController() {
        return this;
    }

    public void closeActivity(final ActionEx action) {
        viewerActivity.showInterstial();
        LOG.d("ViewerActivityController closeActivity");
    }

    public void closeActivityFinal(final Runnable action) {

        Safe.run(new Runnable() {

            @Override
            public void run() {

                TTSEngine.get().stop();
                TTSNotification.hideNotification();

                LOG.d("closeActivity 1");
                if (documentModel != null) {
                    documentModel.recycle();
                }

                LOG.d("closeActivity 2");
                LOG.d("closeActivity 3");
                getManagedComponent().finish();

                System.gc();
                //BitmapManager.clear("finish");

                if (action != null) {
                    action.run();
                }
            }
        });

        LOG.d("closeActivity DONE");
    }

    public void closeActivity1(final ActionEx action) {
        getManagedComponent().finish();

        System.gc();
        //BitmapManager.clear("finish");

        LOG.d("ViewerActivityController closeActivity1");
    }


    @Override
    public void onBookSettingsChanged(final AppBook oldSettings, final AppBook newSettings, final AppBook.Diff diff) {
        if (newSettings == null) {
            return;
        }

        boolean redrawn = false;
        if (diff.isSplitPagesChanged() || diff.isCropPagesChanged()) {
            redrawn = true;
            final IViewController newDc = switchDocumentController(newSettings);
            if (!diff.isFirstTime() && newDc != null) {
                newDc.init(null);
                newDc.show();
            }
        }

        if (diff.isFirstTime()) {
            getZoomModel().initZoom(newSettings.getZoom());
        }

        final IViewController dc = getDocumentController();

        if (!redrawn && (diff.isEffectsChanged())) {
            redrawn = true;
            dc.toggleRenderingEffects();
        }

        if (diff.isAnimationTypeChanged()) {
            dc.updateAnimationType();
        }

        // currentPageChanged(PageIndex.NULL, documentModel.getCurrentIndex());
        //currentPageChanged(newSettings.currentPage.do, -1);

    }

    public DocumentWrapperUI getWrapperControlls() {
        return wrapperControlls;
    }

    final class BookLoadTask extends BaseAsyncTask<String, Throwable> implements IProgressIndicator, Runnable {

        private String m_fileName;
        private final String m_password;
        private final Runnable onBookLoaded;

        public BookLoadTask(final String fileName, final String password, Runnable onBookLoaded) {
            super(getManagedComponent());
            m_fileName = fileName;
            m_password = password;
            this.onBookLoaded = onBookLoaded;
        }

        @Override
        public void run() {
            execute();
        }

        @Override
        public void onBookCancel() {
            super.onBookCancel();
            LOG.d("onBookCancel");
            TempHolder.get().loadingCancelled = true;
            closeActivity(null);
        }

        @Override
        protected Throwable doInBackground(final String... params) {
            try {
                m_fileName = getActivity().getIntent().getData().getPath();


                getView().waitForInitialization();
                documentModel.open(m_fileName, m_password);
                getDocumentController().init(this);
                return null;
            } catch (final MuPdfPasswordException pex) {
                return pex;
            } catch (final Exception e) {
                LOG.e(e);
                return e;
            } catch (final Throwable th) {
                LOG.e(th);
                return th;
            } finally {
            }
        }

        @Override
        protected void onPostExecute(Throwable result) {
            try {
                LOG.d("onPostExecute");
                if (TempHolder.get().loadingCancelled) {
                    super.onPostExecute(result);
                    closeActivity(null);
                    return;
                }


                wrapperControlls.onLoadBookFinish();
                if (result == null) {
                    try {
                        getDocumentController().show();

                        final DocumentModel dm = getDocumentModel();
                        currentPageChanged(dm.getCurrentIndex().docIndex, -1);
                        onBookLoaded.run();

                    } catch (final Throwable th) {
                        result = th;
                    }
                }

                super.onPostExecute(result);

                if (result instanceof MuPdfPasswordException) {
                    final MuPdfPasswordException pex = (MuPdfPasswordException) result;
                    final int promptId = pex.isWrongPasswordEntered() ? R.string.msg_wrong_password : R.string.msg_password_required;

                    askPassword(m_fileName, promptId);

                } else if (result != null) {
                    final String msg = result.getMessage();
                    showErrorDlg(R.string.msg_unexpected_error, msg);
                }
            } catch (final Throwable th) {
                LOG.e(th);
            }

        }

        @Override
        public void setProgressDialogMessage(final int resourceID, final Object... args) {
            publishProgress(getManagedComponent().getString(resourceID, args));
        }
    }

}
