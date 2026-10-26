package com.adssetup.adssdk.sdk;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

public class NativeAdPlacement implements DefaultLifecycleObserver {
    public interface Listener {
        void onAdLoaded();

        void onAdFailedToLoad(@NonNull LoadAdError error);

        default void onAdClicked() {
        }

        default void onAdImpression() {
        }
    }

    public static void loadNativeAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull NativeAdView adView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        adView.load(adUnitId);
    }

    private Listener listener;
    private AdLoader adLoader;
    private NativeAd nativeAd;
    private NativeAdView container;
    private int layoutRes = R.layout.native_large_ad_layout;
    private boolean destroyed;

    void setLayout(@LayoutRes int layoutRes) {
        this.layoutRes = layoutRes;
    }

    void load(@NonNull NativeAdView container, @NonNull String adUnitId) {
        if (destroyed || adLoader != null) {
            return;
        }
        this.container = container;
        container.showShimmer();

        adLoader = new AdLoader.Builder(container.getContext(), adUnitId).forNativeAd(ad -> {
            if (destroyed) {
                ad.destroy();
                return;
            }
            nativeAd = ad;
            render(container, ad);
            container.hideShimmer();
            if (listener != null) {
                listener.onAdLoaded();
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                if (destroyed) {
                    return;
                }
                container.hideShimmer();
                container.setVisibility(View.GONE);
                if (listener != null) {
                    listener.onAdFailedToLoad(error);
                }
            }

            @Override
            public void onAdClicked() {
                if (listener != null) {
                    listener.onAdClicked();
                }
            }

            @Override
            public void onAdImpression() {
                if (listener != null) {
                    listener.onAdImpression();
                }
            }
        }).withNativeAdOptions(new NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build()).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void render(@NonNull NativeAdView parent, @NonNull NativeAd ad) {
        com.google.android.gms.ads.nativead.NativeAdView adView = (com.google.android.gms.ads.nativead.NativeAdView) LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);

        TextView headline = adView.findViewById(R.id.ad_headline);
        headline.setText(ad.getHeadline());
        adView.setHeadlineView(headline);

        MediaView media = adView.findViewById(R.id.ad_media);
        if (media != null) {
            adView.setMediaView(media);
            if (ad.getMediaContent() != null) {
                media.setMediaContent(ad.getMediaContent());
            }
        }

        TextView body = adView.findViewById(R.id.ad_body);
        bindText(body, ad.getBody());
        adView.setBodyView(body);

        Button cta = adView.findViewById(R.id.ad_call_to_action);
        bindText(cta, ad.getCallToAction());
        adView.setCallToActionView(cta);

        ImageView icon = adView.findViewById(R.id.ad_app_icon);
        if (ad.getIcon() != null) {
            icon.setImageDrawable(ad.getIcon().getDrawable());
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
        }
        adView.setIconView(icon);

        adView.setNativeAd(ad);

        parent.removeAllViews();
        parent.addView(adView);
    }

    private static void bindText(TextView view, @Nullable String text) {
        if (text == null || text.isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }
    }

    public void destroy() {
        destroyed = true;
        listener = null;
        if (container != null) {
            container.removeAllViews();
            container = null;
        }
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
        adLoader = null;
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        owner.getLifecycle().removeObserver(this);
        destroy();
    }
}