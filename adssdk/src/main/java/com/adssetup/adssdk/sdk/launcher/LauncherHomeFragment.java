package com.adssetup.adssdk.sdk.launcher;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.adssetup.adssdk.sdk.AdsController;
import com.adssetup.adssdk.sdk.BannerAdCustomView;
import com.adssetup.adssdk.sdk.BannerAdPlacement;
import com.adssetup.adssdk.sdk.NativeAdCustomView;
import com.adssetup.adssdk.sdk.NativeAdPlacement;
import com.adssetup.adssdk.sdk.R;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LauncherHomeFragment extends Fragment implements LauncherPagerAdapter.Listener {
    private static final String STATE_PAGE = "state_page";
    private static final String GOOGLE_APP = "com.google.android.googlequicksearchbox";
    private static final int COLUMNS = 4;
    private static final int ROWS = 6;
    private static final long REFRESH_DEBOUNCE_MS = 400;
    private LauncherViewModel viewModel;
    private ViewPager2 pager;
    private PageIndicatorView indicator;
    private ImageView[] dockViews;
    private LauncherPagerAdapter adapter;
    private LauncherRootLayout rootLayout;
    private View homeContent;
    private View drawerView;
    private androidx.recyclerview.widget.RecyclerView drawerList;
    private DrawerAppsAdapter drawerAdapter;
    private EditText searchInput;
    private ImageView searchIcon;
    private ImageView searchClear;
    private boolean pixelSearch;
    private boolean samsungSearch;
    private boolean motorolaSearch;
    private int imeBottom;
    private View drawerAdView;
    private boolean drawerAdEnabled;
    private boolean drawerAdShown;
    private LauncherAdRefresh drawerRefresh;

    private void setUpDrawerAd() {
        drawerAdView = null;
        drawerAdEnabled = false;
        drawerAdShown = false;
        drawerRefresh = null;
        View container = drawerView.findViewById(R.id.drawer_ad);
        String type = AdsController.getLauncherDrawerAdType();
        boolean banner = "banner".equalsIgnoreCase(type);
        boolean nativeAd = "native".equalsIgnoreCase(type);
        String adUnitId = banner ? AdsController.getLauncherDrawerBannerId() : nativeAd ? AdsController.getLauncherDrawerNativeId() : "";
        if (!AdsController.isAdsEnabled() || !AdsController.isLauncherDrawerAdShow() || adUnitId.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        drawerAdView = drawerView.findViewById(banner ? R.id.drawer_banner_ad : R.id.drawer_native_ad);
        if (drawerAdView instanceof NativeAdCustomView) {
            styleDrawerNative((NativeAdCustomView) drawerAdView);
            container.setMinimumHeight(0);
        }
        LauncherAdRefresh refresh = new LauncherAdRefresh(requireContext(), "launcher_drawer_ad", AdsController.isQuizPriority() ? 0 : AdsController.getLauncherDrawerAdShowPerDay(), nativeAd ? LauncherAdRefresh.NATIVE_MAX_AGE_MS : 0L);
        drawerRefresh = refresh;
        if (drawerAdView instanceof BannerAdCustomView) {
            ((BannerAdCustomView) drawerAdView).setLoadListener(refresh::onResult);
        } else {
            ((NativeAdCustomView) drawerAdView).setLoadListener(refresh::onResult);
        }
        drawerAdEnabled = true;
        container.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (drawerAdShown && drawerAdView != null && drawerAdView.getVisibility() == View.GONE) {
                drawerAdShown = false;
                v.post(() -> v.setVisibility(View.GONE));
            }
        });
    }

    private void onDrawerAdVisible() {
        if (!drawerAdEnabled || drawerAdView == null || drawerRefresh == null) return;
        if (drawerRefresh.onVisible() == LauncherAdRefresh.Action.NONE) return;
        drawerAdShown = true;
        drawerAdView.setVisibility(View.VISIBLE);
        drawerView.findViewById(R.id.drawer_ad).setVisibility(View.VISIBLE);
        if (drawerAdView instanceof BannerAdCustomView) {
            BannerAdPlacement.refreshBannerAd(requireActivity(), AdsController.getLauncherDrawerBannerId(), (BannerAdCustomView) drawerAdView);
        } else {
            NativeAdPlacement.refreshNativeAd(requireActivity(), AdsController.getLauncherDrawerNativeId(), (NativeAdCustomView) drawerAdView);
        }
    }

    private int searchBarColor() {
        View bar = drawerView == null ? null : drawerView.findViewById(R.id.search_bar);
        if (bar != null && bar.getBackground() instanceof GradientDrawable) {
            ColorStateList color = ((GradientDrawable) bar.getBackground()).getColor();
            if (color != null) return color.getDefaultColor();
        }
        return ContextCompat.getColor(requireContext(), R.color.drawer_search_bar_background);
    }

    private void styleDrawerNative(@NonNull NativeAdCustomView nativeView) {
        nativeView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                applyDrawerNativeStyle(child);
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
            }
        });
    }

    private void applyDrawerNativeStyle(@NonNull View child) {
        if (getContext() == null || drawerView == null) return;
        int bg = searchBarColor();
        boolean lightBackground = ColorUtils.calculateLuminance(bg) > 0.5;
        int primary = ContextCompat.getColor(requireContext(), lightBackground ? R.color.drawer_ad_text_on_light : R.color.drawer_ad_text_on_dark);
        int secondary = ContextCompat.getColor(requireContext(), lightBackground ? R.color.drawer_ad_text_secondary_on_light : R.color.drawer_ad_text_secondary_on_dark);
        View panel = child;
        if (child.getId() == R.id.native_ad_view && child instanceof ViewGroup && ((ViewGroup) child).getChildCount() > 0) {
            panel = ((ViewGroup) child).getChildAt(0);
        }
        panel.setBackgroundColor(bg);
        styleNativeTree(panel, bg, primary, secondary);
    }

    private static void styleNativeTree(@NonNull View v, int bg, int primary, int secondary) {
        if (v instanceof CardView) ((CardView) v).setCardBackgroundColor(bg);
        if (v instanceof TextView) {
            int id = v.getId();
            if (id == R.id.ad_headline || id == R.id.tvQZAppTitle)
                ((TextView) v).setTextColor(primary);
            else if (id == R.id.ad_body || id == R.id.tvQZAppDescription)
                ((TextView) v).setTextColor(secondary);
        }
        if (v instanceof ViewGroup && !(v instanceof ShimmerFrameLayout)) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++)
                styleNativeTree(group.getChildAt(i), bg, primary, secondary);
        }
    }

    private void setMotorolaKeyboardResize(boolean resize) {
        if (!motorolaSearch || getActivity() == null) return;
        android.view.Window w = requireActivity().getWindow();
        int mode = w.getAttributes().softInputMode & ~android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
        w.setSoftInputMode(mode | (resize ? android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE : android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN));
    }

    private boolean samsungGrid;
    private int navInsetBottom;
    private PageIndicatorView drawerIndicator;
    private ImageView searchMenu;
    private Boolean navLightBefore;
    private View recentLayer;
    private androidx.recyclerview.widget.RecyclerView recentList;
    private DrawerAppsAdapter recentAdapter;
    private RecentHeaderAdapter recentHeader;
    private TextView recentEmpty;
    private TextView recentTitle;
    private List<AppInfo> suggestedCache;
    private List<AppInfo> suggestedFor;
    private TextView searchEmpty;
    private boolean searchMode;
    private Toast blockToast;
    private View bottomSearchRow;
    private String lastQuery = "";
    private boolean scrollTopPending;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<AppInfo> apps = Collections.emptyList();
    private HomeLayout homeLayout;
    private boolean appsDirty = true;
    private boolean built;
    private HomeOverlays overlays;
    private HomeDragController dragController;
    private boolean receiverRegistered;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            viewModel.refresh();
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.removeCallbacks(refreshRunnable);
            handler.postDelayed(refreshRunnable, REFRESH_DEBOUNCE_MS);
        }
    };

    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (searchMode) {
                exitSearch();
                return;
            }
            if (overlays != null && overlays.closeTop()) {
                return;
            }
            if (rootLayout != null && rootLayout.isOpen()) {
                rootLayout.closeDrawer(true);
            } else if (pager != null) {
                pager.setCurrentItem(0, true);
            }
        }
    };


    /**
     * Detects a right swipe that starts on the first Home page, where the pager has nothing to scroll to, and opens
     * the registered main container fragment (see {@link LauncherNavigation}). It only observes touches (never intercepts), fires once per gesture, and ignores
     * gestures while the drawer, search or an overlay is open.
     */
    private final class FirstPageRightSwipe implements androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {
        private float downX;
        private float downY;
        private boolean tracking;

        @Override
        public boolean onInterceptTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull MotionEvent e) {
            track(rv, e);
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull MotionEvent e) {
            track(rv, e);
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }

        private void track(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    tracking = pager != null && pager.getCurrentItem() == 0 && !searchMode && (rootLayout == null || !rootLayout.isOpen()) && (overlays == null || !overlays.isAnyOpen());
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!tracking || e.getPointerCount() != 1) return;
                    float dx = e.getX() - downX;
                    float dy = Math.abs(e.getY() - downY);
                    float threshold = 40 * rv.getResources().getDisplayMetrics().density;
                    if (dx > threshold && dx > dy * 2f) {
                        tracking = false;
                        LauncherNavigation.openMainContainer(requireActivity());
                    }
                    break;
                default:
                    tracking = false;
                    break;
            }
        }
    }

    private void updateBackEnabled() {
        boolean drawerOpen = rootLayout != null && rootLayout.isOpen();
        boolean overlayOpen = (overlays != null && overlays.isAnyOpen()) || searchMode;
        boolean enable = drawerOpen || overlayOpen || viewModel.getCurrentPage() != 0;
        if (backCallback.isEnabled() != enable) backCallback.setEnabled(enable);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LauncherViewModel.class);
        homeLayout = viewModel.getHomeLayout();
        if (savedInstanceState != null) {
            viewModel.setCurrentPage(savedInstanceState.getInt(STATE_PAGE, 0));
        } else {
            int pending = pendingReturnPage();
            if (pending >= 0) viewModel.setCurrentPage(pending);
        }
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);
        registerPackageReceiver();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_launcher_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        pager = view.findViewById(R.id.pager);
        indicator = view.findViewById(R.id.page_indicator);
        if (DeviceLayout.isPixel(requireContext())) {
            bottomSearchRow = view.findViewById(R.id.bottom_search_row);
            bottomSearchRow.setVisibility(View.VISIBLE);
            view.findViewById(R.id.bottom_search_bar).setOnClickListener(x -> onSearchClicked());
            view.findViewById(R.id.bottom_mic).setOnClickListener(x -> openVoiceSearch());
        }

        rootLayout = (LauncherRootLayout) view;
        rootLayout.setHomeScrollProvider(() -> pager != null && adapter != null && pager.getCurrentItem() == adapter.getItemCount() - 1 ? adapter.greetingScroller() : null);
        homeContent = view.findViewById(R.id.home_content);
        drawerView = view.findViewById(R.id.drawer);
        drawerList = view.findViewById(R.id.drawer_list);
        setUpDrawerAd();
        final boolean pixelDrawer = DeviceLayout.isPixel(requireContext());
        if (pixelDrawer) view.findViewById(R.id.drawer_handle).setVisibility(View.VISIBLE);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            homeContent.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            imeBottom = motorolaSearch ? windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom : 0;
            drawerView.setPadding(bars.left, bars.top, bars.right, motorolaSearch ? Math.max(bars.bottom, imeBottom) : 0);
            if (pixelDrawer) {
                float r = 30 * getResources().getDisplayMetrics().density;
                android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                shape.setColor(0xCCFFFFFF);
                shape.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
                drawerView.setBackground(new android.graphics.drawable.InsetDrawable(shape, 0, bars.top, 0, 0));
            }
            navInsetBottom = motorolaSearch ? bars.bottom : 0;
            drawerList.setPadding(drawerList.getPaddingLeft(), 0, drawerList.getPaddingRight(), 0);
            View drawerAd = drawerView.findViewById(R.id.drawer_ad);
            ViewGroup.MarginLayoutParams adLp = (ViewGroup.MarginLayoutParams) drawerAd.getLayoutParams();
            if (motorolaSearch) {
                int adVis = imeBottom > 0 || !drawerAdShown ? View.GONE : View.VISIBLE;
                if (drawerAd.getVisibility() != adVis) drawerAd.setVisibility(adVis);
            }
            int adBottom = motorolaSearch ? 0 : bars.bottom;
            if (adLp.bottomMargin != adBottom) {
                adLp.bottomMargin = adBottom;
                drawerAd.setLayoutParams(adLp);
            }
            applyGridInsets();
            return WindowInsetsCompat.CONSUMED;
        });

        adapter = new LauncherPagerAdapter(this);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        View pagerRv = pager.getChildAt(0);
        if (pagerRv instanceof androidx.recyclerview.widget.RecyclerView) {
            ((androidx.recyclerview.widget.RecyclerView) pagerRv).setItemViewCacheSize(8);
            ((androidx.recyclerview.widget.RecyclerView) pagerRv).addOnItemTouchListener(new FirstPageRightSwipe());
        }
        pager.setSaveEnabled(false);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float offset, int px) {
                indicator.setPosition(position + offset);
                if (adapter != null && offset > 0f && position == adapter.getItemCount() - 2) {
                    onEnterLastPage();
                    if (!leavingLastPage) setLastPageChromeHidden(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (adapter == null || pager == null) return;
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    leavingLastPage = pager.getCurrentItem() == adapter.getItemCount() - 1;
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    leavingLastPage = false;
                    setLastPageChromeHidden(pager.getCurrentItem() == adapter.getItemCount() - 1);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (adapter != null) {
                    int last = adapter.getItemCount() - 1;
                    if (position == last) {
                        onEnterLastPage();
                        setLastPageChromeHidden(true);
                    } else {
                        if (selectedPage == last) leavingLastPage = true;
                        gamesPickedForVisit = false;
                        setLastPageChromeHidden(false);
                    }
                    selectedPage = position;
                }
                viewModel.setCurrentPage(position);
                updateBackEnabled();
            }
        });
        pager.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) {
                if (chromeDelta > 0 && r - l == or - ol && Math.abs((b - t) - (ob - ot)) == chromeDelta)
                    return;
                updatePages();
            }
        });

        setUpDrawer();
        setUpSearch(view);
        setUpHomeEditing();

        dockViews = new ImageView[]{view.findViewById(R.id.dock_phone), view.findViewById(R.id.dock_messages), view.findViewById(R.id.dock_browser), view.findViewById(R.id.dock_camera)};
        applyDockIconSize(DeviceLayout.iconPx(requireContext()));
        viewModel.getDock().observe(getViewLifecycleOwner(), items -> {
            if (items != null && homeLayout.setDockDefaults(items)) {
                appsDirty = true;
                updatePages();
            }
        });

        viewModel.getApps().observe(getViewLifecycleOwner(), list -> {
            apps = list;
            appsDirty = true;
            applyDrawerState();
            updatePages();
        });
        restoreDrawerIfPending();
    }

    @SuppressLint("InvalidSetHasFixedSize")
    private void setUpDrawer() {
        drawerAdapter = new DrawerAppsAdapter(this::onAppClicked, this::onDrawerAppLongClicked);
        recentHeader = new RecentHeaderAdapter(COLUMNS, this::onAppClicked, this::onDrawerAppLongClicked);
        androidx.recyclerview.widget.GridLayoutManager drawerLm = new androidx.recyclerview.widget.GridLayoutManager(requireContext(), COLUMNS);
        drawerLm.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 && recentHeader != null && recentHeader.getItemCount() > 0 ? COLUMNS : 1;
            }
        });
        drawerList.setLayoutManager(drawerLm);
        samsungGrid = DeviceLayout.isSamsung() && !noDrawer() && requireContext().getSharedPreferences(PREFS_DRAWER, Context.MODE_PRIVATE).getBoolean(KEY_GRID, false);
        if (samsungGrid) setUpGridPager();
        drawerList.setAdapter(new androidx.recyclerview.widget.ConcatAdapter(new androidx.recyclerview.widget.ConcatAdapter.Config.Builder().setIsolateViewTypes(true).setStableIdMode(androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS).build(), recentHeader, drawerAdapter));
        drawerList.setHasFixedSize(true);
        drawerList.setItemAnimator(null);
        drawerList.setItemViewCacheSize(COLUMNS * 2);
        if (apps != null && !apps.isEmpty()) drawerAdapter.submitList(apps);
        drawerList.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateDrawerCellSize());
        if (noDrawer()) {
            rootLayout.setDrawerEnabled(false);
            drawerView.setVisibility(View.GONE);
        }
        rootLayout.bindDrawer(drawerView, drawerList, homeContent, open -> {
            if (DeviceLayout.isPixel(requireContext())) {
                android.view.Window w = requireActivity().getWindow();
                androidx.core.view.WindowInsetsControllerCompat wic = androidx.core.view.WindowCompat.getInsetsController(w, w.getDecorView());
                if (open) {
                    if (navLightBefore == null)
                        navLightBefore = wic.isAppearanceLightNavigationBars();
                    wic.setAppearanceLightNavigationBars(true);
                } else if (navLightBefore != null) {
                    wic.setAppearanceLightNavigationBars(navLightBefore);
                    navLightBefore = null;
                }
            }
            setMotorolaKeyboardResize(open);
            if (open) {
                refreshRecentHeader();
                onDrawerAdVisible();
            }
            if (!open) clearDrawerReturn();
            if (!open && searchMode) exitSearch();
            updateBackEnabled();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpSearch(@NonNull View view) {
        searchInput = view.findViewById(R.id.search_input);
        searchIcon = view.findViewById(R.id.search_icon);
        searchClear = view.findViewById(R.id.search_clear);
        pixelSearch = DeviceLayout.isPixel(requireContext());
        if (pixelSearch) styleAsPixelSearch(view);
        samsungSearch = DeviceLayout.isSamsung();
        if (samsungSearch) styleAsSamsungSearch(view);
        motorolaSearch = DeviceLayout.isMotorola() && !noDrawer();
        if (motorolaSearch) styleAsMotorolaSearch(view);
        matchAdToSearchBar(view);
        recentLayer = view.findViewById(R.id.recent_layer);
        recentList = view.findViewById(R.id.recent_list);
        recentEmpty = view.findViewById(R.id.recent_empty);
        recentTitle = view.findViewById(R.id.recent_title);
        searchEmpty = view.findViewById(R.id.search_empty);
        if (samsungSearch) {
            android.graphics.drawable.GradientDrawable card = new android.graphics.drawable.GradientDrawable();
            card.setColor(0x994A4C4C);
            card.setCornerRadius(20 * getResources().getDisplayMetrics().density);
            view.findViewById(R.id.recent_card).setBackground(card);
        }
        if (pixelSearch) {
            recentTitle.setTextColor(0xFF313131);
            searchEmpty.setTextColor(0xFF313131);
            android.graphics.drawable.GradientDrawable card = new android.graphics.drawable.GradientDrawable();
            card.setColor(0x99FFFFFF);
            card.setCornerRadius(20 * getResources().getDisplayMetrics().density);
            view.findViewById(R.id.recent_card).setBackground(card);
            recentEmpty.setTextColor(0xFF313131);
        }

        recentAdapter = new DrawerAppsAdapter(this::onAppClicked, this::onDrawerAppLongClicked);
        recentList.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), COLUMNS));
        recentList.setAdapter(recentAdapter);
        recentList.setItemAnimator(null);
        recentList.setNestedScrollingEnabled(false);

        searchInput.setOnTouchListener((v, ev) -> {
            if (ev.getActionMasked() == android.view.MotionEvent.ACTION_UP) enterSearch();
            return false;
        });
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() > 0 && !searchMode) enterSearch();
                applyDrawerState();
            }
        });
        searchInput.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        searchClear.setOnClickListener(v -> {
            if (samsungSearch) {
                exitSearch();
                return;
            }
            searchInput.setText("");
            searchInput.requestFocus();
        });
        searchIcon.setOnClickListener(v -> {
            if (searchMode) exitSearch();
            else enterSearch();
        });
        recentLayer.setOnClickListener(v -> exitSearch());
    }

    private void styleAsMotorolaSearch(@NonNull View view) {
        float d = getResources().getDisplayMetrics().density;
        View bar = view.findViewById(R.id.search_bar);
        ViewGroup parent = (ViewGroup) bar.getParent();
        parent.removeView(bar);
        parent.addView(bar);
        View ad = parent.findViewById(R.id.drawer_ad);
        if (ad != null) {
            parent.removeView(ad);
            parent.addView(ad);
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bar.getLayoutParams();
        lp.topMargin = Math.round(8 * d);
        lp.bottomMargin = Math.round(12 * d);
        bar.setLayoutParams(lp);
        bar.setBackgroundResource(R.drawable.bg_search_bar);
        searchIcon.setColorFilter(0xFF313131);
        searchInput.setHint(R.string.search_hint);
        searchInput.setHintTextColor(0xFF313131);
        searchInput.setTextColor(0xFF313131);
        searchClear.setColorFilter(0xFF313131);
        applyMotorolaSearchGravity();
    }

    private void matchAdToSearchBar(@NonNull View view) {
        // The native ad paints the search bar color itself; a second translucent layer behind it would not match the bar.
        view.findViewById(R.id.drawer_ad).setBackgroundColor(drawerAdView instanceof NativeAdCustomView ? android.graphics.Color.TRANSPARENT : searchBarColor());
    }

    private void applyMotorolaSearchGravity() {
        if (!motorolaSearch || searchInput == null) return;
        float d = getResources().getDisplayMetrics().density;
        if (searchMode) {
            searchInput.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
            searchInput.setPaddingRelative(searchInput.getPaddingStart(), searchInput.getPaddingTop(), 0, searchInput.getPaddingBottom());
        } else {
            searchInput.setGravity(android.view.Gravity.CENTER);
            searchInput.setPaddingRelative(searchInput.getPaddingStart(), searchInput.getPaddingTop(), Math.round(40 * d), searchInput.getPaddingBottom());
        }
    }

    private void styleAsSamsungSearch(@NonNull View view) {
        float d = getResources().getDisplayMetrics().density;
        searchIcon.setVisibility(View.GONE);
        searchInput.setHint(R.string.search_hint);
        searchInput.setHintTextColor(0xFFBDBDBD);
        searchInput.setPaddingRelative((int) (20 * d), searchInput.getPaddingTop(), searchInput.getPaddingEnd(), searchInput.getPaddingBottom());
        searchClear.setColorFilter(0xFFBDBDBD);
        searchMenu = view.findViewById(R.id.search_menu);
        searchMenu.setVisibility(View.VISIBLE);
        searchMenu.setOnClickListener(this::showDrawerLayoutPopup);
    }

    private static final String PREFS_DRAWER = "samsung_drawer";
    private static final String KEY_GRID = "grid";
    private ViewPager2 drawerPager;
    private DrawerGridPagerAdapter gridAdapter;

    private void setUpGridPager() {
        if (drawerPager != null) return;
        drawerPager = drawerView.findViewById(R.id.drawer_pager);
        drawerIndicator = drawerView.findViewById(R.id.drawer_indicator);
        gridAdapter = new DrawerGridPagerAdapter(this::onAppClicked, this::onDrawerAppLongClicked);
        drawerPager.setAdapter(gridAdapter);
        drawerPager.setOffscreenPageLimit(1);
        drawerPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
                if (drawerIndicator != null) drawerIndicator.setPosition(position + offset);
            }
        });
        drawerPager.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int h = b - t;
            if (h > 0 && gridAdapter != null)
                gridAdapter.setCellHeight(h / DrawerGridPagerAdapter.ROWS);
        });
        applyGridInsets();
    }

    private void applyGridInsets() {
        if (drawerPager == null || drawerIndicator == null) return;
        float d = getResources().getDisplayMetrics().density;
        ViewGroup.MarginLayoutParams pl = (ViewGroup.MarginLayoutParams) drawerPager.getLayoutParams();
        int pagerBottom = navInsetBottom + Math.round(36 * d);
        if (pl.bottomMargin != pagerBottom) {
            pl.bottomMargin = pagerBottom;
            drawerPager.setLayoutParams(pl);
        }
        ViewGroup.MarginLayoutParams il = (ViewGroup.MarginLayoutParams) drawerIndicator.getLayoutParams();
        if (il.bottomMargin != navInsetBottom) {
            il.bottomMargin = navInsetBottom;
            drawerIndicator.setLayoutParams(il);
        }
    }

    private void updateDrawerCellSize() {
        if (drawerAdapter == null || drawerList == null) return;
        if (motorolaSearch && imeBottom > 0) return;
        float d = getResources().getDisplayMetrics().density;
        int visible = drawerList.getHeight() - drawerList.getPaddingBottom();
        int minCell = DeviceLayout.iconPx(requireContext()) + Math.round(30 * d);
        int cellH = Math.max(minCell, Math.round(visible / (float) ROWS * 0.88f));
        drawerAdapter.setCellHeight(cellH);
        if (recentHeader != null) recentHeader.setCellHeight(cellH);
    }

    private void showDrawerContent(@NonNull List<AppInfo> shown, boolean listArea, boolean resetPage) {
        boolean empty = shown.isEmpty();
        if (!samsungGrid) {
            drawerList.setVisibility(listArea && !empty ? View.VISIBLE : View.INVISIBLE);
            return;
        }
        drawerList.setVisibility(View.GONE);
        boolean showPager = listArea && !empty;
        if (showPager) {
            gridAdapter.submit(shown);
            if (resetPage) drawerPager.setCurrentItem(0, false);
            drawerIndicator.setCount(DrawerGridPagerAdapter.pagesFor(shown.size()));
            drawerIndicator.setPosition(drawerPager.getCurrentItem());
        }
        drawerPager.setVisibility(showPager ? View.VISIBLE : View.INVISIBLE);
        drawerIndicator.setVisibility(showPager ? View.VISIBLE : View.GONE);
    }

    private void setDrawerGrid(boolean grid) {
        if (!DeviceLayout.isSamsung() || noDrawer() || samsungGrid == grid) return;
        samsungGrid = grid;
        requireContext().getSharedPreferences(PREFS_DRAWER, Context.MODE_PRIVATE).edit().putBoolean(KEY_GRID, grid).apply();
        if (grid) setUpGridPager();
        else if (drawerPager != null) {
            drawerPager.setVisibility(View.GONE);
            drawerIndicator.setVisibility(View.GONE);
        }
        lastQuery = null;
        scrollTopPending = true;
        applyDrawerState();
    }

    private void showDrawerLayoutPopup(@NonNull View anchor) {
        @SuppressLint("InflateParams") View content = LayoutInflater.from(requireContext()).inflate(R.layout.popup_drawer_layout, null);
        final android.widget.PopupWindow popup = new android.widget.PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(12 * getResources().getDisplayMetrics().density);
        bindLayoutRow(content, R.id.txt_list, R.id.chk_list, !samsungGrid);
        bindLayoutRow(content, R.id.txt_grid, R.id.chk_grid, samsungGrid);
        content.findViewById(R.id.row_list).setOnClickListener(v -> {
            popup.dismiss();
            setDrawerGrid(false);
        });
        content.findViewById(R.id.row_grid).setOnClickListener(v -> {
            popup.dismiss();
            setDrawerGrid(true);
        });
        content.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        View bar = anchor.getParent() instanceof View ? (View) anchor.getParent() : anchor;
        float d = getResources().getDisplayMetrics().density;
        int[] loc = new int[2];
        bar.getLocationOnScreen(loc);
        int w = Math.min(content.getMeasuredWidth(), bar.getWidth());
        popup.setWidth(w);
        int x = Math.max(0, loc[0] + bar.getWidth() - w);
        int y = loc[1] + bar.getHeight() + Math.round(8 * d);
        popup.showAtLocation(bar.getRootView(), android.view.Gravity.TOP | android.view.Gravity.START, x, y);
    }

    private void bindLayoutRow(@NonNull View content, int textId, int checkId, boolean selected) {
        TextView t = content.findViewById(textId);
        t.setTextColor(selected ? 0xFF3E6FDB : 0xFF1F1F1F);
        t.setTypeface(t.getTypeface(), selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        content.findViewById(checkId).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    private void styleAsPixelSearch(@NonNull View view) {
        float d = getResources().getDisplayMetrics().density;
        View bar = view.findViewById(R.id.search_bar);
        bar.setBackgroundResource(R.drawable.bg_search_bar);
        bar.setPadding((int) (2 * d), 0, (int) (6 * d), 0);
        searchIcon.setImageResource(R.drawable.ic_google_g);
        searchIcon.setContentDescription(null);
        searchInput.setHint(null);
        searchInput.setTextColor(0xFF313131);
        searchInput.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17);
        searchClear.setColorFilter(0xFF313131);
        view.findViewById(R.id.search_mic).setVisibility(View.VISIBLE);
        view.findViewById(R.id.search_mic).setOnClickListener(x -> openVoiceSearch());
    }

    private List<AppInfo> suggestedFor(List<AppInfo> installed) {
        if (suggestedCache == null || suggestedFor != installed) {
            suggestedCache = SuggestedApps.resolve(requireContext(), installed);
            suggestedFor = installed;
        }
        return suggestedCache;
    }

    private void enterSearch() {
        if (searchMode || searchInput == null) return;
        searchMode = true;
        if (!pixelSearch) searchIcon.setImageResource(R.drawable.ic_drawer_back);
        applyMotorolaSearchGravity();
        searchInput.requestFocus();
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && !restoringDrawer)
            imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        applyDrawerState();
        refreshSearchGestures();
    }

    private void exitSearch() {
        if (searchInput == null) return;
        searchMode = false;
        if (!pixelSearch) searchIcon.setImageResource(R.drawable.ic_drawer_search);
        applyMotorolaSearchGravity();
        searchInput.setText("");
        searchInput.clearFocus();
        hideKeyboard();
        applyDrawerState();
        refreshSearchGestures();
    }

    private void hideKeyboard() {
        if (searchInput == null) return;
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private final LauncherRootLayout.SearchSwipeHandler searchSwipeHandler = new LauncherRootLayout.SearchSwipeHandler() {
        @Override
        public boolean isContentAtTop() {
            if (searchInput == null || normalize(searchInput.getText()).isEmpty()) return true;
            return drawerList == null || !drawerList.canScrollVertically(-1);
        }

        @Override
        public boolean consumeSwipeForKeyboard() {
            if (searchInput == null) return false;
            WindowInsetsCompat wi = ViewCompat.getRootWindowInsets(searchInput);
            if (wi == null || !wi.isVisible(WindowInsetsCompat.Type.ime())) return false;
            hideKeyboard();
            return true;
        }
    };

    private void refreshSearchGestures() {
        if (rootLayout != null) {
            rootLayout.setDrawerGesturesBlocked(overlays != null && overlays.isAnyOpen());
            rootLayout.setSearchSwipeHandler(searchMode ? searchSwipeHandler : null);
        }
        updateBackEnabled();
    }

    private static String normalize(CharSequence q) {
        return q == null ? "" : q.toString().trim().toLowerCase(java.util.Locale.getDefault());
    }

    private boolean noDrawer() {
        return DeviceLayout.mode(requireContext()) == DeviceLayout.Mode.ALL_APPS_ON_HOME;
    }

    private void refreshRecentHeader() {
        if (recentHeader == null || searchInput == null || apps == null) return;
        boolean plain = !searchMode && normalize(searchInput.getText()).isEmpty();
        boolean brand = DeviceLayout.isMotorola() || DeviceLayout.isPixel(requireContext());
        recentHeader.submit(plain && brand && !noDrawer() ? RecentApps.load(requireContext(), apps) : Collections.emptyList());
    }

    private void applyDrawerState() {
        if (drawerAdapter == null || searchInput == null || noDrawer()) return;
        refreshRecentHeader();
        String q = searchMode ? normalize(searchInput.getText()) : "";
        searchClear.setVisibility(samsungSearch ? (searchMode ? View.VISIBLE : View.GONE) : searchInput.getText().length() > 0 ? View.VISIBLE : View.GONE);
        if (samsungSearch && searchMenu != null)
            searchMenu.setVisibility(searchMode ? View.GONE : View.VISIBLE);
        String prevQuery = lastQuery;
        lastQuery = q;
        if (!searchMode || !q.isEmpty()) {
            List<AppInfo> shown = apps;
            if (!q.isEmpty()) {
                shown = new java.util.ArrayList<>();
                for (AppInfo a : apps) {
                    if (a.label.toLowerCase(java.util.Locale.getDefault()).contains(q))
                        shown.add(a);
                }
            }
            if (!q.equals(prevQuery)) scrollTopPending = true;
            drawerAdapter.submitList(shown, () -> {
                if (drawerList == null) return;
                if (scrollTopPending) {
                    scrollTopPending = false;
                    drawerList.stopScroll();
                    ((androidx.recyclerview.widget.GridLayoutManager) Objects.requireNonNull(drawerList.getLayoutManager())).scrollToPositionWithOffset(0, 0);
                }
                applyPendingDrawerScroll();
            });
            recentLayer.setVisibility(View.GONE);
            showDrawerContent(shown, true, !q.equals(prevQuery));
            searchEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            List<AppInfo> shown = RecentApps.load(requireContext(), apps);
            boolean suggestedMode = shown.isEmpty();
            if (suggestedMode) shown = suggestedFor(apps);
            shown = firstDistinct(shown, suggestedMode ? 8 : 4);
            recentTitle.setText(suggestedMode ? R.string.suggested_apps_title : R.string.recent_apps_title);
            recentAdapter.submitList(shown);
            recentEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
            recentList.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);
            showDrawerContent(Collections.emptyList(), false, false);
            searchEmpty.setVisibility(View.GONE);
            recentLayer.setVisibility(View.VISIBLE);
        }
    }

    private static List<AppInfo> firstDistinct(List<AppInfo> src, int max) {
        List<AppInfo> out = new java.util.ArrayList<>(Math.min(max, src.size()));
        java.util.Set<ComponentName> seen = new java.util.HashSet<>();
        for (AppInfo a : src) {
            if (out.size() >= max) break;
            if (seen.add(a.component)) out.add(a);
        }
        return out;
    }

    private void bindDock() {
        if (dockViews == null || homeLayout == null) return;
        HomeItem[] items = homeLayout.dockItems();
        for (int i = 0; i < dockViews.length; i++) {
            final ImageView v = dockViews[i];
            final HomeItem item = i < items.length ? items[i] : null;
            v.animate().cancel();
            v.setAlpha(1f);
            v.setScaleX(1f);
            v.setScaleY(1f);
            v.setForeground(null);
            v.setVisibility(View.VISIBLE);
            if (item == null) {
                v.setImageDrawable(null);
                v.setContentDescription(null);
                v.setOnClickListener(null);
                v.setOnLongClickListener(null);
                v.setClickable(false);
                v.setLongClickable(false);
                continue;
            }
            v.setImageBitmap(item.icon);
            v.setContentDescription(item.label);
            v.setOnClickListener(x -> onItemClicked(x, item));
            v.setOnLongClickListener(x -> {
                onItemLongClicked(x, item);
                return true;
            });
        }
    }

    private android.graphics.drawable.GradientDrawable dockRing;

    private int dockCellAt(float rx, float ry) {
        if (dockViews == null || dockViews.length == 0 || !(dockViews[0].getParent() instanceof View))
            return -1;
        View row = (View) dockViews[0].getParent();
        if (row.getHeight() <= 0 || row.getVisibility() != View.VISIBLE) return -1;
        float slack = 8 * getResources().getDisplayMetrics().density;
        int[] l = new int[2];
        row.getLocationOnScreen(l);
        if (ry < l[1] - slack || ry > l[1] + row.getHeight() + slack || rx < l[0] || rx > l[0] + row.getWidth())
            return -1;
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < dockViews.length; i++) {
            dockViews[i].getLocationOnScreen(l);
            float d = Math.abs(rx - (l[0] + dockViews[i].getWidth() / 2f));
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private boolean dockNearCenter(int cell, float rx, float ry) {
        if (dockViews == null || cell < 0 || cell >= dockViews.length) return false;
        ImageView v = dockViews[cell];
        int[] l = new int[2];
        v.getLocationOnScreen(l);
        return Math.hypot(rx - (l[0] + v.getWidth() / 2f), ry - (l[1] + v.getHeight() / 2f)) < v.getWidth() * 0.36f;
    }

    private void setDockDragState(@Nullable String hiddenKey, int hoverCell, boolean merge) {
        if (dockViews == null || homeLayout == null) return;
        if (dockRing == null) {
            dockRing = new android.graphics.drawable.GradientDrawable();
            dockRing.setColor(0x40FFFFFF);
            dockRing.setCornerRadius(18 * getResources().getDisplayMetrics().density);
        }
        for (int i = 0; i < dockViews.length; i++) {
            ImageView v = dockViews[i];
            HomeItem it = homeLayout.dockItem(i);
            v.setAlpha(it != null && it.key.equals(hiddenKey) ? 0f : 1f);
            boolean hover = i == hoverCell;
            v.setForeground(hover && !merge ? dockRing : null);
            float scale = hover && merge ? 1.18f : 1f;
            v.setScaleX(scale);
            v.setScaleY(scale);
        }
    }

    private void updatePages() {
        updatePages(false);
    }

    private void applyDockIconSize(int iconPx) {
        if (dockViews == null) return;
        float dd = getResources().getDisplayMetrics().density;
        int pad = Math.max(0, Math.round((72 * dd - iconPx) / 2f));
        for (ImageView dv : dockViews) {
            if (dv.getPaddingLeft() != pad) dv.setPadding(pad, pad, pad, pad);
        }
    }

    private void refreshGreetingPage() {
        if (adapter == null || apps == null || homeLayout == null || !isAdded()) return;
        if (gameSelection == null) pickGames();
        adapter.setGreetingData(RecentApps.load(requireContext(), apps), homeLayout.blockBlastItem(), firstDistinct(suggestedFor(apps), 8), gameSelection, this::onAppClicked);
    }

    private List<AppInfo> gameSelection;
    private boolean gamesPickedForVisit;

    private void pickGames() {
        List<AppInfo> all = new java.util.ArrayList<>(homeLayout.quizGames());
        Collections.shuffle(all);
        gameSelection = new java.util.ArrayList<>(all.subList(0, Math.min(8, all.size())));
    }

    private boolean leavingLastPage;
    private int selectedPage = -1;
    private boolean lastChromeHidden;
    private int chromeDelta;

    private void setLastPageChromeHidden(boolean hide) {
        View root = getView();
        if (root == null || indicator == null || hide == lastChromeHidden) return;
        View dock = root.findViewById(R.id.dock);
        if (hide) chromeDelta = indicator.getHeight() + dock.getHeight();
        lastChromeHidden = hide;
        int vis = hide ? View.GONE : View.VISIBLE;
        indicator.setVisibility(vis);
        dock.setVisibility(vis);
    }

    private final Runnable loadLastPageAd = () -> {
        if (!isAdded() || adapter == null || pager == null || pager.getCurrentItem() != adapter.getItemCount() - 1)
            return;
        View page = adapter.greetingScroller();
        if (page instanceof GreetingPageView && page.isAttachedToWindow())
            ((GreetingPageView) page).loadAd(requireActivity());
    };

    private void onEnterLastPage() {
        if (pager != null) {
            pager.removeCallbacks(loadLastPageAd);
            pager.postDelayed(loadLastPageAd, 200);
        }
        if (gamesPickedForVisit || homeLayout == null) return;
        gamesPickedForVisit = true;
        int savedScroll = pendingLastPageScroll();
        if (savedScroll >= 0) {
            if (adapter != null && adapter.greetingScroller() instanceof GreetingPageView) {
                ((GreetingPageView) adapter.greetingScroller()).restoreScroll(savedScroll);
                refreshGreetingPage();
            } else {
                gamesPickedForVisit = false;
            }
            return;
        }
        if (adapter != null && adapter.greetingScroller() instanceof GreetingPageView) {
            ((GreetingPageView) Objects.requireNonNull(adapter.greetingScroller())).scrollToTopNow();
        }
        pickGames();
        refreshGreetingPage();
    }

    private void updatePages(boolean animate) {
        if (pager == null || adapter == null) return;
        int w = pager.getWidth();
        int h = pager.getHeight();
        if (w <= 0 || h <= 0 || apps == null) return;

        int columns = COLUMNS;
        int rows = ROWS;
        int page = adapter.appPageCount() > 0 ? pager.getCurrentItem() : viewModel.getCurrentPage();
        int cellH = Math.max(1, h / rows);
        int homeRows = 3;
        homeLayout.setPage0Capacity(homeRows * columns);
        int gridIcon = AppGridPageView.gridIconPx(requireContext(), cellH);
        applyDockIconSize(gridIcon);
        if (DeviceLayout.setHomeIconPx(gridIcon)) {
            if (drawerAdapter != null)
                drawerAdapter.notifyItemRangeChanged(0, drawerAdapter.getItemCount());
            if (recentHeader != null) recentHeader.rebind();
        }

        if (appsDirty || !built) homeLayout.build(apps);
        if (!apps.isEmpty()) built = true;
        appsDirty = false;
        if (dragController == null || !dragController.isDragging())
            page = homeLayout.compactPages(page, adapter.appPageCount());
        int countBefore = adapter.getItemCount();
        adapter.submit(homeLayout.pages(), columns, rows, homeRows, cellH, animate);
        indicator.setCount(adapter.getItemCount());
        bindDock();
        refreshGreetingPage();

        int target = Math.min(page, adapter.getItemCount() - 1);
        if (adapter.getItemCount() != countBefore && pager.getChildAt(0) instanceof androidx.recyclerview.widget.RecyclerView) {
            ((androidx.recyclerview.widget.RecyclerView) pager.getChildAt(0)).scrollToPosition(target);
        }
        if (pager.getCurrentItem() != target) pager.setCurrentItem(target, false);
        viewModel.setCurrentPage(target);
        indicator.setPosition(target);
        setLastPageChromeHidden(target == adapter.getItemCount() - 1);
        if (target == adapter.getItemCount() - 1 && pendingLastPageScroll() >= 0) {
            androidx.core.view.OneShotPreDrawListener.add(pager, () -> {
                if (pager != null && adapter != null && pager.getCurrentItem() == adapter.getItemCount() - 1)
                    onEnterLastPage();
            });
        }
        updateBackEnabled();
        if (overlays != null) overlays.onLayoutRebuilt();
    }

    public void goToHomePage(boolean returningFromApp) {
        if (pendingReturnPage() >= 0) {
            restorePendingPage();
            handler.removeCallbacks(clearPendingRunnable);
            handler.postDelayed(clearPendingRunnable, PENDING_GRACE_MS);
            return;
        }
        if (drawerReturnPending()) {
            restoreDrawerIfPending();
            return;
        }
        if (returningFromApp && rootLayout != null && rootLayout.isOpen()) return;
        if (rootLayout != null && rootLayout.isOpen()) rootLayout.closeDrawer(true);
        if (pager != null && pager.getCurrentItem() != 0) pager.setCurrentItem(0, true);
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(clearPendingRunnable);
        if (homeLayout != null) homeLayout.flush();
        if (dragController != null) dragController.cancel();
        if (overlays != null) overlays.closeAll();
    }

    @Override
    public void onStart() {
        super.onStart();
        restorePendingPage();
        restoreDrawerIfPending();
        handler.postDelayed(clearPendingRunnable, PENDING_GRACE_MS);
        gamesPickedForVisit = false;
        if (adapter != null && pager != null && pager.getCurrentItem() == adapter.getItemCount() - 1)
            onEnterLastPage();
        else refreshGreetingPage();
        viewModel.refreshDock();
    }

    private void registerPackageReceiver() {
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addDataScheme("package");
            ContextCompat.registerReceiver(requireContext(), packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            receiverRegistered = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiverRegistered) {
            requireContext().unregisterReceiver(packageReceiver);
            receiverRegistered = false;
        }
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dragController != null) dragController.cancel();
        if (rootLayout != null) rootLayout.setGestureHandler(null);
        pager.setAdapter(null);
        pager = null;
        indicator = null;
        rootLayout = null;
        homeContent = null;
        drawerView = null;
        drawerAdView = null;
        drawerAdEnabled = false;
        drawerAdShown = false;
        drawerRefresh = null;
        drawerList = null;
        drawerAdapter = null;
        recentAdapter = null;
        recentHeader = null;
        bottomSearchRow = null;
        searchInput = null;
        searchIcon = null;
        searchClear = null;
        searchMenu = null;
        drawerIndicator = null;
        drawerPager = null;
        gridAdapter = null;
        recentLayer = null;
        recentList = null;
        recentEmpty = null;
        recentTitle = null;
        suggestedCache = null;
        suggestedFor = null;
        searchEmpty = null;
        searchMode = false;
        dockViews = null;
        leavingLastPage = false;
        selectedPage = -1;
        lastChromeHidden = false;
        chromeDelta = 0;
        adapter = null;
        overlays = null;
        dragController = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, viewModel.getCurrentPage());
    }

    private void openGameLink(@NonNull QuizGames.QuizGameItem game) {
        try {
            rememberLaunchSource();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(game.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException e) {
            Toast.makeText(requireContext(), R.string.game_link_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void onAppClicked(@NonNull AppInfo app) {
        QuizGames.QuizGameItem game = QuizGames.forComponent(app.component);
        if (game != null) {
            openGameLink(game);
            return;
        }
        if (HomeLayout.isBlockBlast(app)) {
            if (blockToast != null) blockToast.cancel();
            blockToast = Toast.makeText(requireContext(), R.string.play_game, Toast.LENGTH_SHORT);
            blockToast.show();
            return;
        }
        RecentApps.record(requireContext(), app.component);
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            rememberLaunchSource();
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(requireContext(), R.string.app_launch_failed, Toast.LENGTH_SHORT).show();
            viewModel.refresh();
        }
    }

    private void setUpHomeEditing() {
        overlays = new HomeOverlays(rootLayout, homeLayout, new HomeOverlays.Callbacks() {
            @Override
            public void onAppClick(@NonNull AppInfo app) {
                onAppClicked(app);
            }

            @Override
            public void onAppInfo(@NonNull AppInfo app) {
                openAppInfo(app.component);
            }

            @Override
            public void onAppUninstall(@NonNull AppInfo app) {
                uninstallApp(app.component);
            }

            @Override
            public boolean isDedicated(@NonNull AppInfo app) {
                return LauncherHomeFragment.isDedicated(app.component);
            }

            @Override
            public boolean isSystemApp(@NonNull AppInfo app) {
                return LauncherHomeFragment.this.isSystemApp(app.component);
            }

            @Override
            public boolean canRemoveFromFolder(@NonNull String folderId) {
                return !noDrawer() && !HomeLayout.MORE_GAMES_FOLDER_ID.equals(folderId);
            }

            @Override
            public void onFolderChanged() {
                updatePages(false);
            }

            @Override
            public void onFolderDragExit(@NonNull String folderId, @NonNull AppInfo app) {
                dragController.armFromFolder(app, folderId);
                overlays.closeFolder(true);
            }

            @Override
            public void onOverlayStateChanged() {
                if (rootLayout != null && overlays != null) {
                    rootLayout.setDrawerGesturesBlocked(overlays.isAnyOpen() || searchMode);
                }
                updateBackEnabled();
            }
        });
        dragController = new HomeDragController(rootLayout, pager, adapter, homeLayout, new HomeDragController.Host() {
            @Override
            public void onLayoutChanged(boolean animate) {
                updatePages(animate);
            }

            @Override
            public int dockCellAt(float rawX, float rawY) {
                return LauncherHomeFragment.this.dockCellAt(rawX, rawY);
            }

            @Override
            public boolean dockNearCenter(int cell, float rawX, float rawY) {
                return LauncherHomeFragment.this.dockNearCenter(cell, rawX, rawY);
            }

            @Override
            public void setDockDragState(@Nullable String hiddenKey, int hoverCell, boolean merge) {
                LauncherHomeFragment.this.setDockDragState(hiddenKey, hoverCell, merge);
            }

            @Override
            public void dismissMenu() {
                overlays.dismissMenu();
            }

            @Override
            public void onDrawerDragStarted() {
                if (rootLayout == null) return;
                rootLayout.setPreserveDrawerScroll(true);
                rootLayout.closeDrawer(true);
            }

            @Override
            public void onDrawerDragEnded(boolean placed) {
                if (rootLayout == null) return;
                if (!placed) rootLayout.openDrawer(true);
                rootLayout.setPreserveDrawerScroll(false);
            }
        });
        rootLayout.setGestureHandler(dragController);
    }

    @Override
    public void onItemClicked(@NonNull View cell, @NonNull HomeItem item) {
        if (item.app != null) onAppClicked(item.app);
        else if (item.folderId != null) overlays.openFolder(item.folderId, cell);
    }

    @Override
    public void onItemLongClicked(@NonNull View cell, @NonNull HomeItem item) {
        cell.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        dragController.arm(item);
        final AppInfo app = item.app;
        if (app != null) {
            if (isDedicated(app.component)) {
            } else {
                final boolean system = isSystemApp(app.component);
                final boolean canRemove = !noDrawer();
                overlays.setMenuApp(app.component);
                overlays.showMenu(cell, getString(R.string.menu_app_info), () -> openAppInfo(app.component), system ? null : getString(R.string.menu_uninstall), system ? null : () -> uninstallApp(app.component), canRemove ? getString(R.string.menu_remove_from_home) : null, canRemove ? () -> removeFromHome(app) : null);
            }
        } else if (!HomeLayout.MORE_GAMES_FOLDER_ID.equals(item.folderId)) {
            final String folderId = item.folderId;
            overlays.showMenu(cell, getString(R.string.menu_rename), () -> overlays.openFolderForRename(Objects.requireNonNull(folderId), cell), getString(R.string.menu_ungroup), () -> {
                homeLayout.removeFolder(folderId);
                updatePages(true);
            });
        }
    }

    private void onDrawerAppLongClicked(@NonNull View cell, @NonNull AppInfo app) {
        cell.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        dragController.armFromDrawer(app, cell);
        if (isDedicated(app.component)) return;
        final boolean system = isSystemApp(app.component);
        final boolean addable = homeLayout.canAddToHome(app);
        overlays.setMenuApp(app.component);
        overlays.showMenu(cell, getString(R.string.menu_app_info), () -> openAppInfo(app.component), system ? null : getString(R.string.menu_uninstall), system ? null : () -> uninstallApp(app.component), addable ? getString(R.string.menu_add_to_home) : null, addable ? () -> addToHome(app) : null);
    }

    private void addToHome(@NonNull AppInfo app) {
        if (homeLayout.addToHome(app)) {
            updatePages(true);
            Toast.makeText(requireContext(), R.string.toast_added_home, Toast.LENGTH_SHORT).show();
        }
    }

    static boolean isDedicated(@NonNull ComponentName c) {
        return HomeLayout.BLOCK_BLAST_COMPONENT.equals(c) || QuizGames.forComponent(c) != null;
    }

    private boolean isSystemApp(@NonNull ComponentName component) {
        try {
            int f = requireContext().getPackageManager().getApplicationInfo(component.getPackageName(), 0).flags;
            return (f & (android.content.pm.ApplicationInfo.FLAG_SYSTEM | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void uninstallApp(@NonNull ComponentName component) {
        String pkg = component.getPackageName();
        try {
            requireContext().getPackageManager().getApplicationInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(requireContext(), R.string.toast_not_installed, Toast.LENGTH_SHORT).show();
            viewModel.refresh();
            return;
        }
        try {
            rememberPageForSystemScreen();
            startActivity(new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            clearPendingReturn();
            Toast.makeText(requireContext(), R.string.toast_uninstall_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void removeFromHome(@NonNull AppInfo app) {
        if (homeLayout.hide(app)) {
            updatePages(true);
            Toast.makeText(requireContext(), R.string.toast_removed_home, Toast.LENGTH_SHORT).show();
        }
    }

    private static final String DRAWER_RETURN_PREFS = "launcher_drawer_return";
    private static final long DRAWER_RETURN_WINDOW_MS = 30L * 60L * 1000L;
    private boolean restoringDrawer;
    private int pendingDrawerScrollPos = -1;
    private int pendingDrawerScrollOffset;

    private void rememberLaunchSource() {
        if (!isAdded()) return;
        android.content.SharedPreferences.Editor edit = requireContext().getSharedPreferences(DRAWER_RETURN_PREFS, Context.MODE_PRIVATE).edit().clear();
        boolean fromDrawer = rootLayout != null && rootLayout.isOpen() && !noDrawer();
        if (fromDrawer) {
            edit.putBoolean("from_drawer", true).putLong("at", System.currentTimeMillis());
            edit.putBoolean("search", searchMode).putString("query", searchMode && searchInput != null ? searchInput.getText().toString() : "");
            androidx.recyclerview.widget.RecyclerView.LayoutManager lm = drawerList == null ? null : drawerList.getLayoutManager();
            if (lm instanceof androidx.recyclerview.widget.LinearLayoutManager) {
                int pos = ((androidx.recyclerview.widget.LinearLayoutManager) lm).findFirstVisibleItemPosition();
                View first = pos >= 0 ? lm.findViewByPosition(pos) : null;
                edit.putInt("scroll_pos", pos).putInt("scroll_off", first == null ? 0 : first.getTop() - drawerList.getPaddingTop());
            }
            edit.putInt("grid_page", samsungGrid && drawerPager != null ? drawerPager.getCurrentItem() : -1);
        }
        edit.apply();
        if (fromDrawer) clearPendingReturn();
        else rememberPageForSystemScreen();
    }

    private boolean drawerReturnPending() {
        if (!isAdded() || noDrawer()) return false;
        android.content.SharedPreferences p = requireContext().getSharedPreferences(DRAWER_RETURN_PREFS, Context.MODE_PRIVATE);
        long at = p.getLong("at", 0L);
        return p.getBoolean("from_drawer", false) && at != 0L && Math.abs(System.currentTimeMillis() - at) <= DRAWER_RETURN_WINDOW_MS;
    }

    private void clearDrawerReturn() {
        if (isAdded())
            requireContext().getSharedPreferences(DRAWER_RETURN_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private void restoreDrawerIfPending() {
        if (rootLayout == null || !drawerReturnPending() || rootLayout.isOpen()) return;
        android.content.SharedPreferences p = requireContext().getSharedPreferences(DRAWER_RETURN_PREFS, Context.MODE_PRIVATE);
        restoringDrawer = true;
        try {
            if (p.getBoolean("search", false) && searchInput != null) {
                String query = p.getString("query", "");
                if (!query.isEmpty()) searchInput.setText(query);
                enterSearch();
            }
            pendingDrawerScrollPos = p.getInt("scroll_pos", -1);
            pendingDrawerScrollOffset = p.getInt("scroll_off", 0);
            int gridPage = p.getInt("grid_page", -1);
            if (gridPage >= 0 && samsungGrid && drawerPager != null && drawerPager.getAdapter() != null && gridPage < drawerPager.getAdapter().getItemCount()) {
                drawerPager.setCurrentItem(gridPage, false);
            }
            rootLayout.openDrawer(false);
            applyPendingDrawerScroll();
        } finally {
            restoringDrawer = false;
        }
    }

    private void applyPendingDrawerScroll() {
        if (pendingDrawerScrollPos < 0 || drawerList == null || drawerAdapter == null || drawerAdapter.getItemCount() == 0)
            return;
        androidx.recyclerview.widget.RecyclerView.LayoutManager lm = drawerList.getLayoutManager();
        if (lm instanceof androidx.recyclerview.widget.LinearLayoutManager) {
            ((androidx.recyclerview.widget.LinearLayoutManager) lm).scrollToPositionWithOffset(Math.min(pendingDrawerScrollPos, drawerList.getAdapter() == null ? 0 : Math.max(0, drawerList.getAdapter().getItemCount() - 1)), pendingDrawerScrollOffset);
        }
        pendingDrawerScrollPos = -1;
    }

    private static final String NAV_PREFS = "launcher_nav_state";
    private static final String NAV_PAGE = "return_page";
    private static final String NAV_AT = "return_at";
    private static final String NAV_SCROLL = "return_scroll";
    private static final int LAST_PAGE_MARKER = 10_000;
    private static final long RETURN_WINDOW_MS = 10L * 60L * 1000L;

    private void rememberPageForSystemScreen() {
        if (!isAdded()) return;
        boolean ready = pager != null && adapter != null && adapter.appPageCount() > 0;
        int page = ready ? pager.getCurrentItem() : viewModel.getCurrentPage();
        boolean last = ready && page == adapter.getItemCount() - 1;
        int scroll = 0;
        if (last && adapter.greetingScroller() instanceof GreetingPageView)
            scroll = adapter.greetingScroller().getScrollY();
        requireContext().getSharedPreferences(NAV_PREFS, Context.MODE_PRIVATE).edit().clear().putInt(NAV_PAGE, last ? LAST_PAGE_MARKER : page).putInt(NAV_SCROLL, scroll).putLong(NAV_AT, System.currentTimeMillis()).apply();
    }

    private int pendingLastPageScroll() {
        if (pendingReturnPage() < LAST_PAGE_MARKER) return -1;
        return requireContext().getSharedPreferences(NAV_PREFS, Context.MODE_PRIVATE).getInt(NAV_SCROLL, 0);
    }

    private int pendingReturnPage() {
        if (!isAdded()) return -1;
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences(NAV_PREFS, Context.MODE_PRIVATE);
        long at = prefs.getLong(NAV_AT, 0L);
        if (at == 0L || Math.abs(System.currentTimeMillis() - at) > RETURN_WINDOW_MS) return -1;
        return prefs.getInt(NAV_PAGE, -1);
    }

    private static final long PENDING_GRACE_MS = 1500L;
    private final Runnable clearPendingRunnable = () -> {
        clearPendingReturn();
        clearDrawerReturn();
    };

    private void restorePendingPage() {
        int pending = pendingReturnPage();
        if (pending <= 0 || pager == null || adapter == null || adapter.appPageCount() == 0) return;
        if (pager.getCurrentItem() != 0) return;
        int target = Math.min(pending, adapter.getItemCount() - 1);
        if (target > 0) {
            pager.setCurrentItem(target, false);
            viewModel.setCurrentPage(target);
        }
    }

    private void clearPendingReturn() {
        if (isAdded())
            requireContext().getSharedPreferences(NAV_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private void openAppInfo(@NonNull ComponentName component) {
        if (HomeLayout.BLOCK_BLAST_COMPONENT.equals(component) || QuizGames.forComponent(component) != null)
            return;
        String pkg = component.getPackageName();
        try {
            requireContext().getPackageManager().getApplicationInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(requireContext(), R.string.toast_not_installed, Toast.LENGTH_SHORT).show();
            viewModel.refresh();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            rememberPageForSystemScreen();
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            clearPendingReturn();
            Toast.makeText(requireContext(), R.string.toast_app_info_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void openVoiceSearch() {
        Intent[] tries = {new Intent(android.speech.RecognizerIntent.ACTION_WEB_SEARCH).setPackage(GOOGLE_APP), new Intent(android.speech.RecognizerIntent.ACTION_WEB_SEARCH), new Intent(Intent.ACTION_VOICE_COMMAND)};
        if (!startFirst(tries)) {
            Toast.makeText(requireContext(), R.string.app_launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean startFirst(@NonNull Intent[] intents) {
        for (Intent i : intents) {
            try {
                startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            } catch (ActivityNotFoundException | SecurityException ignored) {
            }
        }
        return false;
    }

    public void onVoiceSearchClicked() {
        openVoiceSearch();
    }

    public void onSearchClicked() {
        Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(GOOGLE_APP);
        try {
            if (intent == null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            }
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(requireContext(), R.string.app_launch_failed, Toast.LENGTH_SHORT).show();
        }
    }
}