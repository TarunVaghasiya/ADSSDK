package com.adssetup.adssdk.config;

import android.content.Context;

import androidx.annotation.NonNull;

import com.adssetup.adssdk.sdk.AdsController;

public final class AdsConfigManager {
    private static AdsConfig config;
    public static final String TYPE_BANNER = "banner";
    public static final String TYPE_NATIVE = "native";
    public static final String TYPE_INTERSTITIAL = "inter";
    public static final String TYPE_APP_OPEN = "appopen";

    private AdsConfigManager() {
    }

    public static synchronized void load(@NonNull Context context) {
        if (config == null) {
            config = AdsConfigParser.parse(context.getApplicationContext());
            AdsController.setAdsEnabled(config.isAppAllAdsShow());
            AdsController.setQuizPriority("quiz".equalsIgnoreCase(config.getAdPriority()));
            AdsController.setQuizFallbackEnabled(config.isGoogleAdFailedShowQuiz());
            AdsController.setNativeColors(config.getNativeAdLabelColor(), config.getNativeAdButtonColor());
            AdsConfig.QuizAdsDesign design = config.getQuizAdsDesign();
            AdsController.setQuizDesign(design.getQuizAppIcon(), design.getQuizNativeMedia(), design.getQuizInterstitialMedia(), design.getQuizAppOpenMedia(), design.getQuizTitle(), design.getQuizDescription(), design.getQuizButtonText());
        }
    }

    @NonNull
    public static synchronized AdsConfig get() {
        if (config == null) {
            config = new AdsConfig();
        }
        return config;
    }

    public static String loadMode(String configured) {
        return "preload".equalsIgnoreCase(configured) ? "preload" : "load";
    }

    public static boolean isType(String configured, @NonNull String type) {
        return type.equalsIgnoreCase(configured);
    }
}