package com.adssetup.adssdk.sdk.launcher;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.adssetup.adssdk.sdk.R;

import java.util.Objects;

final class HomeDragController implements LauncherRootLayout.GestureHandler {
    interface Host {
        void onLayoutChanged(boolean animate);

        void dismissMenu();

        void onDrawerDragStarted();

        void onDrawerDragEnded(boolean placed);

        int dockCellAt(float rawX, float rawY);

        boolean dockNearCenter(int cell, float rawX, float rawY);

        void setDockDragState(@androidx.annotation.Nullable String hiddenKey, int hoverCell, boolean merge);
    }

    private enum Mode {NONE, SELF, EMPTY, SWAP, MERGE}

    private static final long EDGE_DWELL_MS = 420;
    private static final long EDGE_REPEAT_MS = 900;
    private static final long EXTRA_PAGE_MS = 900;
    private static final float EDGE_ZONE_FRACTION = 0.11f;
    private final LauncherRootLayout root;
    private final ViewPager2 pager;
    private final RecyclerView inner;
    private final LauncherPagerAdapter adapter;
    private final HomeLayout layout;
    private final Host host;
    private final int slop;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final int[] loc = new int[2];
    private boolean armed;
    private boolean dragging;
    private HomeItem item;
    private float startX, startY, lastX, lastY;
    private final ImageView shadow;
    private final float shadowHalf;
    private final int edgePx;
    private boolean pendingFinish;
    private Mode mode = Mode.NONE;
    private int hoverPage = -1;
    private int hoverCell = -1;
    private String targetKey;
    private int edgeDir;
    private long edgeStart;
    private int generation;
    private String sourceFolderId;
    private AppInfo drawerApp;
    private String drawerFolderId;
    private float originX = Float.NaN, originY = Float.NaN;

