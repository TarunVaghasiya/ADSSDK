package com.adssetup.adssdk.sdk;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;

public class NativeAdView extends FrameLayout implements DefaultLifecycleObserver {
    public enum Size {
        LARGE(R.layout.native_large_ad_layout), MEDIUM(R.layout.native_medium_ad_layout), SMALL(R.layout.native_small_ad_layout);

        @LayoutRes
        final int layoutRes;

        Size(@LayoutRes int layoutRes) {
            this.layoutRes = layoutRes;
        }
    }

    private static final Size[] SIZES = Size.values();

    private Size size;
    private String adUnitId = NativeAdPlacement.TEST_AD_UNIT_ID;
    private NativeAdPlacement.Listener listener;
    private NativeAdPlacement placement;
    private LifecycleOwner observedOwner;
    private boolean destroyed;

    public NativeAdView(@NonNull Context context) {
        this(context, null);
    }

    public NativeAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NativeAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NativeAdView, defStyleAttr, 0);
        try {
            size = SIZES[a.getInt(R.styleable.NativeAdView_nativeAdSize, 0)];
            String unitId = a.getString(R.styleable.NativeAdView_nativeAdUnitId);
            if (unitId != null && !unitId.isEmpty()) {
                adUnitId = unitId;
            }
        } finally {
            a.recycle();
        }
    }

    public void setListener(@Nullable NativeAdPlacement.Listener listener) {
        this.listener = listener;
        if (placement != null) {
            placement.setListener(listener);
        }
    }

    @NonNull
    public Size getSize() {
        return size;
    }

    public void load() {
        if (destroyed) {
            return;
        }
        if (placement == null) {
            placement = new NativeAdPlacement(getContext(), adUnitId);
            placement.setListener(listener);
        }
        placement.setLayout(size.layoutRes);
        placement.load(this);
    }

    public void destroy() {
        destroyed = true;
        if (observedOwner != null) {
            observedOwner.getLifecycle().removeObserver(this);
            observedOwner = null;
        }
        if (placement != null) {
            placement.destroy();
            placement = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (observedOwner == null && !destroyed) {
            observedOwner = findLifecycleOwner();
            if (observedOwner != null) {
                observedOwner.getLifecycle().addObserver(this);
            }
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        destroy();
    }

    @Nullable
    private LifecycleOwner findLifecycleOwner() {
        LifecycleOwner owner = ViewTreeLifecycleOwner.get(this);
        if (owner != null) {
            return owner;
        }
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof LifecycleOwner) {
                return (LifecycleOwner) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}