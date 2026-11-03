package com.adssetup.adssdk.config;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class AdsConfigParser {
    private static final String ASSET_NAME = "ads_config.json";

    private AdsConfigParser() {
    }

    @NonNull
    static AdsConfig parse(@NonNull Context context) {
        try (InputStream stream = context.getAssets().open(ASSET_NAME)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return parse(new JSONObject(buffer.toString(StandardCharsets.UTF_8.name())));
        } catch (IOException | JSONException e) {
            return new AdsConfig();
        }
    }

    @NonNull
    static AdsConfig parse(@NonNull JSONObject root) {
        AdsConfig config = new AdsConfig();
        config.setAppAllAdsShow(root.optBoolean("App_All_Ads_Show", config.isAppAllAdsShow()));
        config.setAdPriority(string(root, "Ad_Priority", config.getAdPriority()));
        config.setGoogleAdFailedShowQuiz(root.optBoolean("Google_Ad_Failed_Show_Quiz", config.isGoogleAdFailedShowQuiz()));
        config.setInterstitialLoadType(string(root, "Interstitial_Load_Type", config.getInterstitialLoadType()));
        config.setAppOpenLoadType(string(root, "App_Open_Load_Type", config.getAppOpenLoadType()));

        JSONObject main = root.optJSONObject("MainScreen");
        if (main != null) {
            AdsConfig.MainScreen mainScreen = config.getMainScreen();
            mainScreen.setMainAdShow(main.optBoolean("Main_Ad_Show", mainScreen.isMainAdShow()));
            mainScreen.setMainAdType(string(main, "Main_Ad_Type", mainScreen.getMainAdType()));
            mainScreen.setMainBannerId(string(main, "Main_Banner_Id", mainScreen.getMainBannerId()));
            mainScreen.setMainNativeId(string(main, "Main_Native_Id", mainScreen.getMainNativeId()));
            mainScreen.setMainButtonAdShow(main.optBoolean("Main_Button_Ad_Show", mainScreen.isMainButtonAdShow()));
            mainScreen.setMainButtonAdType(string(main, "Main_Button_Ad_Type", mainScreen.getMainButtonAdType()));
            mainScreen.setMainInterstitialId(string(main, "Main_Interstitial_Id", mainScreen.getMainInterstitialId()));
            mainScreen.setMainAppOpenId(string(main, "Main_App_Open_Id", mainScreen.getMainAppOpenId()));
            mainScreen.setMainPipAdShow(main.optBoolean("Main_PIP_Ad_Show", mainScreen.isMainPipAdShow()));
            mainScreen.setMainPipId(string(main, "Main_PIP_Id", mainScreen.getMainPipId()));
        }

        JSONObject second = root.optJSONObject("SecondScreen");
        if (second != null) {
            AdsConfig.SecondScreen secondScreen = config.getSecondScreen();
            secondScreen.setSecondAdShow(second.optBoolean("Second_Ad_Show", secondScreen.isSecondAdShow()));
            secondScreen.setSecondAdType(string(second, "Second_Ad_Type", secondScreen.getSecondAdType()));
            secondScreen.setSecondBannerId(string(second, "Second_Banner_Id", secondScreen.getSecondBannerId()));
            secondScreen.setSecondNativeId(string(second, "Second_Native_Id", secondScreen.getSecondNativeId()));
            secondScreen.setSecondButtonAdShow(second.optBoolean("Second_Button_Ad_Show", secondScreen.isSecondButtonAdShow()));
            secondScreen.setSecondButtonAdType(string(second, "Second_Button_Ad_Type", secondScreen.getSecondButtonAdType()));
            secondScreen.setSecondInterstitialId(string(second, "Second_Interstitial_Id", secondScreen.getSecondInterstitialId()));
            secondScreen.setSecondAppOpenId(string(second, "Second_App_Open_Id", secondScreen.getSecondAppOpenId()));
        }

        JSONObject quiz = root.optJSONObject("QuizAdsDesign");
        if (quiz != null) {
            AdsConfig.QuizAdsDesign design = config.getQuizAdsDesign();
            design.setQuizAppIcon(stringList(quiz, "Quiz_App_Icon"));
            design.setQuizNativeMedia(stringList(quiz, "Quiz_Native_Media"));
            design.setQuizInterstitialMedia(stringList(quiz, "Quiz_Interstitial_Media"));
            design.setQuizAppOpenMedia(stringList(quiz, "Quiz_App_Open_Media"));
            design.setQuizTitle(stringList(quiz, "Quiz_Title"));
            design.setQuizDescription(stringList(quiz, "Quiz_Description"));
            design.setQuizButtonText(string(quiz, "Quiz_Button_Text", design.getQuizButtonText()));
        }
        return config;
    }

    @NonNull
    private static List<String> stringList(@NonNull JSONObject json, @NonNull String key) {
        List<String> values = new ArrayList<>();
        JSONArray array = json.optJSONArray(key);
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    @NonNull
    private static String string(@NonNull JSONObject json, @NonNull String key, @NonNull String fallback) {
        String value = json.optString(key, fallback).trim();
        return value.isEmpty() ? fallback : value;
    }
}