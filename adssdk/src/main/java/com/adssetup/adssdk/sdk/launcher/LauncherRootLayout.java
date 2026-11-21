package com.adssetup.adssdk.sdk.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class LauncherRootLayout extends FrameLayout {
    public interface OnDrawerStateListener {
        void onDrawerStateChanged(boolean open);
    }

    public interface GestureHandler {
        boolean isActive();

        void onEvent(MotionEvent ev);
    }

    public interface SearchSwipeHandler {
        boolean isContentAtTop();

        boolean consumeSwipeForKeyboard();
    }

    private static final float VERTICAL_BIAS = 1.5f;
    private SearchSwipeHandler searchSwipeHandler;
    private boolean swipeConsumed;

    public void setSearchSwipeHandler(@Nullable SearchSwipeHandler h) {
        searchSwipeHandler = h;
    }

    private GestureHandler gestureHandler;
    private boolean drawerGesturesBlocked;
    private boolean forwardAllTouch;

    public interface HomeScrollProvider {
        @Nullable
        View currentScroller();
    }

    private HomeScrollProvider homeScrollProvider;
    private boolean scrollerOwnsGesture;

    public void setHomeScrollProvider(@Nullable HomeScrollProvider p) {
        homeScrollProvider = p;
    }

    private boolean preserveDrawerScroll;
    private boolean drawerEnabled = true;
    private float lastRawX, lastRawY;

    public void setGestureHandler(@Nullable GestureHandler h) {
        gestureHandler = h;
    }

    public void setDrawerGesturesBlocked(boolean blocked) {
        drawerGesturesBlocked = blocked;
    }

    public void setForwardAllTouch(boolean on) {
        forwardAllTouch = on;
    }

    public void setPreserveDrawerScroll(boolean on) {
        preserveDrawerScroll = on;
        if (!on && !open && progress <= 0f && drawerList != null) drawerList.scrollToPosition(0);
    }

    public float getLastRawX() {
        return lastRawX;
    }

    public float getLastRawY() {
        return lastRawY;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        lastRawX = ev.getRawX();
        lastRawY = ev.getRawY();
        if (forwardAllTouch && gestureHandler != null && gestureHandler.isActive() && ev.getActionMasked() != MotionEvent.ACTION_DOWN) {
            gestureHandler.onEvent(ev);
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private final int touchSlop;
    private final float flingVelocity;
    private View drawer;
    private RecyclerView drawerList;
    private View homeContent;
    private OnDrawerStateListener stateListener;
    private float progress;
    private boolean open;
    private boolean dragging;
    private boolean startedAtTop;
    private float downX, downY, dragStartY, dragBaseProgress;
    private VelocityTracker tracker;
    private ValueAnimator animator;

    public LauncherRootLayout(@NonNull Context c, @Nullable AttributeSet a) {
        super(c, a);
        ViewConfiguration vc = ViewConfiguration.get(c);
        touchSlop = vc.getScaledTouchSlop();
        flingVelocity = 250f * c.getResources().getDisplayMetrics().density;
    }

    public void bindDrawer(@NonNull View drawer, @NonNull RecyclerView list, @Nullable View home, @Nullable OnDrawerStateListener listener) {
        this.drawer = drawer;
        this.drawerList = list;
        this.homeContent = home;
        this.stateListener = listener;
        applyProgress(progress);
    }

    public void setDrawerEnabled(boolean enabled) {
        drawerEnabled = enabled;
        if (!enabled && drawer != null) {
            if (animator != null) animator.cancel();
            open = false;
            applyProgress(0f);
            drawer.setVisibility(GONE);
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void openDrawer(boolean animate) {
        settle(true, animate, 0f);
    }

    public void closeDrawer(boolean animate) {
        settle(false, animate, 0f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        applyProgress(progress);
    }

    private void applyProgress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        if (drawer == null) return;
        drawer.setTranslationY((1f - progress) * getHeight());
        if (progress <= 0f) {
            if (drawer.getVisibility() == VISIBLE && drawerList != null && !preserveDrawerScroll) {
                drawerList.scrollToPosition(0);
            }
            drawer.setVisibility(drawerEnabled ? INVISIBLE : GONE);
        } else {
            drawer.setVisibility(VISIBLE);
        }
        if (homeContent != null) {
            float a = Math.max(0f, 1f - progress * 1.5f);
            homeContent.setAlpha(a);
            homeContent.setVisibility(a <= 0f ? INVISIBLE : VISIBLE);
        }
    }

    private void settle(boolean toOpen, boolean animate, float unusedVelocity) {
        if (drawer == null || (toOpen && !drawerEnabled)) return;
        if (animator != null) animator.cancel();
        final boolean changed = open != toOpen;
        open = toOpen;
        float target = toOpen ? 1f : 0f;
        if (!animate || getHeight() == 0) {
            applyProgress(target);
            if (changed && stateListener != null) stateListener.onDrawerStateChanged(open);
            return;
        }
        if (changed && stateListener != null) stateListener.onDrawerStateChanged(open);
        float distance = Math.abs(target - progress);
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration((long) (120 + 220 * distance));
        animator.setInterpolator(new DecelerateInterpolator(1.6f));
        animator.addUpdateListener(a -> applyProgress((float) a.getAnimatedValue()));
        drawer.setLayerType(LAYER_TYPE_HARDWARE, null);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                drawer.setLayerType(LAYER_TYPE_NONE, null);
            }
        });
        animator.start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (gestureHandler != null && gestureHandler.isActive()) {
            gestureHandler.onEvent(ev);
            return true;
        }
        if (drawer == null || drawerGesturesBlocked || !drawerEnabled) return false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetTracker();
                tracker.addMovement(ev);
                downX = ev.getX();
                downY = ev.getY();
                swipeConsumed = false;
            {
                View sc = homeScrollProvider != null ? homeScrollProvider.currentScroller() : null;
                scrollerOwnsGesture = sc != null;
            }
            startedAtTop = searchSwipeHandler != null ? searchSwipeHandler.isContentAtTop() : drawerList == null || !drawerList.canScrollVertically(-1);
            if (animator != null && animator.isRunning()) {
                animator.cancel();
                beginDrag(ev.getY());
                return true;
            }
            dragging = false;
            return false;
            case MotionEvent.ACTION_MOVE:
                if (tracker != null) tracker.addMovement(ev);
                if (dragging) return true;
                float dy = ev.getY() - downY;
                float dx = ev.getX() - downX;
                if (!open) {
                    if (!scrollerOwnsGesture && dy < -touchSlop && Math.abs(dy) > Math.abs(dx) * VERTICAL_BIAS) {
                        beginDrag(ev.getY());
                        return true;
                    }
                } else if (startedAtTop && dy > touchSlop && dy > Math.abs(dx) * VERTICAL_BIAS) {
                    if (searchSwipeHandler != null) {
                        if (swipeConsumed) return true;
                        if (searchSwipeHandler.consumeSwipeForKeyboard()) {
                            swipeConsumed = true;
                            return true;
                        }
                    }
                    beginDrag(ev.getY());
                    return true;
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                recycleTracker();
                return false;
            default:
                return false;
        }
    }

    private void beginDrag(float y) {
        dragging = true;
        dragStartY = y;
        dragBaseProgress = progress;
        drawer.setLayerType(LAYER_TYPE_HARDWARE, null);
        getParent().requestDisallowInterceptTouchEvent(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (gestureHandler != null && gestureHandler.isActive()) {
            gestureHandler.onEvent(ev);
            return true;
        }
        if (!dragging) return super.onTouchEvent(ev);
        if (tracker == null) resetTracker();
        tracker.addMovement(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                int h = Math.max(1, getHeight());
                applyProgress(dragBaseProgress + (dragStartY - ev.getY()) / h);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tracker.computeCurrentVelocity(1000);
                float vy = tracker.getYVelocity();
                dragging = false;
                recycleTracker();
                boolean toOpen;
                if (vy < -flingVelocity) toOpen = true;
                else if (vy > flingVelocity) toOpen = false;
                else toOpen = progress > 0.5f;
                settle(toOpen, true, vy);
                return true;
            default:
                return true;
        }
    }

    private void resetTracker() {
        if (tracker == null) tracker = VelocityTracker.obtain();
        else tracker.clear();
    }

    private void recycleTracker() {
        if (tracker != null) {
            tracker.recycle();
            tracker = null;
        }
    }
}