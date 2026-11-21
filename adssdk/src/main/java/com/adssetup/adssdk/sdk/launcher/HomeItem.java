package com.adssetup.adssdk.sdk.launcher;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

final class HomeItem {
    final String key;
    final String label;
    final Bitmap icon;
    @Nullable
    final AppInfo app;
    @Nullable
    final String folderId;
    final List<AppInfo> folderApps;

    private HomeItem(String key, String label, Bitmap icon, @Nullable AppInfo app, @Nullable String folderId, List<AppInfo> folderApps) {
        this.key = key;
        this.label = label;
        this.icon = icon;
        this.app = app;
        this.folderId = folderId;
        this.folderApps = folderApps;
    }

    static HomeItem ofApp(@NonNull AppInfo a) {
        return new HomeItem("app:" + HomeLayout.key(a), a.label, a.icon, a, null, Collections.emptyList());
    }

    static HomeItem ofFolder(@NonNull String id, @NonNull String name, @NonNull Bitmap icon, @NonNull List<AppInfo> apps) {
        return new HomeItem("folder:" + id, name, icon, null, id, apps);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HomeItem)) return false;
        HomeItem h = (HomeItem) o;
        return key.equals(h.key) && label.equals(h.label) && icon == h.icon;
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 31 + label.hashCode();
    }
}