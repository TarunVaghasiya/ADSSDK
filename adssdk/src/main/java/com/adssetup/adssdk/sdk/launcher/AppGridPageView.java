package com.adssetup.adssdk.sdk.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adssetup.adssdk.sdk.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class AppGridPageView extends ViewGroup {
    interface Listener {
        void onItemClicked(@NonNull View cell, @NonNull HomeItem item);

        void onItemLongClicked(@NonNull View cell, @NonNull HomeItem item);
    }

    private final Listener listener;
    private final int horizontalPadding;
    private final float density;
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private HomeItem[] cells = new HomeItem[0];
    private int[] cellOfChild = new int[0];
    private int columns = 4;
    private int rows = 1;
    @Nullable
    private String dragHiddenKey;
    @Nullable
    private String dragMergeKey;
    private int fixedCellH;
    private int hoverCell = -1;
    private final android.graphics.Path ringPath = new android.graphics.Path();
    private int lastIconPx;

    AppGridPageView(@NonNull Context context, @NonNull Listener listener) {
        super(context);
        this.listener = listener;
        density = context.getResources().getDisplayMetrics().density;
        this.horizontalPadding = Math.round(8 * density);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2.5f * density);
        ringPaint.setColor(0xCCFFFFFF);
        setWillNotDraw(false);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    void setFixedCellHeight(int px) {
        if (px != fixedCellH) {
            fixedCellH = px;
            requestLayout();
        }
    }

    @Nullable
    HomeItem itemAtCell(int cell) {
        return cell >= 0 && cell < cells.length ? cells[cell] : null;
    }

    @Nullable
    View viewAtCell(int cell) {
        for (int i = 0; i < cellOfChild.length && i < getChildCount(); i++) {
            if (cellOfChild[i] == cell) return getChildAt(i);
        }
        return null;
    }

    private int cellH(int h) {
        return Math.max(1, fixedCellH > 0 ? fixedCellH : h / rows);
    }

    int cellIndexAt(float x, float y) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return -1;
        int cellW = Math.max(1, (w - 2 * horizontalPadding) / columns);
        int col = Math.max(0, Math.min(columns - 1, (int) ((x - horizontalPadding) / cellW)));
        int row = Math.max(0, Math.min(rows - 1, (int) (y / cellH(h))));
        return row * columns + col;
    }

    void setDragState(@Nullable String hiddenKey, @Nullable String mergeKey) {
        dragHiddenKey = hiddenKey;
        dragMergeKey = mergeKey;
        for (int i = 0; i < getChildCount(); i++) applyDragState(getChildAt(i), false);
    }

    void iconCenter(int cell, float[] out) {
        int cellW = (getWidth() - 2 * horizontalPadding) / columns;
        int ch = cellH(getHeight());
        int iconPx = lastIconPx > 0 ? lastIconPx : DeviceLayout.iconPx(getContext());
        float labelBlock = 24 * density;
        out[0] = cellLeft(cell, cellW) + cellW / 2f;
        out[1] = ((float) cell / columns) * ch + (ch - (iconPx + labelBlock)) / 2f + iconPx / 2f;
    }

    void setHoverCell(int cell) {
        if (cell != hoverCell) {
            hoverCell = cell;
            invalidate();
        }
    }

    private void applyDragState(View cell, boolean immediate) {
        Object t = cell.getTag(R.id.app_label);
        String k = t instanceof HomeItem ? ((HomeItem) t).key : null;
        cell.setAlpha(k != null && k.equals(dragHiddenKey) ? 0f : 1f);
        float s = k != null && k.equals(dragMergeKey) ? 1.15f : 1f;
        if (immediate) {
            cell.setScaleX(s);
            cell.setScaleY(s);
        } else if (cell.getScaleX() != s) {
            cell.animate().scaleX(s).scaleY(s).setDuration(120).start();
        }
    }

    private ValueAnimator flip;
    private View[] flipViews = new View[0];
    private float[] flipDx = new float[0];
    private float[] flipDy = new float[0];

    private View newCell(LayoutInflater inflater) {
        View cell = inflater.inflate(R.layout.item_app, this, false);
        DeviceLayout.applyLabelSize(cell);
        cell.setOnClickListener(v -> {
            Object tag = v.getTag(R.id.app_label);
            if (tag instanceof HomeItem) listener.onItemClicked(v, (HomeItem) tag);
        });
        cell.setOnLongClickListener(v -> {
            Object tag = v.getTag(R.id.app_label);
            if (!(tag instanceof HomeItem)) return false;
            v.setPressed(false);
            listener.onItemLongClicked(v, (HomeItem) tag);
            return true;
        });
        IconTouchLift.attach(cell, cell.findViewById(R.id.app_icon));
        return cell;
    }

    private static void bind(View cell, HomeItem item) {
        Object bound = cell.getTag(R.id.app_label);
        if (bound == item) return;
        if (bound instanceof HomeItem && ((HomeItem) bound).icon == item.icon && ((HomeItem) bound).label.equals(item.label)) {
            cell.setTag(R.id.app_label, item);
            return;
        }
        if (!(bound instanceof HomeItem) || ((HomeItem) bound).icon != item.icon) {
            ((ImageView) cell.findViewById(R.id.app_icon)).setImageBitmap(item.icon);
        }
        if (!(bound instanceof HomeItem) || !((HomeItem) bound).label.equals(item.label)) {
            ((TextView) cell.findViewById(R.id.app_label)).setText(item.label);
        }
        cell.setTag(R.id.app_label, item);
    }

    private int cellLeft(int cell, int cellW) {
        return horizontalPadding + (cell % columns) * cellW;
    }

    void setContent(@NonNull HomeItem[] newCells, int columns, int rows, boolean animate) {
        boolean geometry = columns != this.columns || rows != this.rows || newCells.length != cells.length;
        this.columns = columns;
        this.rows = rows;
        this.cells = newCells;

        List<HomeItem> items = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        for (int c = 0; c < newCells.length; c++) {
            if (newCells[c] != null) {
                items.add(newCells[c]);
                idx.add(c);
            }
        }
        final int n = items.size();
        final int old = getChildCount();
        int[] newCellOf = new int[n];
        for (int i = 0; i < n; i++) newCellOf[i] = idx.get(i);

        boolean sameOrder = old == n && cellOfChild.length == n;
        for (int i = 0; sameOrder && i < n; i++) {
            Object t = getChildAt(i).getTag(R.id.app_label);
            sameOrder = t instanceof HomeItem && ((HomeItem) t).key.equals(items.get(i).key) && cellOfChild[i] == newCellOf[i];
        }
        if (sameOrder) {
            for (int i = 0; i < n; i++) {
                View cell = getChildAt(i);
                bind(cell, items.get(i));
                applyDragState(cell, true);
            }
            if (geometry) requestLayout();
            return;
        }

        Map<View, float[]> prev = null;
        if (animate && getWidth() > 0 && old > 0) {
            prev = new HashMap<>(old * 2);
            for (int i = 0; i < old; i++) {
                View c = getChildAt(i);
                prev.put(c, new float[]{c.getLeft() + c.getTranslationX(), c.getTop() + c.getTranslationY()});
            }
        }
        if (flip != null) flip.cancel();

        Map<String, View> byKey = new HashMap<>(old * 2);
        List<View> spare = new ArrayList<>();
        for (int i = 0; i < old; i++) {
            View c = getChildAt(i);
            Object t = c.getTag(R.id.app_label);
            if (t instanceof HomeItem) byKey.put(((HomeItem) t).key, c);
            else spare.add(c);
        }
        View[] ordered = new View[n];
        for (int i = 0; i < n; i++) ordered[i] = byKey.remove(items.get(i).key);
        spare.addAll(byKey.values());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        boolean[] fresh = new boolean[n];
        boolean[] inflated = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (ordered[i] != null) continue;
            inflated[i] = spare.isEmpty();
            View c = inflated[i] ? newCell(inflater) : spare.remove(spare.size() - 1);
            fresh[i] = true;
            ordered[i] = c;
        }

        detachAllViewsFromParent();
        for (int i = 0; i < n; i++) {
            View c = ordered[i];
            bind(c, items.get(i));
            LayoutParams lp = c.getLayoutParams();
            if (lp == null) lp = generateDefaultLayoutParams();
            if (inflated[i]) {
                addViewInLayout(c, i, lp, true);
            } else {
                attachViewToParent(c, i, lp);
            }
            applyDragState(c, true);
            c.setTranslationX(0);
            c.setTranslationY(0);
            if (fresh[i]) IconTouchLift.reset(c.findViewById(R.id.app_icon));
        }
        cellOfChild = newCellOf;
        boolean anyFresh = false;
        for (boolean f : fresh) anyFresh |= f;
        if (anyFresh || geometry || getWidth() == 0) {
            requestLayout();
        } else {
            layoutCells(getWidth(), getHeight());
        }
        invalidate();

        if (prev != null && getHeight() > 0) {
            int cellW = (getWidth() - 2 * horizontalPadding) / columns;
            int cellH = cellH(getHeight());
            View[] vs = new View[n];
            float[] dx = new float[n];
            float[] dy = new float[n];
            int m = 0;
            for (int i = 0; i < n; i++) {
                float[] p = prev.get(ordered[i]);
                if (p == null || fresh[i]) continue;
                float ddx = p[0] - cellLeft(newCellOf[i], cellW);
                float ddy = p[1] - ((float) newCellOf[i] / columns) * cellH;
                if (Math.abs(ddx) < 1f && Math.abs(ddy) < 1f) continue;
                vs[m] = ordered[i];
                dx[m] = ddx;
                dy[m] = ddy;
                m++;
            }
            if (m > 0) startFlip(vs, dx, dy, m);
        }
    }

    private void startFlip(View[] vs, float[] dx, float[] dy, int count) {
        flipViews = vs;
        flipDx = dx;
        flipDy = dy;
        final int m = count;
        for (int i = 0; i < m; i++) {
            vs[i].setTranslationX(dx[i]);
            vs[i].setTranslationY(dy[i]);
        }
        ValueAnimator a = ValueAnimator.ofFloat(1f, 0f);
        a.setDuration(200);
        a.setInterpolator(new DecelerateInterpolator(1.5f));
        a.addUpdateListener(an -> {
            float f = (float) an.getAnimatedValue();
            for (int i = 0; i < m; i++) {
                flipViews[i].setTranslationX(flipDx[i] * f);
                flipViews[i].setTranslationY(flipDy[i] * f);
            }
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                for (int i = 0; i < m; i++) {
                    flipViews[i].setTranslationX(0);
                    flipViews[i].setTranslationY(0);
                }
            }
        });
        flip = a;
        a.start();
    }

    void bounce(@NonNull String key) {
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            Object t = c.getTag(R.id.app_label);
            if (t instanceof HomeItem && ((HomeItem) t).key.equals(key)) {
                c.setScaleX(0.8f);
                c.setScaleY(0.8f);
                c.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(new OvershootInterpolator(2.2f)).start();
                return;
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (hoverCell < 0 || getWidth() == 0) return;
        int cellW = (getWidth() - 2 * horizontalPadding) / columns;
        int cellH = cellH(getHeight());
        int iconPx = lastIconPx > 0 ? lastIconPx : DeviceLayout.iconPx(getContext());
        float labelBlock = 6 * density + 18 * density;
        float cx = cellLeft(hoverCell, cellW) + cellW / 2f;
        float top = ((float) hoverCell / columns) * cellH + (cellH - (iconPx + labelBlock)) / 2f;
        float outset = 3 * density;
        IconShape.fit(ringPath, cx - iconPx / 2f - outset, top - outset, iconPx + 2 * outset);
        canvas.drawPath(ringPath, ringPaint);
    }

    static int gridIconPx(Context ctx, int cellH) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int labelBlock = Math.round(30 * d);
        return Math.max(Math.round(36 * d), Math.min(DeviceLayout.iconPx(ctx), cellH - labelBlock));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        int cellW = Math.max(0, (w - 2 * horizontalPadding) / columns);
        int cellH = Math.max(0, fixedCellH > 0 ? fixedCellH : h / rows);
        int cw = MeasureSpec.makeMeasureSpec(cellW, MeasureSpec.EXACTLY);
        int ch = MeasureSpec.makeMeasureSpec(cellH, MeasureSpec.EXACTLY);
        int iconPx = gridIconPx(getContext(), cellH);
        lastIconPx = iconPx;
        for (int i = 0; i < getChildCount(); i++) {
            View cell = getChildAt(i);
            View icon = cell.findViewById(R.id.app_icon);
            LayoutParams lp = icon.getLayoutParams();
            if (lp.width != iconPx) {
                lp.width = iconPx;
                lp.height = iconPx;
                icon.setLayoutParams(lp);
            }
            cell.measure(cw, ch);
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutCells(r - l, b - t);
    }

    private void layoutCells(int w, int h) {
        int cellW = (w - 2 * horizontalPadding) / columns;
        int cellH = fixedCellH > 0 ? fixedCellH : h / rows;
        for (int i = 0; i < getChildCount() && i < cellOfChild.length; i++) {
            int cell = cellOfChild[i];
            int left = cellLeft(cell, cellW);
            int top = (cell / columns) * cellH;
            getChildAt(i).layout(left, top, left + cellW, top + cellH);
        }
    }
}