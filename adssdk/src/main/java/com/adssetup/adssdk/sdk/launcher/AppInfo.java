package com.adssetup.adssdk.sdk.launcher;

import android.content.ComponentName;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class AppInfo {
    @NonNull
    public final ComponentName component;
    @NonNull
    public final String label;
    @NonNull
    public final Bitmap icon;

    public AppInfo(@NonNull ComponentName component, @NonNull String label, @NonNull Bitmap icon) {
        this.component = component;
        this.label = label;
        this.icon = icon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppInfo)) return false;
        AppInfo other = (AppInfo) o;
        return component.equals(other.component) && label.equals(other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, label);
    }
}