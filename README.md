# QQ音乐HD 增强

面向 **QQ音乐HD（`com.tencent.qqmusicpad`）** 的 LSPosed 模块（Modern Xposed API）。

## 功能

| 功能 | 说明 |
|---|---|
| 字体缩放 | 全局放大 / 缩小（50%–200%），预设 85% / 100% / 115% / 130% / 150%；Hook `TextView.setRawTextSize`，歌词（自绘 View）不受影响，不会跳动 |
| 禁止更新提醒 | 屏蔽「发现新版本」升级弹窗与自动检查 |
| 启动状态栏适配 | 清除启动页主题遗留的 `FLAG_FULLSCREEN`，冷启动时状态栏直接呈现透明 + 正常图标，不再先黑几秒 |
| 隐藏桌面图标 | 关闭后桌面无入口；设置页声明 `MODULE_SETTINGS`，LSPosed 模块列表的「打开」依然可用 |

其他：设置页与 LSPosed 的偏好通过 Remote Preferences 同步（300ms 防抖）；滑杆只在松手时落盘；
缩放 100% 且未开启其他功能时不注入任何 Hook；**debug 构建输出日志，release 构建无任何日志输出**。

## 环境

- LSPosed（Modern Xposed API `io.github.libxposed:api:102.0.0`）
- Android 8.0+（minSdk 26），目标 App：QQ音乐HD （com.tencent.qqmusicpad）
- 构建：JDK 17 + Android Gradle Plugin 9.4.0 + Gradle 9.6.0（wrapper 已固定），compileSdk 37

## 构建

```bash
./gradlew assembleRelease   # 发布包：R8 混淆 + 无日志
./gradlew assembleDebug     # 调试包：保留类名与日志
```

签名：根目录存在 `keystore.properties` 时，debug / release 使用同一套签名；
不存在时回退到默认 debug 签名（两个构建产物签名一致，可互相覆盖安装）。

## 安装

1. LSPosed → 模块 → 勾选作用域「QQ音乐HD」
2. 强制停止QQ音乐HD，后冷启动生效
3. 打开「QQ音乐HD 增强」调整设置，改动会自动同步到目标进程（下次启动生效）

## 说明

- 仅修改目标 App 进程内的少量方法（`TextView.setRawTextSize`、更新回调、`AppStarterActivity.onCreate`），
  不含任何网络、存储或后台逻辑。
- 仅供个人学习与个性化定制，请勿用于商业用途。
