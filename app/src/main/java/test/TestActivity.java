package test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.R;

import java.util.ArrayList;

public class TestActivity extends Activity implements View.OnTouchListener,
        SurfaceHolder.Callback {

    private SurfaceView mSurface;
    private DrawingThread mThread;
    static int dx = 0;
    static int dy = 0;
    ScaleGestureDetector scaleGestureDetector;
    static float zoom =1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSurface = new SurfaceView(this);
        mSurface.setOnTouchListener(this);
        mSurface.getHolder().addCallback(this);

        setContentView(mSurface);

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                mThread.addItem((int) 100, 100);
                mThread.addItem((int) 500, 500);
            }
        }, 200);

         scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoom =zoom* detector.getScaleFactor();
                LOG.d("ScaleGestureDetector onScale", detector.getScaleFactor(), detector.getCurrentSpan(), detector.getPreviousSpan());

                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                LOG.d("ScaleGestureDetector onScaleBegin", detector.getScaleFactor());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                LOG.d("ScaleGestureDetector onScaleEnd", detector.getScaleFactor());

            }
        });
    }

    public void onClick(View v) {
        mThread.clearItems();
    }

    int initX, initY;



    public boolean onTouch(View v, MotionEvent event) {
        boolean res = scaleGestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // mThread.addItem((int) event.getX(), (int) event.getY());
            initX = (int) event.getX();
            initY = (int) event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // mThread.addItem((int) event.getX(), (int) event.getY());
            dx = (int) event.getX()-initX;
            dy = (int)event.getY()-initY;
        }
        return res;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new DrawingThread(holder, BitmapFactory.decodeResource(
                getResources(), R.drawable.gdrive));
        mThread.start();


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        mThread.updateSize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread.quit();
        mThread = null;
    }

    private static class DrawingThread extends HandlerThread implements
            Handler.Callback {
        private static final int MSG_ADD = 100;
        private static final int MSG_MOVE = 101;
        private static final int MSG_CLEAR = 102;
        private int mDrawingWidth, mDrawingHeight;
        private SurfaceHolder mDrawingSurface;
        private Paint mPaint;
        private Handler mReceiver;
        private Bitmap mIcon;
        private ArrayList<DrawingItem> mLocations;
        private Paint bPaint;

        private class DrawingItem {
            // Current location marker
            int x, y;
            // Direction markers for motion
            boolean horizontal, vertical;

            public DrawingItem(int x, int y, boolean horizontal,
                               boolean vertical) {
                this.x = x;
                this.y = y;
                this.horizontal = horizontal;
                this.vertical = vertical;
            }
        }

        public DrawingThread(SurfaceHolder holder, Bitmap icon) {
            super("DrawingThread");
            mDrawingSurface = holder;
            mLocations = new ArrayList<DrawingItem>();
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mIcon = icon;

            bPaint = new Paint();
            bPaint.setColor(Color.RED);
            bPaint.setStrokeWidth(2);
            bPaint.setStyle(Paint.Style.STROKE);


        }

        @Override
        protected void onLooperPrepared() {
            mReceiver = new Handler(getLooper(), this);
            // Start the rendering
            mReceiver.sendEmptyMessage(MSG_MOVE);
        }

        @Override
        public boolean quit() {
            // Clear all messages before dying
            mReceiver.removeCallbacksAndMessages(null);
            return super.quit();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD:
                    // Create a new item at the touch location,
                    // with a randomized start direction
                    DrawingItem newItem = new DrawingItem(msg.arg1, msg.arg2,
                            Math.round(Math.random()) == 0, Math.round(Math
                            .random()) == 0);
                    mLocations.add(newItem);
                    break;
                case MSG_CLEAR:
                    // Remove all objects
                    mLocations.clear();
                    break;
                case MSG_MOVE:
                    // Render a frame
                    Canvas c = mDrawingSurface.lockCanvas();
                    c.save();


                    if (c == null) {
                        break;
                    }
                    // Clear Canvas first
                    c.drawColor(Color.BLACK);

                    c.translate(dx*3,dy*3);
                    c.scale(zoom,zoom);
                    //c.getMatrix().postTranslate(dx,dy);
                    //c.getMatrix().preScale(2,2);

                    c.drawRect(1,1,mDrawingWidth-1, mDrawingHeight-1,bPaint);

                    // Draw each item
                    for (DrawingItem item : mLocations) {
                        // Update location
                        item.x += (item.horizontal ? 5 : -5);
                        if (item.x >= (mDrawingWidth - mIcon.getWidth())) {
                            item.horizontal = false;
                        } else if (item.x <= 0) {
                            item.horizontal = true;
                        }
                        item.y += (item.vertical ? 5 : -5);
                        if (item.y >= (mDrawingHeight - mIcon.getHeight())) {
                            item.vertical = false;
                        } else if (item.y <= 0) {
                            item.vertical = true;
                        }
                        // Draw to the Canvas
                        c.drawBitmap(mIcon, item.x, item.y, mPaint);
                    }


                    c.restore();



                    // Release to be rendered to the screen
                    mDrawingSurface.unlockCanvasAndPost(c);
                    break;
            }
            // Post the next frame
            mReceiver.sendEmptyMessage(MSG_MOVE);
            return true;
        }

        public void updateSize(int width, int height) {
            mDrawingWidth = width;
            mDrawingHeight = height;
        }

        public void addItem(int x, int y) {
            // Pass the location into the Handler using Message arguments
            Message msg = Message.obtain(mReceiver, MSG_ADD, x, y);
            mReceiver.sendMessage(msg);
        }

        public void clearItems() {
            mReceiver.sendEmptyMessage(MSG_CLEAR);
        }
    }
}
