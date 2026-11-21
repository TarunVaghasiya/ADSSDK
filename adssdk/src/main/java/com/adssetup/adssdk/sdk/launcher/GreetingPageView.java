package com.adssetup.adssdk.sdk.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.adssetup.adssdk.sdk.AdsController;
import com.adssetup.adssdk.sdk.BannerAdCustomView;
import com.adssetup.adssdk.sdk.BannerAdPlacement;
import com.adssetup.adssdk.sdk.NativeAdCustomView;
import com.adssetup.adssdk.sdk.NativeAdPlacement;
import com.adssetup.adssdk.sdk.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class GreetingPageView extends ScrollView {
    private static final int SUGGESTED_COLUMNS = 4;
    private final LinearLayout content;
    private final TextView title;
    private final TextView date;
    private final LinearLayout recentRows;
    private final TextView recentNone;
    private final android.widget.ImageView gameIcon;
    private final LinearLayout suggestedRows;
    private final LinearLayout gamesRows;
    private boolean receiverRegistered;
    private boolean adSetUp;
    private boolean adShown;
    private String adUnitIdInUse = "";
    private View adView;
    private LauncherAdRefresh adRefresh;
    private Consumer<AppInfo> onApp;
    private AppInfo game;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    GreetingPageView(@NonNull Context context) {
        super(context);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setOverScrollMode(OVER_SCROLL_NEVER);
        setVerticalScrollBarEnabled(false);
        setClipToPadding(false);
        float d = getResources().getDisplayMetrics().density;
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int side = Math.round(14 * d);
        content.setPadding(side, 0, side, Math.round(14 * d));
        addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LayoutInflater.from(context).inflate(R.layout.page_greeting, content, true);
        title = findViewById(R.id.greeting_title);
        date = findViewById(R.id.greeting_date);
        recentRows = findViewById(R.id.recent_rows);
        recentNone = findViewById(R.id.recent_none);
        View gameBox = findViewById(R.id.game_box);
        gameIcon = findViewById(R.id.game_icon);
        suggestedRows = findViewById(R.id.suggested_rows);
        gamesRows = findViewById(R.id.games_rows);
        final float radius = 22 * d;
        gameBox.setClipToOutline(true);
        gameBox.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        gameBox.setOnClickListener(v -> {
            if (game != null && onApp != null) onApp.accept(game);
        });
        refresh();
        Activity host = activityOf(context);
        if (host != null) setUpAd(host, true);
    }

    @Nullable
    private static Activity activityOf(@NonNull Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * Builds the ad slot before the page is first drawn, so it is laid out (with the shimmer inside) from the first
     * frame instead of appearing when the pager settles on the page. [deferred] is true when called at construction:
     * an ad that already exists is then only adopted later, from {@link #loadAd}, so a page that is merely being
     * created cannot take it from the page on screen.
     */
    private void setUpAd(@NonNull Activity activity, boolean deferred) {
        if (adSetUp) return;
        View container = findViewById(R.id.greeting_ad);
        String type = AdsController.getLauncherLastPageAdType();
        boolean banner = "banner".equalsIgnoreCase(type);
        boolean nativeAd = "native".equalsIgnoreCase(type);
        String adUnitId = banner ? AdsController.getLauncherLastPageBannerId() : nativeAd ? AdsController.getLauncherLastPageNativeId() : "";
        if (!AdsController.isAdsEnabled() || !AdsController.isLauncherLastPageAdShow() || adUnitId.isEmpty()) {
            adSetUp = true;
            container.setVisibility(GONE);
            return;
        }
        LauncherAdRefresh refresh = LauncherAdRefresh.shared(getContext(), "launcher_last_page_ad", AdsController.isQuizPriority() ? 0 : AdsController.getLauncherLastPageAdShowPerDay(), nativeAd ? LauncherAdRefresh.NATIVE_MAX_AGE_MS : 0L);
        View shared = refresh.sharedAdView(activity);
        boolean adopt = shared != null && (shared instanceof BannerAdCustomView) == banner;
        if (adopt && deferred) return;
        adSetUp = true;
        adUnitIdInUse = adUnitId;
        adRefresh = refresh;
        ViewGroup box = (ViewGroup) container;
        float density = getResources().getDisplayMetrics().density;
        int slotWidth = Math.min(Math.round(300 * density), getResources().getDisplayMetrics().widthPixels - Math.round(28 * density));
        if (adopt) {
            // The pager gave this page a new holder: move the ad that is already loaded (or loading) into it.
            if (shared.getParent() instanceof ViewGroup)
                ((ViewGroup) shared.getParent()).removeView(shared);
            box.addView(shared, shared.getLayoutParams());
            adView = shared;
        } else if (banner) {
            adView = findViewById(R.id.greeting_banner_ad);
            ViewGroup.LayoutParams lp = adView.getLayoutParams();
            lp.width = slotWidth;
            adView.setLayoutParams(lp);
            refresh.shareAdView(adView, activity);
        } else {
            adView = LayoutInflater.from(lightContext(getContext())).inflate(R.layout.view_greeting_native_ad, box, false);
            box.addView(adView);
            refresh.shareAdView(adView, activity);
        }
        if (banner) ((BannerAdCustomView) adView).setLoadListener(refresh::onResult);
        else ((NativeAdCustomView) adView).setLoadListener(refresh::onResult);

        boolean show = adopt ? adView.getVisibility() == VISIBLE : refresh.wouldRequest(activity);
        adView.setVisibility(show ? VISIBLE : GONE);
        container.setVisibility(show ? VISIBLE : GONE);
        adShown = show;
        if (show && !adopt) {
            if (banner) ((BannerAdCustomView) adView).showPlaceholder(slotWidth);
            else ((NativeAdCustomView) adView).showPlaceholder();
        }
        container.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (adShown && adView.getVisibility() == GONE) {
                adShown = false;
                v.post(() -> v.setVisibility(GONE));
            }
        });
    }

    void loadAd(@NonNull Activity activity) {
        View container = findViewById(R.id.greeting_ad);
        setUpAd(activity, false);
        if (adRefresh == null || adView == null) return;
        if (adRefresh.onVisible(activity) == LauncherAdRefresh.Action.NONE) {
            adShown = adView.getVisibility() == VISIBLE;
            container.setVisibility(adShown ? VISIBLE : GONE);
            return;
        }
        adShown = true;
        container.setVisibility(VISIBLE);
        adView.setVisibility(VISIBLE);
        final String adUnitId = adUnitIdInUse;
        if (adView instanceof BannerAdCustomView) {
            adView.post(() -> BannerAdPlacement.refreshBannerAdKeepingCurrent(activity, adUnitId, (BannerAdCustomView) adView));
        } else {
            NativeAdPlacement.refreshNativeAdKeepingCurrent(activity, adUnitId, (NativeAdCustomView) adView);
        }
    }

    @NonNull
    private static Context lightContext(@NonNull Context base) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_NO;
        ContextThemeWrapper light = new ContextThemeWrapper(base, R.style.Theme_LauncherSDK);
        light.applyOverrideConfiguration(config);
        return light;
    }

    void setTopOffset(int px) {
        if (content.getPaddingTop() != px) {
            content.setPadding(content.getPaddingLeft(), px, content.getPaddingRight(), content.getPaddingBottom());
        }
    }

    void bind(@NonNull List<AppInfo> recentApps, AppInfo blockBlast, @NonNull List<AppInfo> suggested, @NonNull List<AppInfo> games, @NonNull Consumer<AppInfo> click) {
        this.onApp = click;
        this.game = blockBlast;
        if (blockBlast != null) {
            gameIcon.setImageBitmap(blockBlast.icon);
        }
        float d = getResources().getDisplayMetrics().density;
        LayoutInflater inf = LayoutInflater.from(getContext());

        recentRows.removeAllViews();
        int n = Math.min(4, recentApps.size());
        recentNone.setVisibility(n == 0 ? VISIBLE : GONE);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            recentRows.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            for (int c = 0; c < 2; c++) {
                int i = r * 2 + c;
                row.addView(miniCell(inf, row, i < n ? recentApps.get(i) : null));
            }
        }

        suggestedRows.removeAllViews();
        int m = Math.min(8, suggested.size());
        int rows = (m + SUGGESTED_COLUMNS - 1) / SUGGESTED_COLUMNS;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            suggestedRows.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(100 * d)));
            for (int c = 0; c < SUGGESTED_COLUMNS; c++) {
                int i = r * SUGGESTED_COLUMNS + c;
                row.addView(miniCell(inf, row, i < m ? suggested.get(i) : null));
            }
        }
        findViewById(R.id.suggested_section).setVisibility(m == 0 ? GONE : VISIBLE);

        gamesRows.removeAllViews();
        int g = Math.min(8, games.size());
        int gRows = (g + SUGGESTED_COLUMNS - 1) / SUGGESTED_COLUMNS;
        for (int r = 0; r < gRows; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            gamesRows.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(100 * d)));
            for (int c = 0; c < SUGGESTED_COLUMNS; c++) {
                int i = r * SUGGESTED_COLUMNS + c;
                row.addView(miniCell(inf, row, i < g ? games.get(i) : null));
            }
        }
        findViewById(R.id.games_section).setVisibility(g == 0 ? GONE : VISIBLE);
    }

    private static void shrinkIcon(android.widget.ImageView icon) {
        DeviceLayout.applyIconSize(icon);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null && lp.width > 0) {
            int px = Math.round(lp.width * 0.88f);
            lp.width = px;
            lp.height = px;
            icon.setLayoutParams(lp);
        }
    }

    private View.OnLayoutChangeListener scrollRestorer;

    void restoreScroll(int y) {
        cancelScrollRestore();
        if (y <= 0) return;
        scrollTo(0, y);
        scrollRestorer = (v, l, t, r, b, ol, ot, or, ob) -> {
            scrollTo(0, y);
            if (content.getHeight() - getHeight() >= y) {
                cancelScrollRestore();
            }
        };
        content.addOnLayoutChangeListener(scrollRestorer);
        postDelayed(this::cancelScrollRestore, 4000);
    }

    private void cancelScrollRestore() {
        if (scrollRestorer != null) {
            content.removeOnLayoutChangeListener(scrollRestorer);
            scrollRestorer = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
        if (ev.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) cancelScrollRestore();
        return super.onInterceptTouchEvent(ev);
    }

    void scrollToTopNow() {
        scrollTo(0, 0);
    }

    private View miniCell(LayoutInflater inf, ViewGroup parent, AppInfo app) {
        View cell = inf.inflate(R.layout.item_app, parent, false);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        DeviceLayout.applyLabelSize(cell);
        android.widget.ImageView icon = cell.findViewById(R.id.app_icon);
        shrinkIcon(icon);
        if (app != null) {
            icon.setImageBitmap(app.icon);
            ((TextView) cell.findViewById(R.id.app_label)).setText(app.label);
            cell.setOnClickListener(v -> {
                if (onApp != null) onApp.accept(app);
            });
        } else {
            cell.setVisibility(INVISIBLE);
            cell.setOnClickListener(null);
            cell.setClickable(false);
        }
        return cell;
    }

    @StringRes
    static int greetingFor(int hour) {
        if (hour >= 5 && hour < 12) return R.string.greeting_morning;
        if (hour >= 12 && hour < 17) return R.string.greeting_afternoon;
        if (hour >= 17 && hour < 21) return R.string.greeting_evening;
        return R.string.greeting_night;
    }

    void refresh() {
        Calendar now = Calendar.getInstance();
        int res = greetingFor(now.get(Calendar.HOUR_OF_DAY));
        CharSequence g = getContext().getString(res);
        if (!g.equals(title.getText())) title.setText(g);
        Locale locale = Locale.getDefault();
        SimpleDateFormat f = new SimpleDateFormat("EEEE, MMMM d", locale);
        f.setTimeZone(now.getTimeZone());
        String dt = f.format(new Date(now.getTimeInMillis()));
        if (!dt.contentEquals(date.getText())) date.setText(dt);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_DATE_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            androidx.core.content.ContextCompat.registerReceiver(getContext(), receiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (receiverRegistered) {
            getContext().unregisterReceiver(receiver);
            receiverRegistered = false;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) refresh();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) refresh();
    }
}
