# Design: GitHub 授權續期、401 攔截自動續推與多語系 FAQ (Technical Design)

## Context

請參閱 `proposal.md` 之問題背景。目前系統透過 `GitHubAuthManager` 發起 OAuth Web Flow，使用固定的 redirect URI `gitnotetaking://oauth/github` 且無附帶 `state` 參數。當 GitHub 回傳之 Access Token 因 8 小時過期失效時，JGit 於 `MyGitUtility.push` 遭遇 401 Unauthorized 僅回傳 `false`，UI 一律顯示「同步失敗」Toast。此外，修改畫面與下載清單缺乏重新綁定授權之通道。

## Goals / Non-Goals

**Goals:**
- 建立基於 RFC 6749 `state` 參數之精準回調分流機制（`create_new` vs `reauth_<repoId>`）。
- 於 `MyGitUtility` 建立 401 Unauthorized 錯誤標記（`GIT_STATUS_AUTH_FAILED = -5`）。
- 實作 401 授權失效專屬引導對話框，使用者一鍵授權回跳後「自動背景接續完成 Push」，保障本地修改零遺失。
- 於 `ModifyRemoteGitActivity` 提供「透過 GitHub 網頁重新授權」按鈕。
- 於「建立 GitHub 筆記」清單開放點選已下載筆記並更新授權。
- 產出 4 國語系完整的排錯與 OAuth App 設定 FAQ 文件。
- 撰寫單元測試覆蓋狀態判定與 State 解析邏輯。

**Non-Goals:**
- 不在此次變更修改 SQLite 資料庫結構或導入複雜的 Refresh Token 定時排程輪詢（藉由直接提供原地一鍵授權與引導管理員關閉 Token Expiration，以最精簡可靠之架構根治問題）。
- 不更動非 GitHub 類型（如本地 Local Git 或自建 GitLab/Gitea）之同步認證邏輯。

## Decisions

### Decision 1: OAuth State 格式採用 `reauth_<repoId>`
- **選擇**：以資料庫的主鍵 ID（整數）組合前綴，格式為 `reauth_12`。
- **理由**：
  - 避免將整段 URL (`https://github.com/...`) 放入 Query String 造成編碼（URL Encoding）遺失或特殊字元解析錯誤。
  - 主鍵 ID 為不可變數值，可直接透過 `RemoteGitDAO` 高效讀取與更新該筆資料。
- **替代方案評估**：
  - *將 Remote URL 放入 State*：易因斜線與特殊符號造成 Android Intent URI 解析異常，已被否決。
  - *全域暫存記憶體記錄目前重驗證的 ID*：若系統在瀏覽器授權期間因記憶體不足殺死背景 Activity，全域變數會遺失；而 State 隨 GitHub Deep Link 攜帶回傳，100% 保證存活。

### Decision 2: 401 狀態碼定義與非同步自動補推架構
- **選擇**：
  - 於 `MyGitUtility` 新增常數 `public static final int GIT_STATUS_AUTH_FAILED = -5;`。
  - 當 `GitUtil.checkRemoteRepository` 遭遇包含 `not authorized` 之 `TransportException` 時，將該 `RemoteGit` 之狀態設為 `GIT_STATUS_AUTH_FAILED`。
  - `MainActivity` 在同步結束後檢查狀態，若為 `GIT_STATUS_AUTH_FAILED`，彈出專屬對話框：「GitHub 授權已失效，是否立即重新登入？」。
  - 使用者點擊授權時，將待推之 Repo URL 暫存於 `sPendingPushRemoteUrl`，當 `onNewIntent` 成功換回 Token 並更新 DB 後，自動呼叫 `MyGitUtility.push` 進行背景補推。

### Decision 3: 修改畫面 (`ModifyRemoteGitActivity`) 增設網頁授權入口
- **選擇**：在介面中偵測目前 Remote URL 是否為 `github.com`。若為是，於密碼輸入框下方顯示按鈕【透過 GitHub 網頁重新登入】。點擊後以 `reauth_<id>` 發起 Web OAuth，完成後由 `MainActivity` 更新 DB，回到修改畫面時即時刷新欄位。

### Decision 4: 多語系 FAQ 獨立文件化
- **選擇**：建立獨立資料夾 `docs/`，並以 4 種語系維護 `FAQ.md`（繁中）、`FAQ_zh-CN.md`（簡中）、`FAQ_en.md`（英文）、`FAQ_ja.md`（日文）。
- **理由**：包含圖文級的 GitHub Developer Settings 操作步驟，適合獨立成正式說明文件，方便使用者與開發者直接以 Markdown 瀏覽。

## Risks / Trade-offs

- **[風險 1] 使用者在瀏覽器授權期間點擊取消或關閉分頁**
  - **緩解措施**：`MainActivity.onNewIntent` 偵測到使用者取消回調時，彈出 Toast 提示「已取消授權」，清除暫存之待推狀態，本地 Commit 完好保全，不破壞任何本地資料。
- **[風險 2] 在無網路環境下點擊重新授權**
  - **緩解措施**：啟動 CustomTabs 前先行呼叫 `MyApplication.isNetworkConnected()`，若無網路則彈出「無網路連線」Toast 阻擋，避免開啟無效空白網頁。
- **[風險 3] 儲存庫在重新授權回跳前被外部刪除**
  - **緩解措施**：解析 `reauth_<repoId>` 時，對 `RemoteGitDAO.getById` 進行空值防呆檢查，查無記錄時提示「筆記記錄不存在」並安全終止流程。
