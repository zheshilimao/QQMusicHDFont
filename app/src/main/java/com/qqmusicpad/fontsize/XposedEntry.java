package com.qqmusicpad.fontsize;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/** LSPosed Modern API entry point. */
public final class XposedEntry extends XposedModule {
    private boolean initialized;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        XposedCompat.setModule(this);
        XposedCompat.log("[QQ音乐HD字体] onModuleLoaded 模块已加载");
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (initialized || !ModuleConfig.TARGET_PACKAGE.equals(param.getPackageName())) return;
        initialized = true;
        XposedCompat.setModule(this);
        try {
            ModuleConfig.Settings settings = ModuleConfig.readInTargetProcess();
            XposedCompat.log("[QQ音乐HD字体] 目标进程 " + param.getPackageName()
                    + "，fontScale=" + settings.fontScale
                    + "，disableUpdate=" + settings.disableUpdate
                    + "，fixStatusBar=" + settings.fixStatusBar);
            if (!settings.hasAnyHook()) {
                XposedCompat.log("[QQ音乐HD字体] 无启用功能，跳过注入");
                return;
            }
            if (settings.fixStatusBar) {
                try {
                    StatusBarHooks.install(param.getClassLoader());
                } catch (Throwable throwable) {
                    XposedCompat.log("[QQ音乐HD字体] 状态栏 Hook 注入失败", throwable);
                }
            }
            if (settings.isFontEnabled()) {
                try {
                    FontHooks.install(settings);
                } catch (Throwable throwable) {
                    XposedCompat.log("[QQ音乐HD字体] 字体 Hook 注入失败", throwable);
                }
            }
            if (settings.disableUpdate) {
                try {
                    UpdateHooks.install(param.getClassLoader());
                } catch (Throwable throwable) {
                    XposedCompat.log("[QQ音乐HD字体] 更新 Hook 注入失败", throwable);
                }
            }
        } catch (Throwable throwable) {
            XposedCompat.log("[QQ音乐HD字体] 注入失败", throwable);
        }
    }
}
