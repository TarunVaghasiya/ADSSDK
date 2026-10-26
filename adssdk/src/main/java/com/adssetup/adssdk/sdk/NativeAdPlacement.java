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
import com.google.android.gms.ads.nativead.NativeAdView;

public class NativeAdPlacement implements DefaultLifecycleObserver {
    private Listener listener;
    private AdLoader adLoader;
    private NativeAd nativeAd;
    private NativeAdCustomView container;
    private int layoutRes = R.layout.native_large_ad_layout;
    private boolean destroyed;

    public interface Listener {
        void onAdLoaded();

        void onAdFailedToLoad(@NonNull LoadAdError error);

        default void onAdClicked() {
        }

        default void onAdImpression() {
        }
    }

    public static void loadNativeAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull NativeAdCustomView adView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        adView.load(adUnitId);
    }

    void setLayout(@LayoutRes int layoutRes) {
        this.layoutRes = layoutRes;
    }

    void load(@NonNull NativeAdCustomView container, @NonNull String adUnitId) {
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

    private void render(@NonNull NativeAdCustomView parent, @NonNull NativeAd ad) {
        NativeAdView adView = (NativeAdView) LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);

        ImageView icon = adView.findViewById(R.id.ad_app_icon);
        TextView headline = adView.findViewById(R.id.ad_headline);
        TextView body = adView.findViewById(R.id.ad_body);
        MediaView media = adView.findViewById(R.id.ad_media);
        Button action = adView.findViewById(R.id.ad_call_to_action);

        if (ad.getIcon() != null) {
            icon.setImageDrawable(ad.getIcon().getDrawable());
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
        }
        adView.setIconView(icon);

        headline.setText(ad.getHeadline());
        adView.setHeadlineView(headline);

        bindText(body, ad.getBody());
        adView.setBodyView(body);

        if (media != null) {
            adView.setMediaView(media);
            if (ad.getMediaContent() != null) {
                media.setMediaContent(ad.getMediaContent());
            }
        }

        bindText(action, ad.getCallToAction());
        adView.setCallToActionView(action);

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