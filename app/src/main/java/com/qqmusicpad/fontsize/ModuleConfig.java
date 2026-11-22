package com.qqmusicpad.fontsize;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 模块配置。
 *
 * <p>模块 App 进程使用普通 SharedPreferences；目标 App（QQ音乐HD）进程通过
 * LSPosed Modern API 的 Remote Preferences 读取配置。</p>
 */
public final class ModuleConfig {

    public static final String TARGET_PACKAGE = "com.tencent.qqmusicpad";

    public static final String PREFS_NAME = "font_settings";

    /** 字体缩放比例，1.0 表示原始大小。 */
    public static final String KEY_FONT_SCALE = "font_scale";
    /** 禁止更新提醒。 */
    public static final String KEY_DISABLE_UPDATE = "disable_update";
    /** 隐藏桌面图标。 */
    public static final String KEY_HIDE_ICON = "hide_icon";
    /** 启动页状态栏适配。 */
    public static final String KEY_FIX_STATUS_BAR = "fix_status_bar";

    public static final float DEFAULT_FONT_SCALE = 1.0f;
    public static final float MIN_FONT_SCALE = 0.5f;
    public static final float MAX_FONT_SCALE = 2.0f;

    private ModuleConfig() {
    }

    private static SharedPreferences open(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static float getFontScale(Context context) {
        return clamp(open(context).getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE));
    }

    public static void setFontScale(Context context, float value) {
        open(context).edit().putFloat(KEY_FONT_SCALE, clamp(value)).apply();
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return open(context).getBoolean(key, defaultValue);
    }

    public static void setBoolean(Context context, String key, boolean value) {
        open(context).edit().putBoolean(key, value).apply();
    }

    public static float clamp(float value) {
        if (Float.isNaN(value)) return DEFAULT_FONT_SCALE;
        if (value < MIN_FONT_SCALE) return MIN_FONT_SCALE;
        if (value > MAX_FONT_SCALE) return MAX_FONT_SCALE;
        return value;
    }

    /** 在目标进程读取 Remote Preferences 快照。 */
    public static Settings readInTargetProcess() {
        float scale = DEFAULT_FONT_SCALE;
        boolean disableUpdate = false;
        boolean fixStatusBar = true;
        try {
            SharedPreferences preferences = XposedCompat.remotePreferences(PREFS_NAME);
            scale = preferences.getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE);
            disableUpdate = preferences.getBoolean(KEY_DISABLE_UPDATE, false);
            fixStatusBar = preferences.getBoolean(KEY_FIX_STATUS_BAR, true);
        } catch (Throwable throwable) {
            XposedCompat.log("[QQ音乐HD字体] 读取 Remote Preferences 失败，使用默认值", throwable);
        }
        return new Settings(clamp(scale), disableUpdate, fixStatusBar);
    }

    /** 配置快照。 */
    public static final class Settings {
        public final float fontScale;
        public final boolean disableUpdate;
        public final boolean fixStatusBar;

        Settings(float fontScale, boolean disableUpdate, boolean fixStatusBar) {
            this.fontScale = fontScale;
            this.disableUpdate = disableUpdate;
            this.fixStatusBar = fixStatusBar;
        }

        public boolean isFontEnabled() {
            return Math.abs(fontScale - DEFAULT_FONT_SCALE) > 0.001f;
        }

        public boolean hasAnyHook() {
            return isFontEnabled() || disableUpdate || fixStatusBar;
        }
    }
}
