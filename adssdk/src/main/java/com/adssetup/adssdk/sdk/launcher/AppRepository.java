package com.adssetup.adssdk.sdk.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AppRepository {
    private static final java.util.Map<ComponentName, AppInfo> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<ComponentName, Long> STAMPS = new java.util.concurrent.ConcurrentHashMap<>();

    private AppRepository() {
    }

    @WorkerThread
    @NonNull
    static List<AppInfo> loadApps(@NonNull Context context) {
        Context app = context.getApplicationContext();
        initMask(app);
        PackageManager pm = app.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(query, 0);

        int iconPx = DeviceLayout.iconPx(app);
        List<AppInfo> result = new ArrayList<>(resolved.size());
        java.util.Set<ComponentName> seen = new java.util.HashSet<>();
        for (ResolveInfo ri : resolved) {
            if (ri.activityInfo == null) continue;
            if (app.getPackageName().equals(ri.activityInfo.packageName)) continue;
            ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            seen.add(cn);
            long stamp = ri.activityInfo.applicationInfo != null ? new java.io.File(ri.activityInfo.applicationInfo.sourceDir).lastModified() : 0L;
            AppInfo cached = CACHE.get(cn);
            Long cachedStamp = STAMPS.get(cn);
            if (cached != null && cachedStamp != null && cachedStamp == stamp) {
                result.add(cached);
                continue;
            }
            CharSequence label = ri.loadLabel(pm);
            AppInfo info = new AppInfo(cn, label.toString(), toBitmap(ri.loadIcon(pm), iconPx));
            CACHE.put(cn, info);
            STAMPS.put(cn, stamp);
            result.add(info);
        }
        CACHE.keySet().retainAll(seen);
        STAMPS.keySet().retainAll(seen);
        final Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        result.sort((a, b) -> {
            int c = collator.compare(a.label, b.label);
            return c != 0 ? c : a.component.compareTo(b.component);
        });
        return result;
    }

    static Bitmap toBitmap(Drawable d, int size) {
        d = wrapLegacyIfNeeded(d, size);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Path ref = referenceMask;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ref != null && d instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable ai = (AdaptiveIconDrawable) d;
                Path mask = new Path(ref);
                Matrix m = new Matrix();
                m.setScale(size / 100f, size / 100f);
                mask.transform(m);
                canvas.clipPath(mask);
                int ext = size / 4;
                for (Drawable layer : new Drawable[]{ai.getBackground(), ai.getForeground()}) {
                    if (layer == null) continue;
                    layer.setBounds(-ext, -ext, size + ext, size + ext);
                    layer.draw(canvas);
                }
            } else {
                d.setBounds(0, 0, size, size);
                d.draw(canvas);
            }
        }
        bmp.prepareToDraw();
        return bmp;
    }

    private static volatile Path referenceMask;

    static synchronized void initMask(Context ctx) {
        if (referenceMask != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !DeviceLayout.isVivo())
            return;
        referenceMask = IconShape.vivoSquircle();
    }

    private static Drawable wrapLegacyIfNeeded(Drawable d, int size) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || d instanceof AdaptiveIconDrawable) {
            return d;
        }
        Bitmap probe = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, size, size);
        d.draw(new Canvas(probe));

        int m = Math.max(1, size / 24);
        boolean cornersOpaque = true;
        int[][] corners = {{m, m}, {size - 1 - m, m}, {m, size - 1 - m}, {size - 1 - m, size - 1 - m}};
        for (int[] p : corners) {
            if (android.graphics.Color.alpha(probe.getPixel(p[0], p[1])) < 240)
                cornersOpaque = false;
        }

        int minX = size, minY = size, maxX = -1, maxY = -1, opaque = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (android.graphics.Color.alpha(probe.getPixel(x, y)) > 16) {
                    opaque++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        float cover = maxX < 0 ? 0f : Math.min(maxX - minX + 1, maxY - minY + 1) / (float) size;
        float area = opaque / (float) (size * size);
        boolean preShapedTile = !cornersOpaque && cover >= 0.9f && area >= 0.7f;

        int bgColor = 0xFFFFFFFF;
        if (preShapedTile || (cornersOpaque && referenceMask != null)) {
            int c = (size - 1) / 2, e = Math.max(1, size / 24);
            int[] edge = {probe.getPixel(c, e), probe.getPixel(c, size - 1 - e), probe.getPixel(e, c), probe.getPixel(size - 1 - e, c)};
            int r = 0, g = 0, b = 0, n = 0;
            for (int px : edge) {
                if (android.graphics.Color.alpha(px) >= 200) {
                    r += android.graphics.Color.red(px);
                    g += android.graphics.Color.green(px);
                    b += android.graphics.Color.blue(px);
                    n++;
                }
            }
            if (n > 0) bgColor = android.graphics.Color.rgb(r / n, g / n, b / n);
        }
        probe.recycle();
        float inset = (cornersOpaque || preShapedTile) ? 1f / 6f : 0.25f;
        Drawable fg = new android.graphics.drawable.InsetDrawable(d, inset);
        Drawable bg = new android.graphics.drawable.ColorDrawable(bgColor);
        return new AdaptiveIconDrawable(bg, fg);
    }
}