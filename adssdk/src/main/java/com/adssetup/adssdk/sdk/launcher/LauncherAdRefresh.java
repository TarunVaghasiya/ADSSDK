package com.adssetup.adssdk.sdk.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Decides when a launcher ad placement may request a new ad. Nothing here schedules work: the caller asks
 * {@link #onVisible()} each time the placement becomes visible, so ads are only requested while it is on screen.
 *
 * <p>The configured "per day" count is converted to an interval of 24 h / count, never shorter than
 * {@link #MIN_INTERVAL_MS} (the shortest refresh interval AdMob allows). A count of zero, a negative or an invalid
 * value disables timed refresh: the placement then loads once per view, as before. The time of the last successful
 * load is persisted, so the interval also applies across activity recreation and process restarts.
 */
final class LauncherAdRefresh {
    enum Action {NONE, LOAD, REFRESH}

    static final long MIN_INTERVAL_MS = 30_000L;
    static final long NATIVE_MAX_AGE_MS = 60L * 60L * 1000L;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long MAX_BACKOFF_MS = 10L * 60L * 1000L;
    private static final String PREFS = "launcher_ad_refresh";

    private final Context appContext;
    private final SharedPreferences prefs;
    private final String key;
    private final long intervalMs;
    private final long maxLiveAgeMs;
    private boolean inFlight;
    private boolean attempted;
    private boolean live;
    private int failures;
    private long nextAttemptElapsed;
    private View adView;
    private WeakReference<Object> ownerActivity;
    private static final Map<String, LauncherAdRefresh> SHARED = new HashMap<>();

    /**
     * One state per key for the whole process, for placements whose view can be replaced while it is on screen
     * (a RecyclerView may recreate its holder), so the replacement does not start a second request.
     */
    static synchronized LauncherAdRefresh shared(@NonNull Context context, @NonNull String key, int showPerDay, long maxLiveAgeMs) {
        LauncherAdRefresh existing = SHARED.get(key);
        if (existing != null && existing.intervalMs == intervalFor(showPerDay) && existing.maxLiveAgeMs == maxLiveAgeMs) return existing;
        LauncherAdRefresh created = new LauncherAdRefresh(context, key, showPerDay, maxLiveAgeMs);
        SHARED.put(key, created);
        return created;
    }

    private static boolean same(@Nullable WeakReference<Object> ref, @Nullable Object o) {
        return ref != null && ref.get() == o;
    }

    /**
     * @param maxLiveAgeMs longest time a loaded ad may stay on screen before it must be replaced when the placement
     *                     becomes visible again (the provider treats native ads as expired after about an hour), or 0
     */
    LauncherAdRefresh(@NonNull Context context, @NonNull String key, int showPerDay, long maxLiveAgeMs) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.key = key;
        this.intervalMs = intervalFor(showPerDay);
        this.maxLiveAgeMs = maxLiveAgeMs;
    }

    static long intervalFor(int showPerDay) {
        if (showPerDay <= 0) return 0L;
        return Math.max(MIN_INTERVAL_MS, DAY_MS / showPerDay);
    }

    boolean isTimed() {
        return intervalMs > 0;
    }

    /**
     * The ad view that holds the loaded ad. Pager holders are recycled and replaced, so the view is kept here and moved
     * into whichever holder is on screen instead of requesting another ad.
     */
    @Nullable
    View sharedAdView(@Nullable Object activity) {
        return same(ownerActivity, activity) ? adView : null;
    }

    void shareAdView(@NonNull View view, @Nullable Object activity) {
        adView = view;
        ownerActivity = new WeakReference<>(activity);
        if (activity instanceof androidx.lifecycle.LifecycleOwner) {
            // Held strongly so a recycled pager holder cannot drop the loaded ad; released with its activity.
            ((androidx.lifecycle.LifecycleOwner) activity).getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull androidx.lifecycle.LifecycleOwner owner) {
                    if (adView == view) {
                        adView = null;
                        live = false;
                    }
                }
            });
        }
    }

    /** Non-mutating twin of {@link #onVisible(Object)}: would a request start right now? */
    boolean wouldRequest(@Nullable Object activity) {
        boolean sameActivity = activity == null || same(ownerActivity, activity);
        if (sameActivity && inFlight) return false;
        if (!isTimed()) return !(sameActivity && attempted);
        boolean liveNow = sameActivity && live;
        long age = persistedAge();
        long replaceAfter = maxLiveAgeMs > 0 ? Math.min(intervalMs, maxLiveAgeMs) : intervalMs;
        if (liveNow ? age < replaceAfter : age < intervalMs) return false;
        return SystemClock.elapsedRealtime() >= nextAttemptElapsed;
    }

    Action onVisible() {
        return onVisible(null);
    }

    /**
     * @param activity the activity hosting the placement. A different activity means the previous ad view is gone, so
     *                 whatever was live or in flight no longer counts.
     */
    Action onVisible(@Nullable Object activity) {
        if (activity != null && !same(ownerActivity, activity)) {
            live = false;
            inFlight = false;
            attempted = false;
            adView = null;
            ownerActivity = new WeakReference<>(activity);
        }
        if (inFlight) return Action.NONE;
        if (!isTimed()) {
            if (attempted) return Action.NONE;
            attempted = true;
            inFlight = true;
            return Action.LOAD;
        }
        long age = persistedAge();
        long replaceAfter = maxLiveAgeMs > 0 ? Math.min(intervalMs, maxLiveAgeMs) : intervalMs;
        if (live ? age < replaceAfter : age < intervalMs) return Action.NONE;
        if (SystemClock.elapsedRealtime() < nextAttemptElapsed) return Action.NONE;
        attempted = true;
        inFlight = true;
        return live ? Action.REFRESH : Action.LOAD;
    }

    void onResult(boolean success) {
        inFlight = false;
        long now = SystemClock.elapsedRealtime();
        if (success) {
            live = true;
            failures = 0;
            nextAttemptElapsed = 0L;
            prefs.edit().putLong(key + "_wall", System.currentTimeMillis()).putLong(key + "_elapsed", now).putInt(key + "_boot", bootCount()).apply();
        } else {
            live = false;
            failures++;
            long backoff = Math.min(MAX_BACKOFF_MS, MIN_INTERVAL_MS << Math.min(failures - 1, 4));
            nextAttemptElapsed = now + backoff;
        }
    }

    boolean isLive() {
        return live;
    }

    private long persistedAge() {
        long wall = prefs.getLong(key + "_wall", 0L);
        if (wall == 0L) return Long.MAX_VALUE;
        long storedElapsed = prefs.getLong(key + "_elapsed", -1L);
        long nowElapsed = SystemClock.elapsedRealtime();
        if (prefs.getInt(key + "_boot", -1) == bootCount() && storedElapsed >= 0L && nowElapsed >= storedElapsed) {
            return nowElapsed - storedElapsed;
        }
        long wallAge = System.currentTimeMillis() - wall;
        return wallAge < 0L ? Long.MAX_VALUE : wallAge;
    }

    private int bootCount() {
        return Settings.Global.getInt(appContext.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
    }
}
