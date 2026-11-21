package com.adssetup.adssdk.sdk.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.adssetup.adssdk.sdk.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LauncherPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface Listener {
        void onItemClicked(@NonNull View cell, @NonNull HomeItem item);

        void onItemLongClicked(@NonNull View cell, @NonNull HomeItem item);

        void onSearchClicked();

        void onVoiceSearchClicked();
    }

    private static final int TYPE_HOME = 0;
    private static final int TYPE_APPS = 1;
    private static final int TYPE_GREETING = 2;

    static final class HomeHolder extends RecyclerView.ViewHolder {
        final AppGridPageView grid;
        final View rowSearch;
        final View rowClock;

        HomeHolder(@NonNull View v, @NonNull AppGridPageView grid) {
            super(v);
            this.grid = grid;
            this.rowSearch = v.findViewById(R.id.row_search);
            this.rowClock = v.findViewById(R.id.row_clock);
        }

        void applyRows(int cellH) {
            if (cellH <= 0) return;
            setHeight(rowSearch, cellH);
            setHeight(rowClock, (rowSearch.getVisibility() == View.GONE ? 3 : 2) * cellH);
            setHeight(grid, 3 * cellH);
        }

        private static void setHeight(View v, int h) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp.height != h) {
                lp.height = h;
                v.setLayoutParams(lp);
            }
        }
    }

    private final Listener listener;
    private List<HomeItem[]> pages = Collections.singletonList(new HomeItem[0]);
    private int columns = 4;
    private int rows = 1;
    private int homeRows = 0;
    private int cellHeight = 0;
    private boolean animate;
    @Nullable
    private String dragHiddenKey;
    @Nullable
    private String dragMergeKey;
    @Nullable
    private static final Object GREETING_REBIND = new Object();
    private RecyclerView attachedRv;
    private List<AppInfo> greetingRecent = Collections.emptyList();
    @Nullable
    private AppInfo greetingGame;
    private List<AppInfo> greetingSuggested = Collections.emptyList();
    private List<AppInfo> greetingGames = Collections.emptyList();
    @Nullable
    private java.util.function.Consumer<AppInfo> greetingClick;
    private boolean greetingBoundOnce;

    LauncherPagerAdapter(@NonNull Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    void submit(@NonNull List<HomeItem[]> newPages, int columns, int rows, int homeRows, int cellHeight, boolean animate) {
        final List<HomeItem[]> old = pages;
        final boolean geometryChanged = columns != this.columns || rows != this.rows || homeRows != this.homeRows || cellHeight != this.cellHeight;
        this.columns = columns;
        this.rows = rows;
        this.homeRows = homeRows;
        this.cellHeight = cellHeight;
        this.pages = newPages;
        this.animate = animate;

        if (!geometryChanged && attachedRv != null && old.size() == newPages.size()) {
            for (int p = 0; p < newPages.size(); p++) {
                if (java.util.Arrays.equals(old.get(p), newPages.get(p))) continue;
                AppGridPageView v = pageView(attachedRv, p);
                if (v != null) {
                    bindGrid(v, p);
                } else {
                    notifyItemChanged(p);
                }
            }
            this.animate = false;
            return;
        }

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return old.size();
            }

            @Override
            public int getNewListSize() {
                return newPages.size();
            }

            @Override
            public boolean areItemsTheSame(int o, int n) {
                return o == n;
            }

            @Override
            public boolean areContentsTheSame(int o, int n) {
                return !geometryChanged && java.util.Arrays.equals(old.get(o), newPages.get(n));
            }
        }, false);
        diff.dispatchUpdatesTo(this);
        if (geometryChanged) notifyItemChanged(newPages.size(), GREETING_REBIND);
        this.animate = false;
    }

    private void bindGrid(AppGridPageView grid, int position) {
        if (position == 0) {
            grid.setFixedCellHeight(cellHeight);
            grid.setContent(pages.get(0), columns, Math.max(1, homeRows), animate);
        } else {
            grid.setContent(pages.get(position), columns, rows, animate);
        }
        grid.setDragState(dragHiddenKey, dragMergeKey);
    }

    void setHover(@NonNull RecyclerView inner, int page, int cell) {
        for (int i = 0; i < inner.getChildCount(); i++) {
            View child = inner.getChildAt(i);
            AppGridPageView g = gridOf(inner.getChildViewHolder(child));
            if (g != null) g.setHoverCell(inner.getChildAdapterPosition(child) == page ? cell : -1);
        }
    }

    void setDragState(@NonNull RecyclerView inner, @Nullable String hiddenKey, @Nullable String mergeKey) {
        dragHiddenKey = hiddenKey;
        dragMergeKey = mergeKey;
        for (int i = 0; i < inner.getChildCount(); i++) {
            RecyclerView.ViewHolder vh = inner.getChildViewHolder(inner.getChildAt(i));
            AppGridPageView g = gridOf(vh);
            if (g != null) g.setDragState(hiddenKey, mergeKey);
        }
    }

    @Nullable
    private static AppGridPageView gridOf(@Nullable RecyclerView.ViewHolder vh) {
        if (vh instanceof HomeHolder) return ((HomeHolder) vh).grid;
        if (vh != null && vh.itemView instanceof AppGridPageView)
            return (AppGridPageView) vh.itemView;
        return null;
    }

    @Nullable
    AppGridPageView pageView(@NonNull RecyclerView inner, int adapterPosition) {
        return gridOf(inner.findViewHolderForAdapterPosition(adapterPosition));
    }

    void bounce(@NonNull RecyclerView inner, @NonNull String key) {
        for (int i = 0; i < inner.getChildCount(); i++) {
            AppGridPageView g = gridOf(inner.getChildViewHolder(inner.getChildAt(i)));
            if (g != null) g.bounce(key);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        attachedRv = rv;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        if (attachedRv == rv) attachedRv = null;
    }

    @Override
    public int getItemCount() {
        return pages.size() + 1;
    }

    void setGreetingData(@NonNull List<AppInfo> recent, @Nullable AppInfo game, @NonNull List<AppInfo> suggested, @NonNull List<AppInfo> games, @NonNull java.util.function.Consumer<AppInfo> click) {
        boolean same = greetingBoundOnce && greetingGame == game && sameApps(greetingRecent, recent) && sameApps(greetingSuggested, suggested) && sameApps(greetingGames, games);
        greetingGames = new ArrayList<>(games);
        greetingSuggested = new ArrayList<>(suggested);
        greetingRecent = new ArrayList<>(recent);
        greetingGame = game;
        greetingClick = click;
        greetingBoundOnce = true;
        if (same) return;
        // Never replace the Last Page holder for a data change: a notifyItemChanged without a payload makes RecyclerView
        // swap the page between two view instances, and the loaded ad belongs to only one of them.
        View live = greetingScroller();
        if (live instanceof GreetingPageView && greetingClick != null) {
            ((GreetingPageView) live).bind(greetingRecent, greetingGame, greetingSuggested, greetingGames, greetingClick);
        } else {
            notifyItemChanged(pages.size(), GREETING_REBIND);
        }
    }

    private static boolean sameApps(List<AppInfo> a, List<AppInfo> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            AppInfo x = a.get(i);
            AppInfo y = b.get(i);
            if (x != y && !(x.component.equals(y.component) && x.icon == y.icon && x.label.equals(y.label)))
                return false;
        }
        return true;
    }

    @Nullable
    View greetingScroller() {
        if (attachedRv == null) return null;
        RecyclerView.ViewHolder vh = attachedRv.findViewHolderForAdapterPosition(pages.size());
        return vh != null && vh.itemView instanceof GreetingPageView ? vh.itemView : null;
    }

    int appPageCount() {
        return pages.size();
    }

    @Override
    public long getItemId(int position) {
        return position == pages.size() ? -1L : position;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == pages.size()) return TYPE_GREETING;
        return position == 0 ? TYPE_HOME : TYPE_APPS;
    }

    private AppGridPageView.Listener gridListener() {
        return new AppGridPageView.Listener() {
            @Override
            public void onItemClicked(@NonNull View cell, @NonNull HomeItem item) {
                listener.onItemClicked(cell, item);
            }

            @Override
            public void onItemLongClicked(@NonNull View cell, @NonNull HomeItem item) {
                listener.onItemLongClicked(cell, item);
            }
        };
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_GREETING) {
            return new RecyclerView.ViewHolder(new GreetingPageView(parent.getContext())) {
            };
        }
        if (viewType == TYPE_HOME) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_home, parent, false);
            v.findViewById(R.id.search_bar).setOnClickListener(x -> listener.onSearchClicked());
            v.findViewById(R.id.home_mic).setOnClickListener(x -> listener.onVoiceSearchClicked());
            AppGridPageView grid = new AppGridPageView(parent.getContext(), gridListener());
            ((LinearLayout) v).addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
            if (DeviceLayout.isPixel(parent.getContext())) {
                v.findViewById(R.id.row_search).setVisibility(View.GONE);
                View clockRow = v.findViewById(R.id.row_clock);
                clockRow.setPadding(clockRow.getPaddingLeft(), clockRow.getPaddingTop() + Math.round(12 * parent.getResources().getDisplayMetrics().density), clockRow.getPaddingRight(), clockRow.getPaddingBottom());
            }
            HomeHolder h = new HomeHolder(v, grid);
            h.applyRows(cellHeight);
            return h;
        }
        return new RecyclerView.ViewHolder(new AppGridPageView(parent.getContext(), gridListener())) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.itemView instanceof GreetingPageView) {
            float d = holder.itemView.getResources().getDisplayMetrics().density;
            GreetingPageView gp = (GreetingPageView) holder.itemView;
            gp.setTopOffset(Math.round(6 * d));
            if (greetingClick != null)
                gp.bind(greetingRecent, greetingGame, greetingSuggested, greetingGames, greetingClick);
            return;
        }
        if (holder instanceof HomeHolder) ((HomeHolder) holder).applyRows(cellHeight);
        AppGridPageView g = gridOf(holder);
        if (g != null) bindGrid(g, position);
    }
}