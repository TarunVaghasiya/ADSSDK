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

/**
 * Loads a Google native ad and renders it in an SDK {@link NativeAdView}, with a shimmer while loading.
 *
 * <p>Usage: create it, call {@link #bindLifecycle(LifecycleOwner)} so the ad is released
 * automatically, then call {@link #load(NativeAdView, String)}. The Mobile Ads SDK must already be
 * initialized ({@code MobileAds.initialize}).
 */
public class NativeAdPlacement implements DefaultLifecycleObserver {

    public interface Listener {
        void onAdLoaded();

        void onAdFailedToLoad(@NonNull LoadAdError error);

        default void onAdClicked() {
        }

        default void onAdImpression() {
        }
    }

    /**
     * Loads an ad into {@code adView}: the shimmer shows while loading, then the ad replaces it.
     * If loading fails the shimmer is removed and {@code adView} becomes {@code GONE}. A view
     * that already requested an ad is not loaded again, and nothing happens once the Activity is
     * finishing.
     */
    public static void loadNativeAd(@NonNull Activity activity, @NonNull String adUnitId,
                                    @NonNull NativeAdView adView) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        adView.load(adUnitId);
    }

    @Nullable
    private Listener listener;
    @Nullable
    private AdLoader adLoader;
    @Nullable
    private NativeAd nativeAd;
    @Nullable
    private NativeAdView container;
    @LayoutRes
    private int layoutRes = R.layout.native_large_ad_layout;
    private boolean loading;
    private boolean destroyed;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /**
     * Selects the ad layout (default: the large one). Takes effect on the next load.
     */
    void setLayout(@LayoutRes int layoutRes) {
        this.layoutRes = layoutRes;
    }

    /**
     * Destroys the ad automatically when the owner is destroyed.
     */
    public void bindLifecycle(@NonNull LifecycleOwner owner) {
        owner.getLifecycle().addObserver(this);
    }

    /**
     * Requests an ad and shows it in {@code container}. A new call replaces any ad currently
     * shown. Ignored while a request is in flight or after {@link #destroy()}.
     */
    void load(@NonNull NativeAdView container, @NonNull String adUnitId) {
        if (destroyed || loading) {
            return;
        }
        this.container = container;
        loading = true;
        container.showShimmer();

        adLoader = new AdLoader.Builder(container.getContext(), adUnitId)
                .forNativeAd(ad -> {
                    loading = false;
                    if (destroyed || this.container == null) {
                        // Released while the request was in flight: don't leak the ad.
                        ad.destroy();
                        return;
                    }
                    if (nativeAd != null) {
                        nativeAd.destroy();
                    }
                    nativeAd = ad;
                    render(ad);
                    this.container.hideShimmer();
                    if (listener != null) {
                        listener.onAdLoaded();
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loading = false;
                        if (destroyed || NativeAdPlacement.this.container == null) {
                            return;
                        }
                        NativeAdPlacement.this.container.hideShimmer();
                        NativeAdPlacement.this.container.setVisibility(View.GONE);
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
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build())
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void render(@NonNull NativeAd ad) {
        NativeAdView parent = container;
        if (parent == null) {
            return;
        }
        com.google.android.gms.ads.nativead.NativeAdView adView =
                (com.google.android.gms.ads.nativead.NativeAdView) LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);

        // Required asset: title (headline).
        TextView headline = adView.findViewById(R.id.ad_headline);
        headline.setText(ad.getHeadline());
        adView.setHeadlineView(headline);

        // Media, for layouts that have a media view (the small layout does not).
        MediaView media = adView.findViewById(R.id.ad_media);
        if (media != null) {
            adView.setMediaView(media);
            if (ad.getMediaContent() != null) {
                media.setMediaContent(ad.getMediaContent());
            }
        }

        // Optional assets: hidden when the ad does not supply them; registered either way.
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

        // Must be the last call: it binds the ad to the view and its registered assets.
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

    /**
     * Releases the ad and its views. Safe to call more than once.
     */
    public void destroy() {
        destroyed = true;
        loading = false;
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
