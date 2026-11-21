package com.adssetup.adssdk.sdk;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;

import java.lang.ref.WeakReference;
import java.util.Collections;

public class NativeAdPlacement {
    private WeakReference<NativeAdCustomView> containerReference;
    private NativeAd nativeAd;
    private boolean requested;
    private boolean destroyed;
    private NativeAdPlacement previous;
    private View adViewInstance;

    public static void loadNativeAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull NativeAdCustomView adView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!AdsController.isAdsEnabled()) {
            adView.setVisibility(View.GONE);
            return;
        }
        if (AdsController.isQuizPriority()) {
            adView.showQuiz();
            return;
        }
        adView.load(adUnitId);
    }

    public static void refreshNativeAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull NativeAdCustomView adView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!AdsController.isAdsEnabled()) {
            adView.setVisibility(View.GONE);
            return;
        }
        if (AdsController.isQuizPriority()) {
            adView.showQuiz();
            return;
        }
        adView.refresh(adUnitId);
    }

    /**
     * Like {@link #refreshNativeAd} but keeps the ad that is on screen until the new one has loaded. The current ad is
     * hidden behind the shimmer meanwhile, replaced only on success and brought back if the request fails.
     */
    public static void refreshNativeAdKeepingCurrent(@NonNull Activity activity, @NonNull String adUnitId, @NonNull NativeAdCustomView adView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!AdsController.isAdsEnabled()) {
            adView.setVisibility(View.GONE);
            return;
        }
        if (AdsController.isQuizPriority()) {
            adView.showQuiz();
            return;
        }
        adView.softRefresh(adUnitId);
    }

    boolean hasAd() {
        return !destroyed && nativeAd != null && adViewInstance != null;
    }

    void setAdVisible(boolean visible) {
        if (adViewInstance != null) {
            adViewInstance.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    void setPrevious(@Nullable NativeAdPlacement placement) {
        previous = placement;
    }

    /** Drops this placement's own ad only, leaving the container's other children alone. */
    private void release() {
        destroyed = true;
        if (adViewInstance != null && adViewInstance.getParent() instanceof ViewGroup) {
            ((ViewGroup) adViewInstance.getParent()).removeView(adViewInstance);
        }
        adViewInstance = null;
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
        containerReference = null;
    }

    /** The refresh did not produce a new ad: stop the shimmer and bring the previous ad back. */
    void softFail(@NonNull NativeAdCustomView container) {
        NativeAdPlacement old = previous;
        previous = null;
        destroyed = true;
        container.hideShimmer();
        if (old != null) {
            container.adoptPlacement(old);
            old.setAdVisible(true);
        }
        container.notifyLoadResult(false);
    }

    void load(@NonNull NativeAdCustomView container, @NonNull String adUnitId, @LayoutRes int layoutRes) {
        if (previous != null && !googleAllowed()) {
            softFail(container);
            return;
        }
        if (destroyed || requested || !googleAllowed()) {
            return;
        }
        requested = true;
        containerReference = new WeakReference<>(container);
        container.showShimmer();

        NativeAdRequest request = new NativeAdRequest.Builder(adUnitId, Collections.singletonList(NativeAd.NativeAdType.NATIVE)).setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT).build();
        NativeAdLoader.load(request, new NativeAdLoaderCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd ad) {
                MainThread.run(() -> onLoaded(ad, layoutRes));
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                MainThread.run(() -> {
                    NativeAdCustomView container = host();
                    if (container != null) {
                        if (previous != null) {
                            softFail(container);
                            return;
                        }
                        container.showFallback();
                        container.notifyLoadResult(false);
                    }
                });
            }
        });
    }

    private void onLoaded(@NonNull NativeAd ad, @LayoutRes int layoutRes) {
        NativeAdCustomView container = host();
        if (container == null) {
            ad.destroy();
            return;
        }
        if (!googleAllowed()) {
            ad.destroy();
            if (previous != null) {
                previous.release();
                previous = null;
            }
            container.showQuiz();
            container.notifyLoadResult(true);
            return;
        }
        try {
            NativeAdView inflated = inflateAd(container, ad, layoutRes);
            container.hideShimmer();
            container.hideFallback();
            container.addView(inflated);
            adViewInstance = inflated;
            nativeAd = ad;
            if (previous != null) {
                previous.release();
                previous = null;
            }
            container.notifyLoadResult(true);
        } catch (RuntimeException e) {
            ad.destroy();
            if (previous != null) {
                softFail(container);
                return;
            }
            container.showFallback();
            container.notifyLoadResult(false);
        }
    }

    @NonNull
    static NativeAdView inflateAd(@NonNull ViewGroup parent, @NonNull NativeAd ad, @LayoutRes int layoutRes) {
        NativeAdView adView = (NativeAdView) LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);

        ImageView icon = adView.findViewById(R.id.ad_app_icon);
        TextView headline = adView.findViewById(R.id.ad_headline);
        TextView body = adView.findViewById(R.id.ad_body);
        MediaView media = adView.findViewById(R.id.ad_media);
        Button action = adView.findViewById(R.id.ad_call_to_action);

        if (ad.getIcon() != null) {
            icon.setImageDrawable(ad.getIcon().getDrawable());
        } else {
            icon.setVisibility(View.GONE);
        }
        adView.setIconView(icon);

        headline.setText(ad.getHeadline());
        adView.setHeadlineView(headline);

        bindText(body, ad.getBody());
        adView.setBodyView(body);

        if (media != null) {
            media.setMediaContent(ad.getMediaContent());
        }

        bindText(action, ad.getCallToAction());
        adView.setCallToActionView(action);

        AdsController.applyNativeColors(adView, R.id.ad_attribution, R.id.ad_call_to_action);

        adView.registerNativeAd(ad, media);
        return adView;
    }

    private static void bindText(@NonNull TextView view, @Nullable String text) {
        if (text == null || text.isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(text);
        }
    }

    private static boolean googleAllowed() {
        return AdsController.isAdsEnabled() && !AdsController.isQuizPriority();
    }

    @Nullable
    private NativeAdCustomView host() {
        return destroyed || containerReference == null ? null : containerReference.get();
    }

    public void destroy() {
        destroyed = true;
        if (previous != null) {
            previous.destroy();
            previous = null;
        }
        if (containerReference != null) {
            NativeAdCustomView container = containerReference.get();
            if (container != null) {
                container.removeAllViews();
            }
            containerReference = null;
        }
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
    }
}