    private final ViewPager2.OnPageChangeCallback pageCb = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrollStateChanged(int state) {
            if (state != ViewPager2.SCROLL_STATE_IDLE || !dragging) return;
            evaluate(lastX, lastY);
            if (pendingFinish) {
                pendingFinish = false;
                finish(true);
                armed = false;
            }
        }
    };

    private final Runnable edgeFlip = new Runnable() {
        @Override
        public void run() {
            if (!dragging || edgeDir == 0) return;
            if (pager.getScrollState() != ViewPager2.SCROLL_STATE_IDLE) {
                h.postDelayed(this, 120);
                return;
            }
            int cur = pager.getCurrentItem();
            int target = cur + edgeDir;
            long held = android.os.SystemClock.uptimeMillis() - edgeStart;
            if (target >= adapter.appPageCount() && edgeDir > 0 && !layout.hasExtraPage() && layout.lastPageHasItems()) {
                if (held < EXTRA_PAGE_MS) {
                    h.postDelayed(this, EXTRA_PAGE_MS - held);
                    return;
                }
                layout.setExtraPage(true);
                host.onLayoutChanged(false);
            }
            if (target >= 0 && target < adapter.appPageCount()) {
                pager.setCurrentItem(target, true);
                edgeStart = android.os.SystemClock.uptimeMillis();
                h.postDelayed(this, EDGE_REPEAT_MS);
            }
        }
    };

    HomeDragController(@NonNull LauncherRootLayout root, @NonNull ViewPager2 pager, @NonNull LauncherPagerAdapter adapter, @NonNull HomeLayout layout, @NonNull Host host) {
        this.root = root;
        this.pager = pager;
        this.inner = (RecyclerView) pager.getChildAt(0);
        this.adapter = adapter;
        this.layout = layout;
        this.host = host;
        this.slop = ViewConfiguration.get(root.getContext()).getScaledTouchSlop();
        float density = root.getResources().getDisplayMetrics().density;
        this.edgePx = Math.max(Math.round(36 * density), Math.round(root.getResources().getDisplayMetrics().widthPixels * EDGE_ZONE_FRACTION));
        int size = Math.round(62 * density);
        this.shadowHalf = size / 2f;
        shadow = new ImageView(root.getContext());
        shadow.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        shadow.setVisibility(View.INVISIBLE);
        root.addView(shadow, new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.START));
    }

    void arm(@NonNull HomeItem it) {
        if (dragging) return;
        item = it;
        armed = true;
        startX = lastX = root.getLastRawX();
        startY = lastY = root.getLastRawY();
    }

    void armFromFolder(@NonNull AppInfo app, @NonNull String folderId) {
        if (dragging) return;
        item = HomeItem.ofApp(app);
        sourceFolderId = folderId;
        armed = true;
        startX = lastX = root.getLastRawX();
        startY = lastY = root.getLastRawY();
        root.setForwardAllTouch(true);
        startDrag();
        moveShadow(lastX, lastY);
        evaluate(lastX, lastY);
    }

    void armFromDrawer(@NonNull AppInfo app, @NonNull View origin) {
        if (dragging) return;
        item = HomeItem.ofApp(app);
        drawerApp = app;
        drawerFolderId = layout.folderIdOf(app);
        sourceFolderId = null;
        int[] o = new int[2];
        View icon = origin.findViewById(R.id.app_icon);
        View ref = icon != null ? icon : origin;
        ref.getLocationOnScreen(o);
        originX = o[0] + ref.getWidth() / 2f;
        originY = o[1] + ref.getHeight() / 2f - ref.getTranslationY();
        armed = true;
        startX = lastX = root.getLastRawX();
        startY = lastY = root.getLastRawY();
    }

    void cancel() {
        pendingFinish = false;
        finish(false);
        armed = false;
    }

    boolean isDragging() {
        return dragging;
    }

    @Override
    public boolean isActive() {
        return armed || dragging;
    }

    @Override
    public void onEvent(MotionEvent ev) {
        float x = ev.getRawX();
        float y = ev.getRawY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                lastX = x;
                lastY = y;
                if (!dragging) {
                    if (Math.hypot(x - startX, y - startY) < slop) return;
                    startDrag();
                }
                moveShadow(x, y);
                evaluate(x, y);
                updateEdge(x);
                break;
            case MotionEvent.ACTION_UP:
                if (dragging && pager.getScrollState() != ViewPager2.SCROLL_STATE_IDLE) {
                    pendingFinish = true;
                } else {
                    finish(true);
                    armed = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                finish(false);
                armed = false;
                break;
            default:
                break;
        }
    }

    private void startDrag() {
        dragging = true;
        generation++;
        host.dismissMenu();
        if (drawerApp != null) host.onDrawerDragStarted();
        pager.registerOnPageChangeCallback(pageCb);
        shadow.animate().cancel();
        shadow.setImageBitmap(item.icon);
        shadow.setScaleX(1f);
        shadow.setScaleY(1f);
        shadow.setAlpha(0.95f);
        moveShadow(lastX, lastY);
        shadow.setVisibility(View.VISIBLE);
        shadow.animate().scaleX(1.12f).scaleY(1.12f).setDuration(120).setInterpolator(new OvershootInterpolator(2f)).start();
        adapter.setDragState(inner, item.key, null);
        host.setDockDragState(item.key, -1, false);
        mode = Mode.NONE;
        hoverPage = hoverCell = -1;
        targetKey = null;
    }

    private void moveShadow(float rx, float ry) {
        if (!dragging) return;
        root.getLocationOnScreen(loc);
        shadow.setTranslationX(rx - loc[0] - shadowHalf);
        shadow.setTranslationY(ry - loc[1] - shadowHalf);
    }

    private void updateEdge(float rx) {
        int w = root.getWidth();
        int dir = rx < edgePx ? -1 : rx > w - edgePx ? 1 : 0;
        if (dir == edgeDir) return;
        edgeDir = dir;
        h.removeCallbacks(edgeFlip);
        edgeStart = android.os.SystemClock.uptimeMillis();
        if (dir != 0) h.postDelayed(edgeFlip, EDGE_DWELL_MS);
    }

    private void clearHover() {
        if (mode == Mode.NONE && hoverCell == -1) return;
        mode = Mode.NONE;
        hoverPage = hoverCell = -1;
        targetKey = null;
        adapter.setHover(inner, -1, -1);
        adapter.setDragState(inner, item.key, null);
        host.setDockDragState(item.key, -1, false);
    }

    private void evaluateDock(int dc, float rx, float ry) {
        HomeItem target = layout.dockItem(dc);
        if (!HomeLayout.dockAccepts(item, target)) {
            clearHover();
            return;
        }
        Mode m;
        String tKey = null;
        if (target == null) {
            m = Mode.EMPTY;
        } else if (target.key.equals(item.key)) {
            m = Mode.SELF;
        } else {
            m = Mode.SWAP;
            if (drawerApp != null || sourceFolderId != null) {
                String own = drawerApp != null ? drawerFolderId : sourceFolderId;
                boolean same = own != null && own.equals(target.folderId);
                m = same ? Mode.SELF : Mode.MERGE;
                tKey = same ? null : target.key;
            } else if (item.app != null && host.dockNearCenter(dc, rx, ry)) {
                m = Mode.MERGE;
                tKey = target.key;
            }
        }
        if (m == mode && hoverPage == HomeLayout.DOCK && dc == hoverCell) return;
        mode = m;
        hoverPage = HomeLayout.DOCK;
        hoverCell = dc;
        targetKey = tKey;
        adapter.setHover(inner, -1, -1);
        adapter.setDragState(inner, item.key, null);
        host.setDockDragState(item.key, m == Mode.SELF ? -1 : dc, m == Mode.MERGE);
    }

    private void evaluate(float rx, float ry) {
        if (!dragging) return;
        int dc = host.dockCellAt(rx, ry);
        if (dc >= 0) {
            evaluateDock(dc, rx, ry);
            return;
        }
        if (pager.getScrollState() != ViewPager2.SCROLL_STATE_IDLE) {
            clearHover();
            return;
        }
        int pos = pager.getCurrentItem();
        AppGridPageView pv = adapter.pageView(inner, pos);
        if (pv == null) {
            clearHover();
            return;
        }
        pv.getLocationOnScreen(loc);
        float lx = rx - loc[0];
        float ly = ry - loc[1];
        if (lx < 0 || ly < 0 || lx >= pv.getWidth() || ly >= pv.getHeight()) {
            clearHover();
            return;
        }
        int cell = pv.cellIndexAt(lx, ly);
        HomeItem target = pv.itemAtCell(cell);
        Mode m;
        String tKey = null;
        if (target == null) {
            m = Mode.EMPTY;
        } else if (target.key.equals(item.key)) {
            m = Mode.SELF;
        } else {
            m = Mode.SWAP;
            if (drawerApp != null) {
                boolean own = drawerFolderId != null && drawerFolderId.equals(target.folderId);
                m = own ? Mode.SELF : Mode.MERGE;
                tKey = own ? null : target.key;
            } else if (sourceFolderId != null) {
                boolean own = target.folderId != null && target.folderId.equals(sourceFolderId);
                m = own ? Mode.SELF : Mode.MERGE;
                tKey = own ? null : target.key;
            }
            View c = pv.viewAtCell(cell);
            if (c != null && item.app != null && drawerApp == null) {
                float cx = c.getLeft() + c.getWidth() / 2f;
                float cy = c.getTop() + c.getHeight() / 2f;
                if (Math.hypot(lx - cx, ly - cy) < c.getWidth() * 0.36f) {
                    m = Mode.MERGE;
                    tKey = target.key;
                }
            }
        }
        if (m == mode && pos == hoverPage && cell == hoverCell) return;
        mode = m;
        hoverPage = pos;
        hoverCell = cell;
        targetKey = tKey;
        adapter.setHover(inner, m == Mode.MERGE ? -1 : pos, cell);
        adapter.setDragState(inner, item.key, tKey);
        host.setDockDragState(item.key, -1, false);
    }

    private void finish(boolean commit) {
        h.removeCallbacks(edgeFlip);
        edgeDir = 0;
        if (!dragging) {
            drawerApp = null;
            drawerFolderId = null;
            return;
        }
        dragging = false;
        pager.unregisterOnPageChangeCallback(pageCb);

        final ImageView s = shadow;
        final HomeItem it = item;
        final int token = generation;
        final Mode m = commit ? mode : Mode.NONE;
        final int page = hoverPage;
        final int cell = hoverCell;
        final String tKey = targetKey;
        mode = Mode.NONE;
        hoverPage = hoverCell = -1;
        targetKey = null;
        adapter.setHover(inner, -1, -1);

        final String srcFolder = sourceFolderId;
        sourceFolderId = null;
        final AppInfo fromDrawer = drawerApp;
        drawerApp = null;
        drawerFolderId = null;
        root.setForwardAllTouch(false);
        String folderKey = null;
        boolean applied = false;
        if (fromDrawer != null) {
            if (m == Mode.SELF) {
                applied = true;
            } else if (m == Mode.MERGE || m == Mode.EMPTY) {
                String res = layout.placeFromDrawer(fromDrawer, page, cell, m == Mode.MERGE ? tKey : null);
                applied = res != null;
                if (applied && m == Mode.MERGE) folderKey = res;
            }
            if (!applied) {
                layout.setExtraPage(false);
                host.onLayoutChanged(true);
                host.setDockDragState(null, -1, false);
                adapter.setDragState(inner, null, null);
                host.onDrawerDragEnded(false);
                if (!Float.isNaN(originX)) {
                    root.getLocationOnScreen(loc);
                    s.animate().translationX(originX - loc[0] - shadowHalf).translationY(originY - loc[1] - shadowHalf).alpha(0f).scaleX(1f).scaleY(1f).setDuration(220).withEndAction(() -> s.setVisibility(View.INVISIBLE)).start();
                } else {
                    s.setVisibility(View.INVISIBLE);
                }
                return;
            }
            host.onDrawerDragEnded(true);
        } else if (srcFolder != null) {
            if (m == Mode.MERGE) {
                folderKey = layout.moveFromFolder(srcFolder, Objects.requireNonNull(it.app).component.flattenToShortString(), tKey);
                applied = folderKey != null;
            } else if (m == Mode.EMPTY) {
                applied = layout.removeFromFolder(srcFolder, HomeLayout.key(Objects.requireNonNull(it.app)), page, cell);
            }
        } else if (m == Mode.MERGE) {
            folderKey = layout.createFolder(it.key, tKey);
        } else if (m == Mode.EMPTY || m == Mode.SWAP) {
            layout.moveTo(it.key, page, cell);
        }
        final boolean restoreToFolder = srcFolder != null && !applied;
        final String glideKey = restoreToFolder ? "folder:" + srcFolder : it.key;
        layout.setExtraPage(false);
        host.onLayoutChanged(true);
        host.setDockDragState(null, -1, false);

        if (folderKey != null) {
            adapter.setDragState(inner, null, null);
            if (s != null) {
                s.animate().alpha(0f).scaleX(0.4f).scaleY(0.4f).setDuration(160).withEndAction(() -> s.setVisibility(View.INVISIBLE)).start();
            }
            final String fk = folderKey;
            h.postDelayed(() -> adapter.bounce(inner, fk), 60);
            return;
        }
        float tx = Float.NaN, ty = Float.NaN;
        AppGridPageView pv = adapter.pageView(inner, pager.getCurrentItem());
        if (pv != null && !restoreToFolder && (m == Mode.EMPTY || m == Mode.SWAP) && page == pager.getCurrentItem() && cell >= 0 && pv.getWidth() > 0) {
            float[] c = new float[2];
            pv.iconCenter(cell, c);
            int[] pl = new int[2];
            pv.getLocationOnScreen(pl);
            root.getLocationOnScreen(loc);
            tx = pl[0] + c[0] - loc[0] - shadowHalf;
            ty = pl[1] + c[1] - loc[1] - shadowHalf;
        } else if (pv != null) {
            for (int i = 0; i < pv.getChildCount(); i++) {
                View c = pv.getChildAt(i);
                Object t = c.getTag(R.id.app_label);
                if (t instanceof HomeItem && ((HomeItem) t).key.equals(glideKey) && c.getWidth() > 0) {
                    View icon = c.findViewById(R.id.app_icon);
                    int[] pl = new int[2];
                    icon.getLocationOnScreen(pl);
                    root.getLocationOnScreen(loc);
                    tx = pl[0] - loc[0] + icon.getWidth() / 2f - shadowHalf - c.getTranslationX();
                    ty = pl[1] - loc[1] + icon.getHeight() / 2f - shadowHalf - c.getTranslationY();
                    break;
                }
            }
        }
        Runnable end = () -> {
            if (s != null) s.setVisibility(View.INVISIBLE);
            if (token == generation) adapter.setDragState(inner, null, null);
        };
        if (s != null && !Float.isNaN(tx)) {
            s.animate().translationX(tx).translationY(ty).alpha(restoreToFolder ? 0f : 1f).scaleX(restoreToFolder ? 0.6f : 1.12f).scaleY(restoreToFolder ? 0.6f : 1.12f).setDuration(restoreToFolder ? 180 : 140).withEndAction(end).start();
        } else {
            end.run();
        }
    }
}