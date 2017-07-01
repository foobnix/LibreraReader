package org.emdev.ui.uimanager;

import com.foobnix.sys.AdvModeController;

import android.app.Activity;
import android.view.View;

public class IUIManager {

    public static void setFullScreenMode(final Activity activity, final View view, final boolean fullScreen) {
        if (fullScreen) {
            AdvModeController.runFullScreen(activity);
        } else {
            AdvModeController.runNormalScreen(activity);
        }
    }

}
