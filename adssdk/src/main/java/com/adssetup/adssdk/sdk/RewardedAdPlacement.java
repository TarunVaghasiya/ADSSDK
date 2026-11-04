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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;

import java.lang.ref.WeakReference;

public class RewardedAdPlacement implements DefaultLifecycleObserver {
    public interface Callback {
        void onRewardEarned(int amount, @NonNull String type);

        void onFinished(boolean shown);
    }

    private static final long TIMEOUT_MS = 15_000;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static boolean inFlight;

    private final WeakReference<Activity> activityReference;
    private final String adUnitId;
    private final Callback callback;
    private final Runnable timeout = () -> finish(false);
    private Dialog dialog;
    private RewardedAd rewardedAd;
    private boolean finished;

    private RewardedAdPlacement(@NonNull Activity activity, @NonNull String adUnitId, @Nullable Callback callback) {
        this.activityReference = new WeakReference<>(activity);
        this.adUnitId = adUnitId;
        this.callback = callback;
    }

    public static boolean isLoading() {
        return inFlight;
    }

    public static void loadRewardedAd(@NonNull Activity activity, @NonNull String adUnitId, @Nullable Callback callback) {
        if (inFlight) {
            return;
        }
        if (adUnitId.trim().isEmpty() || !AdsController.isAdsEnabled() || !isUsable(activity)) {
            if (callback != null) {
                callback.onFinished(false);
            }
            return;
        }
        inFlight = true;
        RewardedAdPlacement placement = new RewardedAdPlacement(activity, adUnitId, callback);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(placement);
        }
        placement.showDialog(activity);
        MAIN.postDelayed(placement.timeout, TIMEOUT_MS);
        SdkAuthorization.authorize(activity, placement::onAuthorized);
    }

    private void onAuthorized(boolean granted) {
        Activity activity = activityReference.get();
        if (finished) {
            return;
        }
        if (!granted || !AdsController.isAdsEnabled() || !isUsable(activity)) {
            finish(false);
            return;
        }
        RewardedAd.load(new AdRequest.Builder(adUnitId).build(), new AdLoadCallback<>() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                MainThread.run(() -> {
                    Activity host = activityReference.get();
                    if (finished || !isUsable(host)) {
                        return;
                    }
                    show(host, ad);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                MainThread.run(() -> finish(false));
            }
        });
    }

    private void show(@NonNull Activity host, @NonNull RewardedAd ad) {
        rewardedAd = ad;
        ad.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                MainThread.run(() -> finish(true));
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                MainThread.run(() -> finish(false));
            }
        });
        MAIN.removeCallbacks(timeout);
        dismissDialog();
        ad.show(host, reward -> MainThread.run(() -> {
            if (callback != null) {
                callback.onRewardEarned(reward.getAmount(), reward.getType());
            }
        }));
    }

    private static boolean isUsable(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        return !(activity instanceof LifecycleOwner) || ((LifecycleOwner) activity).getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
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
        if (rewardedAd != null) {
            rewardedAd.setAdEventCallback(null);
            rewardedAd = null;
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