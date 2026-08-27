## 1. 語言切換選單與字串資源

- [x] 1.1 在 `res/values/arrays.xml`（及各語系）中定義語言選單 entries 與 entryValues
- [x] 1.2 在 `res/xml/settingspreferences.xml` 中新增 `ListPreference` 語言選單
- [x] 1.3 在 `CustomPreferenceFragment.java` 實作語言切換監聽器與 `AppCompatDelegate.setApplicationLocales`

## 2. 全專案輸入框自動 Trim 與防呆

- [x] 2.1 修改 `FileExplorerActivity.java`（新增資料夾、新增文字檔、搜尋文字、附件匯入）加入 `.trim()` 與空值阻擋
- [x] 2.2 修改 `ViewFileActivity.java`（Commit 訊息、拍照存檔檔名）加入 `.trim()`
- [x] 2.3 修改 `CreateLocalGitActivity.java`、`CloneGitActivity.java`、`ModifyLocalGitActivity.java` 與 `ModifyRemoteGitActivity.java` 加入 `.trim()`

## 3. 建置與驗證

- [x] 3.1 執行 `./gradlew compileDebugSources` 與 `./gradlew assembleDebug` 驗證編譯與打包無誤
- [x] 3.2 驗證語言切換功能與各輸入框前後空白過濾邏輯

## 4. 版本控制與變更歸檔

- [x] 4.1 執行 `git add .` 與 `git commit` 保存變更
- [x] 4.2 執行 OpenSpec 變更歸檔並自動同步主文件
