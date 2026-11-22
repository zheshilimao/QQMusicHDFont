package com.qqmusicpad.fontsize;

import java.lang.reflect.Executable;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/** LSPosed Modern API 兼容层（只保留模块实际用到的能力）。 */
public final class XposedCompat {
    private static volatile XposedModule module;
    private XposedCompat() {}

    /**
     * 日志总开关：debug 构建为 true，release 构建为 false。
     *
     * <p>两者都是编译期常量，javac 会直接把值内联到判断处，
     * release 里 {@code if (!LOGGING) return;} 之后的日志代码是死代码，
     * R8 会连方法体一起删除——模块不向 Logcat、也不向 LSPosed 输出任何日志。</p>
     */
    public static final boolean LOGGING = BuildConfig.DEBUG;

    public static void setModule(XposedModule value) { module = value; }

    /** 方法拦截回调；按需覆写「执行前 / 执行后」。 */
    public interface MethodHook {
        default void beforeHookedMethod(MethodHookParam param) throws Throwable {}

        default void afterHookedMethod(MethodHookParam param) throws Throwable {}
    }

    public static final class MethodHookParam {
        /** 被 Hook 方法的 this 对象；静态方法为 null。 */
        public final Object thisObject;
        public final Object[] args;
        private Object result;
        private boolean returnEarly;

        MethodHookParam(Object thisObject, Object[] args) {
            this.thisObject = thisObject;
            this.args = args;
        }

        /** 短路原方法，并把 value 作为返回值。 */
        public void setResult(Object value) {
            result = value;
            returnEarly = true;
        }
    }

    public static XposedInterface.HookHandle hook(Executable executable, MethodHook callback) {
        XposedModule current = module;
        if (current == null) throw new IllegalStateException("Modern Xposed module is not initialized");
        return current.hook(executable)
                .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                .intercept(chain -> {
                    MethodHookParam param = new MethodHookParam(
                            chain.getThisObject(),
                            chain.getArgs().toArray(new Object[0]));
                    try {
                        callback.beforeHookedMethod(param);
                    } catch (Throwable hookError) {
                        log("beforeHook 回调异常", hookError);
                    }
                    if (param.returnEarly) {
                        return param.result;
                    }
                    Object result = chain.proceed(param.args);
                    try {
                        callback.afterHookedMethod(param);
                    } catch (Throwable hookError) {
                        log("afterHook 回调异常", hookError);
                    }
                    return result;
                });
    }

    private static final String TAG = "QQMusicPadFont";

    /** 只在 debug 构建输出；release 里整个方法体都会被 R8 删除。 */
    public static void log(String message) {
        if (!LOGGING) return;
        XposedModule current = module;
        if (current != null) {
            try {
                current.log(android.util.Log.INFO, TAG, message);
            } catch (Throwable ignored) {
            }
        }
        try {
            android.util.Log.i(TAG, message);
        } catch (Throwable ignored) {
        }
    }

    public static void log(String message, Throwable throwable) {
        if (!LOGGING) return;
        XposedModule current = module;
        if (current != null) {
            try {
                current.log(android.util.Log.WARN, TAG, message, throwable);
            } catch (Throwable ignored) {
            }
        }
        try {
            android.util.Log.w(TAG, message, throwable);
        } catch (Throwable ignored) {
        }
    }

    public static android.content.SharedPreferences remotePreferences(String group) {
        XposedModule current = module;
        if (current == null) throw new IllegalStateException("Modern Xposed module is not initialized");
        return current.getRemotePreferences(group);
    }
}
