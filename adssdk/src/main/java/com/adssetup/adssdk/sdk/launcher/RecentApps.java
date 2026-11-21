package com.adssetup.adssdk.sdk.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RecentApps {
    static final int MAX = 10;
    private static final String PREFS = "recent_apps";
    private static final String KEY = "recent_v2";
    private static final String OLD_KEY = "mru";

    private RecentApps() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static final class Entry {
        final String pkg;
        final long time;

        Entry(String pkg, long time) {
            this.pkg = pkg;
            this.time = time;
        }
    }

    private static List<Entry> read(SharedPreferences p) {
        List<Entry> out = new ArrayList<>();
        String json = p.getString(KEY, null);
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String pkg = o.optString("p", "");
                if (pkg.isEmpty()) continue;
                boolean dup = false;
                for (Entry e : out) {
                    if (e.pkg.equals(pkg)) {
                        dup = true;
                        break;
                    }
                }
                if (!dup) out.add(new Entry(pkg, o.optLong("t", 0L)));
            }
        } catch (JSONException e) {
            p.edit().remove(KEY).apply();
            return new ArrayList<>();
        }
        out.sort((a, b) -> Long.compare(b.time, a.time));
        return out;
    }

    private static void write(SharedPreferences p, List<Entry> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry e : list) arr.put(new JSONObject().put("p", e.pkg).put("t", e.time));
        } catch (JSONException ignored) {
            return;
        }
        p.edit().putString(KEY, arr.toString()).remove(OLD_KEY).apply();
    }

    static void record(@NonNull Context ctx, @NonNull ComponentName c) {
        String pkg = c.getPackageName();
        if (pkg.equals(ctx.getPackageName())) return;
        SharedPreferences p = prefs(ctx);
        List<Entry> list = read(p);
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).pkg.equals(pkg)) list.remove(i);
        list.add(0, new Entry(pkg, System.currentTimeMillis()));
        while (list.size() > MAX) list.remove(list.size() - 1);
        write(p, list);
    }

    @NonNull
    static List<AppInfo> load(@NonNull Context ctx, @NonNull List<AppInfo> apps) {
        List<AppInfo> out = new ArrayList<>();
        SharedPreferences p = prefs(ctx);
        List<Entry> stored = read(p);
        if (stored.isEmpty() || apps.isEmpty()) return out;
        Map<String, AppInfo> byPkg = new HashMap<>();
        for (AppInfo a : apps) byPkg.putIfAbsent(a.component.getPackageName(), a);
        List<Entry> valid = new ArrayList<>();
        for (Entry e : stored) {
            AppInfo a = byPkg.get(e.pkg);
            if (a == null) continue;
            valid.add(e);
            out.add(a);
        }
        if (valid.size() != stored.size()) write(p, valid);
        return out;
    }
}