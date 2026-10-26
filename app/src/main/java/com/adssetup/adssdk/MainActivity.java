package com.adssetup.adssdk;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.NativeAdPlacement;
import com.adssetup.adssdk.sdk.NativeAdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {
        });

        NativeAdView nativeAdView = findViewById(R.id.nativeAdView);
        NativeAdPlacement.loadNativeAd(this, "ca-app-pub-3940256099942544/2247696110", nativeAdView);
    }
}