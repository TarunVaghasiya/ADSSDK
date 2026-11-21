package com.adssetup.adssdk.sdk.launcher;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Navigation contract between the launcher (this module) and the app. The app depends on the SDK, so the SDK cannot
 * reference app classes; instead the app registers the fragments the launcher may open, once, from its Application.
 *
 * <pre>
 * // app, MyApplication.onCreate()
 * LauncherNavigation.setMainContainerFragment(MainContainerFragment.class);
 *
 * // anywhere with the launcher activity
 * LauncherNavigation.open(activity, MainContainerFragment.class);
 * </pre>
 */
public final class LauncherNavigation {
    @Nullable
    private static Class<? extends Fragment> mainContainer;

    private LauncherNavigation() {
    }

    /** Registers the fragment opened by a right swipe on the first Home page. */
    public static void setMainContainerFragment(@Nullable Class<? extends Fragment> fragmentClass) {
        mainContainer = fragmentClass;
    }

    @Nullable
    public static Class<? extends Fragment> getMainContainerFragment() {
        return mainContainer;
    }

    /** Opens the registered main container fragment; false if none is registered or the launcher is not showing. */
    public static boolean openMainContainer(@NonNull Activity activity) {
        Class<? extends Fragment> fragmentClass = mainContainer;
        return fragmentClass != null && open(activity, fragmentClass);
    }

    /** Opens [fragmentClass] over the launcher home. The caller manages no transaction, container or back stack. */
    public static boolean open(@NonNull Activity activity, @NonNull Class<? extends Fragment> fragmentClass) {
        return activity instanceof LauncherHomeActivity && ((LauncherHomeActivity) activity).openContainerFragment(fragmentClass);
    }
}
