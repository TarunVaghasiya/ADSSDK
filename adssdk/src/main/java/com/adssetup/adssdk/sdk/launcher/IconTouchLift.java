package com.adssetup.adssdk.sdk.launcher;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;

final class IconTouchLift {
    private static final float LIFT_DP = 5f;
    private static final long UP_MS = 130;
    private static final long DOWN_MS = 190;
    private static final Interpolator EASE = new DecelerateInterpolator(1.6f);

    private IconTouchLift() {
    }

    @SuppressLint("ClickableViewAccessibility")
    static void attach(@NonNull View cell, @NonNull View icon) {
        final float lift = -LIFT_DP * cell.getResources().getDisplayMetrics().density;
        cell.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    animateTo(icon, lift, UP_MS);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animateTo(icon, 0f, DOWN_MS);
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    static void reset(@NonNull View icon) {
        icon.animate().cancel();
        icon.setTranslationY(0f);
    }

    private static void animateTo(View icon, float ty, long ms) {
        icon.animate().cancel();
        icon.animate().translationY(ty).setDuration(ms).setInterpolator(EASE).start();
    }
}