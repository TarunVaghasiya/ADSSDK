package com.adssetup.adssdk.sdk;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class AdsController {
    private static volatile boolean adsEnabled = true;
    private static volatile boolean quizPriority;
    private static volatile boolean quizFallbackEnabled = true;
    private static final Random RANDOM = new Random();
    private static volatile List<String> quizIcons = Collections.emptyList();
    private static volatile List<String> quizMedia = Collections.emptyList();
    private static volatile List<String> quizTitles = Collections.emptyList();
    private static volatile List<String> quizDescriptions = Collections.emptyList();
    private static volatile String quizButtonText = "";

    private AdsController() {
    }

    public static void setAdsEnabled(boolean enabled) {
        adsEnabled = enabled;
    }

    public static boolean isAdsEnabled() {
        return adsEnabled;
    }

    public static void setQuizPriority(boolean quizPriority) {
        AdsController.quizPriority = quizPriority;
    }

    public static boolean isQuizPriority() {
        return quizPriority;
    }

    public static void setQuizFallbackEnabled(boolean enabled) {
        quizFallbackEnabled = enabled;
    }

    public static boolean isQuizFallbackEnabled() {
        return quizFallbackEnabled;
    }

    public static void setQuizDesign(@Nullable List<String> icons, @Nullable List<String> media, @Nullable List<String> titles, @Nullable List<String> descriptions, @Nullable String buttonText) {
        quizIcons = icons == null ? Collections.emptyList() : icons;
        quizMedia = media == null ? Collections.emptyList() : media;
        quizTitles = titles == null ? Collections.emptyList() : titles;
        quizDescriptions = descriptions == null ? Collections.emptyList() : descriptions;
        quizButtonText = buttonText == null ? "" : buttonText;
    }

    static void bindQuizDesign(@NonNull View root) {
        bindImage(root.findViewById(R.id.ivQZAppIcon), pick(quizIcons, RANDOM.nextInt(Integer.MAX_VALUE)));
        bindImage(root.findViewById(R.id.ivQZAppMedia), pick(quizMedia, RANDOM.nextInt(Integer.MAX_VALUE)));
        int textIndex = RANDOM.nextInt(Integer.MAX_VALUE);
        bindText(root.findViewById(R.id.tvQZAppTitle), pick(quizTitles, textIndex));
        bindText(root.findViewById(R.id.tvQZAppDescription), pick(quizDescriptions, textIndex));
        bindText(root.findViewById(R.id.btnQZClick), quizButtonText);
    }

    @Nullable
    private static String pick(@NonNull List<String> values, int randomIndex) {
        return values.isEmpty() ? null : values.get(randomIndex % values.size());
    }

    private static void bindImage(@Nullable View view, @Nullable String url) {
        if (view instanceof ImageView && url != null && !url.isEmpty()) {
            Glide.with(view).load(url).into((ImageView) view);
        }
    }

    private static void bindText(@Nullable View view, @Nullable String text) {
        if (view instanceof TextView && text != null && !text.isEmpty()) {
            ((TextView) view).setText(text);
        }
    }
}