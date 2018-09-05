package org.ebookdroid.ui.viewer;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.events.DecodingProgressListener;
import org.ebookdroid.core.models.DecodingProgressModel;
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
import org.emdev.utils.LengthUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.DocumentWrapperUI;
import com.foobnix.pdf.search.activity.HorizontalModeController;
import com.foobnix.sys.TempHolder;
import com.foobnix.sys.VerticalModeController;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.AppDB;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.InputType;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

public class ViewerActivityController extends ActionController<VerticalViewActivity> implements IActivityController, DecodingProgressListener, CurrentPageListener, IBookSettingsChangeListener {

    private static final String E_MAIL_ATTACHMENT = "[E-mail Attachment]";

    private final AtomicReference<IViewController> ctrl = new AtomicReference<IViewController>(ViewContollerStub.STUB);

    private ZoomModel zoomModel;

    private DecodingProgressModel progressModel;

    private DocumentModel documentModel;

    private String bookTitle;

    private boolean temporaryBook;

    private BookType codecType;

    private final Intent intent;

    private int loadingCount = 0;

    private String m_fileName;

    private String currentSearchPattern;

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

        DocumentController.chooseFullScreen(activity, AppState.get().isFullScreen);

        if (++loadingCount == 1) {
            documentModel = ActivityControllerStub.DM_STUB;

            if (intent == null) {
                return;
            }
            final String scheme = intent.getScheme();
            if (LengthUtils.isEmpty(scheme)) {
                return;
            }
            final Uri data = intent.getData();
            if (data == null) {
                return;
            }

            File file = null;
            if (scheme.equals("content")) {
                bookTitle = CacheManager.getFilePathFromAttachmentIfNeed(getActivity());
                if (BookType.isSupportedExtByPath(bookTitle)) {
                    codecType = BookType.getByUri(bookTitle);
                    file = new File(bookTitle);
                    bookTitle = AppDB.get().getOrCreate(bookTitle).getTitle();
                    LOG.d("codecType 0", codecType);
                }
                if (codecType == null) {
                    codecType = BookType.getByMimeType(ExtUtils.getMimeTypeByUri(intent.getData()));
                    file = new File(bookTitle);
                }

                LOG.d("codecType1", codecType);
            }
            if (codecType == null) {
                bookTitle = LengthUtils.safeString(data.getLastPathSegment(), E_MAIL_ATTACHMENT);
                if (ExtUtils.isTextFomat(data.getLastPathSegment())) {
                    bookTitle = AppDB.get().getOrCreate(data.getPath()).getTitle();
                }
                codecType = BookType.getByUri(data.toString());
                if (codecType == null) {
                    final String type = intent.getType();
                    if (LengthUtils.isNotEmpty(type)) {
                        codecType = BookType.getByMimeType(type);
                    }
                }
                LOG.d("codecType2", codecType);
            }
            if (file == null) {
                file = new File(intent.getData().getPath());
            }

            if (codecType == null) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.app_name) + " " + getActivity().getString(R.string.application_cannot_open_the_book), Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
                return;
            }

            LOG.d("codecType last", codecType);
            documentModel = new DocumentModel(codecType, getView());
            documentModel.addListener(ViewerActivityController.this);
            progressModel = new DecodingProgressModel();
            progressModel.addListener(ViewerActivityController.this);

            final Uri uri = Uri.fromFile(file);
            controller.setCurrentBook(file);
            m_fileName = "";

            if (scheme.equals("content")) {
                temporaryBook = true;
                m_fileName = E_MAIL_ATTACHMENT;
            } else {
                m_fileName = retrieve(activity.getContentResolver(), uri);
            }
            controller.addRecent(uri);
            SettingsManager.getBookSettings(uri.getPath());

            final BookSettings.Diff diff = new BookSettings.Diff(null, SettingsManager.getBookSettings());
            onBookSettingsChanged(null, SettingsManager.getBookSettings(), diff);

            if (intent.hasExtra("id2")) {
                wrapperControlls.showSelectTextMenu();
            }

        }
        wrapperControlls.updateUI();

    }

    public static String retrieve(final ContentResolver resolver, final Uri uri) {
        if (uri.getScheme().equals("file")) {
            return uri.getPath();
        }
        final Cursor cursor = resolver.query(uri, new String[] { "_data" }, null, null, null);
        if ((cursor != null) && cursor.moveToFirst()) {
            return cursor.getString(0);
        }
        throw new RuntimeException("Can't retrieve path from uri: " + uri.toString());
    }

    public void afterPostCreate() {
        setWindowTitle();
        if (loadingCount == 1 && documentModel != ActivityControllerStub.DM_STUB) {
            String stringExtra = intent.getStringExtra(DocumentController.EXTRA_PASSWORD);
            if (stringExtra == null) {
                stringExtra = "";
            }
            startDecoding(m_fileName, stringExtra);
        }

    }

    public void startDecoding(final String fileName, final String password) {
        getManagedComponent().view.getView().post(new BookLoadTask(fileName, password, new Runnable() {

            @Override
            public void run() {

                int pageIntent = intent.getIntExtra(DocumentController.EXTRA_PAGE, 0);
                intent.putExtra(HorizontalModeController.EXTRA_PASSWORD, password);
                double percent = intent.getDoubleExtra(VerticalViewActivity.PERCENT_EXTRA, 0.0);

                intent.putExtra(DocumentController.EXTRA_PAGE, 0);
                intent.putExtra(VerticalViewActivity.PERCENT_EXTRA, 0.0);

                if (percent > 0) {
                    LOG.d("Percent", percent, getDocumentModel().getPageCount());
                    pageIntent = (int) (getDocumentModel().getPageCount() * percent);
                }

                if (pageIntent > 0) {
                    controller.onGoToPage(pageIntent);
                }

                if (onBookLoaded != null) {
                    onBookLoaded.run();
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

        BookSettings bookSettings = SettingsManager.getBookSettings();
        if (bookSettings != null) {
            bookSettings.updateFromAppState();
            bookSettings.save();
        }
    }

    public void onDestroy() {
        if (wrapperControlls != null) {
            wrapperControlls.onDestroy();
        }
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
    }

    public void afterDestroy(boolean finishing) {
        getDocumentController().onDestroy();
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
        final String fileName = action.getParameter("fileName");

        intent.putExtra(DocumentController.EXTRA_PASSWORD, password);
        startDecoding(fileName, password);
    }

    protected IViewController switchDocumentController(final BookSettings bs) {
        if (bs != null) {
            try {
                final IViewController newDc = bs.viewMode.create(this);
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
        final VerticalViewActivity activity = getManagedComponent();
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    activity.setProgressBarIndeterminateVisibility(currentlyDecoding > 0);
                    activity.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, currentlyDecoding == 0 ? 10000 : currentlyDecoding);
                } catch (final Throwable e) {
                }
            }
        });
    }

    @Override
    public void currentPageChanged(final PageIndex newIndex, int pages) {
        final int pageCount = documentModel.getPageCount();
        String pageText = "";
        if (pageCount > 0) {
            pageText = (newIndex.viewIndex + 1) + "/" + pageCount;
        }

        try {
            BookSettings bs = SettingsManager.getBookSettings(controller.getCurrentBook().getPath());
            bs.updateFromAppState();
            bs.currentPageChanged(newIndex, pageCount);
            bs.save();
        } catch (Exception e) {
            LOG.e(e);
        }

        wrapperControlls.updateUI();
        wrapperControlls.setTitle(bookTitle);
        controller.setTitle(bookTitle);
    }

    public void createWrapper(Activity a) {
        try {
            String file = CacheManager.getFilePathFromAttachmentIfNeed(a);
            if (TxtUtils.isEmpty(file)) {
                file = a.getIntent().getData().getPath();
            }
            AppState.get().lastBookPath = file;
            AppState.get().lastClosedActivity = VerticalViewActivity.class.getSimpleName();
            AppState.get().lastMode = VerticalViewActivity.class.getSimpleName();

            LOG.d("lasta save", AppState.get().lastClosedActivity);

            LOG.d("createWrapper", file);
            if (ExtUtils.isTextFomat(file)) {
                AppState.get().isLocked = true;
            } else {
                if (AppState.get().isLockPDF) {
                    AppState.get().isLocked = true;
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

    }

    public void setWindowTitle() {
        getManagedComponent().getWindow().setTitle(bookTitle);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IActivityController#jumpToPage(int, float,
     *      float)
     */
    @Override
    public ViewState jumpToPage(final int viewIndex, final float offsetX, final float offsetY, boolean addToHistory) {
        // getDocumentController().goToPage(viewIndex, offsetX, offsetY);
        ViewState goToPage;
        if (addToHistory) {
            int curY = getDocumentController().getView().getScrollY();
            goToPage = getDocumentController().goToPage(viewIndex);
            controller.getLinkHistory().add(curY);
            wrapperControlls.showHideHistory();
        } else {
            // getDocumentController().goToPage(viewIndex, offsetX, offsetY);
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
        currentPageChanged(documentModel.getCurrentIndex(), -1);
    }

    public void toggleCrop(boolean isCrop) {
        SettingsManager.toggleCropMode(isCrop);
        getDocumentController().toggleRenderingEffects();

        final IViewController newDc = switchDocumentController(SettingsManager.getBookSettings());
        newDc.init(null);
        newDc.show();

        currentPageChanged(documentModel.getCurrentIndex(), -1);

    }

    /**
     * Gets the zoom model.
     * 
     * @return the zoom model
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
    public DecodingProgressModel getDecodingProgressModel() {
        return progressModel;
    }

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
                if (temporaryBook) {
                    SettingsManager.removeCurrentBookSettings();
                } else {
                    SettingsManager.storeBookSettings1();
                }
                LOG.d("closeActivity 3");
                getManagedComponent().finish();

                System.gc();
                BitmapManager.clear("finish");

                if (action != null) {
                    action.run();
                }
            }
        });

        LOG.d("closeActivity DONE");
    }

    public void closeActivity1(final ActionEx action) {
        if (temporaryBook) {
            SettingsManager.removeCurrentBookSettings();
        } else {
            SettingsManager.storeBookSettings1();
        }
        getManagedComponent().finish();

        System.gc();
        BitmapManager.clear("finish");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.common.settings.listeners.ISettingsChangeListener#onBookSettingsChanged(org.ebookdroid.common.settings.books.BookSettings,
     *      org.ebookdroid.common.settings.books.BookSettings,
     *      org.ebookdroid.common.settings.books.BookSettings.Diff,
     *      org.ebookdroid.common.settings.AppSettings.Diff)
     */
    @Override
    public void onBookSettingsChanged(final BookSettings oldSettings, final BookSettings newSettings, final BookSettings.Diff diff) {
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
        currentPageChanged(newSettings.currentPage, -1);

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
                if (intent.getScheme().equals("content")) {
                    m_fileName = CacheManager.getFilePathFromAttachmentIfNeed(getActivity());
                }
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
                if (result == null) {
                    try {
                        getDocumentController().show();

                        final DocumentModel dm = getDocumentModel();
                        currentPageChanged(dm.getCurrentIndex(), -1);
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
