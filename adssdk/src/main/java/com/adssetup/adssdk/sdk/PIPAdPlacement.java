package com.adssetup.adssdk.sdk;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.OnPictureInPictureModeChangedProvider;
import androidx.core.app.PictureInPictureModeChangedInfo;
import androidx.core.util.Consumer;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.ExperimentalApi;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd;
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdOptions;
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPosition;
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPresentationScope;
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdRequest;

import java.lang.ref.WeakReference;

@OptIn(markerClass = ExperimentalApi.class)
public class PIPAdPlacement implements DefaultLifecycleObserver {
    private final WeakReference<Activity> activityReference;
    private final String adUnitId;
    private final PictureInPictureAdPosition position;
    private final Consumer<PictureInPictureModeChangedInfo> pipModeListener = info -> update();
    private PictureInPictureAd pipAd;
    private boolean shown;
    private boolean destroyed;

    private PIPAdPlacement(@NonNull Activity activity, @NonNull String adUnitId, @NonNull PictureInPictureAdPosition position) {
        this.activityReference = new WeakReference<>(activity);
        this.adUnitId = adUnitId;
        this.position = position;
    }

    public static void loadPIPAd(@NonNull Activity activity, @NonNull String adUnitId) {
        loadPIPAd(activity, adUnitId, PictureInPictureAdPosition.DEFAULT);
    }

    public static void loadPIPAd(@NonNull Activity activity, @NonNull String adUnitId, @NonNull PictureInPictureAdPosition position) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!AdsController.isAdsEnabled()) {
            return;
        }
        PIPAdPlacement placement = new PIPAdPlacement(activity, adUnitId, position);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(placement);
        }
        if (activity instanceof OnPictureInPictureModeChangedProvider) {
            ((OnPictureInPictureModeChangedProvider) activity).addOnPictureInPictureModeChangedListener(placement.pipModeListener);
        }
        SdkAuthorization.authorize(activity, placement::onAuthorized);
    }

    private void onAuthorized(boolean granted) {
        if (destroyed) {
            return;
        }
        if (!granted || !AdsController.isAdsEnabled() || !isHostUsable(activityReference.get())) {
            destroy();
            return;
        }
        PictureInPictureAd.load(new PictureInPictureAdRequest.Builder(adUnitId).build(), new AdLoadCallback<>() {
            @Override
            public void onAdLoaded(@NonNull PictureInPictureAd ad) {
                MainThread.run(() -> {
                    if (destroyed || !AdsController.isAdsEnabled()) {
                        ad.destroy();
                        destroy();
                        return;
                    }
                    pipAd = ad;
                    update();
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                MainThread.run(PIPAdPlacement.this::destroy);
            }
        });
    }

    private void update() {
        Activity activity = activityReference.get();
        if (!AdsController.isAdsEnabled()) {
            destroy();
            return;
        }
        if (destroyed || pipAd == null || activity == null) {
            return;
        }
        boolean allowed = isHostUsable(activity) && !activity.isInPictureInPictureMode();
        if (allowed && !shown) {
            shown = true;
            pipAd.show(activity, new PictureInPictureAdOptions.Builder().setPosition(position).setPresentationScope(PictureInPictureAdPresentationScope.SCREEN).build());
        } else if (!allowed) {
            hide();
        }
    }

    private void hide() {
        if (pipAd != null && shown) {
            shown = false;
            pipAd.hide();
        }
    }

    private static boolean isHostUsable(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        return !(activity instanceof LifecycleOwner) || ((LifecycleOwner) activity).getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        update();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        hide();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        destroy();
    }

    private void destroy() {
        if (destroyed) {
            return;
        }
        hide();
        destroyed = true;
        Activity activity = activityReference.get();
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().removeObserver(this);
        }
        if (activity instanceof OnPictureInPictureModeChangedProvider) {
            ((OnPictureInPictureModeChangedProvider) activity).removeOnPictureInPictureModeChangedListener(pipModeListener);
        }
        if (pipAd != null) {
            pipAd.destroy();
            pipAd = null;
        }
    }
}