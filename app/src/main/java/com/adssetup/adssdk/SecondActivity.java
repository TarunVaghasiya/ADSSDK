package com.adssetup.adssdk;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.AppOpenAdPlacement;
import com.adssetup.adssdk.sdk.InterstitialAdPlacement;

public class SecondActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        findViewById(R.id.tvInterstitial2).setOnClickListener(view -> {
            view.setEnabled(false);
            InterstitialAdPlacement.loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712", shown -> view.setEnabled(true));
        });

        findViewById(R.id.tvAppOpen2).setOnClickListener(view -> {
            view.setEnabled(false);
            AppOpenAdPlacement.loadAppOpenAd(this, "ca-app-pub-3940256099942544/9257395921", shown -> view.setEnabled(true));
        });
    }
}