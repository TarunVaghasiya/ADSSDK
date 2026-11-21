package com.adssetup.adssdk.sdk.launcher;

import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class DockItem {
    @NonNull
    public final Intent intent;
    @NonNull
    public final String label;
    @NonNull
    public final Bitmap icon;

    public DockItem(@NonNull Intent intent, @NonNull String label, @NonNull Bitmap icon) {
        this.intent = intent;
        this.label = label;
        this.icon = icon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DockItem)) return false;
        DockItem other = (DockItem) o;
        return Objects.equals(intent.getComponent(), other.intent.getComponent()) && label.equals(other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intent.getComponent(), label);
    }
}