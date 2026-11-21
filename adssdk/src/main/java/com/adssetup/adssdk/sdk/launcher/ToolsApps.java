package com.adssetup.adssdk.sdk.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

final class ToolsApps {
    private ToolsApps() {
    }

    private static final String[] CALCULATOR = {"com.sec.android.app.popupcalculator", "com.google.android.calculator", "com.android.calculator2", "com.miui.calculator", "com.coloros.calculator", "com.oplus.calculator", "com.oneplus.calculator", "com.vivo.calculator", "com.motorola.calculator"};
    private static final String[] CLOCK = {"com.sec.android.app.clockpackage", "com.google.android.deskclock", "com.android.deskclock", "com.coloros.alarmclock", "com.oplus.alarmclock", "com.oneplus.deskclock", "com.android.BBKClock", "com.vivo.alarmclock", "com.miui.clock"};
    private static final String[] FILES = {"com.sec.android.app.myfiles", "com.google.android.apps.nbu.files", "com.google.android.documentsui", "com.android.documentsui", "com.mi.android.globalFileexplorer", "com.coloros.filemanager", "com.oplus.filemanager", "com.oneplus.filemanager", "com.vivo.filemanager", "com.android.filemanager", "com.motorola.files", "com.asus.filemanager"};
    private static final String[] NOTES = {"com.samsung.android.app.notes", "com.samsung.android.app.memo", "com.motorola.notes", "com.miui.notes", "com.oneplus.note", "com.oplus.note", "com.coloros.note", "com.vivo.notes", "com.android.notes", "com.google.android.keep"};
    private static final String[] SETTINGS = {"com.android.settings"};

    @NonNull
    static List<AppInfo> resolve(@NonNull Context ctx, @NonNull List<AppInfo> apps) {
        Map<String, AppInfo> byPkg = new HashMap<>();
        for (AppInfo a : apps) byPkg.putIfAbsent(a.component.getPackageName(), a);
        PackageManager pm = ctx.getPackageManager();
        List<AppInfo> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        add(out, seen, byPkg, SuggestedApps.pkgOf(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)), CALCULATOR);
        add(out, seen, byPkg, SuggestedApps.pkgOf(pm, new Intent(AlarmClock.ACTION_SHOW_ALARMS)), CLOCK);
        add(out, seen, byPkg, SuggestedApps.pkgOf(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, "android.intent.category.APP_FILES")), FILES);
        add(out, seen, byPkg, null, NOTES);
        add(out, seen, byPkg, SuggestedApps.pkgOf(pm, new Intent(Settings.ACTION_SETTINGS)), SETTINGS);
        return out;
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