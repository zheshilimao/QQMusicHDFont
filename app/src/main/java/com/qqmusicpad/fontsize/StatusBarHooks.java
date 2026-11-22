package com.qqmusicpad.fontsize;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Method;

/**
 * 启动页状态栏适配。
 *
 * <p>QQ音乐HD 的启动主题 {@code QQMusicForAppStartThemeFirst} 声明了
 * {@code android:windowFullscreen="true"}，Activity 窗口因此带有
 * {@link WindowManager.LayoutParams#FLAG_FULLSCREEN}。App 自己在
 * {@code MusicAppLifeListener.onActivityStarted} 里调用的
 * {@code ActivityExtKt.v()} 只清掉半透明状态栏标记、设置
 * {@code setSystemUiVisibility(...)}，并没有清 {@code FLAG_FULLSCREEN}；
 * 在 Android 15/16 上系统以窗口 flag 为准，于是冷启动最初的几秒状态栏
 * 是隐藏 / 黑色的，要等后续界面重新布局才恢复。</p>
 *
 * <p>这里在启动页 {@code onCreate} 结束时把窗口标记改成与 App 正常界面
 * 一致的「全屏布局 + 透明状态栏」，让状态栏从第一帧就正常显示。
 * 只动窗口标记，不覆盖 App 自己的状态栏颜色与图标明暗逻辑。</p>
 */
public final class StatusBarHooks {

    private static final String ACTIVITY_CLASS =
            "com.tencent.qqmusiccar.app.activity.AppStarterActivity";

    private StatusBarHooks() {
    }

    public static void install(ClassLoader classLoader) {
        try {
            Class<?> activityClass = Class.forName(ACTIVITY_CLASS, false, classLoader);
            Method onCreate = activityClass.getDeclaredMethod("onCreate", Bundle.class);
            XposedCompat.hook(onCreate, new XposedCompat.MethodHook() {
                @Override
                public void afterHookedMethod(XposedCompat.MethodHookParam param) {
                    if (param.thisObject instanceof Activity) {
                        normalize((Activity) param.thisObject);
                    }
                }
            });
            XposedCompat.log("[QQ音乐HD字体] 启动页状态栏适配已安装");
        } catch (Throwable throwable) {
            XposedCompat.log("[QQ音乐HD字体] 启动页状态栏适配安装失败", throwable);
        }
    }

    private static void normalize(Activity activity) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.flags &= ~(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        attributes.flags |= WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        window.setAttributes(attributes);
        window.setStatusBarColor(Color.TRANSPARENT);

        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
