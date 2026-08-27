## 1. 建置配置更新

- [x] 1.1 修改 `app/build.gradle` 中的 `targetSdkVersion` 由 35 提升至 36，並加入 `android.buildFeatures.buildConfig = true`
- [x] 1.2 執行 `./gradlew compileDebugSources` 驗證編譯是否正常且無 AGP Deprecation 警告

## 2. 驗證與打包

- [x] 2.1 執行 `./gradlew assembleDebug` 驗證除錯 APK 打包成功
- [x] 2.2 檢查產出的 APK 資訊確認 Target API Level 為 36

## 3. 版本控制與變更歸檔

- [x] 3.1 執行 `git add .` 與 `git commit` 保存變更
- [x] 3.2 執行 OpenSpec 變更歸檔並自動同步主文件
