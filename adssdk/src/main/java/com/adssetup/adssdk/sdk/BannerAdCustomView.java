package com.adssetup.adssdk.sdk;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdSize;

import java.lang.ref.WeakReference;

public class BannerAdCustomView extends FrameLayout implements DefaultLifecycleObserver {
    private static final Size[] SIZES = Size.values();
    private final Size size;
    private boolean authorized;
    private BannerAdPlacement placement;
    private ShimmerFrameLayout shimmer;
    private LifecycleOwner observedOwner;
    private boolean destroyed;
    private String adUnitId;
    private AdSize adSize;

    public enum Size {
        SMALL(0f), MEDIUM(0.25f), LARGE(0.4f);

        final float maxHeightFraction;

        Size(float maxHeightFraction) {
            this.maxHeightFraction = maxHeightFraction;
        }
    }

    public BannerAdCustomView(@NonNull Context context) {
        this(context, null);
    }

    public BannerAdCustomView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerAdCustomView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerAdCustomView, defStyleAttr, 0);
        try {
            size = SIZES[typedArray.getInt(R.styleable.BannerAdCustomView_bannerAdSize, 0)];
        } finally {
            typedArray.recycle();
        }
    }

    void load(@NonNull String adUnitId) {
        if (destroyed || placement != null) {
            return;
        }
        placement = new BannerAdPlacement();
        this.adUnitId = adUnitId;

        showPlaceholder();
        WeakReference<BannerAdCustomView> viewReference = new WeakReference<>(this);
        SdkAuthorization.authorize(getContext(), granted -> {
            BannerAdCustomView view = viewReference.get();
            if (view == null || view.destroyed) {
                return;
            }
            if (granted) {
                view.authorized = true;
                view.start();
            } else {
                view.hideShimmer();
                view.setVisibility(GONE);
            }
        });
    }

    private void showPlaceholder() {
        if (getWidth() > 0) {
            showShimmer(resolveAdSize());
        }
    }

    private void start() {
        if (getWidth() == 0) {
            return;
        }
        adSize = resolveAdSize();
        placement.load(this, adUnitId, adSize);
    }

    @NonNull
    private AdSize resolveAdSize() {
        int widthPx = getWidth() - getPaddingLeft() - getPaddingRight();
        int widthDp = Math.round(widthPx / getResources().getDisplayMetrics().density);
        return BannerAdPlacement.adSizeFor(getContext(), size, widthDp);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (adUnitId == null || destroyed || placement == null || width <= 0 || width == oldWidth) {
            return;
        }
        if (!authorized) {
            showPlaceholder();
            return;
        }
        if (adSize == null) {
            start();
            return;
        }
        AdSize newSize = resolveAdSize();
        if (!newSize.equals(adSize)) {
            adSize = newSize;
            placement.resize(this, adUnitId, newSize);
        }
    }

    private int slotHeight(@NonNull AdSize adSize) {
        return adSize.getHeight() > 0 ? adSize.getHeightInPixels(getContext()) : Math.round(getResources().getDisplayMetrics().heightPixels * size.maxHeightFraction);
    }

    void showShimmer(@NonNull AdSize adSize) {
        if (shimmer != null) {
            return;
        }
        shimmer = (ShimmerFrameLayout) LayoutInflater.from(getContext()).inflate(R.layout.shimmer_layout, this, false);
        shimmer.setShimmer(new Shimmer.AlphaHighlightBuilder().setDuration(1200).build());
        shimmer.setLayoutParams(new LayoutParams(adSize.getWidthInPixels(getContext()), slotHeight(adSize), Gravity.CENTER_HORIZONTAL));
        addView(shimmer);
        shimmer.startShimmer();
    }

    void hideShimmer() {
        if (shimmer != null) {
            shimmer.stopShimmer();
            removeView(shimmer);
            shimmer = null;
        }
    }

    public void destroy() {
        destroyed = true;
        hideShimmer();
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
    public void onPause(@NonNull LifecycleOwner owner) {
        if (placement != null) {
            placement.pause();
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (placement != null) {
            placement.resume();
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