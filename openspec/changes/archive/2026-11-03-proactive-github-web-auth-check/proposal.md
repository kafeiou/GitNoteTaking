# 變更提案：GitHub 網頁認證資料庫標記與進入筆記主動過期檢查 (Proposal)

## 為什麼做 (Why)
使用者在實機測試時發現，透過 GitHub 網頁 OAuth 認證建立的筆記儲存庫，在 Token 過期（8 小時）後，日常直接點擊進入筆記目錄（`FileExplorerActivity`）或編輯檔案存檔（`ViewFileActivity`）時，App 並未主動告知認證已過期，而是靜默失敗或僅在主畫面長按手動 Push/Pull 時才攔截。此外，若遠端儲存庫為 Public，JGit 的匿名 Pull 可能通過，導致使用者編輯完筆記準備 Push 時才遭遇 401 失敗，且資料庫亦缺乏明確欄位標記該儲存庫是否為「透過網頁認證（`WEB_OAUTH`）」建立。

## 變更內容 (What Changes)
1. **資料庫明確標記網頁認證 (`AUTH_TYPE`)**：
   - 升級 SQLite `REMOTE_GIT` 資料表（Version 2 $\rightarrow$ 3），透過 `ALTER TABLE` 無損新增 `AUTH_TYPE` 欄位（支援 `WEB_OAUTH` 與 `PASSWORD`）。
   - 自動將手機內既有非 PAT（非 `ghp_` / `github_pat_` 開頭）之 `github.com` 儲存庫遷移標記為 `WEB_OAUTH`，且所有透過網頁 OAuth 建立或重新授權之儲存庫一律寫入 `WEB_OAUTH`。
2. **進入筆記與編輯存檔時主動檢查 401 過期**：
   - 當使用者點擊進入筆記目錄（`FileExplorerActivity`）時，針對 `WEB_OAUTH`（及 `github.com`）儲存庫主動透過 GitHub API (`GET https://api.github.com/user`) 與遠端連線檢查 Token 是否過期（401）。
   - 一旦偵測到過期（`GIT_STATUS_AUTH_FAILED = -5`），立即在 `FileExplorerActivity` 當前畫面主動彈出「GitHub 授權已失效」對話框引導重新取得網頁認證。
   - 若使用者先點選「稍後」，在同一次目錄瀏覽期間（如點開文章閱讀後返回目錄）不重複打擾；但若有「編輯存檔/刪除觸發推送」或「退回主畫面重新進入筆記」則再次主動提醒。
3. **重新授權後自動返回原筆記目錄並同步**：
   - 從 `FileExplorerActivity` 發起重新授權並完成瀏覽器 OAuth 回跳後，系統自動更新 Token、完成雙向同步（Pull + Push），並自動跳轉回原本正在瀏覽的 `FileExplorerActivity` 刷新畫面。
4. **主畫面清單過期紅字標示與 Pull 狀態卡死修復**：
   - 在 `GitList` 主畫面清單中，當狀態為 `GIT_STATUS_AUTH_FAILED` (-5) 時以醒目色標示「(授權已過期)」。
   - 修正 `MyGitUtility.pullWithResult()` 失敗時可能將狀態遺留為 `GIT_STATUS_PULLING` (-4) 的問題。

## 影響範圍 (Impact)
- **規格文件**：`openspec/specs/github-integration/spec.md`
- **資料庫層**：`MyDbHelper.kt`、`RemoteGit.kt`、`RemoteGitDAO.kt`
- **業務與介面層**：`MyGitUtility.java`、`FileExplorerActivity.java`、`ViewFileActivity.java`、`MainActivity.java`、`ModifyRemoteGitActivity.java`、`GitList.java`、4 國語系 `strings.xml`
- **測試層**：`GitHubAuthRenewalUnitTest.java`
