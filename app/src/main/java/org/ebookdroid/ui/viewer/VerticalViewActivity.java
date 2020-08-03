package org.ebookdroid.ui.viewer;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Intents;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.drive.GFile;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.PasswordDialog;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.MyContextWrapper;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.ui.viewer.viewers.PdfSurfaceView;
import org.emdev.ui.AbstractActionActivity;

public class VerticalViewActivity extends AbstractActionActivity<VerticalViewActivity, ViewerActivityController> {
    public static final DisplayMetrics DM = new DisplayMetrics();

    IView view;

    private FrameLayout frameLayout;

    /**
     * Instantiates a new base viewer activity.
     */
    public VerticalViewActivity() {
        super();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        LOG.d("VerticalViewActivity", "onNewIntent");
        if (TTSNotification.ACTION_TTS.equals(intent.getAction())) {
            return;
        }
        if (!intent.filterEquals(getIntent())) {
            finish();
            startActivity(intent);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected ViewerActivityController createController() {
        return new ViewerActivityController(this);
    }

    private Handler handler;

    /**
     * Called when the activity is first created.
     */

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        intetrstialTimeoutSec = ADS.FULL_SCREEN_TIMEOUT_SEC;
        DocumentController.doRotation(this);
        DocumentController.doContextMenu(this);



        FileMetaCore.checkOrCreateMetaInfo(this);

        if (getIntent().getData() != null) {
            String path = getIntent().getData().getPath();
            final AppBook bs = SettingsManager.getBookSettings(path);
            // AppState.get().setNextScreen(bs.isNextScreen);
            if (bs != null) {
                // AppState.get().l = bs.l;
                AppState.get().autoScrollSpeed = bs.s;
                final boolean isTextFormat = ExtUtils.isTextFomat(bs.path);
                AppSP.get().isCut = isTextFormat ? false : bs.sp; //important!!!
                AppSP.get().isCrop = bs.cp;
                AppSP.get().isDouble = false;
                AppSP.get().isDoubleCoverAlone = false;
                AppSP.get().isLocked = bs.getLock(isTextFormat);
                TempHolder.get().pageDelta = bs.d;
                if (AppState.get().isCropPDF && !isTextFormat) {
                    AppSP.get().isCrop = true;
                }
            }
            BookCSS.get().detectLang(path);
        }

        getController().beforeCreate(this);

        BrightnessHelper.applyBrigtness(this);

        if (AppState.get().isDayNotInvert) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }
        super.onCreate(savedInstanceState);

        //FirebaseAnalytics.getInstance(this);

        if (PasswordDialog.isNeedPasswordDialog(this)) {
            return;
        }
        setContentView(R.layout.activity_vertical_view);

        if (!Android6.canWrite(this)) {
            Android6.checkPermissions(this, true);
            return;
        }



        getController().createWrapper(this);
        frameLayout = (FrameLayout) findViewById(R.id.documentView);

        view = new PdfSurfaceView(getController());


        frameLayout.addView(view.getView());

        getController().afterCreate(this);

        // ADS.activate(this, adView);

        handler = new Handler();

        getController().onBookLoaded(new Runnable() {

            @Override
            public void run() {
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        isInitPosition = Dips.screenHeight() > Dips.screenWidth();
                        isInitOrientation = AppState.get().orientation;
                    }
                }, 1000);

            }
        });


    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DocumentController.doRotation(this);

        if (AppState.get().fullScreenMode == AppState.FULL_SCREEN_FULLSCREEN) {
            Keyboards.hideNavigation(this);
        }
        getController().onResume();
        if (handler != null) {
            handler.removeCallbacks(closeRunnable);
        }
        if(AppState.get().inactivityTime!=-1) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            LOG.d("FLAG clearFlags", "FLAG_KEEP_SCREEN_ON","add",AppState.get().inactivityTime);
        }

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            int page = Math.round(getController().getDocumentModel().getPageCount() * Intents.getFloatAndClear(data,DocumentController.EXTRA_PERCENT));
            getController().getDocumentController().goToPage(page);
        }
    }

    boolean needToRestore = false;

    @Override
    protected void onPause() {
        super.onPause();
        LOG.d("onPause", this.getClass());
        getController().onPause();
        needToRestore = AppState.get().isAutoScroll;
        AppState.get().isAutoScroll = false;
        AppProfile.save(this);
        TempHolder.isSeaching = false;
        TempHolder.isActiveSpeedRead.set(false);

        if (handler != null) {
            handler.postDelayed(closeRunnable, AppState.APP_CLOSE_AUTOMATIC);
        }
        GFile.runSyncService(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Analytics.onStart(this);
        if (needToRestore) {
            AppState.get().isAutoScroll = true;
            getController().getListener().onAutoScroll();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    Runnable closeRunnable = new Runnable() {

        @Override
        public void run() {
            LOG.d("Close App");
            getController().closeActivityFinal(null);
            MainTabs2.closeApp(VerticalViewActivity.this);
        }
    };

    @Override
    protected void onDestroy() {

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    Dialog rotationDialog;
    Boolean isInitPosition;
    int isInitOrientation;

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        TempHolder.isActiveSpeedRead.set(false);
        if (isInitPosition == null) {
            return;
        }

        final boolean currentPosition = Dips.screenHeight() > Dips.screenWidth();

        if (ExtUtils.isTextFomat(getIntent()) && isInitOrientation == AppState.get().orientation) {

            if (rotationDialog != null) {
                try {
                    rotationDialog.dismiss();
                } catch (Exception e) {
                    LOG.e(e);
                }
            }

            if (isInitPosition != currentPosition) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setCancelable(false);
                dialog.setMessage(R.string.apply_a_new_screen_orientation_);
                dialog.setPositiveButton(R.string.yes, new AlertDialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doConfigChange();
                        isInitPosition = currentPosition;
                    }
                });
                rotationDialog = dialog.show();
                rotationDialog.getWindow().setLayout((int) (Dips.screenMinWH() * 0.8f), LayoutParams.WRAP_CONTENT);

            }
        } else {
            doConfigChange();
        }

        isInitOrientation = AppState.get().orientation;
    }

    public void doConfigChange() {
        try {
            if (!getController().getDocumentController().isInitialized()) {
                LOG.d("Skip onConfigurationChanged");
                return;
            }
        } catch (Exception e) {
            LOG.e(e);
            return;
        }

        AppProfile.save(this);

        if (ExtUtils.isTextFomat(getIntent())) {

            //float value = getController().getDocumentModel().getPercentRead();
            //Intents.putFloat(getIntent(),DocumentController.EXTRA_PERCENT, value);

            //LOG.d("READ PERCEnt", value);

            getController().closeActivityFinal(new Runnable() {

                @Override
                public void run() {
                    startActivity(getIntent());
                }
            });

        } else {
            getController().onConfigChanged();
            activateAds();
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getController().afterPostCreate();
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 12) {
            return GenericMotionEvent12.onGenericMotionEvent(event, this);
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        // Toast.makeText(this, "onKeyLongPress", Toast.LENGTH_SHORT).show();
        if (CloseAppDialog.checkLongPress(this, event)) {
            CloseAppDialog.showOnLongClickDialog(getController().getActivity(), null, getController().getListener());
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // Toast.makeText(this, "onBackPressed", Toast.LENGTH_SHORT).show();

        if (isInterstialShown()) {
            getController().closeActivityFinal(null);
            return;
        }
        if (getController().getWrapperControlls().checkBack(new KeyEvent(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK))) {
            return;
        }

        if (AppState.get().isShowLongBackDialog) {
            CloseAppDialog.showOnLongClickDialog(getController().getActivity(), null, getController().getListener());
        } else {
            //showInterstial();
            getController().getListener().onCloseActivityAdnShowInterstial();
        }

    }

    private volatile boolean isMyKey = false;

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        LOG.d("onKeyUp");
        if (isMyKey) {
            return true;
        }

        if (getController().getWrapperControlls().dispatchKeyEventUp(event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    long keyTimeout = 0;

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        LOG.d("onKeyDown");
        isMyKey = false;
        int repeatCount = event.getRepeatCount();
        if (repeatCount >= 1 && repeatCount < DocumentController.REPEAT_SKIP_AMOUNT) {
            isMyKey = true;
            return true;
        }

        if (repeatCount == 0 && System.currentTimeMillis() - keyTimeout < 250) {
            LOG.d("onKeyDown timeout", System.currentTimeMillis() - keyTimeout);
            isMyKey = true;
            return true;
        }

        keyTimeout = System.currentTimeMillis();


        if (getController().getWrapperControlls().dispatchKeyEventDown(event)) {
            isMyKey = true;
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onFinishActivity() {
        getController().closeActivityFinal(null);

    }

}
