package com.foobnix.android.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.foobnix.model.AppState;
import com.foobnix.ui2.MainTabs2;

public class Keyboards {

    static Handler handler = new Handler(Looper.getMainLooper());

    public static void hideAlways(Activity context) {
        context.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public static void close(Activity context) {
        if (context == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = context.getCurrentFocus();
        if (currentFocus != null) {
            inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static void close(View currentFocus) {
        if (currentFocus == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) currentFocus.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        // inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);

        //invalidateEink(currentFocus.getContext());

    }


    public static void invalidateEink(View parent) {
        try {

            if (parent != null) {
                if (Dips.isEInk()) {
                    parent.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                parent.invalidate();
                                LOG.d("invalidateEink", parent.getId());
                            } catch (Exception e) {
                                LOG.e(e);
                            }

                        }
                    }, 100);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void hideNavigation(final Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            if (activity instanceof MainTabs2
                    && AppState.get().fullScreenMainMode == AppState.FULL_SCREEN_NORMAL) {
                return;
            } else if (AppState.get().fullScreenMode == AppState.FULL_SCREEN_NORMAL) {
                return;
            }

            final Window window = activity.getWindow();
            final View decorView = window.getDecorView();
            final WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, decorView);

            decorView.postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    WindowCompat.setDecorFitsSystemWindows(window, false);
                    insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.navigationBars() | WindowInsetsCompat.Type.statusBars());
                }
            }, 100);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void hideNavigationOnCreate(final Activity a) {
        try {
            if (AppState.get().fullScreenMode == AppState.FULL_SCREEN_NORMAL) {
                return;
            }
            final Window window = a.getWindow();
            final WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                WindowCompat.setDecorFitsSystemWindows(window, false);
                insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                insetsController.hide(WindowInsetsCompat.Type.navigationBars() | WindowInsetsCompat.Type.statusBars());
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }
}
