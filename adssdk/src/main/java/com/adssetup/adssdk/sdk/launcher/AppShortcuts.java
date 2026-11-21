package com.adssetup.adssdk.sdk.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adssetup.adssdk.sdk.R;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

final class AppShortcuts {
    static final int MAX = 4;

    static final class Entry {
        final CharSequence label;
        @Nullable
        final Drawable icon;
        final Runnable launch;

        Entry(CharSequence label, @Nullable Drawable icon, Runnable launch) {
            this.label = label;
            this.icon = icon;
            this.launch = launch;
        }
    }

    private AppShortcuts() {
    }

    @NonNull
    static List<Entry> load(@NonNull Context c, @NonNull ComponentName app) {
        List<Entry> out = new ArrayList<>();
        try {
            LauncherApps la = c.getSystemService(LauncherApps.class);
            if (Build.VERSION.SDK_INT >= 25 && la != null && la.hasShortcutHostPermission()) {
                fromSystem(c, la, app, out);
            } else {
                fromManifest(c, app, out);
            }
        } catch (RuntimeException | PackageManager.NameNotFoundException e) {
            out.clear();
        }
        return out;
    }

    @androidx.annotation.RequiresApi(25)
    private static void fromSystem(Context c, LauncherApps la, ComponentName app, List<Entry> out) {
        LauncherApps.ShortcutQuery q = new LauncherApps.ShortcutQuery().setPackage(app.getPackageName()).setActivity(app).setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC);
        List<ShortcutInfo> found = la.getShortcuts(q, Process.myUserHandle());
        if (found == null) return;
        List<ShortcutInfo> list = new ArrayList<>();
        for (ShortcutInfo si : found) if (si.isEnabled()) list.add(si);
        list.sort((x, y) -> {
            int m = Boolean.compare(y.isDeclaredInManifest(), x.isDeclaredInManifest());
            return m != 0 ? m : Integer.compare(x.getRank(), y.getRank());
        });
        final int dpi = c.getResources().getDisplayMetrics().densityDpi;
        for (final ShortcutInfo si : list) {
            if (out.size() >= MAX) break;
            CharSequence label = si.getShortLabel() != null ? si.getShortLabel() : si.getLongLabel();
            if (label == null) continue;
            Drawable icon = null;
            try {
                icon = la.getShortcutIconDrawable(si, dpi);
            } catch (RuntimeException ignored) {
            }
            out.add(new Entry(label, icon, () -> {
                try {
                    la.startShortcut(si, null, null);
                } catch (RuntimeException e) {
                    Toast.makeText(c, R.string.app_launch_failed, Toast.LENGTH_SHORT).show();
                }
            }));
        }
    }

    private static final String NS = "http://schemas.android.com/apk/res/android";

    private static void fromManifest(Context c, ComponentName app, List<Entry> out) throws PackageManager.NameNotFoundException {
        PackageManager pm = c.getPackageManager();
        ActivityInfo ai = pm.getActivityInfo(app, PackageManager.GET_META_DATA);
        int xml = ai.metaData == null ? 0 : ai.metaData.getInt("android.app.shortcuts", 0);
        if (xml == 0) return;
        Resources res = pm.getResourcesForApplication(ai.applicationInfo);
        String pkg = app.getPackageName();
        try (XmlResourceParser p = res.getXml(xml)) {
            boolean inShortcut = false;
            boolean enabled = true;
            CharSequence label = null;
            Drawable icon = null;
            Intent intent = null;
            for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT && out.size() < MAX; ev = p.next()) {
                if (ev == XmlPullParser.START_TAG && "shortcut".equals(p.getName())) {
                    inShortcut = true;
                    enabled = p.getAttributeBooleanValue(NS, "enabled", true);
                    intent = null;
                    label = text(res, p, "shortcutShortLabel");
                    if (label == null) label = text(res, p, "shortcutLongLabel");
                    int iconRes = p.getAttributeResourceValue(NS, "icon", 0);
                    icon = iconRes == 0 ? null : pm.getDrawable(pkg, iconRes, ai.applicationInfo);
                } else if (ev == XmlPullParser.START_TAG && inShortcut && intent == null && "intent".equals(p.getName())) {
                    String action = p.getAttributeValue(NS, "action");
                    String tp = p.getAttributeValue(NS, "targetPackage");
                    String tc = p.getAttributeValue(NS, "targetClass");
                    String data = p.getAttributeValue(NS, "data");
                    if (action != null || tc != null) {
                        intent = new Intent(action != null ? action : Intent.ACTION_MAIN);
                        if (tp != null && tc != null)
                            intent.setComponent(new ComponentName(tp, tc));
                        else if (tp != null) intent.setPackage(tp);
                        if (data != null) intent.setData(Uri.parse(data));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                } else if (ev == XmlPullParser.END_TAG && "shortcut".equals(p.getName())) {
                    if (inShortcut && enabled && label != null && intent != null) {
                        final Intent launch = intent;
                        out.add(new Entry(label, icon, () -> {
                            try {
                                c.startActivity(launch);
                            } catch (RuntimeException e) {
                                Toast.makeText(c, R.string.app_launch_failed, Toast.LENGTH_SHORT).show();
                            }
                        }));
                    }
                    inShortcut = false;
                }
            }
        } catch (org.xmlpull.v1.XmlPullParserException | java.io.IOException e) {
            out.clear();
        }
    }

    @Nullable
    private static CharSequence text(Resources res, XmlResourceParser p, String name) {
        int id = p.getAttributeResourceValue(NS, name, 0);
        if (id != 0) {
            try {
                return res.getString(id);
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }
        return p.getAttributeValue(NS, name);
    }
}