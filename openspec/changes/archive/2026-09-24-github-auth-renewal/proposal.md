# Proposal: GitHub 授權續期、401 攔截自動續推與多語系 FAQ (GitHub Auth Renewal & Recovery)

## Why

目前使用者透過「一鍵登入 GitHub 授權」（Web OAuth）下載筆記後，若 GitHub OAuth App 開啟了 Token Expiration（或預設 8 小時過期政策），發放之 Access Token 會在 8 小時後自然失效。
由於目前 App：
1. 底層未實作 Refresh Token 儲存與續期機制；
2. JGit 在同步或推送時遭遇 401 Unauthorized 會吞掉錯誤細節，UI 僅彈出簡短的「同步失敗 (Push failed)」Toast，使用者無法得知授權已過期；
3. 「修改儲存庫」介面僅有帳號與密碼文字輸入框，缺乏「透過 GitHub 網頁重新授權」按鈕；
4. 「建立 GitHub 筆記」清單將已下載儲存庫鎖定為不可點選，導致使用者陷入「無法透過網頁重新授權現有筆記、若刪除筆記重新 Clone 則會遺失手機端未同步修改」的死胡同。

本變更旨在澈底根治此問題，提供「OAuth State 路由重導」、「401 授權過期專屬彈窗一鍵登入」、「授權完成後自動無痛續推未同步修改（筆記零遺失）」、「修改頁面增加網頁重新授權按鈕」，並建立多語系 FAQ 設定與排錯說明文件。

## What Changes

- **OAuth State 路由分流**：在 `GitHubAuthManager` 擴充 `startOAuthWebFlow(activity, state)`，於發起授權請求時帶入 `state`（如 `create_new` 或 `reauth_<repoId>`），回跳時精準區分是「建立新筆記」還是「拯救/更新特定舊筆記的授權」。
- **401 Unauthorized 錯誤主動攔截與狀態傳遞**：改造 `MyGitUtility`，當遠端拋出 `TransportException: not authorized` 時，標記專屬狀態碼 `GIT_STATUS_AUTH_FAILED`，不再只是被動回傳 `false`。
- **401 專屬彈窗引導與斷點自動續推**：當使用者在主畫面或編輯器點選同步遇到 401 時，彈出「GitHub 授權已失效」專屬對話框，提供【一鍵網頁登入】。使用者完成授權回跳後，系統自動將新 Token 寫入資料庫並**自動背景重新執行剛才失敗的 Push**，確保手機上未同步的修改 100% 不遺失。
- **修改儲存庫畫面新增網頁授權按鈕**：在 `ModifyRemoteGitActivity` 當遠端為 GitHub 時，提供【透過 GitHub 網頁重新授權】按鈕，點擊後即可一鍵換發新 Token 並更新儲存庫設定。
- **解除已下載儲存庫之鎖定**：在「建立 GitHub 筆記」清單中，允許點擊已下載的 Repo，提供「更新此筆記授權」選項。
- **新增 4 國語系 FAQ 排錯與設定文件**：於專案目錄建立 `docs/FAQ.md`（繁中）、`docs/FAQ_zh-CN.md`（簡中）、`docs/FAQ_en.md`（英文）、`docs/FAQ_ja.md`（日文），提供 GitHub OAuth 8 小時過期成因、App 內一鍵復原步驟，以及 OAuth App 管理員如何關閉 "Expire user authorization tokens" 達成永不過期之完整指南。
- **自動化單元測試**：於 `inmethod/gitnotetaking/test/` 撰寫 `GitHubAuthRenewalUnitTest.java`，驗證 State 路由解析、401 狀態碼攔截邏輯與極端情況處理。

## Capabilities

### Modified Capabilities
- `github-integration`: 擴充 GitHub 授權管理規範，增加 OAuth `state` 路由、401 授權失效主動攔截與自動斷點續推、儲存庫管理畫面網頁重新授權入口，以及多語系 FAQ 文件指引規範。

## Impact

- **修改程式碼**：
  - `app/src/main/java/inmethod/gitnotetaking/utility/GitHubAuthManager.java`（新增支援 `state` 參數、重構授權回調路由）
  - `app/src/main/java/inmethod/gitnotetaking/utility/MyGitUtility.java`（新增 401 錯誤碼判定與狀態傳遞）
  - `app/src/main/java/inmethod/gitnotetaking/MainActivity.java`（處理 401 彈窗導引、`onNewIntent` 處理 `reauth_<id>` 回跳並自動觸發 Push、下載清單允許點擊更新）
  - `app/src/main/java/inmethod/gitnotetaking/ModifyRemoteGitActivity.java`（加入 GitHub 網頁重新登入按鈕）
  - `app/src/main/res/layout/activity_main_modify_remote.xml`（新增重新授權按鈕排版）
  - 4 國語系 `strings.xml`（新增對話框與按鈕文字）
- **新增文件**：
  - `docs/FAQ.md`（繁中）
  - `docs/FAQ_zh-CN.md`（簡中）
  - `docs/FAQ_en.md`（英文）
  - `docs/FAQ_ja.md`（日文）
- **新增測試**：
  - `app/src/test/java/inmethod/gitnotetaking/test/GitHubAuthRenewalUnitTest.java`
- **相容性影響**：無破壞性變更，純向下相容增強。
