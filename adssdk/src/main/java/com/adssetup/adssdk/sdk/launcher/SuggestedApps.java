package com.adssetup.adssdk.sdk.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.AlarmClock;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SuggestedApps {
    private SuggestedApps() {
    }

    private static final String[] WHATSAPP = {"com.whatsapp", "com.whatsapp.w4b"};
    private static final String[] INSTAGRAM = {"com.instagram.android"};
    private static final String[] SETTINGS = {"com.android.settings"};
    private static final String[] CHROME = {"com.android.chrome"};
    private static final String[] YOUTUBE = {"com.google.android.youtube"};
    private static final String[] FILES = {"com.sec.android.app.myfiles", "com.google.android.apps.nbu.files", "com.google.android.documentsui", "com.android.documentsui", "com.mi.android.globalFileexplorer", "com.coloros.filemanager", "com.oppo.filemanager", "com.vivo.filemanager", "com.motorola.files", "com.asus.filemanager", "com.oneplus.filemanager"};
    private static final String[] CALCULATOR = {"com.sec.android.app.popupcalculator", "com.google.android.calculator", "com.android.calculator2", "com.miui.calculator", "com.coloros.calculator", "com.oneplus.calculator", "com.vivo.calculator", "com.motorola.calculator"};
    private static final String[] ALARM = {"com.sec.android.app.clockpackage", "com.google.android.deskclock", "com.android.deskclock", "com.android.alarmclock", "com.coloros.alarmclock", "com.oneplus.deskclock", "com.miui.clock"};

    @NonNull
    static List<AppInfo> resolve(@NonNull Context ctx, @NonNull List<AppInfo> apps) {
        Map<String, AppInfo> byPkg = new HashMap<>();
        for (AppInfo a : apps) byPkg.putIfAbsent(a.component.getPackageName(), a);
        PackageManager pm = ctx.getPackageManager();
        List<AppInfo> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        add(out, seen, byPkg, null, WHATSAPP);
        add(out, seen, byPkg, null, INSTAGRAM);
        add(out, seen, byPkg, pkgOf(pm, new Intent(Settings.ACTION_SETTINGS)), SETTINGS);
        add(out, seen, byPkg, null, CHROME);
        add(out, seen, byPkg, null, YOUTUBE);
        add(out, seen, byPkg, pkgOf(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, "android.intent.category.APP_FILES")), FILES);
        add(out, seen, byPkg, pkgOf(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)), CALCULATOR);
        add(out, seen, byPkg, pkgOf(pm, new Intent(AlarmClock.ACTION_SHOW_ALARMS)), ALARM);
        return out;
    }

    @Nullable
    static String pkgOf(PackageManager pm, Intent intent) {
        try {
            ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri == null || ri.activityInfo == null) return null;
            String p = ri.activityInfo.packageName;
            return "android".equals(p) ? null : p;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void add(List<AppInfo> out, Set<String> seen, Map<String, AppInfo> byPkg, @Nullable String resolvedPkg, String[] packages) {
        if (resolvedPkg != null) {
            AppInfo a = byPkg.get(resolvedPkg);
            if (a != null && seen.add(resolvedPkg)) {
                out.add(a);
                return;
            }
        }
        for (String p : packages) {
            AppInfo a = byPkg.get(p);
            if (a != null && seen.add(p)) {
                out.add(a);
                return;
            }
        }
    }
}