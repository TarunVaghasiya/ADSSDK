package com.adssetup.adssdk.sdk.launcher;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DefaultLauncherHelper {
    private DefaultLauncherHelper() {
    }

    public static boolean isDefaultLauncher(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = context.getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return rm.isRoleHeld(RoleManager.ROLE_HOME);
            }
        }
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = context.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && context.getPackageName().equals(ri.activityInfo.packageName);
    }

    @Nullable
    public static Intent createRoleRequestIntent(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = context.getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_HOME);
            }
        }
        return null;
    }

    @NonNull
    public static Intent[] fallbackSettingsIntents() {
        return new Intent[]{new Intent(Settings.ACTION_HOME_SETTINGS), new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS), new Intent(Settings.ACTION_SETTINGS)};
    }
}