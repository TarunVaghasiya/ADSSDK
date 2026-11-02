package com.adssetup.adssdk.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class AppOpenAdPlacement implements DefaultLifecycleObserver {
    public static final String MODE_PRELOAD = "preload";

    public interface Callback {
        void onFinished(boolean shown);
    }

    private static final long TIMEOUT_MS = 10_000;
    private static final long CACHE_EXPIRY_MS = 3 * 60 * 60 * 1000L;
    private static final long RETRY_DELAY_MS = 10_000;
    private static final int PRELOAD_RETRIES = 1;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, AppOpenAd> CACHE = new HashMap<>();
    private static final Map<String, Long> CACHED_AT = new HashMap<>();
    private static boolean inFlight;
    private static boolean preloading;
    private static AppOpenAdPlacement waitingForPreload;

    private final WeakReference<Activity> activityReference;
    private final Context appContext;
    private final String adUnitId;
    private final Callback callback;
    private final Runnable timeout = this::showFallback;
    private Dialog dialog;
    private QuizFallbackDialog fallback;
    private AppOpenAd appOpenAd;
    private boolean fromCache;
    private boolean finished;

    private AppOpenAdPlacement(@NonNull Activity activity, @NonNull String adUnitId, @Nullable Callback callback) {
        this.activityReference = new WeakReference<>(activity);
        this.appContext = activity.getApplicationContext();
        this.adUnitId = adUnitId;
        this.callback = callback;
    }

    public static void loadAppOpenAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull String mode, @Nullable Callback callback) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        boolean preloadMode = MODE_PRELOAD.equals(mode);
        if (preloadMode && callback == null) {
            preload(activity.getApplicationContext(), adUnitId, PRELOAD_RETRIES);
            return;
        }
        if (inFlight) {
            return;
        }
        inFlight = true;
        AppOpenAdPlacement placement = new AppOpenAdPlacement(activity, adUnitId, callback);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(placement);
        }
        MAIN.postDelayed(placement.timeout, TIMEOUT_MS);
        AppOpenAd cached = preloadMode ? takeCachedAd(adUnitId, true) : null;
        if (cached != null) {
            placement.show(activity, cached, true);
        } else {
            SdkAuthorization.authorize(activity, granted -> placement.onAuthorized(granted, preloadMode));
        }
    }

    private static void preload(@NonNull Context appContext, @NonNull String adUnitId, int retriesLeft) {
        if (preloading || takeCachedAd(adUnitId, false) != null) {
            return;
        }
        preloading = true;
        SdkAuthorization.authorize(appContext, granted -> {
            if (!granted) {
                preloading = false;
                handOverToWaiting(false);
                return;
            }
            AppOpenAd.load(new AdRequest.Builder(adUnitId).build(), new AdLoadCallback<>() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    MainThread.run(() -> {
                        preloading = false;
                        CACHE.put(adUnitId, ad);
                        CACHED_AT.put(adUnitId, SystemClock.elapsedRealtime());
                        handOverToWaiting(true);
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    MainThread.run(() -> {
                        preloading = false;
                        handOverToWaiting(false);
                        if (retriesLeft > 0) {
                            MAIN.postDelayed(() -> preload(appContext, adUnitId, retriesLeft - 1), RETRY_DELAY_MS);
                        }
                    });
                }
            });
        });
    }

    private static void handOverToWaiting(boolean loaded) {
        AppOpenAdPlacement waiting = waitingForPreload;
        waitingForPreload = null;
        if (waiting == null || waiting.finished) {
            return;
        }
        Activity activity = waiting.activityReference.get();
        AppOpenAd ad = loaded ? takeCachedAd(waiting.adUnitId, true) : null;
        if (ad != null && isUsable(activity)) {
            waiting.show(activity, ad, true);
        } else {
            waiting.requestAd();
        }
    }

    @Nullable
    private static AppOpenAd takeCachedAd(@NonNull String adUnitId, boolean remove) {
        AppOpenAd ad = CACHE.get(adUnitId);
        Long cachedAt = CACHED_AT.get(adUnitId);
        if (ad == null || cachedAt == null) {
            return null;
        }
        boolean expired = SystemClock.elapsedRealtime() - cachedAt >= CACHE_EXPIRY_MS;
        if (remove || expired) {
            CACHE.remove(adUnitId);
            CACHED_AT.remove(adUnitId);
        }
        return expired ? null : ad;
    }

    private void onAuthorized(boolean granted, boolean preloadMode) {
        Activity activity = activityReference.get();
        if (finished) {
            return;
        }
        if (!granted || !isUsable(activity)) {
            finish(false);
            return;
        }
        showDialog(activity);
        if (preloadMode && preloading) {
            waitingForPreload = this;
            return;
        }
        AppOpenAd cached = preloadMode ? takeCachedAd(adUnitId, true) : null;
        if (cached != null) {
            show(activity, cached, true);
        } else {
            requestAd();
        }
    }

    private void requestAd() {
        Activity activity = activityReference.get();
        if (finished || !isUsable(activity)) {
            finish(false);
            return;
        }
        AppOpenAd.load(new AdRequest.Builder(adUnitId).build(), new AdLoadCallback<>() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                MainThread.run(() -> {
                    Activity host = activityReference.get();
                    if (finished || !isUsable(host)) {
                        return;
                    }
                    show(host, ad, false);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                MainThread.run(() -> showFallback());
            }
        });
    }

    private void show(@NonNull Activity host, @NonNull AppOpenAd ad, boolean cached) {
        appOpenAd = ad;
        fromCache = cached;
        ad.setAdEventCallback(new AppOpenAdEventCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                MainThread.run(() -> finish(true));
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                MainThread.run(() -> finish(false));
            }
        });
        dismissFallback();
        dismissDialog();
        MAIN.removeCallbacks(timeout);
        ad.show(host);
    }

    private static boolean isUsable(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        return !(activity instanceof LifecycleOwner) || ((LifecycleOwner) activity).getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    private void showFallback() {
        if (finished || fallback != null) {
            return;
        }
        Activity activity = activityReference.get();
        if (!isUsable(activity)) {
            finish(false);
            return;
        }
        dismissDialog();
        fallback = new QuizFallbackDialog(activity, R.layout.qz_app_open_ad_layout, R.id.llQZClose, Color.TRANSPARENT, () -> {
            fallback = null;
            finish(false);
        });
        fallback.show();
    }

    private void dismissFallback() {
        if (fallback != null) {
            fallback.dismiss();
            fallback = null;
        }
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
        if (waitingForPreload == this) {
            waitingForPreload = null;
        }
        MAIN.removeCallbacks(timeout);
        dismissDialog();
        dismissFallback();
        Activity activity = activityReference.get();
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().removeObserver(this);
        }
        if (appOpenAd != null) {
            appOpenAd.setAdEventCallback(null);
            appOpenAd = null;
        }
        if (fromCache) {
            preload(appContext, adUnitId, PRELOAD_RETRIES);
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