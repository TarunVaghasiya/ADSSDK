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

    public static void loadBannerAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull BannerAdCustomView bannerView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        bannerView.load(adUnitId);
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
        if (destroyed || adView != null) {
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
                        container.hideShimmer();
                        container.hideFallback();
                        container.fitToLoadedAd(view, ad.getAdSize());
                        view.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                MainThread.run(() -> {
                    BannerAdCustomView container = host();
                    if (container != null && adView == view) {
                        container.removeView(view);
                        releaseAdView();
                        container.showFallback();
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