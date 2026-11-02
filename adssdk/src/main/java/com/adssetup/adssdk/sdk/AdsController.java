package com.adssetup.adssdk.sdk;

public final class AdsController {
    private static volatile boolean adsEnabled = true;
    private static volatile boolean quizFallbackEnabled = true;

    private AdsController() {
    }

    public static void setAdsEnabled(boolean enabled) {
        adsEnabled = enabled;
    }

    public static boolean isAdsEnabled() {
        return adsEnabled;
    }

    public static void setQuizFallbackEnabled(boolean enabled) {
        quizFallbackEnabled = enabled;
    }

    public static boolean isQuizFallbackEnabled() {
        return quizFallbackEnabled;
    }
}