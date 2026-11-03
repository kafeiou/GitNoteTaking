# 技術設計：首次同步 (Clone) 與真實日期還原之即時進度顯示 (Design)

## 1. 上下文與架構分析 (Context)
- **原本的問題**：
  1. `MyGitUtility.cloneGit()` 原本呼叫 `aGitUtil.clone(sUserName, sUserPassword, 1)`，未掛載 JGit `ProgressMonitor`，導致 UI 完全無法得知下載百分比、物件數量或已接收的 MB 數。
  2. 由於採用 `depth = 1` 淺層複製，本地端僅有單一 `HEAD` Commit，必須依賴 `syncFileTimestampsFromGitHubApi()` 透過 GitHub GraphQL API 分批查詢每個檔案的真實修改日期。然而原本在 `cloneGit()` 中呼叫的是非同步 `syncFileTimestampsAsync()`，導致 `cloneGit()` 提早返回、`MainActivity` 提早關閉等待對話框，造成兩個問題：(a) 檔案龐大時等待期間只有 10 秒一次的假輪播文字；(b) 對話框關閉時，背景的 GraphQL 日期還原其實還沒跑完。

## 2. 核心設計決策 (Decisions)

### 2.1 `SyncProgressListener` 與全域進度狀態快取
在 `MyGitUtility` 中定義進度回呼介面與靜態進度快取：
```java
public interface SyncProgressListener {
    void onProgress(String statusText);
}
```
- 同時維護 `ConcurrentHashMap<String, String> sCloneProgressMap`，記錄每個 `sRemoteUrl` 目前的最新進度文字。
- 當 `MainActivity` 透過 `waitDialog` 進行同步時，直接透過 `SyncProgressListener` 在主執行緒即時更新 `waitDialog` 的 `R.id.tv_loading_msg`（節流每 150ms 最多刷新一次 UI，避免高頻率繪製卡頓）。
- 當透過 `CloneGitActivity` 在背景執行 `GIT_STATUS_CLONING` 時，`MainActivity` 與 `GitList` 可讀取 `MyGitUtility.getCloneProgress(sRemoteUrl)` 顯示於列表項目上。

### 2.2 JGit `CloneCommand` (`depth = 1`) 與 `ProgressMonitor` 實作
在 `MyGitUtility.cloneGit()` 中：
1. 使用 JGit 原生 `Git.cloneRepository()`：
   - `.setURI(sRemoteUrl)`
   - `.setDirectory(new File(sLocalDirectory))`
   - `.setDepth(1)`（保留淺層複製優勢，僅下載最新一層 Commit 物件，因此 `ProgressMonitor` 的 `totalWork` 即為 `depth=1` 實際需要下載的物件總數）
   - 若有指定分支（`sRemoteName` 非空），設定 `.setBranch(sRemoteName)`；若該分支不存在則自動退回遠端預設分支。
   - 若有帳密（`sUserName` / `sUserPassword`），設定 `UsernamePasswordCredentialsProvider`。
2. 自訂 `ProgressMonitor` 攔截 JGit 任務階段（`beginTask(String title, int totalWork)`、`update(int completed)`）：
   - **階段識別**：
     - `Receiving objects`（下載筆記資料）：計算 `percent = (current * 100) / totalWork`，並同時計算本地 `.git/objects/pack`（含 `incoming_*.pack` 暫存檔）目前已寫入之位元組數（轉換為 `MB`，例如 `12.4 MB`）。
       顯示格式：`正在下載筆記資料... 42% (210/500, 12.4 MB)`
     - `Resolving deltas`（解析資料差異）：
       顯示格式：`正在解析資料差異... 75% (90/120)`
     - `Checking out files` / `Updating references`（寫入筆記檔案）：
       顯示格式：`正在寫入筆記檔案... 80% (280/350)`
     - 其他前置階段（如 `Counting objects` / `Finding sources`）：
       顯示格式：`正在準備同步資料...`

### 2.3 彌補 `depth = 1` 的第 4 階段：檔案真實日期同步進度
當 JGit `cloneRepository()` 完成檔案檢出後：
1. 先執行快速的本地 `syncFileTimestampsWithGit(sLocalDirectory)`。
2. 針對 GitHub 儲存庫，**同步（Synchronous）**呼叫 `syncFileTimestampsFromGitHubApi(context, sLocalDirectory, sRemoteUrl, sUserPassword, progressListener)`（不再丟到 detached thread 導致對話框提早關閉）：
   - 掃描工作目錄下所有非 `.git` 檔案總數 `totalFiles`（例如 `350` 個檔案）。
   - 每批（100 個檔案）發送前與完成後，透過 `progressListener` 即時回報：
     `正在還原檔案真實日期... 57% (200/350)`
3. 待所有批次完成後，`cloneGit()` 才正式回傳 `true` 並關閉等待對話框，確保使用者進入筆記目錄時，所有檔案與真實日期皆已 100% 就緒。

### 2.4 移除 `MainActivity` 固定計時輪播
- 移除 `MainActivity` 中每 10 秒切換一次固定字串的 `startLoadingMessageCycle()` / `stopLoadingMessageCycle()`。
- 新增 `updateWaitDialogMessage(String msg)`，讓等待對話框直接呈現真實進度文字。
