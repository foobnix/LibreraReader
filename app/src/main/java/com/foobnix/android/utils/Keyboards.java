package com.foobnix.android.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

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

    public static void hideNavigation(final Activity a) {
        try {
            if (a == null) {
                return;
            }
            if (a instanceof MainTabs2 && AppState.get().fullScreenMainMode == AppState.FULL_SCREEN_NORMAL) {
                return;
            } else if (AppState.get().fullScreenMode == AppState.FULL_SCREEN_NORMAL) {
                return;
            }

            final View decorView = a.getWindow().getDecorView();
            decorView.postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 19) {
                        decorView.setSystemUiVisibility(//
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE //
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION//
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN//
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION//
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN//
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);//
                    } else if (Build.VERSION.SDK_INT >= 14) {
                        decorView.setSystemUiVisibility( //
                                View.SYSTEM_UI_FLAG_LOW_PROFILE //
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN); //
                    }
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
            final View decorView = a.getWindow().getDecorView();
            if (Build.VERSION.SDK_INT >= 19) {
                decorView.setSystemUiVisibility(//
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE //
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION//
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN//
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION//
                                | View.SYSTEM_UI_FLAG_FULLSCREEN//
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);//
            } else if (Build.VERSION.SDK_INT >= 14) {
                decorView.setSystemUiVisibility( //
                        View.SYSTEM_UI_FLAG_LOW_PROFILE //
                                | View.SYSTEM_UI_FLAG_FULLSCREEN); //
            }
        } catch (Exception e) {
            LOG.e(e);
        }

    }

}