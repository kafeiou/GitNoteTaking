## 1. Deep Link 配置與 Manifest 設定

- [x] 1.1 在 `app/src/main/AndroidManifest.xml` 中為 `MainActivity` 設定 `android:launchMode="singleTop"` 並註冊 Deep Link Scheme（`gitnotetaking://oauth/github`）之 `<intent-filter>`，並驗證 Manifest 語法無誤。

## 2. 多語系字串資源建立

- [x] 2.1 在 5 國語系 `strings.xml`（`values`, `values-zh-rTW`, `values-zh-rHK`, `values-zh-rCN`, `values-ja`）新增「一鍵登入 GitHub 授權 (推薦)」按鈕文字、分割提示與相關連線字串，並確認字串對應無缺漏。

## 3. 雙軌並存 UI 與 OAuth 流程實作

- [x] 3.1 在 `MainActivity.java` 的 `startCreateGitHubNoteFlow()` 中加入頂部「🐙 一鍵登入 GitHub 授權 (推薦)」按鈕，並完整保留既有 PAT 輸入框、4 步驟指引與剪貼簿自動帶入功能。
- [x] 3.2 在 `MainActivity.java` 實作點擊 OAuth 按鈕開啟 Chrome Custom Tabs（帶入 Client ID `Ov23licBa82hfK5H5sos` 與 scope），並實作 `onNewIntent` / `handleOAuthCallback` 接收授權碼。
- [x] 3.3 在 `MainActivity.java` 實作背景 Token 交換（POST `https://github.com/login/oauth/access_token`），取得 Token 後自動接續既有的 `fetchGitHubUserAndRepos` 與 Repo 挑選對話框。

## 4. 編譯與整合驗證

- [x] 4.1 執行 `./gradlew assembleDebug` 驗證專案編譯與 APK 打包成功無錯誤。
