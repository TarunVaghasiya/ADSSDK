package com.adssetup.adssdk;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.BannerAdCustomView;
import com.adssetup.adssdk.sdk.BannerAdPlacement;
import com.adssetup.adssdk.sdk.NativeAdCustomView;
import com.adssetup.adssdk.sdk.NativeAdPlacement;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BannerAdCustomView bannerAdCustomView = findViewById(R.id.bannerAdCustomView);
        BannerAdPlacement.loadBannerAd(this, "ca-app-pub-3940256099942544/9214589741", bannerAdCustomView);

        NativeAdCustomView nativeAdCustomView = findViewById(R.id.nativeAdCustomView);
        NativeAdPlacement.loadNativeAd(this, "ca-app-pub-3940256099942544/2247696110", nativeAdCustomView);
    }
}