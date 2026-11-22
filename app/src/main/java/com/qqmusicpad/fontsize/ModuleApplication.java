package com.qqmusicpad.fontsize;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/**
 * 把设置界面的私有偏好同步到 LSPosed Remote Preferences。
 *
 * <p>滑杆拖动会连续触发偏好变更；这里做 300ms 防抖，把多次变更合并成一次
 * Binder 提交，避免频繁跨进程写入带来的额外功耗与卡顿。</p>
 */
public final class ModuleApplication extends Application
        implements XposedServiceHelper.OnServiceListener {

    private static final long SYNC_DEBOUNCE_MS = 300L;

    private static volatile boolean serviceReady;
    private static volatile Runnable stateListener;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private SharedPreferences localPreferences;
    private volatile XposedService xposedService;
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (preferences, key) -> scheduleSync(SYNC_DEBOUNCE_MS);

    private final Runnable syncTask = this::performSync;

    public static boolean isServiceReady() {
        return serviceReady;
    }

    public static void setStateListener(Runnable listener) {
        stateListener = listener;
        if (listener != null) MAIN_HANDLER.post(listener);
    }

    public static void clearStateListener(Runnable listener) {
        if (stateListener == listener) stateListener = null;
    }

    private static void notifyStateChanged() {
        Runnable listener = stateListener;
        if (listener != null) MAIN_HANDLER.post(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        localPreferences = getSharedPreferences(ModuleConfig.PREFS_NAME, MODE_PRIVATE);
        localPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onServiceBind(XposedService service) {
        xposedService = service;
        scheduleSync(0L);
    }

    @Override
    public void onServiceDied(XposedService service) {
        if (xposedService == service) {
            xposedService = null;
            serviceReady = false;
            notifyStateChanged();
        }
    }

    private void scheduleSync(long delayMs) {
        MAIN_HANDLER.removeCallbacks(syncTask);
        if (delayMs <= 0L) {
            MAIN_HANDLER.post(syncTask);
        } else {
            MAIN_HANDLER.postDelayed(syncTask, delayMs);
        }
    }

    private void performSync() {
        final XposedService service = xposedService;
        final SharedPreferences local = localPreferences;
        if (service == null || local == null) return;
        syncExecutor.execute(() -> {
            boolean ready = syncAll(local, service);
            if (xposedService == service) {
                serviceReady = ready;
                notifyStateChanged();
            }
        });
    }

    private static boolean syncAll(SharedPreferences local, XposedService service) {
        try {
            SharedPreferences remote = service.getRemotePreferences(ModuleConfig.PREFS_NAME);
            SharedPreferences.Editor editor = remote.edit();
            editor.putFloat(ModuleConfig.KEY_FONT_SCALE,
                    ModuleConfig.clamp(local.getFloat(
                            ModuleConfig.KEY_FONT_SCALE, ModuleConfig.DEFAULT_FONT_SCALE)));
            editor.putBoolean(ModuleConfig.KEY_DISABLE_UPDATE,
                    local.getBoolean(ModuleConfig.KEY_DISABLE_UPDATE, false));
            editor.putBoolean(ModuleConfig.KEY_FIX_STATUS_BAR,
                    local.getBoolean(ModuleConfig.KEY_FIX_STATUS_BAR, true));
            return editor.commit();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
