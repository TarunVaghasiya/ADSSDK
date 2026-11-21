package com.adssetup.adssdk;

import android.app.Application;

import com.adssetup.adssdk.config.AdsConfigManager;
import com.adssetup.adssdk.sdk.SdkAuthorization;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AdsConfigManager.load(this);
        SdkAuthorization.init(this);
    }
}