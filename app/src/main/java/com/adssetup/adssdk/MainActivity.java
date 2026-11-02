package com.adssetup.adssdk;

import android.content.Intent;
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
import com.adssetup.adssdk.sdk.PIPAdPlacement;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdsConfig config = AdsConfigManager.get();
        AdsConfig.MainScreen main = config.getMainScreen();
        boolean adsEnabled = config.isAppAllAdsShow();

        View rlAdView = findViewById(R.id.rlAdView);
        BannerAdCustomView bannerAdCustomView = findViewById(R.id.bannerAdCustomView);
        NativeAdCustomView nativeAdCustomView = findViewById(R.id.nativeAdCustomView);

        boolean showBanner = adsEnabled && main.isMainAdShow() && AdsConfigManager.isType(main.getMainAdType(), AdsConfigManager.TYPE_BANNER);
        boolean showNative = adsEnabled && main.isMainAdShow() && AdsConfigManager.isType(main.getMainAdType(), AdsConfigManager.TYPE_NATIVE);

        rlAdView.setVisibility(showBanner || showNative ? View.VISIBLE : View.GONE);
        bannerAdCustomView.setVisibility(showBanner ? View.VISIBLE : View.GONE);
        nativeAdCustomView.setVisibility(showNative ? View.VISIBLE : View.GONE);
        if (showBanner) {
            BannerAdPlacement.loadBannerAd(this, main.getMainBannerId(), bannerAdCustomView);
        } else if (showNative) {
            NativeAdPlacement.loadNativeAd(this, main.getMainNativeId(), nativeAdCustomView);
        }

        boolean buttonAdEnabled = adsEnabled && main.isMainButtonAdShow();
        findViewById(R.id.tvAds1).setOnClickListener(view -> {
            if (buttonAdEnabled && AdsConfigManager.isType(main.getMainButtonType(), AdsConfigManager.TYPE_INTERSTITIAL)) {
                view.setEnabled(false);
                InterstitialAdPlacement.loadInterstitialAd(this, main.getMainInterstitialId(), "load", shown -> {
                    view.setEnabled(true);
                    openSecond();
                });
            } else if (buttonAdEnabled && AdsConfigManager.isType(main.getMainButtonType(), AdsConfigManager.TYPE_APP_OPEN)) {
                view.setEnabled(false);
                AppOpenAdPlacement.loadAppOpenAd(this, main.getMainAppOpenId(), "load", shown -> {
                    view.setEnabled(true);
                    openSecond();
                });
            } else {
                openSecond();
            }
        });

        if (adsEnabled && main.isMainPipAdShow()) {
            PIPAdPlacement.loadPIPAd(this, main.getMainPipId());
        }
    }

    private void openSecond() {
        if (!isFinishing() && !isDestroyed()) {
            startActivity(new Intent(this, SecondActivity.class));
        }
    }
}