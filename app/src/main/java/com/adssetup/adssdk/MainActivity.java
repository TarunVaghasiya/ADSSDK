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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BannerAdCustomView bannerAdCustomView = findViewById(R.id.bannerAdCustomView);
        BannerAdPlacement.loadBannerAd(this, "ca-app-pub-3940256099942544/9214589741", bannerAdCustomView);

        View tvInterstitial1 = findViewById(R.id.tvInterstitial1);
        tvInterstitial1.setOnClickListener(view -> InterstitialAdPlacement.loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712", shown -> openSecond()));

        View tvAppOpen1 = findViewById(R.id.tvAppOpen1);
        tvAppOpen1.setOnClickListener(view -> AppOpenAdPlacement.loadAppOpenAd(this, "ca-app-pub-3940256099942544/9257395921", shown -> openSecond()));

        NativeAdCustomView nativeAdCustomView = findViewById(R.id.nativeAdCustomView);
        NativeAdPlacement.loadNativeAd(this, "ca-app-pub-3940256099942544/2247696110", nativeAdCustomView);
    }

    private void openSecond() {
        if (!isFinishing() && !isDestroyed()) {
            startActivity(new Intent(this, SecondActivity.class));
        }
    }
}