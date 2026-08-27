## Why

為符合 Google Play 政策要求（自 2026 年 8 月 31 日起，所有應用程式更新必須以最新 Android 版本發布 1 年內的目標 API 級別為目標），本專案需要將 `targetSdkVersion` 升級至 Android 16 (API 級別 36)，以確保應用程式可持續在 Google Play 上發布更新，並提供符合最新 Android 安全性與效能標準的使用者體驗。

## What Changes

- **Target SDK 升級**：將 `app/build.gradle` 的 `targetSdkVersion` 由 `35` 升級為 `36`。
- **Build Features 顯式配置**：在 `app/build.gradle` 中顯式加入 `android.buildFeatures.buildConfig = true`，消除 AGP 10 的 Deprecation Warning。
- **建置與打包驗證**：確保在 Gradle 8.13、AGP 8.13.2 與 Java 21 Toolchain 環境下順利完成編譯與除錯打包 (`assembleDebug`)。

## Capabilities

### New Capabilities
- `platform-compatibility`: 定義應用程式對 Android 16 (API 36) 的目標版本規範、Edge-to-Edge 相容性、16KB Page Size 支援與建置要求。

### Modified Capabilities
<!-- 無現存 Capabilities 需修改 -->

## Impact

- **建置腳本**：修改 `app/build.gradle`。
- **應用程式相容性**：確保在 Android 16 (API 36) 及舊版 Android (minSdkVersion 33) 設備上穩定運作。
- **依賴庫**：已確認 JGit、Jakarta 工具庫與 AndroidX 依賴庫在 API 36 下相容。
