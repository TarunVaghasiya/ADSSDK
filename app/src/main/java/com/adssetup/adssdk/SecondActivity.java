package com.adssetup.adssdk;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.AppOpenAdPlacement;
import com.adssetup.adssdk.sdk.InterstitialAdPlacement;

public class SecondActivity extends AppCompatActivity {
    private static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    private static final String APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        InterstitialAdPlacement.loadInterstitialAd(this, INTERSTITIAL_ID, "preload", null);
        AppOpenAdPlacement.loadAppOpenAd(this, APP_OPEN_ID, "preload", null);

        findViewById(R.id.tvInterstitial2).setOnClickListener(view -> {
            view.setEnabled(false);
            InterstitialAdPlacement.loadInterstitialAd(this, INTERSTITIAL_ID, "preload", shown -> view.setEnabled(true));
        });

        findViewById(R.id.tvAppOpen2).setOnClickListener(view -> {
            view.setEnabled(false);
            AppOpenAdPlacement.loadAppOpenAd(this, APP_OPEN_ID, "preload", shown -> view.setEnabled(true));
        });
    }
}