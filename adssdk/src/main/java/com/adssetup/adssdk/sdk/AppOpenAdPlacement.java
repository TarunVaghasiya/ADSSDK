package com.adssetup.adssdk.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback;

import java.lang.ref.WeakReference;

public class AppOpenAdPlacement implements DefaultLifecycleObserver {
    private static final long TIMEOUT_MS = 10_000;
    private static final AdRequest AD_REQUEST = new AdRequest.Builder().build();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static boolean inFlight;

    private final WeakReference<Activity> activityReference;
    private final String adUnitId;
    private final Callback callback;
    private final Runnable timeout = () -> finish(false);
    private Dialog dialog;
    private AppOpenAd appOpenAd;
    private boolean finished;

    public interface Callback {
        void onFinished(boolean shown);
    }

    private AppOpenAdPlacement(@NonNull Activity activity, @NonNull String adUnitId, @Nullable Callback callback) {
        this.activityReference = new WeakReference<>(activity);
        this.adUnitId = adUnitId;
        this.callback = callback;
    }

    public static void loadAppOpenAd(@NonNull Activity activity, @NonNull String adUnitId, @Nullable Callback callback) {
        if (inFlight || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        inFlight = true;
        AppOpenAdPlacement placement = new AppOpenAdPlacement(activity, adUnitId, callback);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(placement);
        }
        MAIN.postDelayed(placement.timeout, TIMEOUT_MS);
        SdkAuthorization.authorize(activity, placement::onAuthorized);
    }

    private void onAuthorized(boolean granted) {
        Activity activity = activityReference.get();
        if (finished) {
            return;
        }
        if (!granted || activity == null || activity.isFinishing() || activity.isDestroyed()) {
            finish(false);
            return;
        }
        if (dialog == null) {
            showDialog(activity);
        }
        AppOpenAd.load(activity, adUnitId, AD_REQUEST, new AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Activity host = activityReference.get();
                if (finished || host == null || host.isFinishing() || host.isDestroyed()) {
                    return;
                }
                appOpenAd = ad;
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        finish(true);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                        finish(false);
                    }
                });
                dismissDialog();
                MAIN.removeCallbacks(timeout);
                ad.show(host);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                finish(false);
            }
        });
    }

    @SuppressLint("InflateParams")
    private void showDialog(@NonNull Activity activity) {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(LayoutInflater.from(activity).inflate(R.layout.loading_dialog, null));
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void dismissDialog() {
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = null;
        }
    }

    private void finish(boolean shown) {
        if (finished) {
            return;
        }
        finished = true;
        inFlight = false;
        MAIN.removeCallbacks(timeout);
        dismissDialog();
        Activity activity = activityReference.get();
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().removeObserver(this);
        }
        if (appOpenAd != null) {
            appOpenAd.setFullScreenContentCallback(null);
            appOpenAd = null;
        }
        if (callback != null) {
            callback.onFinished(shown);
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        finish(false);
    }
}