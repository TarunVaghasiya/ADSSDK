package com.adssetup.adssdk.config;

import android.content.Context;

import androidx.annotation.NonNull;

public final class AdsConfigManager {
    private static AdsConfig config;
    public static final String TYPE_BANNER = "banner";
    public static final String TYPE_NATIVE = "native";
    public static final String TYPE_INTERSTITIAL = "inter";
    public static final String TYPE_APP_OPEN = "app_open";

    private AdsConfigManager() {
    }

    public static synchronized void load(@NonNull Context context) {
        if (config == null) {
            config = AdsConfigParser.parse(context.getApplicationContext());
        }
    }

    @NonNull
    public static synchronized AdsConfig get() {
        if (config == null) {
            config = new AdsConfig();
        }
        return config;
    }

    public static boolean isType(String configured, @NonNull String type) {
        return type.equalsIgnoreCase(configured);
    }
}