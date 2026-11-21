package com.adssetup.adssdk.sdk.launcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.adssetup.adssdk.sdk.R;

import java.util.ArrayList;
import java.util.List;

final class HomeOverlays {
    interface Callbacks {
        void onAppClick(@NonNull AppInfo app);

        void onAppInfo(@NonNull AppInfo app);

        void onAppUninstall(@NonNull AppInfo app);

        boolean isDedicated(@NonNull AppInfo app);

        boolean isSystemApp(@NonNull AppInfo app);

        boolean canRemoveFromFolder(@NonNull String folderId);

        void onFolderChanged();

        void onFolderDragExit(@NonNull String folderId, @NonNull AppInfo app);

        void onOverlayStateChanged();
    }

    private final Callbacks cb;
    private final HomeLayout layout;
    private final float density;
    private final View menuLayer;
    private final View menuCard;
    private final TextView menuPrimary;
    private final TextView menuSecondary;
    private final View menuDivider;
    private final TextView menuTertiary;
    private final View menuGap0, menuGap1, menuGap2, menuGap3;
    private final android.widget.LinearLayout menuShortcuts;
    private final View menuShortcutDivider;
    @Nullable
    private android.content.ComponentName menuApp;
    private final View menuDivider2;
    private final View folderLayer;
    private final View folderCard;
    private final EditText folderName;
    private static final int FOLDER_COLUMNS = 3;
    private static final int FOLDER_VISIBLE_ROWS = 3;
    private final RecyclerView folderList;
    private final FolderAdapter folderAdapter = new FolderAdapter();
    private final ItemTouchHelper touchHelper;
    @Nullable
    private String openFolderId;
    private boolean dirty;

