package com.adssetup.adssdk.sdk;

public final class AdsController {
    private static volatile boolean adsEnabled = true;

    private AdsController() {
    }

    public static void setAdsEnabled(boolean enabled) {
        adsEnabled = enabled;
    }

    public static boolean isAdsEnabled() {
        return adsEnabled;
    }
}