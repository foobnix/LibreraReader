package org.ebookdroid.ui.viewer.viewers;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.ViewState;
import org.emdev.utils.concurrent.Flag;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class DrawThread extends Thread {

    private final SurfaceHolder surfaceHolder;

    private final BlockingQueue<ViewState> queue = new ArrayBlockingQueue<ViewState>(16, true);

    private final Flag stop = new Flag();

    public DrawThread(final SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public void finish() {
        stop.set();
        try {
            this.join();
        } catch (final InterruptedException e) {
        }
    }

    @Override
    public void run() {
        while (!stop.get()) {
            draw(false);
        }
    }

    protected void draw(final boolean useLastState) {
        final ViewState viewState = takeTask(250, TimeUnit.MILLISECONDS, useLastState);
        if (viewState == null) {
            return;
        }
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas(null);
            EventPool.newEventDraw(viewState, canvas, null).process();
        } catch (final Throwable th) {
            th.printStackTrace();
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                } catch (Exception e) {
                }
            }
        }
    }

    public ViewState takeTask(final long timeout, final TimeUnit unit, final boolean useLastState) {
        ViewState task = null;
        try {
            task = queue.poll(timeout, unit);
            if (task != null && useLastState) {
                final ArrayList<ViewState> list = new ArrayList<ViewState>();
                // Workaround for possible ConcurrentModificationException
                while (true) {
                    list.clear();
                    try {
                        if (queue.drainTo(list) > 0) {
                            task = list.get(list.size() - 1);
                        }
                        break;
                    } catch (Throwable ex) {
                        // Go to next attempt
                        ex.printStackTrace();
                    }
                }
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
        } catch (Throwable ex) {
            // Go to next attempt
            ex.printStackTrace();
        }
        return task;
    }

    public void draw(final ViewState viewState) {
        if (viewState != null) {
            // Workaround for possible ConcurrentModificationException
            while (true) {
                try {
                    queue.offer(viewState);
                    break;
                } catch (Throwable ex) {
                    // Go to next attempt
                    ex.printStackTrace();
                }
            }
        }
    }
}
