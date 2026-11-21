package com.adssetup.adssdk.sdk.launcher;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.adssetup.adssdk.sdk.R;

public class DrawerAppsAdapter extends ListAdapter<AppInfo, DrawerAppsAdapter.VH> {
    public interface OnAppClick {
        void onAppClicked(@NonNull AppInfo app);
    }

    private static final DiffUtil.ItemCallback<AppInfo> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppInfo a, @NonNull AppInfo b) {
            return a.component.equals(b.component);
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull AppInfo a, @NonNull AppInfo b) {
            return a.label.equals(b.label) && a.icon == b.icon;
        }
    };

    public interface OnAppLongClick {
        void onAppLongClicked(@NonNull View cell, @NonNull AppInfo app);
    }

    private final OnAppClick listener;
    private final OnAppLongClick longListener;
    private int cellHeight;

    public DrawerAppsAdapter(@NonNull OnAppClick listener, @NonNull OnAppLongClick longListener) {
        super(DIFF);
        this.listener = listener;
        this.longListener = longListener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).component.hashCode();
    }

    public void setCellHeight(int px) {
        if (px != cellHeight) {
            cellHeight = px;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        DeviceLayout.applyLabelSize(v);
        final VH vh = new VH(v);
        IconTouchLift.attach(v, vh.icon);
        v.setOnClickListener(x -> {
            int p = vh.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) listener.onAppClicked(getItem(p));
        });
        v.setOnLongClickListener(x -> {
            int p = vh.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) return false;
            x.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            longListener.onAppLongClicked(x, getItem(p));
            return true;
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppInfo app = getItem(position);
        IconTouchLift.reset(h.icon);
        DeviceLayout.applyIconSize(h.icon);
        h.icon.setImageBitmap(app.icon);
        h.label.setText(app.label);
        DeviceLayout.styleDrawerLabel(h.label);
        ViewGroup.LayoutParams lp = h.itemView.getLayoutParams();
        if (cellHeight > 0 && lp.height != cellHeight) {
            lp.height = cellHeight;
            h.itemView.setLayoutParams(lp);
        }
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        VH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.app_icon);
            label = v.findViewById(R.id.app_label);
        }
    }
}