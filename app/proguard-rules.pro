# Modern API entry class is referenced by META-INF/xposed/java_init.list.
-keep class com.qqmusicpad.fontsize.XposedEntry { public <init>(); *; }

# Preserve hook callback behavior while allowing R8 to optimize the rest of the app.
-keep class com.qqmusicpad.fontsize.XposedCompat { *; }
-keep class com.qqmusicpad.fontsize.XposedCompat$* { *; }
-keep class com.qqmusicpad.fontsize.FontHooks { *; }
-keep class com.qqmusicpad.fontsize.FontHooks$* { *; }
-keep class com.qqmusicpad.fontsize.UpdateHooks { *; }
-keep class com.qqmusicpad.fontsize.UpdateHooks$* { *; }

# 双保险：release 里删除所有直接调用的 android.util.Log（模块日志已由 BuildConfig.DEBUG 门控）。
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Keep generic signatures used by reflective target method resolution.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
