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

    void load(@NonNull NativeAdCustomView container, @NonNull String adUnitId, @LayoutRes int layoutRes) {
        if (destroyed || requested || !AdsController.isAdsEnabled()) {
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
                        collapse(container);
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
        try {
            container.hideShimmer();
            container.hideFallback();
            container.addView(inflateAd(container, ad, layoutRes));
            nativeAd = ad;
        } catch (RuntimeException e) {
            ad.destroy();
            collapse(container);
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
            ad.getMediaContent();
            media.setMediaContent(ad.getMediaContent());
        }

        bindText(action, ad.getCallToAction());
        adView.setCallToActionView(action);

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

    private static void collapse(@NonNull NativeAdCustomView container) {
        container.showFallback();
    }

    @Nullable
    private NativeAdCustomView host() {
        return destroyed || containerReference == null ? null : containerReference.get();
    }

    public void destroy() {
        destroyed = true;
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