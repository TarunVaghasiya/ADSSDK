package com.adssetup.adssdk.sdk.launcher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;

import com.adssetup.adssdk.sdk.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

final class DeviceLayout {
    enum Mode {APP_DRAWER, ALL_APPS_ON_HOME}

    private static Mode cached;

    private DeviceLayout() {
    }

    private static boolean isAllAppsManufacturer() {
        return false;
    }

    @NonNull
    static String manufacturer() {
        String m = Build.MANUFACTURER;
        return m == null ? "" : m.trim().toLowerCase(Locale.ROOT);
    }

    static void applyLabelSize(@NonNull View cell) {
        if (!isSamsung()) return;
        android.widget.TextView label = cell.findViewById(R.id.app_label);
        if (label != null) label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
    }

    static boolean isSamsung() {
        return manufacturer().equals("samsung");
    }

    @NonNull
    static synchronized Mode mode(@NonNull Context ctx) {
        if (cached == null) {
            Mode m = isAllAppsManufacturer() ? Mode.ALL_APPS_ON_HOME : Mode.APP_DRAWER;
            Mode o = debugOverride(ctx.getApplicationContext());
            cached = o != null ? o : m;
        }
        return cached;
    }

    static boolean isPixel(@NonNull Context ctx) {
        if (manufacturer().equals("google")) return true;
        return "pixel".equals(debugValue(ctx.getApplicationContext()));
    }

    static void styleDrawerLabel(@NonNull android.widget.TextView label) {
        if (!isPixel(label.getContext())) return;
        label.setTextColor(0xFF313131);
        label.setShadowLayer(0f, 0f, 0f, 0);
    }

    static float iconScale(@NonNull Context ctx) {
        return isPixel(ctx) || isMotorola() ? 1.13f : 1f;
    }

    static int iconPx(@NonNull Context ctx) {
        return Math.round(62 * iconScale(ctx) * ctx.getResources().getDisplayMetrics().density);
    }

    private static int homeIconPx = 0;

    static boolean setHomeIconPx(int px) {
        if (px == homeIconPx) return false;
        homeIconPx = px;
        return true;
    }

    static void applyIconSize(@NonNull android.widget.ImageView icon) {
        int px = homeIconPx > 0 ? homeIconPx : iconPx(icon.getContext());
        android.view.ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null && (lp.width != px || lp.height != px)) {
            lp.width = px;
            lp.height = px;
            icon.setLayoutParams(lp);
        }
    }

    static boolean isVivo() {
        return manufacturer().equals("vivo");
    }

    static boolean isMotorola() {
        return manufacturer().equals("motorola");
    }

    private static String debugValue(Context app) {
        if ((app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null;
        File f = new File(app.getFilesDir(), "layout_mode");
        if (!f.isFile()) return null;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String s = r.readLine();
            return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    private static Mode debugOverride(Context app) {
        if ((app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null;
        File f = new File(app.getFilesDir(), "layout_mode");
        if (!f.isFile()) return null;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String s = r.readLine();
            if (s == null) return null;
            s = s.trim().toLowerCase(Locale.ROOT);
            if (s.equals("all")) return Mode.ALL_APPS_ON_HOME;
            if (s.equals("drawer")) return Mode.APP_DRAWER;
        } catch (Exception ignored) {
        }
        return null;
    }
}