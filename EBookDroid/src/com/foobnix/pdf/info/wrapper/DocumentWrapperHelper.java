package com.foobnix.pdf.info.wrapper;

import android.app.Activity;
import android.view.WindowManager;

public class DocumentWrapperHelper {

	public static void restoreFullScreen(Activity a) {
        if (AppState.get().isFullScreen) {
			a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	public static void changeFullScreen(Activity a) {
        AppState.get().isFullScreen = !AppState.get().isFullScreen;
		restoreFullScreen(a);
	}

}
