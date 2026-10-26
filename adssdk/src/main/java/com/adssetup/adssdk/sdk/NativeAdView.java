package com.adssetup.adssdk.sdk;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.LoadAdError;

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
    private NativeAdPlacement.Listener listener;
    private NativeAdPlacement placement;
    private ShimmerFrameLayout shimmer;
    private View sizeReference;
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
        } finally {
            a.recycle();
        }
    }

    private final NativeAdPlacement.Listener callbacks = new NativeAdPlacement.Listener() {
        @Override
        public void onAdLoaded() {
            if (listener != null) {
                listener.onAdLoaded();
            }
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError error) {
            if (listener != null) {
                listener.onAdFailedToLoad(error);
            }
        }

        @Override
        public void onAdClicked() {
            if (listener != null) {
                listener.onAdClicked();
            }
        }

        @Override
        public void onAdImpression() {
            if (listener != null) {
                listener.onAdImpression();
            }
        }
    };

    void load(@NonNull String adUnitId) {
        if (destroyed || placement != null) {
            return;
        }
        placement = new NativeAdPlacement();
        placement.setListener(callbacks);
        placement.setLayout(size.layoutRes);
        placement.load(this, adUnitId);
    }

    void showShimmer() {
        if (shimmer != null) {
            return;
        }
        shimmer = (ShimmerFrameLayout) LayoutInflater.from(getContext()).inflate(R.layout.shimmer_layout, this, false);
        shimmer.setShimmer(new Shimmer.AlphaHighlightBuilder().setDuration(1200).build());

        sizeReference = LayoutInflater.from(getContext()).inflate(size.layoutRes, this, false);
        TextView body = sizeReference.findViewById(R.id.ad_body);
        body.setLines(body.getMaxLines());

        addView(shimmer);
        shimmer.startShimmer();
    }

    void hideShimmer() {
        if (shimmer != null) {
            shimmer.stopShimmer();
            removeView(shimmer);
            shimmer = null;
            sizeReference = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (shimmer != null) {
            int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
            sizeReference.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.UNSPECIFIED);
            shimmer.getLayoutParams().height = sizeReference.getMeasuredHeight();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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