    HomeOverlays(@NonNull View root, @NonNull HomeLayout layout, @NonNull Callbacks cb) {
        this.cb = cb;
        this.layout = layout;
        density = root.getResources().getDisplayMetrics().density;

        menuLayer = root.findViewById(R.id.menu_layer);
        menuCard = root.findViewById(R.id.item_menu);
        menuPrimary = root.findViewById(R.id.menu_primary);
        menuSecondary = root.findViewById(R.id.menu_secondary);
        menuDivider = root.findViewById(R.id.menu_divider);
        menuTertiary = root.findViewById(R.id.menu_tertiary);
        menuGap0 = root.findViewById(R.id.menu_gap0);
        menuGap1 = root.findViewById(R.id.menu_gap1);
        menuGap2 = root.findViewById(R.id.menu_gap2);
        menuGap3 = root.findViewById(R.id.menu_gap3);
        menuShortcuts = root.findViewById(R.id.menu_shortcuts);
        menuShortcutDivider = root.findViewById(R.id.menu_shortcut_divider);
        menuDivider2 = root.findViewById(R.id.menu_divider2);
        menuLayer.setOnClickListener(v -> dismissMenu());

        folderLayer = root.findViewById(R.id.folder_layer);
        folderCard = root.findViewById(R.id.folder_card);
        android.graphics.drawable.GradientDrawable panel = new android.graphics.drawable.GradientDrawable();
        panel.setColor(DeviceLayout.isPixel(root.getContext()) ? 0xCCFFFFFF : 0xE6969696);
        panel.setCornerRadius(20 * density);
        folderCard.setBackground(panel);
        folderCard.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 20 * density);
            }
        });
        folderCard.setClipToOutline(true);
        folderName = root.findViewById(R.id.folder_name);
        if (DeviceLayout.isPixel(root.getContext())) folderName.setTextColor(0xFF313131);
        if (DeviceLayout.isSamsung())
            folderName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        folderList = root.findViewById(R.id.folder_list);
        folderLayer.setOnClickListener(v -> closeFolder(true));
        folderList.setLayoutManager(new GridLayoutManager(root.getContext(), FOLDER_COLUMNS));
        folderList.setAdapter(folderAdapter);
        folderList.setItemAnimator(null);
        folderName.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitName();
                folderName.clearFocus();
                hideKeyboard();
                return true;
            }
            return false;
        });

        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION)
                    return false;
                dismissMenu();
                if (layout.reorderInFolder(openFolderId, from, to)) {
                    folderAdapter.move(from, to);
                    dirty = true;
                }
                return true;
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder vh, int actionState) {
                super.onSelectedChanged(vh, actionState);
            }

            private boolean handedOff;

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive && menuOpen && Math.hypot(dX, dY) > 12 * density) {
                    folderList.post(HomeOverlays.this::dismissMenu);
                }
                if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || !isCurrentlyActive || handedOff || openFolderId == null)
                    return;
                int[] l = new int[2];
                rv.getLocationOnScreen(l);
                float cx = l[0] + vh.itemView.getLeft() + dX + vh.itemView.getWidth() / 2f;
                float cy = l[1] + vh.itemView.getTop() + dY + vh.itemView.getHeight() / 2f;
                int[] cl = new int[2];
                folderCard.getLocationOnScreen(cl);
                boolean outside = cx < cl[0] || cx > cl[0] + folderCard.getWidth() || cy < cl[1] || cy > cl[1] + folderCard.getHeight();
                int p = vh.getBindingAdapterPosition();
                if (!outside || p == RecyclerView.NO_POSITION || p >= folderAdapter.list.size())
                    return;
                handedOff = true;
                final AppInfo app = folderAdapter.list.get(p);
                final String id = openFolderId;
                folderList.post(() -> {
                    long now = android.os.SystemClock.uptimeMillis();
                    MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, cx, cy, 0);
                    folderList.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    cb.onFolderDragExit(id, app);
                });
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                handedOff = false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
            }
        });
        touchHelper.attachToRecyclerView(folderList);
        android.os.Looper.myQueue().addIdleHandler(() -> {
            RecyclerView.RecycledViewPool pool = folderList.getRecycledViewPool();
            pool.setMaxRecycledViews(0, 12);
            for (int i = 0; i < 8; i++)
                pool.putRecycledView(folderAdapter.createViewHolder(folderList, 0));
            return false;
        });
    }

    private boolean menuOpen;

    boolean isMenuShowing() {
        return menuOpen;
    }

    boolean isFolderOpen() {
        return openFolderId != null;
    }

    boolean isAnyOpen() {
        return isMenuShowing() || isFolderOpen();
    }

    boolean closeTop() {
        if (isMenuShowing()) {
            dismissMenu();
            return true;
        }
        if (isFolderOpen()) {
            closeFolder(true);
            return true;
        }
        return false;
    }

    void closeAll() {
        dismissMenu();
        closeFolder(false);
    }

    void showMenu(@NonNull View anchor, @NonNull String labelA, @NonNull Runnable a, @Nullable String labelB, @Nullable Runnable b) {
        showMenu(anchor, labelA, a, labelB, b, null, null);
    }

    void showMenu(@NonNull View anchor, @NonNull String labelA, @NonNull Runnable a, @Nullable String labelB, @Nullable Runnable b, @Nullable String labelC, @Nullable Runnable c) {
        bindRow(menuPrimary, labelA, a);
        bindRow(menuSecondary, labelB, b);
        bindRow(menuTertiary, labelC, c);
        menuDivider.setVisibility(View.GONE);
        menuDivider2.setVisibility(View.GONE);
        menuOpen = true;
        menuLayer.setVisibility(View.VISIBLE);
        final int unspec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int natural;
        int sum = 0;
        int rows = 0;
        for (TextView row : new TextView[]{menuPrimary, menuSecondary, menuTertiary}) {
            if (row.getVisibility() == View.GONE) continue;
            row.measure(unspec, unspec);
            sum += row.getMeasuredWidth();
            rows++;
        }
        menuGap0.setVisibility(View.VISIBLE);
        menuGap1.setVisibility(rows >= 2 ? View.VISIBLE : View.GONE);
        menuGap2.setVisibility(rows >= 3 ? View.VISIBLE : View.GONE);
        menuGap3.setVisibility(View.VISIBLE);
        natural = sum + (rows + 1) * Math.round(16 * density);
        bindShortcuts(menuApp);
        menuApp = null;
        for (int i = 0; i < menuShortcuts.getChildCount(); i++) {
            menuShortcuts.getChildAt(i).measure(unspec, unspec);
            natural = Math.max(natural, menuShortcuts.getChildAt(i).getMeasuredWidth());
        }
        View layerParent = (View) menuLayer.getParent();
        int maxW = Math.max(0, layerParent.getWidth() - (int) (16 * density));
        int cardW = maxW > 0 ? Math.min(natural, maxW) : natural;
        ViewGroup.LayoutParams clp = menuCard.getLayoutParams();
        if (clp.width != cardW) {
            clp.width = cardW;
            menuCard.setLayoutParams(clp);
        }
        menuCard.measure(View.MeasureSpec.makeMeasureSpec(cardW, View.MeasureSpec.EXACTLY), unspec);
        int mw = menuCard.getMeasuredWidth();
        int mh = menuCard.getMeasuredHeight();
        int[] al = new int[2];
        int[] rl = new int[2];
        anchor.getLocationOnScreen(al);
        View parent = (View) menuLayer.getParent();
        parent.getLocationOnScreen(rl);
        float margin = 8 * density;
        float x = al[0] - rl[0] + anchor.getWidth() / 2f - mw / 2f;
        x = Math.max(margin, Math.min(parent.getWidth() - mw - margin, x));
        androidx.core.view.WindowInsetsCompat wi = androidx.core.view.ViewCompat.getRootWindowInsets(anchor);
        androidx.core.graphics.Insets bars = wi == null ? androidx.core.graphics.Insets.NONE : wi.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
        float topLimit = margin + bars.top;
        float bottomLimit = parent.getHeight() - margin - bars.bottom;
        float above = al[1] - rl[1] - mh - 4 * density;
        float y = above >= topLimit ? above : al[1] - rl[1] + anchor.getHeight() + 4 * density;
        y = Math.max(topLimit, Math.min(bottomLimit - mh, y));
        menuCard.setTranslationX(Math.round(x));
        menuCard.setTranslationY(Math.round(y));
        menuCard.setAlpha(0f);
        menuCard.setScaleX(0.9f);
        menuCard.setScaleY(0.9f);
        menuCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start();
        cb.onOverlayStateChanged();
    }

    void setMenuApp(@Nullable android.content.ComponentName app) {
        menuApp = app;
    }

    private void bindShortcuts(@Nullable android.content.ComponentName app) {
        menuShortcuts.removeAllViews();
        final Context c = menuCard.getContext();
        final int icon = Math.round(24 * density);
        for (final AppShortcuts.Entry e : app == null ? new ArrayList<AppShortcuts.Entry>() : AppShortcuts.load(c, app)) {
            TextView row = new TextView(c);
            row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setMinWidth(Math.round(200 * density));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setText(e.label);
            row.setSingleLine(true);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setTextColor(0xFF1F1B16);
            row.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
            row.setCompoundDrawablePadding(Math.round(20 * density));
            row.setPaddingRelative(Math.round(20 * density), Math.round(14 * density), Math.round(20 * density), Math.round(14 * density)); // equal side padding; 28dp of padding gives every pair of shortcuts the same visible gap
            if (e.icon != null) {
                e.icon.setBounds(0, 0, icon, icon);
                row.setCompoundDrawablesRelative(e.icon, null, null, null);
            }
            TypedValueRipple.apply(row);
            row.setOnClickListener(v -> {
                dismissMenu();
                e.launch.run();
            });
            menuShortcuts.addView(row);
        }
        boolean any = menuShortcuts.getChildCount() > 0;
        menuShortcuts.setVisibility(any ? View.VISIBLE : View.GONE);
        menuShortcutDivider.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private static final class TypedValueRipple {
        static void apply(View v) {
            android.util.TypedValue tv = new android.util.TypedValue();
            if (v.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
                v.setBackgroundResource(tv.resourceId);
            }
            v.setClickable(true);
        }
    }

    private void bindRow(TextView row, @Nullable String label, @Nullable Runnable action) {
        if (label == null || action == null) {
            row.setVisibility(View.GONE);
            row.setOnClickListener(null);
            return;
        }
        row.setVisibility(View.VISIBLE);
        row.setText(label);
        final Context c = row.getContext();
        int icon = label.equals(c.getString(R.string.menu_app_info)) ? R.drawable.ic_menu_info : label.equals(c.getString(R.string.menu_uninstall)) ? R.drawable.ic_menu_delete : label.equals(c.getString(R.string.menu_add_to_home)) ? R.drawable.ic_menu_add_home : label.equals(c.getString(R.string.menu_remove_from_home)) || label.equals(c.getString(R.string.menu_remove_from_folder)) ? R.drawable.ic_menu_close : label.equals(c.getString(R.string.menu_rename)) ? R.drawable.ic_menu_edit : R.drawable.ic_menu_close;
        android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(c, icon);
        if (d != null) {
            int s = Math.round(24 * c.getResources().getDisplayMetrics().density);
            d = d.mutate();
            d.setBounds(0, 0, s, s);
        }
        row.setCompoundDrawablesRelative(null, d, null, null);
        row.setOnClickListener(v -> {
            dismissMenu();
            action.run();
        });
    }

    void dismissMenu() {
        if (!menuOpen) return;
        menuOpen = false;
        menuCard.animate().cancel();
        menuCard.animate().alpha(0f).setDuration(90).withEndAction(() -> {
            if (!menuOpen) menuLayer.setVisibility(View.INVISIBLE);
        }).start();
        cb.onOverlayStateChanged();
    }

    void openFolder(@NonNull String folderId, @Nullable View anchor) {
        HomeLayout.Folder f = layout.getFolder(folderId);
        if (f == null) return;
        openFolderId = folderId;
        dirty = false;
        folderName.setText(f.name);
        folderName.clearFocus();
        refreshFolderList();
        folderLayer.setVisibility(View.VISIBLE);
        folderLayer.setAlpha(0f);
        View layerParent = (View) folderLayer.getParent();
        int pw = layerParent.getWidth();
        int ph = layerParent.getHeight();
        int cardW = pw - Math.round(40 * density);
        folderCard.measure(View.MeasureSpec.makeMeasureSpec(cardW, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        float cardLeft = (pw - cardW) / 2f;
        float cardTop = (ph - folderCard.getMeasuredHeight()) / 2f;
        float px = cardW / 2f;
        float py = folderCard.getMeasuredHeight() / 2f;
        if (anchor != null) {
            int[] al = new int[2];
            int[] pl = new int[2];
            anchor.getLocationOnScreen(al);
            layerParent.getLocationOnScreen(pl);
            px = al[0] - pl[0] + anchor.getWidth() / 2f - cardLeft;
            py = al[1] - pl[1] + anchor.getHeight() / 2f - cardTop;
        }
        folderCard.setPivotX(px);
        folderCard.setPivotY(py);
        folderCard.setScaleX(0.3f);
        folderCard.setScaleY(0.3f);
        folderCard.setAlpha(0f);
        folderLayer.animate().alpha(1f).setDuration(160).start();
        folderCard.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(240).setInterpolator(new android.view.animation.OvershootInterpolator(0.9f)).start();
        cb.onOverlayStateChanged();
    }

    void openFolderForRename(@NonNull String folderId, @Nullable View anchor) {
        openFolder(folderId, anchor);
        if (openFolderId == null) return;
        folderName.postDelayed(() -> {
            if (openFolderId == null) return;
            folderName.requestFocus();
            folderName.selectAll();
            InputMethodManager imm = (InputMethodManager) folderName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(folderName, InputMethodManager.SHOW_IMPLICIT);
        }, 260);
    }

    void closeFolder(boolean animate) {
        if (openFolderId == null) return;
        commitName();
        hideKeyboard();
        openFolderId = null;
        boolean changed = dirty;
        dirty = false;
        folderLayer.animate().cancel();
        if (animate) {
            folderCard.animate().cancel();
            folderCard.animate().scaleX(0.6f).scaleY(0.6f).alpha(0f).setDuration(150).start();
            folderLayer.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                if (openFolderId == null) folderLayer.setVisibility(View.GONE);
            }).start();
        } else {
            folderLayer.setVisibility(View.GONE);
        }
        if (changed) cb.onFolderChanged();
        cb.onOverlayStateChanged();
    }

    void onLayoutRebuilt() {
        if (openFolderId == null) return;
        if (layout.getFolder(openFolderId) == null) {
            dirty = false;
            closeFolder(false);
        } else {
            refreshFolderList();
        }
    }

    private void commitName() {
        if (openFolderId == null) return;
        if (layout.renameFolder(openFolderId, folderName.getText().toString())) dirty = true;
        HomeLayout.Folder f = layout.getFolder(openFolderId);
        if (f != null && !f.name.contentEquals(folderName.getText())) folderName.setText(f.name);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) folderName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(folderName.getWindowToken(), 0);
    }

    private void refreshFolderList() {
        List<AppInfo> apps = layout.folderApps(openFolderId);
        folderAdapter.set(apps);
        int rows = Math.max(1, Math.min(FOLDER_VISIBLE_ROWS, (apps.size() + FOLDER_COLUMNS - 1) / FOLDER_COLUMNS));
        ViewGroup.LayoutParams lp = folderList.getLayoutParams();
        int h = Math.round(rows * 100 * density);
        if (lp.height != h) {
            lp.height = h;
            folderList.setLayoutParams(lp);
        }
    }

    private void removeFromFolder(AppInfo app) {
        if (openFolderId == null) return;
        String id = openFolderId;
        if (layout.removeFromFolderOnly(id, HomeLayout.key(app))) {
            dirty = true;
            cb.onFolderChanged();
        }
    }

    private final class FolderAdapter extends RecyclerView.Adapter<DrawerAppsAdapter.VH> {
        private final List<AppInfo> list = new ArrayList<>();

        FolderAdapter() {
            setHasStableIds(true);
        }

        @SuppressLint("NotifyDataSetChanged")
        void set(List<AppInfo> apps) {
            list.clear();
            list.addAll(apps);
            notifyDataSetChanged();
        }

        void move(int from, int to) {
            list.add(to, list.remove(from));
            notifyItemMoved(from, to);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).component.hashCode();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @NonNull
        @Override
        public DrawerAppsAdapter.VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
            DeviceLayout.applyLabelSize(v);
            final DrawerAppsAdapter.VH vh = new DrawerAppsAdapter.VH(v);
            IconTouchLift.attach(v, vh.icon);
            v.setOnClickListener(x -> {
                int p = vh.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) cb.onAppClick(list.get(p));
            });
            v.setOnLongClickListener(x -> {
                int p = vh.getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return false;
                AppInfo app = list.get(p);
                x.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                if (!cb.isDedicated(app)) {
                    setMenuApp(app.component);
                    final boolean canLeave = openFolderId != null && cb.canRemoveFromFolder(openFolderId);
                    showMenu(x, parent.getContext().getString(R.string.menu_app_info), () -> cb.onAppInfo(app), cb.isSystemApp(app) ? null : parent.getContext().getString(R.string.menu_uninstall), cb.isSystemApp(app) ? null : () -> cb.onAppUninstall(app), canLeave ? parent.getContext().getString(R.string.menu_remove_from_folder) : null, canLeave ? () -> removeFromFolder(app) : null);
                }
                touchHelper.startDrag(vh);
                return true;
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull DrawerAppsAdapter.VH h, int position) {
            AppInfo app = list.get(position);
            IconTouchLift.reset(h.icon);
            DeviceLayout.applyIconSize(h.icon);
            h.icon.setImageBitmap(app.icon);
            h.label.setText(app.label);
            DeviceLayout.styleDrawerLabel(h.label);
        }
    }
}