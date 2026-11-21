package com.adssetup.adssdk.sdk;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;

import java.lang.ref.WeakReference;

public class BannerAdPlacement {
    private WeakReference<BannerAdCustomView> containerReference;
    private AdView adView;
    private boolean destroyed;
    private BannerAdPlacement previous;

    public static void loadBannerAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull BannerAdCustomView bannerView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!AdsController.isAdsEnabled()) {
            bannerView.setVisibility(View.GONE);
            return;
        }
        if (AdsController.isQuizPriority()) {
            bannerView.showQuiz();
            return;
        }
        bannerView.load(adUnitId);
    }

    public static void refreshBannerAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull BannerAdCustomView bannerView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!AdsController.isAdsEnabled()) {
            bannerView.setVisibility(View.GONE);
            return;
        }
        if (AdsController.isQuizPriority()) {
            bannerView.showQuiz();
            return;
        }
        bannerView.refresh(adUnitId);
    }

    public static void refreshBannerAdKeepingCurrent(@NonNull Activity activity, @NonNull String adUnitId, @NonNull BannerAdCustomView bannerView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!AdsController.isAdsEnabled()) {
            bannerView.setVisibility(View.GONE);
            return;
        }
        if (AdsController.isQuizPriority()) {
            bannerView.showQuiz();
            return;
        }
        bannerView.softRefresh(adUnitId);
    }

    boolean hasAd() {
        return !destroyed && adView != null && adView.getVisibility() == View.VISIBLE;
    }

    void setAdVisible(boolean visible) {
        if (adView != null) {
            adView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    void setPrevious(@Nullable BannerAdPlacement placement) {
        previous = placement;
    }

    /** Drops this placement's own ad only, leaving the container's other children alone. */
    private void release() {
        destroyed = true;
        BannerAdCustomView container = containerReference == null ? null : containerReference.get();
        if (container != null && adView != null) {
            container.removeView(adView);
        }
        releaseAdView();
        containerReference = null;
    }

    /** The refresh did not produce a new ad: stop the shimmer and bring the previous ad back. */
    void softFail(@NonNull BannerAdCustomView container) {
        BannerAdPlacement old = previous;
        previous = null;
        if (adView != null) {
            container.removeView(adView);
            releaseAdView();
        }
        destroyed = true;
        container.hideShimmer();
        if (old != null) {
            container.adoptPlacement(old);
            old.setAdVisible(true);
        }
        container.notifyLoadResult(false);
    }

    @NonNull
    static AdSize adSizeFor(@NonNull Context context, @NonNull BannerAdCustomView.Size size, int widthDp) {
        if (size.maxHeightFraction == 0f) {
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp);
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int maxHeightDp = Math.round(metrics.heightPixels / metrics.density * size.maxHeightFraction);
        return AdSize.getInlineAdaptiveBannerAdSize(widthDp, maxHeightDp);
    }

    void load(@NonNull BannerAdCustomView container, @NonNull String adUnitId, @NonNull AdSize adSize) {
        if (destroyed || adView != null || !googleAllowed()) {
            return;
        }
        containerReference = new WeakReference<>(container);
        container.showShimmer(adSize);

        AdView view = new AdView(container.getContext());
        adView = view;
        view.setVisibility(View.INVISIBLE);
        container.addView(view, new FrameLayout.LayoutParams(adSize.getWidthInPixels(container.getContext()), container.pendingAdHeight(adSize), Gravity.CENTER_HORIZONTAL));
        view.loadAd(new BannerAdRequest.Builder(adUnitId, adSize).build(), new AdLoadCallback<>() {
            @Override
            public void onAdLoaded(@NonNull BannerAd ad) {
                MainThread.run(() -> {
                    BannerAdCustomView container = host();
                    if (container != null && adView == view) {
                        if (!googleAllowed()) {
                            container.removeView(view);
                            releaseAdView();
                            if (previous != null) {
                                previous.release();
                                previous = null;
                            }
                            container.showQuiz();
                            container.notifyLoadResult(true);
                            return;
                        }
                        container.hideShimmer();
                        container.hideFallback();
                        container.fitToLoadedAd(view, ad.getAdSize());
                        view.setVisibility(View.VISIBLE);
                        if (previous != null) {
                            previous.release();
                            previous = null;
                        }
                        container.notifyLoadResult(true);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                MainThread.run(() -> {
                    BannerAdCustomView container = host();
                    if (container != null && adView == view) {
                        if (previous != null) {
                            softFail(container);
                            return;
                        }
                        container.removeView(view);
                        releaseAdView();
                        container.showFallback();
                        container.notifyLoadResult(false);
                    }
                });
            }
        });
    }

    void resize(@NonNull BannerAdCustomView container, @NonNull String adUnitId, @NonNull AdSize adSize) {
        if (destroyed) {
            return;
        }
        if (adView != null) {
            container.removeView(adView);
            releaseAdView();
        }
        container.hideShimmer();
        load(container, adUnitId, adSize);
    }

    private static boolean googleAllowed() {
        return AdsController.isAdsEnabled() && !AdsController.isQuizPriority();
    }

    @Nullable
    private BannerAdCustomView host() {
        return destroyed || containerReference == null ? null : containerReference.get();
    }

    private void releaseAdView() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }

    public void destroy() {
        destroyed = true;
        if (previous != null) {
            previous.destroy();
            previous = null;
        }
        if (containerReference != null) {
            BannerAdCustomView container = containerReference.get();
            if (container != null) {
                container.removeAllViews();
            }
            containerReference = null;
        }
        releaseAdView();
    }
}
