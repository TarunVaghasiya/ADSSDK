package com.adssetup.adssdk.sdk.launcher;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;

import androidx.annotation.NonNull;

final class IconShape {
    private static final float MASK_SIZE = 100f;
    private static Path mask;
    private static final Matrix MATRIX = new Matrix();

    private IconShape() {
    }

    @NonNull
    static Path vivoSquircle() {
        final double n = 4.5;
        Path p = new Path();
        final int steps = 360;
        for (int i = 0; i <= steps; i++) {
            double t = 2 * Math.PI * i / steps;
            double c = Math.cos(t), s = Math.sin(t);
            float x = (float) (MASK_SIZE / 2 + MASK_SIZE / 2 * Math.signum(c) * Math.pow(Math.abs(c), 2 / n));
            float y = (float) (MASK_SIZE / 2 + MASK_SIZE / 2 * Math.signum(s) * Math.pow(Math.abs(s), 2 / n));
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    private static synchronized Path mask() {
        if (mask == null) {
            Path p = null;
            if (DeviceLayout.isVivo()) p = vivoSquircle();
            if (p == null && Build.VERSION.SDK_INT >= 26) {
                try {
                    AdaptiveIconDrawable d = new AdaptiveIconDrawable(new ColorDrawable(0), new ColorDrawable(0));
                    d.setBounds(0, 0, (int) MASK_SIZE, (int) MASK_SIZE);
                    Path m = d.getIconMask();
                    if (m != null && !m.isEmpty()) p = new Path(m);
                } catch (Throwable ignored) {
                }
            }
            if (p == null) {
                p = new Path();
                p.addRoundRect(0, 0, MASK_SIZE, MASK_SIZE, 22f, 22f, Path.Direction.CW);
            }
            mask = p;
        }
        return mask;
    }

    static synchronized void fit(@NonNull Path out, float left, float top, float size) {
        out.reset();
        out.set(mask());
        MATRIX.setScale(size / MASK_SIZE, size / MASK_SIZE);
        MATRIX.postTranslate(left, top);
        out.transform(MATRIX);
    }
}