
package org.thismetalsky.multitouchbugtest;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.lang.Math;
import java.util.Random;

public class TouchTestView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "MultitouchTest";

    // we support this many pointers in multitouch
    private static final int MAX_POINTER_COUNT = 5;

    private GameThread     thread;
    private Random randomizer;

    private Paint paintTouch[] = new Paint[MAX_POINTER_COUNT];
    private Paint paintText = new Paint();

    private float originX       = 0f;
    private float originY       = 0f;
    private int   canvasWidth   = 0;
    private int   canvasHeight  = 0;

    // for touch events
    private Touch touch[] = new Touch[MAX_POINTER_COUNT];
    private boolean stupid = false;
    private boolean swapTouch = false;
    private int     lastDown = 0;

    public TouchTestView(Context context) {
        super(context);

        randomizer = new Random();

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // set up the thread
        thread = new GameThread(holder, context, this, new Handler() {
            @Override
            public void handleMessage(Message m) {
                Log.d(TAG, m.getData().getString("text"));
            }
        });

        // setup the paints
        paintTouch[0] = new Paint();
        paintTouch[0].setColor(context.getResources().getColor(R.color.touchA));
        paintTouch[1] = new Paint();
        paintTouch[1].setColor(context.getResources().getColor(R.color.touchB));
        paintTouch[2] = new Paint();
        paintTouch[2].setColor(context.getResources().getColor(R.color.touchC));
        paintTouch[3] = new Paint();
        paintTouch[3].setColor(context.getResources().getColor(R.color.touchD));
        paintTouch[4] = new Paint();
        paintTouch[4].setColor(context.getResources().getColor(R.color.touchE));
        for (Paint p: paintTouch) {
            p.setAntiAlias(true);
            p.setStrokeWidth(7.5f);
            p.setStyle(Paint.Style.STROKE);
        }

        // line paint
        paintText.setColor(context.getResources().getColor(R.color.line));
        paintText.setAntiAlias(true);
        paintText.setTextSize(52f);

        setClickable(true);
        setFocusable(true);

        // setup our touch event objects
        for (int i=0; i < MAX_POINTER_COUNT; i++) {
            touch[i] = new Touch();
        }
    }

    // surface holder callbacks

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        canvasWidth     = width;
        canvasHeight    = height;
        originX         = canvasWidth/2;
        originY         = canvasHeight/2;
    }
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setReady();
        thread.start();
    }
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;

        thread.setNotReady();
        while (retry) {
            try {
                thread.join();
                retry = false;
            }
            catch (InterruptedException e) {}
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                int pointerId = event.getPointerId(0);
                touch[0].active = true;
                touch[0].x = event.getX(0);
                touch[0].y = event.getY(0);
                break;

            case MotionEvent.ACTION_UP:
                for (Touch t : touch) {
                    t.active = false;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                pointerId = event.getPointerId(pointerIndex);
                if (pointerIndex < MAX_POINTER_COUNT)
                    touch[pointerIndex].active = false;
                break;

            // second pointer down
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                pointerId = event.getPointerId(pointerIndex);
                if (pointerIndex < MAX_POINTER_COUNT) {
                    touch[pointerIndex].active = true;
                    touch[pointerIndex].x = event.getX(pointerIndex);
                    touch[pointerIndex].y = event.getY(pointerIndex);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

                // less than max pointers, only do the active ones
                if (event.getPointerCount() < MAX_POINTER_COUNT) {
                    int pointer = 0;
                    for (int i=0; i < MAX_POINTER_COUNT ; i++) {
                        if (pointer >= event.getPointerCount())
                            break;

                        if (touch[i].active) {
                            touch[i].x = event.getX(pointer);
                            touch[i].y = event.getY(pointer);
                            pointer++;
                        }
                    }
                }

                // multiple pointers, do the right thing
                else {
                    for (int i=0; (i < event.getPointerCount()) && (i < MAX_POINTER_COUNT); i++) {
                        touch[i].x = event.getX(i);
                        touch[i].y = event.getY(i);
                    }
                }
                break;
        }

        return super.onTouchEvent(event);
    }


    // class for touch events
    class Touch {
        public float x = 0f;
        public float y = 0f;
        public boolean active = false;
    }

    public GameThread getThread() { return thread; }

    class GameThread extends Thread {

        private Handler             handler;
        private SurfaceHolder       surfaceHolder;
        private TouchTestView       view;
        private Context             context;

        private boolean             ready = false;
        private boolean             running = false;

        public GameThread(SurfaceHolder s, Context c, TouchTestView v, Handler h) {
            context         = c;
            handler         = h;
            surfaceHolder   = s;
            view            = v;
        }

        @Override
        public void run() {
            while (ready) {
                Canvas c = null;
                try {
                    c = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) { 
                        if (running == true)
                            doDraw(c);
                    }
                }
                finally {
                    if (c != null)
                        surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }

        // thread interaction stuff
        public void setReady() {
            synchronized (surfaceHolder) {
                ready = true;
            }
        }
        public void setNotReady() {
            synchronized (surfaceHolder) {
                ready = false;
            }
        }
        public void setPaused() {
            synchronized (surfaceHolder) {
                if (running)
                    running = false;
            }
        }
        public void setRunning() {
            synchronized (surfaceHolder) {
                running = true;
            }
        }


        public void doStart() {
            synchronized (surfaceHolder) {

                running = true;
            }
        }

        // graphics time
        public void doDraw(Canvas canvas) {
            // clear screen
            canvas.drawColor(Color.BLACK);
            // draw where the pointer is
        
            for (int i=0; i < MAX_POINTER_COUNT; i++) {
                if (touch[i].active) {
                    canvas.drawCircle(touch[i].x, touch[i].y, 75f, paintTouch[i]);
                    drawCenteredText(canvas, new PointF(touch[i].x, touch[i].y - 90f),
                                        paintText, Integer.toString(i + 1));
                }
            }
        }

        // screen coordinate to cartesian
        private PointF s2c(float x, float y) {
            return new PointF(- (originX - x), originY - y);
        }
        // cartesian to screen
        private PointF c2s(float x, float y) {
            return new PointF(originX + x, originY - y);
        }
    }

    // draw text centered on a point
    private void drawCenteredText(Canvas canvas, PointF loc, Paint paint, String text) {
        float width = paint.measureText(text) / 2;
        canvas.drawText(text, loc.x - width, loc.y, paint);
    }
}
