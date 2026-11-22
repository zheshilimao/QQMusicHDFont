package com.qqmusicpad.fontsize;

import java.lang.reflect.Method;

/**
 * 全局字体缩放（整体放大）。
 *
 * <p>只 Hook {@code TextView.setRawTextSize(float, boolean)}：所有 TextView 文字
 * （XML 的 sp/dp、代码 setTextSize 的各单位、设计系统 px）最终都汇聚到这里。
 * 歌词（lyricengine）是自定义 View 用 TextPaint 直接绘制，不走 TextView，
 * 因此不会被放大，避免滚动到当前行时的跳动/二次放大。</p>
 */
public final class FontHooks {

    private static volatile float userScale = 1.0f;

    private FontHooks() {
    }

    public static void install(ModuleConfig.Settings settings) {
        userScale = settings.fontScale;
        hookTextViewSetRawTextSize();
        XposedCompat.log("[QQ音乐HD字体] 字体缩放已安装，scale=" + userScale);
    }

    private static void hookTextViewSetRawTextSize() {
        try {
            Method method = android.widget.TextView.class.getDeclaredMethod(
                    "setRawTextSize", float.class, boolean.class);
            XposedCompat.hook(method, new XposedCompat.MethodHook() {
                @Override
                public void beforeHookedMethod(XposedCompat.MethodHookParam param) {
                    if (userScale == 1.0f) return;
                    float size = (Float) param.args[0];
                    param.args[0] = size * userScale;
                }
            });
            XposedCompat.log("[QQ音乐HD字体] hook TextView.setRawTextSize 成功");
        } catch (Throwable throwable) {
            XposedCompat.log("[QQ音乐HD字体] hook TextView.setRawTextSize 失败", throwable);
        }
    }
}
