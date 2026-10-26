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

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.lang.ref.WeakReference;

public class NativeAdPlacement {
    private static final NativeAdOptions AD_OPTIONS = new NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build();
    private static final AdRequest AD_REQUEST = new AdRequest.Builder().build();
    private WeakReference<NativeAdCustomView> containerReference;
    private AdLoader adLoader;
    private NativeAd nativeAd;
    private boolean destroyed;

    public static void loadNativeAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull NativeAdCustomView adView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        adView.load(adUnitId);
    }

    void load(@NonNull NativeAdCustomView container, @NonNull String adUnitId, @LayoutRes int layoutRes) {
        if (destroyed || adLoader != null) {
            return;
        }
        containerReference = new WeakReference<>(container);
        container.showShimmer();

        adLoader = new AdLoader.Builder(container.getContext().getApplicationContext(), adUnitId).forNativeAd(ad -> onLoaded(ad, layoutRes)).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                NativeAdCustomView container = host();
                if (container != null) {
                    collapse(container);
                }
            }
        }).withNativeAdOptions(AD_OPTIONS).build();
        adLoader.loadAd(AD_REQUEST);
    }

    private void onLoaded(@NonNull NativeAd ad, @LayoutRes int layoutRes) {
        NativeAdCustomView container = host();
        if (container == null) {
            ad.destroy();
            return;
        }
        try {
            container.hideShimmer();
            container.addView(inflateAd(container, ad, layoutRes));
            nativeAd = ad;
        } catch (RuntimeException e) {
            ad.destroy();
            collapse(container);
        }
    }

    @NonNull
    private static NativeAdView inflateAd(@NonNull NativeAdCustomView parent, @NonNull NativeAd ad, @LayoutRes int layoutRes) {
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
            adView.setMediaView(media);
            if (ad.getMediaContent() != null) {
                media.setMediaContent(ad.getMediaContent());
            }
        }

        bindText(action, ad.getCallToAction());
        adView.setCallToActionView(action);

        adView.setNativeAd(ad);
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
        container.hideShimmer();
        container.setVisibility(View.GONE);
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
        adLoader = null;
    }
}