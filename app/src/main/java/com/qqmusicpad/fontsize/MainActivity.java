package com.qqmusicpad.fontsize;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** 模块设置界面（现代化单页）。 */
public final class MainActivity extends Activity {

    private static final float PREVIEW_BASE_SP = 18.0f;

    private SeekBar scaleSeek;
    private TextView scaleValue;
    private TextView previewText;
    private TextView statusText;
    private Switch updateSwitch;
    private Switch iconSwitch;
    private Switch statusBarSwitch;
    private int lastPercent = -1;

    private final Runnable stateListener = this::updateStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        scaleValue = findViewById(R.id.scaleValue);
        previewText = findViewById(R.id.previewText);
        scaleSeek = findViewById(R.id.scaleSeek);
        updateSwitch = findViewById(R.id.updateSwitch);
        iconSwitch = findViewById(R.id.iconSwitch);
        statusBarSwitch = findViewById(R.id.statusBarSwitch);

        float scale = ModuleConfig.getFontScale(this);
        scaleSeek.setProgress(Math.round(scale * 100.0f) - 50);
        applyScale(scale);

        scaleSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 拖动过程中只刷新预览，不写磁盘、不触发跨进程同步。
                applyScale(ModuleConfig.clamp((progress + 50) / 100.0f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 松手时才落盘，一次拖动只产生一次写入。
                ModuleConfig.setFontScale(MainActivity.this,
                        ModuleConfig.clamp((seekBar.getProgress() + 50) / 100.0f));
            }
        });

        findViewById(R.id.presetSmall).setOnClickListener(v -> selectPreset(85));
        findViewById(R.id.presetStandard).setOnClickListener(v -> selectPreset(100));
        findViewById(R.id.presetLarge).setOnClickListener(v -> selectPreset(115));
        findViewById(R.id.presetXLarge).setOnClickListener(v -> selectPreset(130));
        findViewById(R.id.presetHuge).setOnClickListener(v -> selectPreset(150));

        updateSwitch.setChecked(ModuleConfig.getBoolean(
                this, ModuleConfig.KEY_DISABLE_UPDATE, false));
        updateSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                ModuleConfig.setBoolean(MainActivity.this,
                        ModuleConfig.KEY_DISABLE_UPDATE, isChecked));

        iconSwitch.setChecked(ModuleConfig.getBoolean(
                this, ModuleConfig.KEY_HIDE_ICON, false));
        iconSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ModuleConfig.setBoolean(MainActivity.this, ModuleConfig.KEY_HIDE_ICON, isChecked);
            applyHideIcon(isChecked);
        });

        statusBarSwitch.setChecked(ModuleConfig.getBoolean(
                this, ModuleConfig.KEY_FIX_STATUS_BAR, true));
        statusBarSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                ModuleConfig.setBoolean(MainActivity.this,
                        ModuleConfig.KEY_FIX_STATUS_BAR, isChecked));
    }

    private void selectPreset(int percent) {
        scaleSeek.setProgress(percent - 50);
        ModuleConfig.setFontScale(this, ModuleConfig.clamp(percent / 100.0f));
    }

    private void applyScale(float scale) {
        int percent = Math.round(scale * 100);
        if (percent != lastPercent) {
            lastPercent = percent;
            scaleValue.setText(percent + "%");
        }
        previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, PREVIEW_BASE_SP * scale);
    }

    /**
     * 隐藏 / 显示桌面图标（通过禁用启动器 activity-alias 实现）。
     *
     * <p>设置页 {@code MainActivity} 本身不参与桌面入口，并声明了
     * {@code de.robv.android.xposed.category.MODULE_SETTINGS}，因此隐藏图标后
     * 仍可从 LSPosed 模块列表的「打开」重新进入。</p>
     */
    private void applyHideIcon(boolean hide) {
        try {
            PackageManager manager = getPackageManager();
            // 兜底：确保设置页本身始终可用。
            manager.setComponentEnabledSetting(
                    new ComponentName(this, MainActivity.class.getName()),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            ComponentName alias = new ComponentName(
                    this, getPackageName() + ".MainActivityLauncher");
            manager.setComponentEnabledSetting(alias,
                    hide ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Throwable ignored) {
            // 隐藏失败不影响其他功能。
        }
    }

    private void updateStatus() {
        if (statusText == null) return;
        boolean ready = ModuleApplication.isServiceReady();
        statusText.setText(ready ? R.string.status_active : R.string.status_inactive);
        statusText.setTextColor(getColor(ready
                ? R.color.status_badge_active
                : R.color.status_inactive));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ModuleApplication.setStateListener(stateListener);
        updateStatus();
    }

    @Override
    protected void onPause() {
        ModuleApplication.clearStateListener(stateListener);
        super.onPause();
    }
}
