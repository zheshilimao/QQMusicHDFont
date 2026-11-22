package com.qqmusicpad.fontsize;

import java.lang.reflect.Method;

/**
 * 禁止更新提醒。
 *
 * <p>QQ音乐HD 的升级逻辑位于 {@code com.tencent.qqmusiccar.business.upgrade}：</p>
 * <ul>
 *   <li>{@code UpgradeLifeCycleCallback.e(BaseActivity)} 在页面进入前台时注册
 *       生命周期监听，触发自动检查更新；</li>
 *   <li>{@code UpdateManager.H(Runnable, Runnable)} 弹出「发现新版本」对话框
 *       （带「立即更新 / 稍后再说」）。</li>
 * </ul>
 *
 * <p>分别 Hook 这两处：前者阻止自动检查，后者阻止弹窗，双保险。</p>
 */
public final class UpdateHooks {

    private UpdateHooks() {
    }

    public static void install(ClassLoader loader) {
        blockAutoCheck(loader);
        blockUpdateDialog(loader);
    }

    private static void blockAutoCheck(ClassLoader loader) {
        try {
            Class<?> baseActivity = Class.forName(
                    "com.tencent.qqmusiccar.v2.activity.BaseActivity", false, loader);
            Class<?> callback = Class.forName(
                    "com.tencent.qqmusiccar.business.upgrade.UpgradeLifeCycleCallback", false, loader);
            Method method = callback.getMethod("e", baseActivity);
            XposedCompat.hook(method, new XposedCompat.MethodHook() {
                @Override
                public void beforeHookedMethod(XposedCompat.MethodHookParam param) {
                    param.setResult(null); // 不注册监听，跳过自动检查更新。
                }
            });
            XposedCompat.log("[QQ音乐HD字体] hook UpgradeLifeCycleCallback.e 成功");
        } catch (Throwable throwable) {
            XposedCompat.log("[QQ音乐HD字体] hook UpgradeLifeCycleCallback.e 失败", throwable);
        }
    }

    private static void blockUpdateDialog(ClassLoader loader) {
        try {
            Class<?> manager = Class.forName(
                    "com.tencent.qqmusiccar.business.upgrade.UpdateManager", false, loader);
            Method method = manager.getMethod("H", Runnable.class, Runnable.class);
            XposedCompat.hook(method, new XposedCompat.MethodHook() {
                @Override
                public void beforeHookedMethod(XposedCompat.MethodHookParam param) {
                    param.setResult(null); // 不弹出升级对话框。
                }
            });
            XposedCompat.log("[QQ音乐HD字体] hook UpdateManager.H 成功");
        } catch (Throwable throwable) {
            XposedCompat.log("[QQ音乐HD字体] hook UpdateManager.H 失败", throwable);
        }
    }
}
