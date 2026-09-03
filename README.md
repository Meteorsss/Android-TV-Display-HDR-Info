# Android TV 屏幕与 HDR 检测

一个无需 root、无需原生库的 Android TV 检测应用。面向 Android 9（API 28）和 32 位电视，同时可运行于更新系统。

## 检测内容

- 当前活动的**物理显示模式**：`Display.Mode.getPhysicalWidth()` / `getPhysicalHeight()`
- 系统报告的最高物理模式及全部可用模式、刷新率
- 应用 UI / 逻辑渲染尺寸，用来解释“4K 面板但 UI 是 1920×1080”的情况
- HDR 显示能力：Dolby Vision、HDR10、HLG，以及系统可能报告的 HDR10+
- HDR 期望亮度数据与宽色域状态
- Android 版本、设备型号和 CPU ABI

应用是纯 Java 实现，不包含 `.so` 原生库，因此 APK 不受 armeabi-v7a / arm64-v8a 差异限制，可在 32 位 Android 9 电视上安装。

## 重要说明

Android 官方公开 API 能读取的是系统报告的**显示输出模式**。大多数 4K Android TV 会把 UI 以 1920×1080 逻辑尺寸渲染，再由系统合成到 3840×2160 物理输出模式，本应用会同时显示两者。

但电视厂商可能隐藏、固定或错误上报底层面板参数。如果 `Display.Mode` 与全部 `supportedModes` 都只报告 1920×1080，普通第三方应用无法绕过固件、在无 root/系统权限下直接证明内置面板像素数量；需要结合工程菜单、厂商资料或 ADB 系统信息确认。

HDR 能力列表表示系统/显示设备上报的能力，不保证某个播放器、解码器、片源或 HDMI 端口一定能够使用该格式。

## GitHub Actions 自动编译

每次推送到 `main`，或在 Actions 页面手动运行 **Build Android APK**，工作流会：

1. 安装 JDK 17 和 Gradle 8.11.1
2. 编译 Debug APK
3. 运行 Android Lint
4. 上传 APK 为 Actions Artifact

下载路径：仓库 → **Actions** → 最新一次运行 → **Artifacts** → `tv-display-hdr-info-debug-apk`

生成的文件名为 `app-debug.apk`。

## 安装

可以将 APK 复制到 U 盘后在电视文件管理器安装，也可以开启电视的 USB/网络调试后使用：

```bash
adb install -r app-debug.apk
```

若系统阻止安装，请在电视设置中允许对应文件管理器或安装器“安装未知应用”。

## 技术参数

- `minSdk 24`
- `targetSdk 35`
- `compileSdk 35`
- Android Gradle Plugin 8.9.2
- Java 8 源码兼容级别
- 无第三方运行时依赖

## License

MIT
