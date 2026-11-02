package com.adssetup.adssdk.sdk;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

final class MainThread {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private MainThread() {
    }

    static void run(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN.post(runnable);
        }
    }
}