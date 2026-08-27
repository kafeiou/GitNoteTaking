## 1. 多語系資源建置與字串更新

- [x] 1.1 建立 `app/src/main/res/values-zh-rCN/strings.xml`（簡體中文全 App 語系支援）
- [x] 1.2 建立 `app/src/main/res/values-ja/strings.xml`（日文 日本語全 App 精簡標準名詞語系支援）
- [x] 1.3 更新 `app/src/main/res/values/strings.xml`、`values-zh-rTW/strings.xml` 與 `values-zh-rHK/strings.xml` 加入權限前置說明字串

## 2. 權限核心模組與底部面板實作

- [x] 2.1 建立底部滑出面板佈局 `app/src/main/res/layout/dialog_permission_bottom_sheet.xml`
- [x] 2.2 建立 `app/src/main/java/inmethod/gitnotetaking/utility/PermissionHelper.java`，實作權限檢查、BottomSheet 前置說明面板、靜默取消阻擋與跳轉設定頁功能

## 3. UI 與功能整合

- [x] 3.1 修改 `app/src/main/java/inmethod/gitnotetaking/MainActivity.java`，移除開機強索權限邏輯
- [x] 3.2 修改 `app/src/main/java/inmethod/gitnotetaking/ViewFileActivity.java`，在「拍照存檔」與附件操作整合 `PermissionHelper`

## 4. 建置與驗證

- [x] 4.1 執行 `./gradlew compileDebugSources` 與 `./gradlew assembleDebug` 驗證編譯與打包無誤
- [x] 4.2 驗證多語系字串與底部面板前置流程邏輯完整

## 5. 版本控制與變更歸檔

- [x] 5.1 執行 `git add .` 與 `git commit` 保存變更
- [x] 5.2 執行 OpenSpec 變更歸檔並自動同步主文件
