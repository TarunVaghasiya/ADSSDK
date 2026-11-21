package com.adssetup.adssdk.sdk.launcher;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.adssetup.adssdk.sdk.R;

public class LauncherHomeActivity extends AppCompatActivity {
    private boolean visible;

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