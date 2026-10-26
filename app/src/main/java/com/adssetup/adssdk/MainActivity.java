package com.adssetup.adssdk;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.NativeAdCustomView;
import com.adssetup.adssdk.sdk.NativeAdPlacement;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NativeAdCustomView nativeAdCustomView = findViewById(R.id.nativeAdCustomView);
        NativeAdPlacement.loadNativeAd(this, "ca-app-pub-3940256099942544/2247696110", nativeAdCustomView);
    }
}