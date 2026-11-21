package com.adssetup.adssdk;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.config.AdsConfig;
import com.adssetup.adssdk.config.AdsConfigManager;
import com.adssetup.adssdk.sdk.AppOpenAdPlacement;
import com.adssetup.adssdk.sdk.BannerAdCustomView;
import com.adssetup.adssdk.sdk.BannerAdPlacement;
import com.adssetup.adssdk.sdk.InterstitialAdPlacement;
import com.adssetup.adssdk.sdk.NativeAdCustomView;
import com.adssetup.adssdk.sdk.NativeAdPlacement;
import com.adssetup.adssdk.sdk.NativeFullAdPlacement;
import com.adssetup.adssdk.sdk.PIPAdPlacement;
import com.adssetup.adssdk.sdk.launcher.DefaultLauncherHelper;

public class MainActivity extends AppCompatActivity {
    private TextView tvSetLauncher;

    private final ActivityResultLauncher<Intent> homeRoleRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> onHomeRoleRequestFinished());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSetLauncher = findViewById(R.id.tvSetLauncher);
        tvSetLauncher.setOnClickListener(view -> requestDefaultLauncher());
        refreshLauncherButton();

        AdsConfig config = AdsConfigManager.get();
        AdsConfig.MainScreen main = config.getMainScreen();

        View rlAdView = findViewById(R.id.rlAdView);
        BannerAdCustomView bannerAdCustomView = findViewById(R.id.bannerAdCustomView);
        NativeAdCustomView nativeAdCustomView = findViewById(R.id.nativeAdCustomView);

        boolean showBanner = main.isMainAdShow() && AdsConfigManager.isType(main.getMainAdType(), AdsConfigManager.TYPE_BANNER);
        boolean showNative = main.isMainAdShow() && AdsConfigManager.isType(main.getMainAdType(), AdsConfigManager.TYPE_NATIVE);

        rlAdView.setVisibility(showBanner || showNative ? View.VISIBLE : View.GONE);
        bannerAdCustomView.setVisibility(showBanner ? View.VISIBLE : View.GONE);
        nativeAdCustomView.setVisibility(showNative ? View.VISIBLE : View.GONE);
        if (showBanner) {
            BannerAdPlacement.loadBannerAd(this, main.getMainBannerId(), bannerAdCustomView);
        } else if (showNative) {
            NativeAdPlacement.loadNativeAd(this, main.getMainNativeId(), nativeAdCustomView);
        }

        boolean buttonAdEnabled = main.isMainButtonAdShow();
        if (buttonAdEnabled && AdsConfigManager.isType(main.getMainButtonAdType(), AdsConfigManager.TYPE_INTERSTITIAL) && InterstitialAdPlacement.MODE_PRELOAD.equals(AdsConfigManager.loadMode(config.getInterstitialLoadType()))) {
            InterstitialAdPlacement.loadInterstitialAd(this, main.getMainInterstitialId(), InterstitialAdPlacement.MODE_PRELOAD, null);
        } else if (buttonAdEnabled && AdsConfigManager.isType(main.getMainButtonAdType(), AdsConfigManager.TYPE_APP_OPEN) && AppOpenAdPlacement.MODE_PRELOAD.equals(AdsConfigManager.loadMode(config.getAppOpenLoadType()))) {
            AppOpenAdPlacement.loadAppOpenAd(this, main.getMainAppOpenId(), AppOpenAdPlacement.MODE_PRELOAD, null);
        }
        findViewById(R.id.tvPreloadAd).setOnClickListener(view -> {
            if (buttonAdEnabled && AdsConfigManager.isType(main.getMainButtonAdType(), AdsConfigManager.TYPE_INTERSTITIAL)) {
                view.setEnabled(false);
                InterstitialAdPlacement.loadInterstitialAd(this, main.getMainInterstitialId(), AdsConfigManager.loadMode(config.getInterstitialLoadType()), shown -> {
                    view.setEnabled(true);
                    openSecond();
                });
            } else if (buttonAdEnabled && AdsConfigManager.isType(main.getMainButtonAdType(), AdsConfigManager.TYPE_APP_OPEN)) {
                view.setEnabled(false);
                AppOpenAdPlacement.loadAppOpenAd(this, main.getMainAppOpenId(), AdsConfigManager.loadMode(config.getAppOpenLoadType()), shown -> {
                    view.setEnabled(true);
                    openSecond();
                });
            } else {
                openSecond();
            }
        });

        findViewById(R.id.tvFullNative).setOnClickListener(view -> NativeFullAdPlacement.show(this, main.getMainNativeId()));

        if (main.isMainPipAdShow()) {
            PIPAdPlacement.loadPIPAd(this, main.getMainPipId());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLauncherButton();
    }

    private void refreshLauncherButton() {
        tvSetLauncher.setText(DefaultLauncherHelper.isDefaultLauncher(this) ? R.string.btn_change_launcher : R.string.btn_set_launcher);
    }

    private void requestDefaultLauncher() {
        Intent roleIntent = DefaultLauncherHelper.isDefaultLauncher(this) ? null : DefaultLauncherHelper.createRoleRequestIntent(this);
        if (roleIntent != null) {
            try {
                homeRoleRequest.launch(roleIntent);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
            }
        }
        for (Intent settings : DefaultLauncherHelper.fallbackSettingsIntents()) {
            try {
                startActivity(settings);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
            }
        }
        Toast.makeText(this, R.string.launcher_unavailable, Toast.LENGTH_SHORT).show();
    }

    private void onHomeRoleRequestFinished() {
        refreshLauncherButton();
        Toast.makeText(this, DefaultLauncherHelper.isDefaultLauncher(this) ? R.string.launcher_set_success : R.string.launcher_not_set, Toast.LENGTH_SHORT).show();
    }

    private void openSecond() {
        if (!isFinishing() && !isDestroyed()) {
            startActivity(new Intent(this, SecondActivity.class));
        }
    }
}