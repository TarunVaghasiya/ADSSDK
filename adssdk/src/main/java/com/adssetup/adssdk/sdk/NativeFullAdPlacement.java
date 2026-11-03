package com.adssetup.adssdk.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;

import java.lang.ref.WeakReference;
import java.util.Collections;

@SuppressLint("StaticFieldLeak")
public class NativeFullAdPlacement implements DefaultLifecycleObserver {
    private static final long TIMEOUT_MS = 10_000;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static NativeFullAdPlacement active;

    private final WeakReference<Activity> activityReference;
    private final String adUnitId;
    private final Runnable timeout = this::showFallback;
    private Dialog dialog;
    private FrameLayout root;
    private ShimmerFrameLayout shimmer;
    private NativeAd nativeAd;
    private boolean contentShown;
    private boolean finished;

    private NativeFullAdPlacement(@NonNull Activity activity, @NonNull String adUnitId) {
        this.activityReference = new WeakReference<>(activity);
        this.adUnitId = adUnitId;
    }

    public static void show(@NonNull Activity activity, @NonNull String adUnitId) {
        if (active != null || !isUsable(activity) || !AdsController.isAdsEnabled()) {
            return;
        }
        NativeFullAdPlacement placement = new NativeFullAdPlacement(activity, adUnitId);
        active = placement;
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(placement);
        }
        placement.open(activity);
        if (AdsController.isQuizPriority()) {
            placement.showQuiz();
            return;
        }
        placement.showShimmer(activity);
        MAIN.postDelayed(placement.timeout, TIMEOUT_MS);
        SdkAuthorization.authorize(activity, placement::onAuthorized);
    }

    private static boolean isUsable(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        return !(activity instanceof LifecycleOwner) || ((LifecycleOwner) activity).getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    private void open(@NonNull Activity activity) {
        root = new FrameLayout(activity);
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setCancelable(true);
        dialog.setOnDismissListener(d -> finish());
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(activity, R.color.main_background)));
        }
        dialog.show();
    }

    private void showShimmer(@NonNull Activity activity) {
        shimmer = (ShimmerFrameLayout) LayoutInflater.from(activity).inflate(R.layout.shimmer_layout, root, false);
        shimmer.setShimmer(new Shimmer.AlphaHighlightBuilder().setDuration(1200).build());
        root.addView(shimmer);
        shimmer.startShimmer();
    }

    private void hideShimmer() {
        if (shimmer != null) {
            shimmer.stopShimmer();
            root.removeView(shimmer);
            shimmer = null;
        }
    }

    private void onAuthorized(boolean granted) {
        Activity activity = activityReference.get();
        if (finished || contentShown) {
            return;
        }
        if (!granted || !AdsController.isAdsEnabled() || !isUsable(activity)) {
            close();
            return;
        }
        NativeAdRequest request = new NativeAdRequest.Builder(adUnitId, Collections.singletonList(NativeAd.NativeAdType.NATIVE)).setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT).build();
        NativeAdLoader.load(request, new NativeAdLoaderCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd ad) {
                MainThread.run(() -> onLoaded(ad));
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                MainThread.run(() -> showFallback());
            }
        });
    }

    private void onLoaded(@NonNull NativeAd ad) {
        Activity activity = activityReference.get();
        if (finished || contentShown || !AdsController.isAdsEnabled() || !isUsable(activity)) {
            ad.destroy();
            if (!finished && !contentShown) {
                close();
            }
            return;
        }
        try {
            NativeAdView adView = NativeAdPlacement.inflateAd(root, ad, R.layout.native_full_ad_layout);
            View close = adView.findViewById(R.id.ivClose);
            if (close != null) {
                close.setOnClickListener(v -> close());
            }
            nativeAd = ad;
            display(adView);
        } catch (RuntimeException e) {
            ad.destroy();
            showFallback();
        }
    }

    private void showFallback() {
        if (finished || contentShown) {
            return;
        }
        if (!AdsController.isAdsEnabled() || !AdsController.isQuizFallbackEnabled()) {
            close();
            return;
        }
        showQuiz();
    }

    private void showQuiz() {
        Activity activity = activityReference.get();
        if (finished || contentShown) {
            return;
        }
        if (!AdsController.isAdsEnabled() || !isUsable(activity)) {
            close();
            return;
        }
        try {
            View quiz = LayoutInflater.from(activity).inflate(R.layout.qz_full_ad_layout, root, false);
            AdsController.bindQuizDesign(quiz);
            View close = quiz.findViewById(R.id.ivQZClose);
            if (close != null) {
                close.setVisibility(View.VISIBLE);
                close.setOnClickListener(v -> close());
            }
            display(quiz);
        } catch (RuntimeException e) {
            close();
        }
    }

    private void display(@NonNull View content) {
        MAIN.removeCallbacks(timeout);
        hideShimmer();
        contentShown = true;
        root.addView(content);
    }

    private void close() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        } else {
            finish();
        }
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        if (active == this) {
            active = null;
        }
        MAIN.removeCallbacks(timeout);
        hideShimmer();
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
        if (dialog != null) {
            dialog.setOnDismissListener(null);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = null;
        }
        Activity activity = activityReference.get();
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().removeObserver(this);
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        finish();
    }
}