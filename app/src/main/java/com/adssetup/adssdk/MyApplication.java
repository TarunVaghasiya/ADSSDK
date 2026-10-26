package com.adssetup.adssdk;

import android.app.Application;

import com.adssetup.adssdk.sdk.SdkAuthorization;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SdkAuthorization.init(this);
    }
}