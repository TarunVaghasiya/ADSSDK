package com.adssetup.adssdk.sdk.launcher;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adssetup.adssdk.sdk.R;

import java.util.ArrayList;
import java.util.List;

final class RecentHeaderAdapter extends RecyclerView.Adapter<RecentHeaderAdapter.VH> {
    private final int columns;
    private final DrawerAppsAdapter.OnAppClick click;
    private final DrawerAppsAdapter.OnAppLongClick longClick;
    private final List<AppInfo> apps = new ArrayList<>();
    private int cellHeight = 0;

    RecentHeaderAdapter(int columns, @NonNull DrawerAppsAdapter.OnAppClick click, @NonNull DrawerAppsAdapter.OnAppLongClick longClick) {
        this.columns = columns;
        this.click = click;
        this.longClick = longClick;
        setHasStableIds(true);
    }

    void submit(@NonNull List<AppInfo> recent) {
        List<AppInfo> next = new ArrayList<>(recent.subList(0, Math.min(columns, recent.size())));
        boolean wasShown = !apps.isEmpty();
        if (sameApps(next)) return;
        apps.clear();
        apps.addAll(next);
        boolean shown = !apps.isEmpty();
        if (wasShown && shown) notifyItemChanged(0);
        else if (shown) notifyItemInserted(0);
        else notifyItemRemoved(0);
    }

    private boolean sameApps(List<AppInfo> next) {
        if (next.size() != apps.size()) return false;
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i) != apps.get(i) && !next.get(i).component.equals(apps.get(i).component))
                return false;
        }
        return true;
    }

    void setCellHeight(int px) {
        if (px == cellHeight) return;
        cellHeight = px;
        if (!apps.isEmpty()) notifyItemChanged(0);
    }

    void rebind() {
        if (!apps.isEmpty()) notifyItemChanged(0);
    }

    @Override
    public int getItemCount() {
        return apps.isEmpty() ? 0 : 1;
    }

    @Override
    public long getItemId(int position) {
        return 1L;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final LinearLayout row;
        final View[] cells;

        VH(@NonNull View v, LinearLayout row, View[] cells) {
            super(v);
            this.row = row;
            this.cells = cells;
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final float d = parent.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(parent.getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        View[] cells = new View[columns];
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        for (int i = 0; i < columns; i++) {
            View cell = inflater.inflate(R.layout.item_app, row, false);
            DeviceLayout.applyLabelSize(cell);
            IconTouchLift.attach(cell, cell.findViewById(R.id.app_icon));
            row.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            cells[i] = cell;
        }
        box.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View divider = new View(parent.getContext());
        divider.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(1 * d)));
        dl.setMargins(Math.round(8 * d), Math.round(4 * d), Math.round(8 * d), Math.round(8 * d));
        if (DeviceLayout.isPixel(parent.getContext())) {
            int h = Math.max(2, Math.round(1.5f * d));
            dl = new LinearLayout.LayoutParams(Math.round(148 * d), h);
            dl.gravity = Gravity.CENTER_HORIZONTAL;
            dl.setMargins(0, Math.round(12 * d), 0, Math.round(12 * d));
            android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
            pill.setColor(0xFF313131);
            pill.setCornerRadius(h / 2f);
            divider.setBackground(pill);
        }
        box.addView(divider, dl);
        box.setGravity(Gravity.TOP);
        return new VH(box, row, cells);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        for (int i = 0; i < columns; i++) {
            final View cell = h.cells[i];
            ViewGroup.LayoutParams lp = cell.getLayoutParams();
            if (cellHeight > 0 && lp.height != cellHeight) {
                lp.height = cellHeight;
                cell.setLayoutParams(lp);
            }
            IconTouchLift.reset(cell.findViewById(R.id.app_icon));
            if (i >= apps.size()) {
                cell.setVisibility(View.INVISIBLE);
                cell.setOnClickListener(null);
                cell.setOnLongClickListener(null);
                continue;
            }
            final AppInfo app = apps.get(i);
            cell.setVisibility(View.VISIBLE);
            DeviceLayout.applyIconSize(cell.findViewById(R.id.app_icon));
            ((ImageView) cell.findViewById(R.id.app_icon)).setImageBitmap(app.icon);
            TextView label = cell.findViewById(R.id.app_label);
            label.setText(app.label);
            DeviceLayout.styleDrawerLabel(label);
            cell.setOnClickListener(x -> click.onAppClicked(app));
            cell.setOnLongClickListener(x -> {
                x.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                longClick.onAppLongClicked(x, app);
                return true;
            });
        }
    }
}