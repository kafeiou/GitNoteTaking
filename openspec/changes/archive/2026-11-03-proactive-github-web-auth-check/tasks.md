# 任務清單：GitHub 網頁認證標記與進入筆記主動過期檢查 (Tasks)

## 1. 資料庫無損升級與網頁認證標記 (Database & Model)

- [x] 1.1 在 `RemoteGit.kt` 新增 `auth_type` 屬性、常數 `AUTH_TYPE_WEB_OAUTH` / `AUTH_TYPE_PASSWORD` 與 `isWebOAuth()` 輔助方法。
- [x] 1.2 在 `RemoteGitDAO.kt` 新增 `AUTH_TYPE_COLUMN` 欄位定義與讀寫邏輯，並在 `MyDbHelper.kt` 將資料庫版本升至 `3`，實作 `ALTER TABLE` 無損遷移與既有 GitHub 儲存庫自動標記。

## 2. 主動檢查邏輯、跨畫面重新授權回跳與視覺提示 (Proactive Check & UI)

- [x] 2.1 在 `MyGitUtility.java` 新增 `checkAndMarkGitHubAuthFailure()`，於 `pullWithResult()` 開頭主動驗證 Token 有效性（解決 Public Repo 盲點），並修復 Pull 失敗時狀態卡在 `GIT_STATUS_PULLING` (-4) 的問題。
- [x] 2.2 在 `FileExplorerActivity.java` 與 `ViewFileActivity.java` 實作進入筆記主動過期檢查、同次會話防重複打擾機制（`bHasPromptedAuthInSession`）、存檔/刪除觸發 Push 失敗主動彈窗，以及跨 Activity 重新授權後自動返回 `FileExplorerActivity` 並同步刷新。
- [x] 2.3 在 `MainActivity.java`、`ModifyRemoteGitActivity.java` 確保網頁授權寫入 `AUTH_TYPE_WEB_OAUTH`，並於 `GitList.java` 及 4 國語系 `strings.xml` 新增主畫面「(授權已過期)」紅字狀態顯示。

## 3. 自動化單元測試與驗證 (Unit Testing & Verification)

- [x] 3.1 擴充 `GitHubAuthRenewalUnitTest.java` 測試 `RemoteGit` 的 `AUTH_TYPE` 判斷、資料庫無損遷移 SQL、以及 4 國語系過期狀態字串完整性，並執行 `./gradlew testDebugUnitTest` 確保 100% 通過。
