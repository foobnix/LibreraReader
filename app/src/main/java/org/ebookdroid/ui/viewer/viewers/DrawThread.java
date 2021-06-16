package org.ebookdroid.ui.viewer.viewers;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import com.foobnix.android.utils.LOG;

import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.ViewState;

public class DrawThread extends HandlerThread {


    SurfaceHolder holder;
    Handler mReceiver;

    ViewState viewState;

    public DrawThread(SurfaceHolder holder) {
        super("DrawThread");
        this.holder = holder;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mReceiver = new Handler(getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {

                if (viewState != null && holder != null) {
                    Canvas canvas = holder.lockCanvas();
                    try {
                        EventPool.newEventDraw(viewState, canvas, null).process();
                    } catch (Exception e) {
                        LOG.e(e);
                    } finally {
                        if (canvas != null) {
                            holder.unlockCanvasAndPost(canvas);
                        }
                    }

                }
                return false;
            }

        });
    }

    public void draw(ViewState viewState) {
        this.viewState = viewState;
        if (mReceiver != null) {
            mReceiver.sendEmptyMessage(1);
        }
    }


}





