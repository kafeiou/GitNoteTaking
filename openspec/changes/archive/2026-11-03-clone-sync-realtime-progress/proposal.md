# 變更提案：首次同步 (Clone) 與真實日期還原之即時進度顯示 (Proposal)

## 1. 背景與動機 (Why)
當使用者首次下載（Clone）資料量較大的筆記儲存庫時，因為採用淺層複製（`depth = 1`），系統會依序執行兩個耗時階段：
1. **Git 淺層下載階段（JGit Clone `depth = 1`）**：接收遠端壓縮物件（Receiving objects）、解析差異（Resolving deltas）、以及檢出檔案（Checking out files）。
2. **GitHub 檔案真實日期還原階段（GraphQL API 批次查詢）**：由於 `depth = 1` 僅包含最新一筆 Commit，本地端無歷史紀錄，必須透過 GitHub GraphQL API 分批（每批 100 個檔案）查詢並還原每個檔案的真實最後修改時間。

原先 `MainActivity` 僅使用 10 秒計時器盲目輪播 3 句固定文字（`startLoadingMessageCycle`），且 `syncFileTimestampsAsync` 在背景非同步執行，導致使用者不僅無法得知目前實際下載了多少資料（進度百分比、物件數、已下載 MB 數），對話框甚至會在日期還原尚未完成前就提早關閉。
因此，本變更移除固定計時輪播，改以 **JGit `ProgressMonitor` 即時階段與進度回報（含已下載 MB 數）** 結合 **GraphQL 檔案日期還原進度回報**，在等待對話框與主畫面列表中直接呈現真實同步進度。

## 2. 變更範疇 (What Changes)
1. **JGit `ProgressMonitor` 淺層複製（`depth = 1`）即時進度追蹤**：
   - 在 `MyGitUtility.cloneGit()` 中支援傳入 `SyncProgressListener` 回呼介面（並同步更新靜態進度快取供主畫面列表讀取）。
   - 直接使用 JGit `Git.cloneRepository().setDepth(1).setProgressMonitor(...)` 取代黑盒呼叫，即時攔截 `Receiving objects`、`Resolving deltas`、`Checking out files` 等階段之已完成數、總數與百分比，同時計算本地 `.git/objects/pack` 與工作目錄已接收之實際資料量（MB），動態格式化進度字串（例如：`正在下載筆記資料... 42% (210/500, 12.4 MB)`）。
2. **GraphQL 真實日期還原同步化與進度回報**：
   - 在 `cloneGit()` 完成檔案檢出後，於同一執行緒內執行 `syncFileTimestampsFromGitHubApi`（確保在等待對話框關閉前 100% 完成日期還原），並在每批次（100 個檔案）處理時即時回報進度（例如：`正在還原檔案真實日期... 57% (200/350)`）。
3. **移除假輪播，改為即時進度呈現**：
   - 移除 `MainActivity` 的 `startLoadingMessageCycle()` 10 秒固定文字輪播，改由 `SyncProgressListener` 即時更新 `waitDialog` 中的 `tv_loading_msg`。
   - 支援在 `GitList` 主畫面列表顯示 `GIT_STATUS_CLONING` 時一併呈現即時進度文字。
4. **多語系支援（繁中/簡中/英文/日文）**：
   - 新增各同步階段（連線檢查、下載筆記資料、解析資料差異、檢出筆記檔案、還原檔案真實日期）之 4 國語系格式化字串。

## 3. 影響範圍 (Impact)
- **工具與底層邏輯**：`MyGitUtility.java`
- **UI 與互動邏輯**：`MainActivity.java`、`CloneGitActivity.java`、`GitList.java`
- **多語系資源**：`values/strings.xml`、`values-zh-rTW/strings.xml`、`values-zh-rCN/strings.xml`、`values-ja/strings.xml`
- **單元測試**：新增或擴充單元測試驗證進度格式化、MB 容量換算、階段轉換與 4 國語系字串完整性。
