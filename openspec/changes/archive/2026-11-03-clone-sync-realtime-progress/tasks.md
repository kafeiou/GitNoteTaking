# 任務清單：首次同步 (Clone) 與真實日期還原之即時進度顯示 (Tasks)

## 1. 多語系字串資源 (Multilingual Strings)
- [x] 1.1 在 4 國語系 `strings.xml`（`values`、`values-zh-rTW`、`values-zh-rCN`、`values-ja`）新增同步各階段即時進度格式化字串（准备同步、下載筆記資料含 MB、解析資料差異、寫入筆記檔案、還原檔案真實日期）。

## 2. JGit `ProgressMonitor` 與 GraphQL 日期還原即時進度 (Core Utility)
- [x] 2.1 在 `MyGitUtility.java` 新增 `SyncProgressListener` 介面、每個 `sRemoteUrl` 的即時進度快取（`getCloneProgress`），以及計算 `.git/objects/pack` 已接收位元組數並格式化為 MB 之輔助方法。
- [x] 2.2 更新 `MyGitUtility.cloneGit()` 使用 JGit `Git.cloneRepository().setDepth(1).setProgressMonitor(...)` 即時回報下載物件數、百分比與已接收 MB 數，並同步銜接 `syncFileTimestampsFromGitHubApi` 即時回報每批次檔案日期還原進度，確保在對話框關閉前 100% 完成。

## 3. UI 等待對話框與列表即時進度呈現 (UI Integration)
- [x] 3.1 更新 `MainActivity.java` 移除 `startLoadingMessageCycle()` 假輪播，改為接收 `SyncProgressListener` 即時刷新 `waitDialog` 的 `tv_loading_msg`，並於 `CloneGitActivity.java` 及 `GitList.java` 支援顯示背景複製即時進度。

## 4. 自動化單元測試與驗證 (Unit Testing & Verification)
- [x] 4.1 在 `inmethod/gitnotetaking/test/` 下擴充單元測試驗證進度格式化、容量換算與 4 國語系字串完整性，執行 `./gradlew testDebugUnitTest` 確保 100% 通過，並執行 `openspec validate --changes`。
