package com.financemanager.app.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Floating bottom-bar container that also supports the Telegram-style gesture:
 * hold a tab and drag sideways to scrub through the main pages.
 */
public class BottomNavDock extends FrameLayout {

    public interface Listener {
        boolean canDrag();

        /** Which page sits under this screen X position. */
        int indexAt(float rawX);

        void onDragTo(int index);
    }

    private final int slop;
    @Nullable
    private Listener listener;
    private float downX;
    private float downY;
    private boolean dragging;

    public BottomNavDock(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean fire = false;
        int target = -1;
        boolean consume = false;
        int action = event.getActionMasked();

        if (listener != null && listener.canDrag() && event.getPointerCount() == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                downX = event.getRawX();
                downY = event.getRawY();
                dragging = false;
            } else if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - downX;
                float dy = event.getRawY() - downY;
                if (!dragging && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true;
                }
                if (dragging) {
                    fire = true;
                    target = listener.indexAt(event.getRawX());
                    consume = true;
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (dragging) {
                    consume = true;
                    dragging = false;
                }
            }
        } else {
            dragging = false;
        }

        boolean handled = super.dispatchTouchEvent(event);
        if (fire && target >= 0) {
            listener.onDragTo(target);
        }
        return consume || handled;
    }
}
