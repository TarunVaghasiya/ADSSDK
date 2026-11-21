package com.adssetup.adssdk.sdk.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class PageIndicatorView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float dot;
    private final float extra;
    private final float gap;
    private int count;
    private float position;

    public PageIndicatorView(Context c, AttributeSet a) {
        super(c, a);
        float d = getResources().getDisplayMetrics().density;
        dot = 7f * d;
        extra = 10f * d;
        gap = 10f * d;
        paint.setColor(0xFFFFFFFF);
    }

    public void setCount(int count) {
        if (this.count != count) {
            this.count = count;
            requestLayout();
            invalidate();
        }
    }

    public void setPosition(float position) {
        this.position = position;
        invalidate();
    }

    private float contentWidth() {
        return count == 0 ? 0 : count * dot + extra + (count - 1) * gap;
    }

    @Override
    protected void onMeasure(int w, int h) {
        int width = (int) contentWidth() + getPaddingLeft() + getPaddingRight();
        int height = (int) dot + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(width, w), resolveSize(height, h));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float x = (getWidth() - contentWidth()) / 2f;
        float top = (getHeight() - dot) / 2f;
        for (int i = 0; i < count; i++) {
            float closeness = Math.max(0f, 1f - Math.abs(position - i));
            float w = dot + extra * closeness;
            paint.setAlpha((int) (110 + 145 * closeness));
            rect.set(x, top, x + w, top + dot);
            canvas.drawRoundRect(rect, dot / 2f, dot / 2f, paint);
            x += w + gap;
        }
    }
}