# 技術設計：GitHub 網頁認證標記與進入筆記主動檢查架構 (Design)

## 背景與問題分析 (Context)
1. **日常操作路徑差異**：使用者日常開啟 App 後是直接「點擊筆記項目」進入 `FileExplorerActivity`，而非在主畫面長按選單點選 Push/Pull。先前 `FileExplorerActivity.triggerGitSync(false)` 在背景自動同步失敗時靜默忽略，未檢查 `GIT_STATUS_AUTH_FAILED`。
2. **Public 儲存庫盲點**：若 GitHub 儲存庫為 Public，JGit 執行 `ls-remote` 或 `pull` 時可能不強制驗證 Token 而成功，導致過期的 `WEB_OAUTH` Token 在進入筆記時未被發現，直到使用者編輯完筆記要 Push 時才失敗。因此必須對 `WEB_OAUTH` 儲存庫直接透過 `https://api.github.com/user` 檢查 Bearer Token 有效性。
3. **Pull 失敗狀態殘留**：`MyGitUtility.pullWithResult()` 在開始時將狀態設為 `GIT_STATUS_PULLING` (-4)，若後續 `aGitUtil.pull()` 回傳 `false` 或拋出例外，原程式碼直接 `update(aRemoteGit)` 導致狀態永久卡在 `-4`。

## 決策細節 (Decisions)
1. **SQLite v3 無損遷移 (`MyDbHelper.kt` / `RemoteGitDAO.kt`)**：
   - `MyDbHelper.VERSION` 由 `2` 升至 `3`。
   - `onUpgrade(db, oldVersion, newVersion)` 中，當 `oldVersion < 3` 時執行：
     ```sql
     ALTER TABLE REMOTE_GIT ADD COLUMN AUTH_TYPE TEXT NOT NULL DEFAULT 'PASSWORD';
     UPDATE REMOTE_GIT SET AUTH_TYPE = 'WEB_OAUTH'
       WHERE URL LIKE '%github.com%' AND PWD NOT LIKE 'ghp_%' AND PWD NOT LIKE 'github_pat_%';
     ```
   - 確保既有資料 100% 保留且自動識別網頁認證儲存庫。
2. **同次會話防打擾機制 (`FileExplorerActivity`)**：
   - 在 `FileExplorerActivity` 設置成員變數 `bHasPromptedAuthInSession = false`。
   - 首次從 `MainActivity` 點擊進入 `FileExplorerActivity` 時，主動驗證 Token；若回傳 401 則彈窗並將 `bHasPromptedAuthInSession` 設為 `true`。
   - 若使用者點擊「取消/稍後」並點開某篇筆記 (`ViewFileActivity`) 僅閱讀未修改，返回 `FileExplorerActivity` 時因 `bHasPromptedAuthInSession == true` 不再重複彈窗。
   - 若使用者在 `ViewFileActivity` 修改並儲存筆記（或於目錄刪除/新增檔案）觸發背景 Push 遭遇 401，則強制重置提示旗標並立即彈出重新認證對話框。
3. **跨 Activity 重新授權自動回跳 (`MainActivity` $\leftrightarrow$ `FileExplorerActivity`)**：
   - 當從 `FileExplorerActivity` 點擊【重新登入並同步】時，記錄待回跳之目錄上下文（`sPendingExplorerUrl`、`sPendingExplorerRootDir`、`sPendingExplorerGitName`）。
   - `MainActivity.handleGitHubOAuthCallback` 完成 Token 換取與資料庫更新（同時設定 `auth_type = WEB_OAUTH`）後，自動執行背景同步並透過 Intent 啟動 `FileExplorerActivity`，無縫回到原筆記目錄。
