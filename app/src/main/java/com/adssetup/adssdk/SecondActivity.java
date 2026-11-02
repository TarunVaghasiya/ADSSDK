package com.adssetup.adssdk;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.config.AdsConfig;
import com.adssetup.adssdk.config.AdsConfigManager;
import com.adssetup.adssdk.sdk.AppOpenAdPlacement;
import com.adssetup.adssdk.sdk.BannerAdCustomView;
import com.adssetup.adssdk.sdk.BannerAdPlacement;
import com.adssetup.adssdk.sdk.InterstitialAdPlacement;
import com.adssetup.adssdk.sdk.NativeAdCustomView;
import com.adssetup.adssdk.sdk.NativeAdPlacement;

public class SecondActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        AdsConfig config = AdsConfigManager.get();
        AdsConfig.SecondScreen second = config.getSecondScreen();

        View rlAdView = findViewById(R.id.rlAdView);
        BannerAdCustomView bannerAdCustomView = findViewById(R.id.bannerAdCustomView);
        NativeAdCustomView nativeAdCustomView = findViewById(R.id.nativeAdCustomView);

        boolean showBanner = second.isSecondAdShow() && AdsConfigManager.isType(second.getSecondAdType(), AdsConfigManager.TYPE_BANNER);
        boolean showNative = second.isSecondAdShow() && AdsConfigManager.isType(second.getSecondAdType(), AdsConfigManager.TYPE_NATIVE);

        rlAdView.setVisibility(showBanner || showNative ? View.VISIBLE : View.GONE);
        bannerAdCustomView.setVisibility(showBanner ? View.VISIBLE : View.GONE);
        nativeAdCustomView.setVisibility(showNative ? View.VISIBLE : View.GONE);
        if (showBanner) {
            BannerAdPlacement.loadBannerAd(this, second.getSecondBannerId(), bannerAdCustomView);
        } else if (showNative) {
            NativeAdPlacement.loadNativeAd(this, second.getSecondNativeId(), nativeAdCustomView);
        }

        boolean buttonAdEnabled = second.isSecondButtonAdShow();
        if (buttonAdEnabled && AdsConfigManager.isType(second.getSecondButtonAdType(), AdsConfigManager.TYPE_INTERSTITIAL) && InterstitialAdPlacement.MODE_PRELOAD.equals(AdsConfigManager.loadMode(config.getInterstitialLoadType()))) {
            InterstitialAdPlacement.loadInterstitialAd(this, second.getSecondInterstitialId(), InterstitialAdPlacement.MODE_PRELOAD, null);
        } else if (buttonAdEnabled && AdsConfigManager.isType(second.getSecondButtonAdType(), AdsConfigManager.TYPE_APP_OPEN) && AppOpenAdPlacement.MODE_PRELOAD.equals(AdsConfigManager.loadMode(config.getAppOpenLoadType()))) {
            AppOpenAdPlacement.loadAppOpenAd(this, second.getSecondAppOpenId(), AppOpenAdPlacement.MODE_PRELOAD, null);
        }
        findViewById(R.id.tvAds2).setOnClickListener(view -> {
            if (buttonAdEnabled && AdsConfigManager.isType(second.getSecondButtonAdType(), AdsConfigManager.TYPE_INTERSTITIAL)) {
                view.setEnabled(false);
                InterstitialAdPlacement.loadInterstitialAd(this, second.getSecondInterstitialId(), AdsConfigManager.loadMode(config.getInterstitialLoadType()), shown -> {
                    view.setEnabled(true);
                    openMain();
                });
            } else if (buttonAdEnabled && AdsConfigManager.isType(second.getSecondButtonAdType(), AdsConfigManager.TYPE_APP_OPEN)) {
                view.setEnabled(false);
                AppOpenAdPlacement.loadAppOpenAd(this, second.getSecondAppOpenId(), AdsConfigManager.loadMode(config.getAppOpenLoadType()), shown -> {
                    view.setEnabled(true);
                    openMain();
                });
            } else {
                openMain();
            }
        });
    }

    private void openMain() {
        if (!isFinishing() && !isDestroyed()) {
            SecondActivity.this.finish();
        }
    }
}