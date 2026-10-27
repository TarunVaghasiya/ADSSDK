package com.adssetup.adssdk.sdk;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import java.lang.ref.WeakReference;

public class BannerAdPlacement {
    private static final AdRequest AD_REQUEST = new AdRequest.Builder().build();
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

        adView = new AdView(container.getContext());
        adView.setAdUnitId(adUnitId);
        adView.setAdSize(adSize);
        adView.setVisibility(View.INVISIBLE);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                BannerAdCustomView container = host();
                if (container != null) {
                    container.hideShimmer();
                    adView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                BannerAdCustomView container = host();
                if (container != null) {
                    container.hideShimmer();
                    container.setVisibility(View.GONE);
                    releaseAdView();
                }
            }
        });
        container.addView(adView, new FrameLayout.LayoutParams(adSize.getWidthInPixels(container.getContext()), adSize.getHeight() > 0 ? adSize.getHeightInPixels(container.getContext()) : FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
        adView.loadAd(AD_REQUEST);
    }

    void resize(@NonNull BannerAdCustomView container, @NonNull String adUnitId, @NonNull AdSize adSize) {
        if (destroyed || adView == null) {
            return;
        }
        container.removeView(adView);
        releaseAdView();
        container.hideShimmer();
        load(container, adUnitId, adSize);
    }

    void pause() {
        if (adView != null) {
            adView.pause();
        }
    }

    void resume() {
        if (adView != null) {
            adView.resume();
        }
    }

    @Nullable
    private BannerAdCustomView host() {
        return destroyed || containerReference == null ? null : containerReference.get();
    }

    private void releaseAdView() {
        if (adView != null) {
            adView.setAdListener(new AdListener() {
            });
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