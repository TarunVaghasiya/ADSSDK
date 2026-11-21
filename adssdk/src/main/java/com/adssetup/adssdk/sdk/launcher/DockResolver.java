package com.adssetup.adssdk.sdk.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.telecom.TelecomManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

final class DockResolver {
    static final int SLOT_COUNT = 4;
    private static final String CHROME_PACKAGE = "com.android.chrome";

    private DockResolver() {
    }

    @WorkerThread
    @NonNull
    static List<DockItem> resolve(@NonNull Context context) {
        Context app = context.getApplicationContext();
        PackageManager pm = app.getPackageManager();
        List<DockItem> out = new ArrayList<>(SLOT_COUNT);

        Intent dial = new Intent(Intent.ACTION_DIAL);
        String dialer = null;
        TelecomManager tm = app.getSystemService(TelecomManager.class);
        if (tm != null) dialer = tm.getDefaultDialerPackage();
        out.add(build(app, pm, dial, dialer));

        Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
        out.add(build(app, pm, sms, Telephony.Sms.getDefaultSmsPackage(app)));

        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).addCategory(Intent.CATEGORY_BROWSABLE);
        DockItem chrome = build(app, pm, browser, CHROME_PACKAGE);
        if (chrome == null || !CHROME_PACKAGE.equals(chrome.intent.getComponent() != null ? chrome.intent.getComponent().getPackageName() : null)) {
            chrome = build(app, pm, browser, null);
        }
        out.add(chrome);
        out.add(build(app, pm, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), null));
        return out;
    }

    @Nullable
    private static DockItem build(Context ctx, PackageManager pm, Intent base, @Nullable String preferredPackage) {
        ResolveInfo ri = null;
        if (preferredPackage != null) {
            Intent scoped = new Intent(base).setPackage(preferredPackage);
            ri = pm.resolveActivity(scoped, 0);
        }
        if (ri == null || ri.activityInfo == null) {
            ri = pm.resolveActivity(base, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null && ri.activityInfo != null && "android".equals(ri.activityInfo.packageName)) {
                List<ResolveInfo> all = pm.queryIntentActivities(base, PackageManager.MATCH_DEFAULT_ONLY);
                ri = all.isEmpty() ? null : all.get(0);
            }
        }
        if (ri == null || ri.activityInfo == null || !ri.activityInfo.exported) return null;
        Intent launch = new Intent(base).setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        CharSequence label = ri.loadLabel(pm);
        AppRepository.initMask();
        int px = DeviceLayout.iconPx(ctx);
        return new DockItem(launch, label.toString(), AppRepository.toBitmap(ri.loadIcon(pm), px));
    }
}