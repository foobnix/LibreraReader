package org.ebookdroid.ui.viewer.viewers;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

import com.foobnix.android.utils.LOG;

import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.ViewState;
import org.emdev.utils.concurrent.Flag;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DrawThread extends Thread {

    private final SurfaceHolder surfaceHolder;

    private final BlockingQueue<ViewState> queue = new ArrayBlockingQueue<ViewState>(16, true);

    private final Flag stop = new Flag();

    private final ArrayList<ViewState> list = new ArrayList<ViewState>();


    public DrawThread(final SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public void finish() {
        stop.set();
        try {
            this.join();
        } catch (final InterruptedException e) {
            LOG.e(e);
        }
    }

    @Override
    public void run() {
        while (!stop.get()) {
            draw();
        }
    }

    protected void draw() {
        final ViewState viewState = takeTask(1000, TimeUnit.MILLISECONDS, false);
        if (viewState == null) {
            return;
        }
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas(null);
            EventPool.newEventDraw(viewState, canvas, null).process();
        } catch (final Throwable th) {
            LOG.e(th);
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                } catch (Exception e) {
                    LOG.e(e);
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
                        LOG.e(ex);
                    }
                }
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
        } catch (Throwable ex) {
            // Go to next attempt
            ex.printStackTrace();
            LOG.e(ex);
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
                    LOG.e(ex);
                }
            }
        }
    }
}
