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

/**
 * XML-declared native ad. Add it to a layout, pick a size with {@code app:nativeAdSize}
 * ({@code large}, {@code medium} or {@code small}) and call {@link #load()}:
 *
 * <pre>
 * &lt;com.adssetup.adssdk.sdk.NativeAdView
 *     android:id="@+id/ad"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:nativeAdSize="medium" /&gt;
 * </pre>
 *
 * It wraps {@link NativeAdPlacement}, so loading, callbacks and cleanup are unchanged, and it
 * destroys the ad with its Activity/Fragment lifecycle automatically. It has zero height until an
 * ad loads, so a failed request leaves no empty space. The Mobile Ads SDK must already be
 * initialized. Defaults to Google's test ad unit; override with {@code app:nativeAdUnitId}.
 */
public class NativeAdView extends FrameLayout implements DefaultLifecycleObserver {

    public enum Size {
        /** Media above icon, text and button. */
        LARGE(R.layout.native_large_ad_layout),
        /** Media on the left, icon, text and button on the right. */
        MEDIUM(R.layout.native_medium_ad_layout),
        /** One compact row: icon, text and button. */
        SMALL(R.layout.native_small_ad_layout);

        @LayoutRes
        final int layoutRes;

        Size(@LayoutRes int layoutRes) {
            this.layoutRes = layoutRes;
        }
    }

    private Size size = Size.LARGE;
    private String adUnitId = NativeAdPlacement.TEST_AD_UNIT_ID;
    @Nullable
    private NativeAdPlacement.Listener listener;
    @Nullable
    private NativeAdPlacement placement;
    @Nullable
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
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NativeAdView,
                    defStyleAttr, 0);
            try {
                size = Size.values()[a.getInt(R.styleable.NativeAdView_nativeAdSize, 0)];
                String unitId = a.getString(R.styleable.NativeAdView_nativeAdUnitId);
                if (unitId != null && !unitId.isEmpty()) {
                    adUnitId = unitId;
                }
            } finally {
                a.recycle();
            }
        }
    }

    public void setListener(@Nullable NativeAdPlacement.Listener listener) {
        this.listener = listener;
        if (placement != null) {
            placement.setListener(listener);
        }
    }

    /** Changes the size. Takes effect on the next {@link #load()}. */
    public void setSize(@NonNull Size size) {
        this.size = size;
    }

    @NonNull
    public Size getSize() {
        return size;
    }

    /** Changes the ad unit. Takes effect on the next {@link #load()}. */
    public void setAdUnitId(@NonNull String adUnitId) {
        this.adUnitId = adUnitId;
    }

    /** Requests an ad and shows it in this view. Ignored while loading or after destroy. */
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

    /** Releases the ad. Called automatically on lifecycle destroy; safe to call again. */
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
        removeAllViews();
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
        LifecycleOwner owner = androidx.lifecycle.ViewTreeLifecycleOwner.get(this);
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
