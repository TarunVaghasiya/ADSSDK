package com.adssetup.adssdk;

import android.content.Intent;
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
    private static final String BANNER_ID = "ca-app-pub-3940256099942544/9214589741";
    private static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    private static final String APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921";
    private static final String NATIVE_ID = "ca-app-pub-3940256099942544/2247696110";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BannerAdCustomView bannerAdCustomView = findViewById(R.id.bannerAdCustomView);
        BannerAdPlacement.loadBannerAd(this, BANNER_ID, bannerAdCustomView);

        View tvInterstitial1 = findViewById(R.id.tvInterstitial1);
        tvInterstitial1.setOnClickListener(view -> InterstitialAdPlacement.loadInterstitialAd(this, INTERSTITIAL_ID, "load", shown -> openSecond()));

        View tvAppOpen1 = findViewById(R.id.tvAppOpen1);
        tvAppOpen1.setOnClickListener(view -> AppOpenAdPlacement.loadAppOpenAd(this, APP_OPEN_ID, "load", shown -> openSecond()));

        NativeAdCustomView nativeAdCustomView = findViewById(R.id.nativeAdCustomView);
        NativeAdPlacement.loadNativeAd(this, NATIVE_ID, nativeAdCustomView);
    }

    private void openSecond() {
        if (!isFinishing() && !isDestroyed()) {
            startActivity(new Intent(this, SecondActivity.class));
        }
    }
}