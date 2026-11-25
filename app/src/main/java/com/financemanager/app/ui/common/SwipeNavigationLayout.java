package com.financemanager.app.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

/**
 * Detects horizontal flings over the navigation host and asks its listener to move
 * to the neighbouring top-level page. Vertical scrolling is left untouched, and
 * gestures that start on a horizontally scrollable widget (or a text/slider control)
 * are ignored so those controls keep their normal behaviour.
 */
public class SwipeNavigationLayout extends FrameLayout {

    /** Listener decides whether a swipe is currently allowed and handles it. */
    public interface Listener {
        boolean canSwipe();

        /** @param next true to move one page forward, false to move one page back. */
        void onSwipe(boolean next);
    }

    private final VelocityTracker velocityTracker = VelocityTracker.obtain();
    private final int touchSlop;
    private final int minDistance;
    private final int minVelocity;

    @Nullable
    private Listener listener;
    private boolean tracking;
    private float downX;
    private float downY;
    private long downTime;

    public SwipeNavigationLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration config = ViewConfiguration.get(context);
        touchSlop = config.getScaledTouchSlop();
        minDistance = touchSlop * 4;
        minVelocity = Math.max(config.getScaledMinimumFlingVelocity(), 350);
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean fire = false;
        boolean next = false;
        int action = event.getActionMasked();

        if (listener != null && listener.canSwipe() && event.getPointerCount() == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                velocityTracker.clear();
                velocityTracker.addMovement(event);
                downX = event.getX();
                downY = event.getY();
                downTime = event.getEventTime();
                tracking = true;
            } else if (tracking) {
                velocityTracker.addMovement(event);
                if (action == MotionEvent.ACTION_UP) {
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    velocityTracker.computeCurrentVelocity(1000);
                    float vx = velocityTracker.getXVelocity();
                    long elapsed = event.getEventTime() - downTime;
                    boolean horizontal = Math.abs(dx) > minDistance
                            && Math.abs(dx) > Math.abs(dy) * 1.5f;
                    if (horizontal && elapsed > 0 && Math.abs(vx) > minVelocity
                            && !blocksSwipe(event.getRawX(), event.getRawY())) {
                        fire = true;
                        next = dx < 0;
                    }
                    tracking = false;
                } else if (action == MotionEvent.ACTION_CANCEL) {
                    tracking = false;
                }
            }
        } else {
            tracking = false;
        }

        boolean handled = super.dispatchTouchEvent(event);
        if (fire) {
            listener.onSwipe(next);
        }
        return handled;
    }

    /** True when the view under the gesture wants horizontal touches for itself. */
    private boolean blocksSwipe(float rawX, float rawY) {
        return blocksSwipe(this, rawX, rawY);
    }

    private boolean blocksSwipe(ViewGroup group, float rawX, float rawY) {
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            int[] location = new int[2];
            child.getLocationInWindow(location);
            if (rawX < location[0] || rawX > location[0] + child.getWidth()
                    || rawY < location[1] || rawY > location[1] + child.getHeight()) {
                continue;
            }
            if (wantsHorizontalTouches(child)
                    || (child instanceof ViewGroup && blocksSwipe((ViewGroup) child, rawX, rawY))) {
                return true;
            }
        }
        return false;
    }

    private static boolean wantsHorizontalTouches(View view) {
        return view instanceof EditText
                || view instanceof SeekBar
                || view instanceof Slider
                || view.canScrollHorizontally(1)
                || view.canScrollHorizontally(-1);
    }

}
