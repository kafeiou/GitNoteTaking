## Why

目前使用者在建立 GitHub 筆記時，需要手動到 GitHub 網站產生 Personal Access Token (PAT) 並貼回 App。為了讓操作更加極速流暢，本變更在保留原有 PAT 完整指引的基礎上，引入「雙軌並存架構 (Dual-Track)」，在連線對話框中加入「🐙 一鍵登入 GitHub 授權」按鈕，使用者只需在瀏覽器中點擊授權，即可透過 Deep Link 自動完成登入並取得 note* 筆記清單，大幅降低使用門檻。

## What Changes

- **GitHub OAuth 2.0 授權登入支援**：整合 GitHub OAuth App（Client ID: `Ov23licBa82hfK5H5sos`），透過 Chrome Custom Tabs 啟動授權流程。
- **Deep Link 授權回調**：在 `AndroidManifest.xml` 中註冊 `gitnotetaking://oauth/github`，自動接收 GitHub 回傳的授權碼並於背景換取 Access Token。
- **雙軌並存對話框 (Dual-Track UI)**：
  - 上方提供顯目的「🐙 一鍵登入 GitHub 授權 (最快速推薦)」按鈕。
  - 下方完整保留既有的 PAT 輸入框、4 步驟手動產生指引、`--` 分隔線與剪貼簿自動辨識功能，確保特殊需求與網路環境下的最高容錯彈性。
- **多語系支援**：新增的按鈕與提示訊息完整支援 5 國語言（繁中台灣、繁中香港、簡中、日文、英文）。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `github-integration`: 擴充 GitHub 整合規格，將純 PAT 連線流程升級為「OAuth 一鍵授權 ➕ PAT 手動輸入」雙軌並存機制。

## Impact

- **UI / 畫面**：
  - `MainActivity.java`：修改 `startCreateGitHubNoteFlow()` 連線對話框介面，新增 OAuth 登入按鈕與回調處理方法。
- **設定檔與 Manifest**：
  - `app/src/main/AndroidManifest.xml`：在 `MainActivity` 加入 `gitnotetaking://oauth/github` 的 `<intent-filter>` 與 `singleTop` 啟動模式。
- **語系字串**：
  - `res/values*/strings.xml`：新增 5 國語系對應之 OAuth 按鈕與提示文字。
