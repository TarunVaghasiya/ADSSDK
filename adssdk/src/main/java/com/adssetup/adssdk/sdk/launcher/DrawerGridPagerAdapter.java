package com.adssetup.adssdk.sdk.launcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DrawerGridPagerAdapter extends RecyclerView.Adapter<DrawerGridPagerAdapter.PageVH> {
    static final int COLUMNS = 4;
    static final int ROWS = 7;
    static final int PER_PAGE = COLUMNS * ROWS;

    private final DrawerAppsAdapter.OnAppClick click;
    private final DrawerAppsAdapter.OnAppLongClick longClick;
    private List<AppInfo> apps = Collections.emptyList();
    private int cellHeight;

    DrawerGridPagerAdapter(@NonNull DrawerAppsAdapter.OnAppClick click, @NonNull DrawerAppsAdapter.OnAppLongClick longClick) {
        this.click = click;
        this.longClick = longClick;
    }

    static int pagesFor(int appCount) {
        return (appCount + PER_PAGE - 1) / PER_PAGE;
    }

    @SuppressLint("NotifyDataSetChanged")
    void submit(@NonNull List<AppInfo> list) {
        if (list == apps) return;
        apps = list;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    void setCellHeight(int px) {
        if (px != cellHeight) {
            cellHeight = px;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return pagesFor(apps.size());
    }

    static final class PageVH extends RecyclerView.ViewHolder {
        final DrawerAppsAdapter cells;

        PageVH(@NonNull RecyclerView rv, @NonNull DrawerAppsAdapter cells) {
            super(rv);
            this.cells = cells;
        }
    }

    @NonNull
    @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context c = parent.getContext();
        RecyclerView rv = new RecyclerView(c);
        rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setLayoutManager(new GridLayoutManager(c, COLUMNS) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        rv.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        rv.setItemAnimator(null);
        rv.setNestedScrollingEnabled(false);
        int side = Math.round(8 * c.getResources().getDisplayMetrics().density);
        rv.setPadding(side, 0, side, 0);
        rv.setClipToPadding(true);
        DrawerAppsAdapter cells = new DrawerAppsAdapter(click, longClick);
        rv.setAdapter(cells);
        return new PageVH(rv, cells);
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH h, int position) {
        int from = position * PER_PAGE;
        int to = Math.min(apps.size(), from + PER_PAGE);
        h.cells.submitList(from < to ? new ArrayList<>(apps.subList(from, to)) : Collections.emptyList());
        h.cells.setCellHeight(cellHeight);
    }
}