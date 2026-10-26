package com.adssetup.adssdk;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.NativeAdPlacement;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {
        });

        NativeAdPlacement.loadNativeAd(this, "ca-app-pub-3940256099942544/2247696110", new NativeAdPlacement.Listener() {
            @Override
            public void onAdLoaded() {
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
            }
        });
    }
}