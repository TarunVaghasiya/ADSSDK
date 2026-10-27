package com.adssetup.adssdk;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.AppOpenAdPlacement;
import com.adssetup.adssdk.sdk.BannerAdCustomView;
import com.adssetup.adssdk.sdk.BannerAdPlacement;
import com.adssetup.adssdk.sdk.InterstitialAdPlacement;
import com.adssetup.adssdk.sdk.NativeAdCustomView;
import com.adssetup.adssdk.sdk.NativeAdPlacement;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BannerAdCustomView bannerAdCustomView = findViewById(R.id.bannerAdCustomView);
        BannerAdPlacement.loadBannerAd(this, "ca-app-pub-3940256099942544/9214589741", bannerAdCustomView);

        View tvInterstitial = findViewById(R.id.tvInterstitial);
        tvInterstitial.setOnClickListener(view -> {
            view.setEnabled(false);
            InterstitialAdPlacement.loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712", shown -> view.setEnabled(true));
        });

        View tvAppOpen = findViewById(R.id.tvAppOpen);
        tvAppOpen.setOnClickListener(view -> {
            view.setEnabled(false);
            AppOpenAdPlacement.loadAppOpenAd(this, "ca-app-pub-3940256099942544/9257395921", shown -> view.setEnabled(true));
        });

        NativeAdCustomView nativeAdCustomView = findViewById(R.id.nativeAdCustomView);
        NativeAdPlacement.loadNativeAd(this, "ca-app-pub-3940256099942544/2247696110", nativeAdCustomView);
    }
}