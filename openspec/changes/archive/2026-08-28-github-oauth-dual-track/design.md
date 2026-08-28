## Context

目前 App 支援手動輸入 GitHub PAT 建立筆記。為提升使用者體驗，本設計引入 GitHub OAuth 2.0 Web Flow 一鍵授權，並與現有 PAT 流程整合成雙軌並存介面。相關背景與動機詳見 `proposal.md`。

## Goals / Non-Goals

**Goals:**
- 提供免手動複製 Token 的一鍵 OAuth 授權登入通道。
- 完整保留既有的 4 步驟手動 PAT 輸入指引與剪貼簿自動帶入功能，達到零破壞、雙保險。
- 支援 Android Deep Link 回調（`gitnotetaking://oauth/github`）並自動在背景換取 Token。
- 全面支援 5 國語系。

**Non-Goals:**
- 不建立雲端第三方代理伺服器（保持純 Client-Side 直連 GitHub，確保在各地區均能穩定存取）。
- 不更動原有 JGit Clone、GraphQL 時間同步與 SQLite 本地儲存核心邏輯。

## Decisions

### 1. 授權模式架構：純 Client-Side OAuth 2.0 Web Flow
- **做法**：
  - 發起授權：以 Chrome Custom Tabs 開啟 `https://github.com/login/oauth/authorize?client_id=Ov23licBa82hfK5H5sos&scope=repo,read:user&redirect_uri=gitnotetaking://oauth/github`。
  - 回調接收：在 `AndroidManifest.xml` 中為 `MainActivity` 配置 `launchMode="singleTop"` 與 Deep Link Intent Filter。
  - Token 交換：在背景 Thread 發送 HTTP POST 至 `https://github.com/login/oauth/access_token`，帶入 `client_id`、`client_secret` 與回傳之 `code`，取得 `access_token`。
- **替代方案評估**：
  - *Cloudflare Worker 代理*：雖可隱藏 Client Secret，但在中國大陸等地區存在網域阻斷風險，故決定採用純 App 直連 GitHub 官方伺服器。

### 2. UI 設計：雙軌並存整合對話框與配色風格
- **做法**：
  - 在原有的 GitHub 連線對話框最上方加入推薦區塊與「🐙 一鍵登入 GitHub 授權 (推薦)」按鈕，按鈕採用與 App 一致的 Material 主題色。
  - 下方保留原有的 EditText、4 步驟指引、`--` 分隔線與「前往產生 Token」按鈕。
- **好處**：既有功能無縫保留，新手與進階使用者各取所需。

### 3. 取消與異常流程處理
- **做法**：
  - 若使用者在瀏覽器授權頁面點擊取消或關閉網頁，返回 App 時自動關閉對話框並彈出 Toast 提示「已取消 GitHub 授權」。

### 4. Token 重複使用與切換機制
- **做法**：
  - 每次建立新筆記皆允許點選「一鍵授權」，利用瀏覽器已登入之便利性秒速授權，同時保有切換不同 GitHub 帳號的最高靈活性。
  - 取得之 Token 寫入本機 `RemoteGit` 資料庫作為該 Repo 後續 Push/Pull 的安全憑證。

## Risks / Trade-offs

- **[Risk] 使用者設備未安裝 Chrome 或瀏覽器不支援 Custom Tabs** → **Mitigation**: 使用標準 Intent 降級（Fallback）呼叫系統預設瀏覽器，並有下方 PAT 模式作為備案。
- **[Risk] Deep Link 喚醒時 Activity 被銷毀重建** → **Mitigation**: 在 `onCreate` 與 `onNewIntent` 同步處理 `Intent.getData()`，確保無論 Activity 是否重建皆能正確捕捉 `code`。
