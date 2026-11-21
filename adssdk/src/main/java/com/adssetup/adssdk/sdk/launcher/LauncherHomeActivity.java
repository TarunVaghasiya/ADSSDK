package com.adssetup.adssdk.sdk.launcher;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.R;

public class LauncherHomeActivity extends AppCompatActivity {
    private static final String TAG = "LauncherHomeActivity";
    private boolean visible;

    /**
     * Opens [fragmentClass] over the launcher home, sliding in from the left like a page swipe. The home fragment is
     * hidden rather than replaced, so it keeps its state and is not recreated when the user comes back. Calling this
     * again while the fragment is already open does nothing; Back returns to the previous screen.
     *
     * @return true if the fragment is now open, false if it could not be opened
     */
    public boolean openContainerFragment(@NonNull Class<? extends Fragment> fragmentClass) {
        return openContainerFragment(fragmentClass.getName());
    }

    /** Same as {@link #openContainerFragment(Class)} for a fully qualified class name; unknown or invalid names are ignored. */
    public boolean openContainerFragment(@Nullable String className) {
        if (className == null || className.isEmpty() || isFinishing() || isDestroyed()) return false;
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return false;
        Fragment existing = fm.findFragmentByTag(className);
        if (existing != null && existing.isAdded() && !existing.isHidden()) return true;
        Fragment fragment;
        try {
            Class<?> cls = Class.forName(className, false, getClassLoader());
            if (!Fragment.class.isAssignableFrom(cls)) {
                android.util.Log.w(TAG, className + " is not a Fragment");
                return false;
            }
            fragment = fm.getFragmentFactory().instantiate(getClassLoader(), className);
        } catch (ClassNotFoundException | RuntimeException e) {
            android.util.Log.w(TAG, "Cannot open " + className, e);
            return false;
        }
        androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true).setCustomAnimations(R.anim.container_enter_from_left, R.anim.home_exit_to_right, R.anim.home_enter_from_right, R.anim.container_exit_to_left);
        for (Fragment visible : fm.getFragments()) {
            if (visible.getId() == R.id.fragment_container && !visible.isHidden()) tx.hide(visible);
        }
        tx.add(R.id.fragment_container, fragment, className).addToBackStack(className).commit();
        // Run it now so a second call before the next frame sees the fragment and does not stack a duplicate.
        fm.executePendingTransactions();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        visible = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        visible = false;
    }

    @Override
    protected void onNewIntent(@NonNull android.content.Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasCategory(android.content.Intent.CATEGORY_HOME)) {
            // Home button: close any screen opened over the launcher before returning to the first page.
            if (!getSupportFragmentManager().isStateSaved()) {
                getSupportFragmentManager().popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (f instanceof LauncherHomeFragment)
                ((LauncherHomeFragment) f).goToHomePage(!visible);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_home);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new LauncherHomeFragment()).commit();
        }
    }
}
