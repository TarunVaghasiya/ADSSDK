package com.adssetup.adssdk.sdk;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerDrawable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class AdsController {
    private static volatile boolean adsEnabled = true;
    private static volatile boolean quizPriority;
    private static volatile boolean quizFallbackEnabled = true;
    private static final Random RANDOM = new Random();
    private static final String[] QUIZ_INTERSTITIAL_RATES = {"4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7", "4.8", "4.9"};
    private static final String[] QUIZ_INTERSTITIAL_USERS = {"50K+ Users", "100K+ Users", "500K+ Users", "1M+ Users"};
    private static volatile List<String> quizIcons = Collections.emptyList();
    private static volatile List<String> quizNativeMedia = Collections.emptyList();
    private static volatile List<String> quizInterstitialMedia = Collections.emptyList();
    private static volatile List<String> quizAppOpenMedia = Collections.emptyList();
    private static volatile List<String> quizTitles = Collections.emptyList();
    private static volatile List<String> quizDescriptions = Collections.emptyList();
    private static volatile String quizButtonText = "";
    private static volatile Integer nativeLabelColor;
    private static volatile Integer nativeButtonColor;

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

    public static void setQuizDesign(@Nullable List<String> icons, @Nullable List<String> nativeMedia, @Nullable List<String> interstitialMedia, @Nullable List<String> appOpenMedia, @Nullable List<String> titles, @Nullable List<String> descriptions, @Nullable String buttonText) {
        quizIcons = icons == null ? Collections.emptyList() : icons;
        quizNativeMedia = nativeMedia == null ? Collections.emptyList() : nativeMedia;
        quizInterstitialMedia = interstitialMedia == null ? Collections.emptyList() : interstitialMedia;
        quizAppOpenMedia = appOpenMedia == null ? Collections.emptyList() : appOpenMedia;
        quizTitles = titles == null ? Collections.emptyList() : titles;
        quizDescriptions = descriptions == null ? Collections.emptyList() : descriptions;
        quizButtonText = buttonText == null ? "" : buttonText;
    }

    static void bindQuizDesign(@NonNull View root) {
        bindQuizDesign(root, quizNativeMedia);
    }

    static void bindInterstitialQuizDesign(@NonNull View root) {
        bindQuizDesign(root, quizInterstitialMedia);
        String rate = QUIZ_INTERSTITIAL_RATES[RANDOM.nextInt(QUIZ_INTERSTITIAL_RATES.length)];
        bindText(root.findViewById(R.id.tvQZRate), rate);
        View ratingBar = root.findViewById(R.id.rbQZRating);
        if (ratingBar instanceof RatingBar) {
            try {
                ((RatingBar) ratingBar).setRating(Math.round(Float.parseFloat(rate)));
            } catch (NumberFormatException ignored) {
            }
        }
        bindText(root.findViewById(R.id.tvQZUser), QUIZ_INTERSTITIAL_USERS[RANDOM.nextInt(QUIZ_INTERSTITIAL_USERS.length)]);
    }

    static void bindAppOpenQuizDesign(@NonNull View root) {
        bindQuizDesign(root, quizAppOpenMedia);
    }

    private static void bindQuizDesign(@NonNull View root, @NonNull List<String> media) {
        bindImage(root.findViewById(R.id.ivQZAppIcon), pick(quizIcons, RANDOM.nextInt(Integer.MAX_VALUE)));
        bindImage(root.findViewById(R.id.ivQZAppMedia), pick(media, RANDOM.nextInt(Integer.MAX_VALUE)));
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
        if (!(view instanceof ImageView)) {
            return;
        }
        ImageView imageView = (ImageView) view;
        Context context = view.getContext();
        Glide.with(imageView).clear(imageView);
        ColorDrawable fallback = new ColorDrawable(ContextCompat.getColor(context, R.color.shimmer_block));
        if (url == null || url.isEmpty()) {
            imageView.setImageDrawable(fallback);
            return;
        }
        ShimmerDrawable shimmer = new ShimmerDrawable();
        shimmer.setShimmer(new Shimmer.ColorHighlightBuilder().setBaseColor(ContextCompat.getColor(context, R.color.shimmer_block)).setHighlightColor(ContextCompat.getColor(context, R.color.card_background)).setDuration(1200).build());
        shimmer.startShimmer();
        Glide.with(imageView).load(url).placeholder(shimmer).error(fallback).listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                shimmer.stopShimmer();
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                shimmer.stopShimmer();
                return false;
            }
        }).into(imageView);
    }

    private static void bindText(@Nullable View view, @Nullable String text) {
        if (view instanceof TextView && text != null && !text.isEmpty()) {
            ((TextView) view).setText(text);
        }
    }

    public static void setNativeColors(@Nullable String labelColor, @Nullable String buttonColor) {
        nativeLabelColor = parseColor(labelColor);
        nativeButtonColor = parseColor(buttonColor);
    }

    @Nullable
    private static Integer parseColor(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String hex = value.trim();
        if (hex.isEmpty()) {
            return null;
        }
        try {
            return Color.parseColor(hex.startsWith("#") ? hex : "#" + hex);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static void applyNativeQuizColors(@NonNull View root) {
        applyNativeColors(root, R.id.tvQZLabel, R.id.btnQZClick);
    }

    static void applyNativeColors(@NonNull View root, int labelId, int buttonId) {
        Integer label = nativeLabelColor;
        Integer button = nativeButtonColor;
        if (label != null) {
            View labelView = root.findViewById(labelId);
            if (labelView != null) {
                ViewCompat.setBackgroundTintList(labelView, ColorStateList.valueOf(label));
            }
        }
        if (button != null) {
            View buttonView = root.findViewById(buttonId);
            if (buttonView != null) {
                ViewCompat.setBackgroundTintList(buttonView, ColorStateList.valueOf(button));
            }
        }
    }
}