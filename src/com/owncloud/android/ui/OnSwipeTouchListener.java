package com.owncloud.android.ui;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

public class OnSwipeTouchListener implements OnTouchListener {

    private final Context mContext;
    private final GestureDetector mGestureDetector;
    
    public OnSwipeTouchListener(Context context) {
        mContext = context;
        mGestureDetector = new GestureDetector(context, new GestureListener());
    }

    public boolean onTouch(final View v, final MotionEvent event) {
        //super.onTouch(v, event);
        Log.d("SWIPE", "Swipe listener touched");
        return mGestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    public void onSwipeTop() {
        Toast.makeText(mContext, "top", Toast.LENGTH_SHORT).show();
    }
    public void onSwipeRight() {
        Toast.makeText(mContext, "right", Toast.LENGTH_SHORT).show();
    }
    public void onSwipeLeft() {
        Toast.makeText(mContext, "left", Toast.LENGTH_SHORT).show();
    }
    public void onSwipeBottom() {
        Toast.makeText(mContext, "bottom", Toast.LENGTH_SHORT).show();
    }
    
}    

