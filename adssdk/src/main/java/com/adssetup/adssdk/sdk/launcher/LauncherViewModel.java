package com.adssetup.adssdk.sdk.launcher;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherViewModel extends AndroidViewModel {
    private final MutableLiveData<List<AppInfo>> apps = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean reloadQueued = new AtomicBoolean(false);
    private final MutableLiveData<List<DockItem>> dock = new MutableLiveData<>();
    private volatile List<AppInfo> last;
    private volatile List<DockItem> lastDock;
    private int currentPage = 0;
    private final HomeLayout homeLayout;

    public LauncherViewModel(@NonNull Application application) {
        super(application);
        homeLayout = new HomeLayout(application);
        refresh();
    }

    HomeLayout getHomeLayout() {
        return homeLayout;
    }

    public LiveData<List<AppInfo>> getApps() {
        return apps;
    }

    public LiveData<List<DockItem>> getDock() {
        return dock;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        currentPage = page;
    }

    public void refreshDock() {
        executor.execute(() -> {
            List<DockItem> freshDock = DockResolver.resolve(getApplication());
            if (lastDock == null || !lastDock.equals(freshDock)) {
                lastDock = freshDock;
                dock.postValue(freshDock);
            }
        });
    }

    public void refresh() {
        if (!reloadQueued.compareAndSet(false, true)) return;
        executor.execute(() -> {
            reloadQueued.set(false);
            List<AppInfo> fresh = AppRepository.loadApps(getApplication());
            List<AppInfo> prev = last;
            if (prev == null || !prev.equals(fresh)) {
                last = fresh;
                apps.postValue(fresh);
            }
            List<DockItem> freshDock = DockResolver.resolve(getApplication());
            if (lastDock == null || !lastDock.equals(freshDock)) {
                lastDock = freshDock;
                dock.postValue(freshDock);
            }
        });
    }

    @Override
    protected void onCleared() {
        homeLayout.flush();
        executor.shutdown();
    }
}